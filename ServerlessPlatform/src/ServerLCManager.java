import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.*;

public class ServerLCManager extends DoLogging {
	final static String nfsDir = "/var/serverless/";
	final static String slaveMachineUser = "serverless";
	final static String slaveMachinePasswd = "serverless";
	static HashMap<String, String> allottedMachines = new HashMap<String, String>(); 
	static ArrayList<String> slaveMachines = new ArrayList<String>();
	static int idx = 0;
	String nfsIp;

	// Function to Read slaveMachines.config file
	public void readFile() throws IOException {

		File in = null;
		try {
			//Path of the file to be given of NFS
			String pathToFile = nfsDir+"slaveMachines.cfg"; 
			in = new File(pathToFile);
			Scanner fileScanner = new Scanner(in);
			while(fileScanner.hasNextLine())
				slaveMachines.add(fileScanner.nextLine());
			fileScanner.close();
			Properties properties = new Properties();
			InputStream inputStream = new FileInputStream(nfsDir + "nfsIP.cfg");
			properties.load(inputStream);
			nfsIp = (String)properties.get("nfsServerIp");
			inputStream.close();
		}
		catch (Exception e) {
			System.out.println("Exception occured while reading slaveMachines.config file");
			e.printStackTrace();
		}finally {
			System.out.println("slaveMachines.config file read");
		}
	}

	public static JSONObject constructJSON(String hostName,String ip){

		JSONObject toRet = new JSONObject();
		try {
			toRet.put("hostname", hostName);
			toRet.put("ip", ip);

		}catch (Exception e) {
			e.printStackTrace();
		}
		return toRet;
	}

	public JSONObject serverLCMgr(String cmd, JSONObject jo) throws IOException {
		
		JSONObject finalJSON = null;
		try {
			// GIVE MACHINE
			if(cmd.equals("giveMachine")){
				String hostIp = slaveMachines.get(idx%slaveMachines.size());
				idx++;
				System.out.println("\nCommand Recieved : giveMachine");
				System.out.println("Provisioning new VM");
				StringPair result = VmManager.startNewVM(hostIp, nfsIp);
				String selectedMachine = "null", ipSelected = "null";
				if(result!=null && !result.hostName.equals("null")){
					selectedMachine = result.hostName;
					ipSelected = result.ip;
				}

				if(!selectedMachine.equals("null")){
					java.lang.Thread.sleep(10000);
					//SSH to Start Agent. Boolean argument should be true.
					SSHStartAgent obj = new SSHStartAgent();
					String startAgentCmd = "echo "+slaveMachinePasswd+" > /tmp/.passwd && echo >>  /tmp/.passwd "
							+ "&& sudo -S mount "+nfsIp+":"+nfsDir+" "+nfsDir
							+" < /tmp/.passwd && sudo -S mount --make-rshared "+nfsDir+" < /tmp/.passwd && cd "+nfsDir;
					startAgentCmd += " && java -cp \"./lib/*:\" machineAgent " + selectedMachine;
					System.out.println(startAgentCmd);
					obj.executeFile(slaveMachineUser, slaveMachinePasswd, ipSelected, startAgentCmd, true);
					System.out.println("\nSelected Machine to Provision : "+selectedMachine+", IP : "+ipSelected+"\n");
					allottedMachines.put(selectedMachine, ipSelected);
				}
				else
					System.out.println("\nNo machine available");
				finalJSON = constructJSON(selectedMachine,ipSelected);
				return finalJSON;
			}

			//  STOP-MACHINE
			else if(cmd.equals("killMachine")){

				//{"command":"killMachine", "machineName":"trs20"}
				System.out.println("\nCommand Recieved : killMachine");
				System.out.println("Machine to kill : "+jo.getString("machineName")+"\n");

				String machineName = jo.getString("machineName");

				//SSH to kill Machine. Boolean argument should be false.
				SSHStartAgent obj = new SSHStartAgent();
				String killAgentCommand = "machineAgent " + machineName;
				obj.executeFile(slaveMachineUser, slaveMachinePasswd, allottedMachines.get(machineName), killAgentCommand, false);
				System.out.println("\nMachine killed : "+jo.getString("machineName")+", IP : "+allottedMachines.get(jo.getString("machineName"))+"\n");
				JSONObject toRet = new JSONObject();
				toRet.put("removeMachine", machineName);
				return toRet;				
				//ServerLCManager to Monitoring after killing a machine. Put in the message queue of Monitoring Service.
				//{
				//		“removeMachine”:”trs20”
				//}
			}

		} catch (Exception e) {
			System.out.println("Exception occured while GIVE/KILL Machine");
			e.printStackTrace();
		}
		// if error then return null
		return null;
	}

	public static void main(String[] args) throws Exception {
		ServerLCManager serverLCObj = new ServerLCManager();
		JSONObject request = null;
		serverLCObj.connectToBroker();
		JSONParser parser = new JSONParser();
		serverLCObj.readFile(); //reading slaveMachines.config.
		while(true){
			request = new JSONObject(serverLCObj.listenAndConsume("svcQueueServerLCMgr").toString());
			if(request.getString("command").equals("giveMachine")){
				JSONObject selectedMachine = serverLCObj.serverLCMgr("giveMachine", request);
				serverLCObj.reply((org.json.simple.JSONObject)parser.parse(selectedMachine.toString()));
			}
			else if(request.getString("command").equals("killMachine")){
				JSONObject toMonitoring = serverLCObj.serverLCMgr("killMachine", request);
				String replyMq = serverLCObj.getReplyMq();
				serverLCObj.send("svcQueueMonitoring", (org.json.simple.JSONObject)parser.parse(toMonitoring.toString()), "svcQueueServerLCMgrReply");
				serverLCObj.listenAndFilter();
				serverLCObj.send(replyMq, (org.json.simple.JSONObject) parser.parse("{\"result\":\"Machine killed\"}"), null);
			}
		}
	}
}
