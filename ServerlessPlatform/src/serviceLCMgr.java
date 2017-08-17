import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class serviceLCMgr extends DoLogging{
	private String hostname = null;

	public serviceLCMgr(){
		try {
			Process p = Runtime.getRuntime().exec("hostname");
			BufferedReader Input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			hostname = Input.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void startSvc(String svcName, String machine){	
		JSONObject json =  new JSONObject();
		json.put("command", "runNew");
		json.put("svcName", svcName);
		/*HashMap<String, Object> headersMap = new HashMap<String,Object>();
		headersMap.put("machine", machine);
		// sending object to Agent queue with hostname of the machine who should 
		// read the message in header.*/
		send("svcQueueAgents"+machine,json,"svcQueueServiceLCMgrReply");
		System.out.println("Req fwded to "+"svcQueueAgents"+machine);
		doLogging(json,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"startSvc",listenAndFilter().toString(),"INFO");
		System.out.println("Svc inst started");
	}

	/* {\"command\":\"killOne\", \"svcName\":\"SvcA\", \"machine\":\"kanishtha-Vostro-3558\"} */
	public void killSvc(JSONObject obj){
		/*HashMap<String, Object> headersMap = new HashMap<String,Object>();
		headersMap.put("machine",obj.get("machine"));*/
		if(send("svcQueueAgents"+obj.get("machine").toString(),obj,"svcQueueServiceLCMgrReply")==1)
			doLogging(obj,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killSvc",listenAndFilter().toString(),"INFO");
		else
			doLogging(obj,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killSvc","Failed","ERROR");

	}

	/* {\"command\":\"releaseMachine\", \"source\":\"hemantpc\", \"destination\":\"kanishtha-pc\"} 
	 * Migrate all services from source to destination and then kill the agent running on source
	 * */
	@SuppressWarnings("unchecked")
	public void releaseMachine(JSONObject request){	
		String source = (String) request.get("source");
		String destination = (String) request.get("destination");
		/* Request service list from Monitoring */
		JSONObject svcLC_to_Monitoring = new JSONObject();
		svcLC_to_Monitoring.put("killMachineStats", source);
		if(send("svcQueueMonitoring",svcLC_to_Monitoring,"svcQueueServiceLCMgrReply") == 1){
			JSONObject response = listenAndFilter();
			/* response contains array of svcs */
			JSONArray jArray = (JSONArray) response.get("serviceList");
			for (int i = 0;i < jArray.size();i++){
				JSONObject svc = (JSONObject) jArray.get(i);
				int instances = Integer.parseInt(svc.get("instances").toString());
				String svcName = (String)svc.get("name");
				for (int j=0; j<instances; j++)
					startSvc(svcName, destination);
			}
			/* kill all svcs running on that machine */
			JSONObject jsonToMcAgent = new JSONObject();
			jsonToMcAgent.put("command", "killAll");
			/*HashMap<String, Object> headersMap = new HashMap<String,Object>();
			headersMap.put("machine",source);*/
			if(send("svcQueueAgents"+source,jsonToMcAgent,"svcQueueServiceLCMgrReply")==1)
				doLogging(jsonToMcAgent,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killAll",listenAndFilter().toString(),"INFO");
			else
				doLogging(jsonToMcAgent,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killAll","Failed","ERROR");	
			/* kill the machine */
			JSONObject killMachineReq = new JSONObject();
			killMachineReq.put("command", "killMachine");
			killMachineReq.put("machineName", source);
			if(send("svcQueueServerLCMgr",killMachineReq,"svcQueueServiceLCMgrReply")==1)
				doLogging(killMachineReq,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killMachine",listenAndFilter().toString(),"INFO");
			else
				doLogging(killMachineReq,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"killMachine","Failed","ERROR");

		}else{
			/* Unsuccessful Request Response from monitoring */
			doLogging(request,"serviceLCMgr",hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"releaseMachine","Failed","ERROR");
		}

	}


	public static void main(String[] args) throws ParseException {
		serviceLCMgr svcLCObj =  new serviceLCMgr();
		JSONObject req = null;
		String cmd = null;
		svcLCObj.connectToBroker();
		JSONParser parser = new JSONParser();
		while(true){
			req = svcLCObj.listenAndConsume("svcQueueServiceLCMgr");
			String replyMq = svcLCObj.getReplyMq();
			System.out.println(req);
			cmd =(String)req.get("command");
			if(cmd.equals("runNew")){
				/* {\"command\":\"runNew\", \"svcName\":\"SvcA\", \"machine\":\"hemantpc\" */
				svcLCObj.startSvc((String)req.get("svcName"), (String)req.get("machine"));
				svcLCObj.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Started\"}"), null);
			}
			else if(cmd.equals("killOne")){
				svcLCObj.killSvc(req);
				svcLCObj.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Killed\"}"), null);
			}
			else if(cmd.equals("releaseMachine")){
				svcLCObj.releaseMachine(req);
				svcLCObj.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Released\"}"), null);
			}
			else{
				// Wrong Command
				svcLCObj.doLogging(req,"serviceLCMgr",svcLCObj.hostname,ManagementFactory.getRuntimeMXBean().getName().split("@")[0],"mainRequestListener","Failed","ERROR");
				svcLCObj.send(replyMq, (JSONObject) parser.parse("{\"result\":\"Unknown command\"}"), null);
			}
		}
	}
}
