import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class Monitoring extends DoLogging {
	HashMap<String, MachineStats> machineStats=new HashMap<>();
	HashMap<String, ServiceStats> serviceStats=new HashMap<>();
	public void updateStats(JSONObject jSon) {
		JSONObject tempJO=new JSONObject();
		System.out.println(jSon.toString());
		String machine = (String)jSon.get("machine");
		Set<String> oldSvcsRunning = null;
		if(machineStats.containsKey(machine))
			oldSvcsRunning = machineStats.get(machine).getInstances().keySet();
		try {
			MachineStats md=new MachineStats();
			JSONArray jsonArrayMachine=(JSONArray)jSon.get("stats");
			md.setMachine((String)jSon.get("machine"));
			md.setCpu((Double)((JSONObject)jsonArrayMachine.get(1)).get("cpu"));
			md.setRam((Double)((JSONObject)((JSONObject)jsonArrayMachine.get(1))).get("ram"));
			md.setProcess(((Long)((JSONObject)(JSONObject)jsonArrayMachine.get(1)).get("processes")).intValue());
			HashMap<String, Integer> mapOfSvcListForMchn = new HashMap<>();
			JSONArray JAOfSvcListForMchn = (JSONArray)((JSONObject)jsonArrayMachine.get(0)).get("instances");
			int len = 0;
			if (JAOfSvcListForMchn!=null)
				len=JAOfSvcListForMchn.size();
			for (int i = 0; i < len; i++) {
				tempJO=(JSONObject)JAOfSvcListForMchn.get(i);
				Set<String> s =tempJO.keySet();
				String temp=s.iterator().next();
				mapOfSvcListForMchn.put(temp,Integer.parseInt(tempJO.get(temp).toString()));
			}
			md.setInstances(mapOfSvcListForMchn);
			machineStats.put((String)jSon.get("machine"),md);
			for (int i = 0; i < len	; i++) {
				tempJO=(JSONObject)JAOfSvcListForMchn.get(i);
				Set<String> s =tempJO.keySet();
				String temp=s.iterator().next(); //svcName
				ServiceStats ss;
				if (serviceStats.containsKey(temp)) {
					ss=serviceStats.get(temp);
				}else {
					ss=new ServiceStats();
				}

				if (ss.getServiceName()=="") {
					ss.setServiceName(temp);
					serviceStats.put(temp, ss);
				}

				HashMap<String, Integer>tempHM;
				tempHM=serviceStats.get(temp).getServiceList();
				if (tempHM==null) {
					tempHM=new HashMap<>();
				}
				tempHM.put((String)jSon.get("machine"),Integer.parseInt(tempJO.get(temp).toString()));
				ss.setServiceList(tempHM);
				serviceStats.put(temp,ss);
			}
			/* iterate over complete oldSvcList*/
			if(oldSvcsRunning!=null){
				for(String oldSvc : oldSvcsRunning){
					if(!machineStats.get(machine).getInstances().containsKey(oldSvc)){	//svc vansihed from machine
						if(serviceStats.get(oldSvc).getServiceList().containsKey(machine))
							serviceStats.get(oldSvc).getServiceList().remove(machine);
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void removeMachine(JSONObject jSon) {
		String machineName=(String)jSon.get("removeMachine");
		if(machineStats.containsKey(machineName)){
			HashMap<String, Integer> svcInstances=machineStats.get(machineName).getInstances();
			ArrayList<String>svcList=new ArrayList<>();
			for(Entry<String, Integer> e : svcInstances.entrySet()) {
				if(serviceStats.containsKey(e.getKey())) {
					ServiceStats ss=serviceStats.get(e.getKey());
					HashMap<String, Integer>tempHM=ss.getServiceList();
					if(tempHM.containsKey(machineName)) {
						tempHM.remove(machineName);
						if (tempHM.size()==0) {
							svcList.add(e.getKey());
						}else {
							ss.setServiceList(tempHM);						
							serviceStats.put(e.getKey(), ss);
						}
					}
				}
			}
			for(Iterator<String> it=svcList.iterator();it.hasNext();)
				serviceStats.remove(it.next());
			machineStats.remove(machineName);			
		}
	}
	public JSONObject getCompleteStats() {
		JSONObject jSon=new JSONObject();
		JSONArray jsonArray=new JSONArray();
		JSONObject tempJO;
		MachineStats tempMS;
		ServiceStats tempSS;
		for(Entry<String, MachineStats> e:machineStats.entrySet()) {
			tempJO=new JSONObject();
			tempMS=e.getValue();
			tempJO.put("machine",tempMS.getMachine() );
			tempJO.put("cpu", tempMS.getCpu());
			tempJO.put("ram", tempMS.getRam());
			tempJO.put("processes", tempMS.getProcess());
			jsonArray.add(tempJO);
		}
		jSon.put("machine", jsonArray);
		jsonArray = new JSONArray();
		tempJO = new JSONObject();
		for(Entry<String, ServiceStats> e : serviceStats.entrySet()){
			tempJO = new JSONObject();
			tempSS=e.getValue();
			int total=0;
			HashMap<String, Integer> tempHM=tempSS.getServiceList();
			for(Entry<String, Integer> e1 : tempHM.entrySet()) {
				total+=e1.getValue();
			}
			tempJO.put("name", e.getKey());
			tempJO.put("instances", total);
			jsonArray.add(tempJO);
			total=0;
		}
		jSon.put("svc", jsonArray);
		System.out.println(jSon.toString());
		return jSon;
	}
	public JSONObject svcList(String host){  //data to be sent to service LC Mgr
		JSONArray jsonArray =new JSONArray();
		JSONObject jSon =new JSONObject();
		if(machineStats.containsKey(host)){
			MachineStats ms=machineStats.get(host);
			HashMap<String, Integer> hm=ms.getInstances();
			for(Entry<String, Integer> e: hm.entrySet()) {
				JSONObject tmp =new JSONObject();
				tmp.put("name", e.getKey());
				tmp.put("instances", e.getValue().intValue());
				jsonArray.add(tmp);
			}
		}
		jSon.put("serviceList", jsonArray);
		System.out.println(jSon.toString());
		return jSon;
	}

	public JSONObject getAllMachineLevelStats(){
		JSONObject jSon=new JSONObject();
		JSONArray jsonArray =new JSONArray();
		JSONObject tempJO;
		JSONObject svcJO;
		JSONArray svcLstJA;
		MachineStats tempMS;
		for(Entry<String, MachineStats> e:machineStats.entrySet()) {
			tempJO=new JSONObject(); 
			tempMS=e.getValue();
			tempJO.put("machine",tempMS.getMachine() );
			tempJO.put("cpu", tempMS.getCpu());
			tempJO.put("ram", tempMS.getRam());
			tempJO.put("processes", tempMS.getProcess());
			svcLstJA=new JSONArray();
			for(Entry<String, Integer> e1:tempMS.getInstances().entrySet()) {
				svcJO=new JSONObject();
				svcJO.put("name",e1.getKey());
				svcJO.put("instances", e1.getValue());
				svcLstJA.add(svcJO);
			}
			tempJO.put("svc", svcLstJA);
			jsonArray.add(tempJO);
		}
		jSon.put("machineStats",jsonArray);
		System.out.println(jSon.toString());
		return jSon;
	}

	public JSONObject getAllSvcLevelStats() {
		JSONObject jSon=new JSONObject();
		JSONArray jsonArray =new JSONArray();
		JSONObject tempJO;
		JSONObject machineJO;
		JSONArray machineLstJA;
		ServiceStats tempSS;
		Integer sum = 0;
		for(Entry<String, ServiceStats> e:serviceStats.entrySet()) {
			tempJO=new JSONObject();  
			tempSS=e.getValue();
			tempJO.put("name",tempSS.getServiceName());
			machineLstJA=new JSONArray();
			for(Entry<String, Integer> e1:tempSS.getServiceList().entrySet()) {
				machineJO=new JSONObject();
				machineJO.put("name", e1.getKey());
				machineJO.put("instances", e1.getValue());
				machineLstJA.add(machineJO);
				sum+=e1.getValue();
			}
			tempJO.put("instances",sum);
			sum = 0;
			tempJO.put("machines", machineLstJA);
			jsonArray.add(tempJO);
		}
		jSon.put("svcStats",jsonArray);
		System.out.println(jSon.toString());
		return jSon;
	}

	public static void main(String[] args) {
		Monitoring monitoringObj = new Monitoring();
		JSONObject req = null;
		JSONParser parser = new JSONParser();
		while(true){
			try {
				req = monitoringObj.listenAndConsume("svcQueueMonitoring");
				if(req.containsKey("stats"))	//Agent sent stats to Monitoring
					monitoringObj.updateStats(req);
				else if(req.containsKey("killMachineStats"))//ServiceLCMgr asked for all svc stats corresponding to a machine
					monitoringObj.reply(monitoringObj.svcList((String)req.get("killMachineStats")));
				else if(req.containsKey("removeMachine")){	//ServerLCMgr sent to Monitoring to clear stats
					monitoringObj.removeMachine(req);
					monitoringObj.reply((JSONObject) parser.parse("{\"result\":\"Machine cleared\"}"));
				}
				else if(req.containsKey("giveMachineStats"))	//ServerLCMgr/any other asked for stats (all stats)
					monitoringObj.reply(monitoringObj.getCompleteStats());
				else if(req.containsKey("getAllMachineLevelStats"))	//LB asked for complete m/c level stats
					monitoringObj.reply(monitoringObj.getAllMachineLevelStats());
				else if(req.containsKey("getAllSvcLevelStats"))	//LB asked for complete svc level stats
					monitoringObj.reply(monitoringObj.getAllSvcLevelStats());
				else
					monitoringObj.doLogging(req,"Monitoring",System.getenv("HOSTNAME"),ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"mainRequestListener","Wrong request type","ERROR");

			} catch (Exception e) {
				e.printStackTrace();
				monitoringObj.doLogging(req,"Monitoring",System.getenv("HOSTNAME"),ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"mainRequestListener","Exception : "+e.getMessage(),"ERROR");
			}
		}
	}
} 
