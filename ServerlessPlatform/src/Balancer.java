import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;

public class Balancer extends DoLogging implements Runnable {
	GlobalData gd;
	public Balancer(GlobalData gd) {
		this.gd=gd;
		this.connectToBroker();
	}
	public String getSvcName(String mqName) {
		return mqName.substring(8);
	}
	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		HashMap<String, Date> lastInstanceStarted = new HashMap<>();
		HashMap<String, Integer> queueDepthZeroSince = new HashMap<>();
		int maxQueueDepth=10;
		int maxServiceInstances=100;
		int maxIdleCount=5;
		int minLifeTime=120;
		int maxCpuUsage=50;
		int maxMemUsage=50;
		Set<String> blockedSlaves = new HashSet<String>(); 
		Set<String> systemQueues = new HashSet<String>(Arrays.asList("svcQueueServiceLCMgrReply","svcQueueLogging",
				"svcQueueServiceLCMgr","svcQueueServerLCMgr","svcQueueMonitoring", "svcQueueDocker",
				"svcQueueLoadBalancer","svcQueueAgents","svcQueueLoadBalancerReply",
				"svcQueueAgentsReply","svcQueueLoadRouterReply","svcQueueServerLCMgrReply",
				"svcQueueLoadRouter","svcQueueAPIAdminReply","svcQueueAPIAdmin",
				"svcQueueServiceGateway","svcQueueServiceGatewayReply", "svcQueueAuthorizationService","svcQueueAuthenticationService"));
		try {
			Process p = Runtime.getRuntime().exec("hostname");
			BufferedReader Input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String hostname = Input.readLine();
			
			/* reading rules config file */
			Properties properties = new Properties();
			InputStream inputStream = new FileInputStream(nfsDir + "loadBalancer.cfg");
			properties.load(inputStream);
			maxQueueDepth=Integer.parseInt((String)properties.get("maxQueueDepth"));
			maxServiceInstances=Integer.parseInt((String)properties.get("maxServiceInstances"));
			maxIdleCount=Integer.parseInt((String)properties.get("maxIdleCount"));
			minLifeTime=Integer.parseInt((String)properties.get("minLifeTime"));
			maxCpuUsage=Integer.parseInt((String)properties.get("maxCpuUsage"));
			maxMemUsage=Integer.parseInt((String)properties.get("maxMemUsage"));
			boolean systemSvcsStarted = false;
			InitializeGlobalData updtGd = new InitializeGlobalData();
			while(true){
				GlobalData tmpGd = updtGd.initGlobalData();
				synchronized (gd) {
					gd.wait();
					gd.setMachineStatsHM(tmpGd.getMachineStatsHM());
					gd.setServiceStatHM(tmpGd.getServiceStatHM());
					gd.setTotalSvcInstance(tmpGd.getTotalSvcInstance());
					gd.notifyAll();
				}
				/* get all queue depths */
				HashMap<String, Long> queueDepth = getQueuedepths();
				Iterator<Entry<String,Long>> iter = queueDepth.entrySet().iterator();
				boolean systemQ;
				while (iter.hasNext()) {
					Entry<String,Long> e = iter.next();
					String queue = e.getKey();
					if(!queue.contains("Reply")){
						systemQ = false; 
						for (String pattern: systemQueues){
							if(Pattern.matches(pattern+".*", queue)){
								systemQ = true;
								iter.remove();
								break;
							}					
						}
						if(!systemQ){
							Long depth = e.getValue();
							if(depth==null)
								depth = 0L;
							if(queueDepthZeroSince.containsKey(queue) && depth==0)
								queueDepthZeroSince.put(queue, queueDepthZeroSince.get(queue)+1);
							else
								queueDepthZeroSince.put(queue, 0);
						}
					}
					else
						iter.remove();
				}
				System.out.println("queueDepthZeroSince: " + queueDepthZeroSince.toString());
				System.out.println("queueDepth: " + queueDepth.toString());

				/* applying rules */
				/* release the machine if possible */
				String mach1 = null, mach2 = null;
				String bestMachine = null; /*, worstMachine = null;*/
				for (Entry<String, MachineStats> e : gd.getMachineStatsHM().entrySet()){
					String machine = e.getKey();
					System.out.println("machine : "+machine);
					Double ram = e.getValue().getRam();
					Double cpu = e.getValue().getCpu();
					if(cpu>maxCpuUsage || ram>maxMemUsage)
						blockedSlaves.add(machine);
					else {
						if(blockedSlaves.contains(machine))
							blockedSlaves.remove(machine);
						if(bestMachine==null) {
							bestMachine = machine;
							System.out.println("best machine set to "+bestMachine);
						}
						if(cpu<maxCpuUsage/2 && ram<maxMemUsage/2){
							if(mach1==null)
								mach1 = machine;
							else
								mach2 = machine;
						}
						else if(cpu<gd.getMachineStatsHM().get(bestMachine).getCpu() && 
								ram<gd.getMachineStatsHM().get(bestMachine).getRam()) {
							bestMachine = machine;
							System.out.println("best machine updated to "+bestMachine);
						}
						/*if(worstMachine==null)
							worstMachine = machine;
						else if(cpu>gd.getMachineStatsHM().get(worstMachine).getCpu() && 
							ram>gd.getMachineStatsHM().get(worstMachine).getRam())
							worstMachine = machine;*/
						if(mach1!=null && mach2!=null){
							System.out.println("releasing "+mach1+" to "+mach2);
							/* release the machine */
							JSONObject releaseJson = new JSONObject();
							releaseJson.put("command", "releaseMachine");
							releaseJson.put("source", mach1);
							releaseJson.put("destination", mach2);
							send("svcQueueServiceLCMgr", releaseJson, "svcQueueLoadBalancerReply");
							listenAndFilter();
							if(bestMachine!=null && bestMachine.equals(mach1))
								bestMachine = mach2;
							/*if(worstMachine!=null && worstMachine.equals(mach1))
								worstMachine = null;*/
							if(blockedSlaves.contains(mach1))
								blockedSlaves.remove(mach1);
						}						
					}
				}
				System.out.println("Best mach "+bestMachine+" Blocked slaves: "+blockedSlaves.toString());

				/* start a machine if needed */
				if(bestMachine==null || blockedSlaves.contains(bestMachine)){
					/* start a machine */
					JSONObject giveMachineJson = new JSONObject();
					giveMachineJson.put("command", "giveMachine");
					send("svcQueueServerLCMgr", giveMachineJson, "svcQueueLoadBalancerReply");
					bestMachine = (String) listenAndFilter().get("hostname");
					if(bestMachine==null || bestMachine.equals("null"))
						/* all machines are consumed up, no new machines available */
						bestMachine = null;
				}
				System.out.println("bestMachine: "+bestMachine);
				doLogging(new JSONObject(),"LoadBalancer",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"Best machine",bestMachine,"INFO");

				for (Entry<String, Long> e : queueDepth.entrySet()){
					String queue = e.getKey();
					Long depth = e.getValue();
					String svc = getSvcName(queue);
					if(!queue.contains("Reply")){					
						System.out.println("Checking for "+svc);
						if((depth!=null && depth > maxQueueDepth &&  /* depth crossed the limit */
								gd.getTotalSvcInstance()!=null &&
								gd.getTotalSvcInstance().containsKey(svc) &&  /* total instances less than max limit*/
								gd.getTotalSvcInstance().get(svc)<maxServiceInstances) ||
								(depth!=null && depth>0 && 
								(gd.getTotalSvcInstance()!=null && !gd.getTotalSvcInstance().containsKey(svc) 
										|| gd.getTotalSvcInstance().get(svc)==0))){ /* got one request and no instance of that svc is running */
							/* start a new svc instance */
							if(bestMachine!=null){
								System.out.println("Starting instance for "+svc);
								doLogging(new JSONObject(),"LoadBalancer",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"StartSvc","Starting instance for "+svc,"INFO");
								JSONObject startNewJson = new JSONObject();
								startNewJson.put("command", "runNew");
								startNewJson.put("svcName", svc);
								startNewJson.put("machine", bestMachine);
								send("svcQueueServiceLCMgr", startNewJson, "svcQueueLoadBalancerReply");
								listenAndFilter();
							}
							Calendar cal = Calendar.getInstance();
							lastInstanceStarted.put(svc, cal.getTime());
						}
					}
				}
				System.out.println("lastInstanceStarted: "+lastInstanceStarted.toString());

				/* for those svcs for which I must have an instance running always */
				if(!systemSvcsStarted){					
					BufferedReader br = new BufferedReader(new FileReader(nfsDir + "runAtleastOneInstance.cfg"));
					String line = null;
					while((line = br.readLine())!=null){
						line = line.trim();
						if(!line.startsWith("#") && bestMachine!=null 
								&& (!gd.getTotalSvcInstance().containsKey(line) 
										|| (gd.getTotalSvcInstance().containsKey(line) 
												&& gd.getTotalSvcInstance().get(line)==0))){
							System.out.println("Starting inst of system serv: "+line);
							JSONObject startNewJson = new JSONObject();
							startNewJson.put("command", "runNew");
							startNewJson.put("svcName", line);
							startNewJson.put("machine", bestMachine);
							send("svcQueueServiceLCMgr", startNewJson, "svcQueueLoadBalancerReply");
							listenAndFilter();
						}
					}
					br.close();
					systemSvcsStarted = true;
				}
				
				Iterator<Entry<String, Integer>> it = queueDepthZeroSince.entrySet().iterator();
				while(it.hasNext()){
					Entry<String, Integer> e = it.next();
					String svc = getSvcName(e.getKey());
					int count = e.getValue();
					long currentTime = Calendar.getInstance().getTime().getTime();
					System.out.println("TotalSvcinstance: "+gd.getTotalSvcInstance().toString());
					if(gd.getTotalSvcInstance().containsKey(svc) && gd.getTotalSvcInstance().get(svc) > 0){ /* if an instance of that service is running */
						if(count>maxIdleCount && lastInstanceStarted.containsKey(svc) && 	/* if depth zero since some seconds */
								(currentTime-lastInstanceStarted.get(svc).getTime())>minLifeTime*1000){ /* and has aged above a min age */
							/* kill an instance of the service */
							HashMap<String, Integer> machList = gd.getServiceStatHM().get(svc).getServiceList();
							String machineKill = null;
							Iterator<String> machs = machList.keySet().iterator();
							if(machs.hasNext()) {
								machineKill = machs.next();
								System.out.println("Killing instance of "+svc);
								doLogging(new JSONObject(),"LoadBalancer",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"KillSvc","Killing instance of "+svc,"INFO");
								JSONObject killSvc = new JSONObject();
								killSvc.put("command", "killOne");
								killSvc.put("svcName", svc);
								killSvc.put("machine", machineKill);
								send("svcQueueServiceLCMgr", killSvc, "svcQueueLoadBalancerReply");
								listenAndFilter();
								e.setValue(0);
							}
						}
					}
				}
				java.lang.Thread.sleep(1000); //sleep for 1 second
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
