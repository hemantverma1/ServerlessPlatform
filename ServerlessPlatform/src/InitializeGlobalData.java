import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class InitializeGlobalData extends DoLogging{
	InitializeGlobalData(){
		//if(this.connection==null)
			this.connectToBroker();
	}
	
	@SuppressWarnings("unchecked")
	public GlobalData initGlobalData() {
		/* get machine level stats */
		JSONObject machStatsReq = new JSONObject();
		machStatsReq.put("getAllMachineLevelStats", "ALL");
		send("svcQueueMonitoring", machStatsReq, "svcQueueLoadBalancerReply");
		JSONObject jSonMachLevel = listenAndFilter();
		/* get service level stats */
		JSONObject svcStatsReq = new JSONObject();
		svcStatsReq.put("getAllSvcLevelStats", "ALL");
		send("svcQueueMonitoring", svcStatsReq, "svcQueueLoadBalancerReply");
		JSONObject jSonSvcLevel = listenAndFilter();
		
		//main outcome of below code is this map
		HashMap<String, MachineStats>machineStatsHM=new HashMap<>();
		
		MachineStats tempMS;
		JSONArray jsonArrMchn=(JSONArray)jSonMachLevel.get("machineStats");
		if (jsonArrMchn!=null) {
			for (int i = 0; i<jsonArrMchn.size(); i++) {
				tempMS=new MachineStats();
				tempMS.setMachine((String)((JSONObject)jsonArrMchn.get(i)).get("machine"));
				tempMS.setCpu((Double)((JSONObject)jsonArrMchn.get(i)).get("cpu"));
				tempMS.setRam((Double)((JSONObject)jsonArrMchn.get(i)).get("ram"));
				tempMS.setProcess(((Long)((JSONObject)jsonArrMchn.get(i)).get("processes")).intValue());
				JSONArray jsonArrSvc = (JSONArray)((JSONObject)jsonArrMchn.get(i)).get("svc");
				HashMap<String, Integer> svcInstMap = new HashMap<String, Integer>();
				for(int j=0; j<jsonArrSvc.size(); j++){
					String svc = (String)((JSONObject)jsonArrSvc.get(j)).get("name");
					Integer inst = ((Long)((JSONObject)jsonArrSvc.get(j)).get("instances")).intValue();
					svcInstMap.put(svc, inst);
				}
				tempMS.setInstances(svcInstMap);
				machineStatsHM.put(tempMS.getMachine(), tempMS);
			}
			
		}
		//main outcome of below code are these maps
		HashMap<String, ServiceStats>svcStatsHM=new HashMap<>();
		HashMap<String, Integer> totalSvcInstance = new HashMap<String, Integer>();
		
		ServiceStats tempSS;
		JSONArray jsonArrSvc=(JSONArray)jSonSvcLevel.get("svcStats");
		if (jsonArrSvc!=null) {			
			for (int i = 0; i<jsonArrSvc.size(); i++) {
				tempSS = new ServiceStats();
				JSONObject svc = (JSONObject)jsonArrSvc.get(i);
				tempSS.setServiceName((String)svc.get("name"));
				totalSvcInstance.put((String)svc.get("name"), ((Long)svc.get("instances")).intValue());
				HashMap<String, Integer> machList = new HashMap<String, Integer>();
				JSONArray machines = (JSONArray)svc.get("machines");
				for(int j=0; j<machines.size(); j++){
					String machName = (String)((JSONObject)machines.get(j)).get("name");
					Integer instances = ((Long)((JSONObject)machines.get(j)).get("instances")).intValue();
					machList.put(machName, instances);
				}
				tempSS.setServiceList(machList);
				svcStatsHM.put(tempSS.getServiceName(), tempSS);		
			}
		}
		GlobalData gd = new GlobalData(machineStatsHM, svcStatsHM, totalSvcInstance);
		return gd;
	}
}
