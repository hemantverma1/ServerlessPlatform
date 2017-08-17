import org.json.simple.JSONObject;

public class DoLogging extends communicateWithMQ {
	final static String logMqName = "svcQueueLogging";
	public JSONObject writeInStack(JSONObject jsonObj, String process_type, String method){
		try{
		
			String s = (String) jsonObj.get("stack");
		
			if (method.length()>0) {
				s += "|" + process_type + ":" + method;
				
			}else{
				s += "|" + process_type;
			}
			jsonObj.put("stack", s);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return jsonObj;
	}

	public void doLogging(JSONObject jsonObj, String process_type, 
			String serverid, String processid, String method, String log, String severity){
		try{
			jsonObj.put("processid" , processid);
			jsonObj.put("process_type", process_type);
			jsonObj.put("serverid" , serverid);
			jsonObj.put("method" , method);
			jsonObj.put("log" , log);
			jsonObj.put("severity" , severity);			
			jsonObj = writeInStack(jsonObj , process_type , method);
			System.out.println(jsonObj.toString());
			if(channel==null || !channel.isOpen())
				connectToBroker();
			send(logMqName, jsonObj, null);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}