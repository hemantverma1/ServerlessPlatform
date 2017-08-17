import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class machineAgent extends DoLogging {
	final static int MINPORT = 40000;
	final static int MAXPORT = 50000;
	final static String nfsDir = "/var/serverless/";
	String mqName;
	String replyMqName;
	String serverId;
	/* map to store svcName:[containerIds] mapping*/
	Map<String, ArrayList<String>> containerNames = new HashMap<String, ArrayList<String>>();
	/* map to store containerId:pair<ports> mapping */
	Map<String, pair> allottedPorts = new HashMap<String, pair>();
	HashSet<Integer> allPorts = new HashSet<Integer>(); //stores all available ports in the host

	public static String getMqName(String svcName){
		return "svcQueue"+svcName;
	}

	public static String getReplyMqName(String svcName){
		return "svcQueue"+svcName+"Reply";
	}


	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		machineAgent machineAgentObject = new machineAgent();
		machineAgentObject.serverId = args[0]; //TODO:change this to parse from environment variable
		machineAgentObject.connectToBroker();
		machineAgentObject.mqName = 
				machineAgentObject.createMqFixedName("svcQueueAgents"+machineAgentObject.serverId);
		machineAgentObject.replyMqName = 
				machineAgentObject.createMqFixedName("svcQueueAgents"+machineAgentObject.serverId+"Reply");
		for (int i = MINPORT; i < MAXPORT; i++)
			machineAgentObject.allPorts.add(i);
		machineAgentObject.sendStatsToMonitoring();
		JSONParser parser = new JSONParser();
		System.out.println("Started");
		while (true) {
			JSONObject request = machineAgentObject.listenAndConsume(machineAgentObject.mqName);
			System.out.println(request.toString());
			String command = (String) request.get("command");
			if (command.equals("runNew")) {
				String svcName = (String) request.get("svcName");
				String uniqueId = UUID.randomUUID().toString();
				uniqueId = uniqueId.substring(uniqueId.lastIndexOf('-') + 1);
				uniqueId = svcName + uniqueId;
				while (machineAgentObject.allottedPorts.containsKey(uniqueId)) {
					uniqueId = UUID.randomUUID().toString();
					uniqueId = uniqueId.substring(uniqueId.lastIndexOf('-') + 1);
					uniqueId = svcName + uniqueId;
				}
				int port1 = 3000, port2 = 3001;//mere intialization
				if (machineAgentObject.allPorts.size() > 2) {
					Iterator<Integer> iter = machineAgentObject.allPorts.iterator();
					port1 = iter.next();
					port2 = iter.next();
					machineAgentObject.allPorts.remove(port1);
					machineAgentObject.allPorts.remove(port2);
					pair ports = new pair();
					ports.first = port1;
					ports.second = port2;
					machineAgentObject.allottedPorts.put(uniqueId, ports);
				} else {
					System.out.println("No ports left on the machine to expose to containers");
					continue;
				}
				try {
					//String run = "docker run -d --volume "+nfsDir+":"+nfsDir+":rshared -p "+port1+":3000"+" -p "+port2+":3001"
					String run = "docker run -d --volume "+nfsDir+":"+nfsDir+":private -p "+port1+":3000"+" -p "+port2+":3001"
							+" -e QUEUE_NAME=\""+getMqName(svcName)+"\" -e SERVER_ID=\""+machineAgentObject.serverId+"\" -e PROCESS_ID=\""
							+uniqueId+"\" --name "+uniqueId+" servicelistenerdocker";
					System.out.println(run);
					@SuppressWarnings("unused")
					Process p = Runtime.getRuntime().exec(new String[]{ "bash", "-c", run });
					/*if(p.exitValue()!=0) {//didnt run successfully
						System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
						throw new Exception("Docker run failed");
					}*/					
					if (machineAgentObject.containerNames.containsKey(svcName))
						machineAgentObject.containerNames.get(svcName).add(uniqueId);
					else {
						ArrayList<String> tmp = new ArrayList<String>();
						tmp.add(uniqueId);
						machineAgentObject.containerNames.put(svcName, tmp);
					}

					/* printing hashes */
					System.out.println(machineAgentObject.containerNames.toString());
					System.out.println(machineAgentObject.allottedPorts.toString());
					machineAgentObject.reply((JSONObject) parser.parse("{\"result\":\"Started\"}"));

				} catch (Exception e) {
					e.printStackTrace();
					try {
						machineAgentObject.reply((JSONObject) parser.parse("{\"result\":\"Failed to start\"}"));
					} catch (ParseException e1) {e1.printStackTrace();}
				}
			}
			else if (command.equals("killOne")) {
				String replyMq = machineAgentObject.getReplyMq();
				String svcName = (String) request.get("svcName");
				System.out.println("Killing instance of svc "+svcName);
				System.out.println(machineAgentObject.containerNames.toString());
				if (machineAgentObject.containerNames.containsKey(svcName)) {					
					JSONObject stopListeningJson = new JSONObject();
					stopListeningJson.put("svcName", svcName);
					stopListeningJson.put("command", "stopListening");
					Map<String, Object> headersMap = new HashMap<String, Object>();
					headersMap.put("machine", machineAgentObject.serverId);	//put your only machineId in the header
					machineAgentObject.sendWithHeader(getMqName(svcName), stopListeningJson, machineAgentObject.replyMqName, headersMap);
					JSONObject container  = machineAgentObject.listenAndConsume(machineAgentObject.replyMqName);
					String containerId = (String)container.get("containerId");
					try {
						//String run = "docker kill "+containerId+" && docker rm "+containerId;
						String run = "docker rm "+containerId;
						System.out.println(run);
						@SuppressWarnings("unused")
						Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", run});
						/*if(p.exitValue()!=0) {//didnt run successfully
							System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
							throw new Exception("Docker kill failed for container "+containerId);
						}*/
						pair ports = machineAgentObject.allottedPorts.get(containerId);
						machineAgentObject.allottedPorts.remove(containerId);
						machineAgentObject.containerNames.get(svcName).remove(containerId);
						if(machineAgentObject.containerNames.get(svcName).isEmpty())
							machineAgentObject.containerNames.remove(svcName);
						machineAgentObject.allPorts.add(ports.first);
						machineAgentObject.allPorts.add(ports.second);
						machineAgentObject.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Killed\"}"), null);
					} catch (Exception e) {
						e.printStackTrace();
						try {
							machineAgentObject.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Failed to Kill\"}"), null);
						} catch (ParseException e1) {e1.printStackTrace();}
					}
				}
			} 
			else if (command.equals("killAll")) {
				String replyMq = machineAgentObject.getReplyMq();
				Set<String> allSvcs = machineAgentObject.containerNames.keySet();
				Iterator<String> svcIterator = allSvcs.iterator();
				try {
					while(svcIterator.hasNext()){
						String svcName = svcIterator.next(); //get one svc
						while(machineAgentObject.containerNames.get(svcName).size()>0){
							JSONObject stopListeningJson = new JSONObject();
							stopListeningJson.put("svcName", svcName);
							stopListeningJson.put("command", "stopListening");
							Map<String, Object> headersMap = new HashMap<String, Object>();
							headersMap.put("machine", machineAgentObject.serverId);	//put your only machineId in the header
							machineAgentObject.sendWithHeader(getMqName(svcName), stopListeningJson, machineAgentObject.replyMqName, headersMap);
							JSONObject container  = machineAgentObject.listenAndConsume(machineAgentObject.replyMqName);
							String containerId = (String)container.get("containerId");
							try {
								//String run = "docker kill "+containerId+" && docker rm "+containerId;
								String run = "docker rm "+containerId;
								System.out.println(run);
								@SuppressWarnings("unused")
								Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", run});
								/*if(p.exitValue()!=0) {//didnt run successfully
								System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
								throw new Exception("Docker kill failed for container "+containerId);
							}*/
								pair ports = machineAgentObject.allottedPorts.get(containerId);
								machineAgentObject.allottedPorts.remove(containerId);
								machineAgentObject.containerNames.get(svcName).remove(containerId);
								machineAgentObject.allPorts.add(ports.first);
								machineAgentObject.allPorts.add(ports.second);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						svcIterator.remove();	//one svc got removed
					}
					machineAgentObject.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Killed All\"}"), null);
				}
				catch(Exception e){
					e.printStackTrace();
					try {
						machineAgentObject.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Failed to Kill All\"}"), null);
					} catch (ParseException e1) {e1.printStackTrace();}
				}
			}
			else if (command.equals("die")) {
				break;
			}
			//call sendStatsToMonitoring here
			machineAgentObject.sendStatsToMonitoring();
		}
	}// end of main

	@SuppressWarnings("unchecked")
	public void sendStatsToMonitoring(){
		JSONObject statsJson = new JSONObject();
		statsJson.put("machine", serverId);
		JSONObject t1 = new JSONObject();
		t1.put("type", "svc");
		JSONArray svcInstances = new JSONArray();
		for (Map.Entry<String, ArrayList<String>> entry : containerNames.entrySet()) {
			JSONObject tmp = new JSONObject();
			tmp.put(entry.getKey(), entry.getValue().size());
			svcInstances.add(tmp);
		}
		t1.put("instances", svcInstances);
		JSONObject t2 = new JSONObject();
		t2.put("type", "machine");
		Process p;
		try {
			p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "ps aux | wc -l" });
			/*if(p.exitValue()!=0) {//didnt run successfully
				System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
				throw new Exception();
			}*/
			BufferedReader Input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			t2.put("processes", Long.parseLong(Input.readLine()));

			p = Runtime.getRuntime().exec(new String[] { "bash", "-c",
			"top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'" });
			/*if(p.exitValue()!=0) {//didnt run successfully
				System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
				throw new Exception();
			}*/
			Input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			t2.put("cpu", Double.parseDouble(Input.readLine()));

			p = Runtime.getRuntime()
					.exec(new String[] { "bash", "-c", "free | grep Mem | awk '{print $3/$2 * 100.0}'" });
			/*if(p.exitValue()!=0) {//didnt run successfully
				System.out.println(new BufferedReader(new InputStreamReader(p.getErrorStream())).readLine());
				throw new Exception();
			}*/
			Input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			t2.put("ram", Double.parseDouble(Input.readLine()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONArray stats = new JSONArray();
		stats.add(t1);
		stats.add(t2);
		statsJson.put("stats", stats);
		this.send("svcQueueMonitoring", statsJson, null);
	}
}
