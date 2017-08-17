import java.io.FileReader;
import java.util.ArrayList;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

public class coreServiceFramework extends DoLogging {
	static String tmpDir = "/tmp/";
	
	public String callService(String service, String method, ArrayList<functionParam> params){
		JSONObject reply = null;
        try {
        	JSONParser parser = new JSONParser();
        	JSONObject reqJson = (JSONObject)parser.parse(new FileReader(tmpDir+"request.json"));
    		String currSvc = (String)reqJson.get("svcName");
    		String currFunc = (String)reqJson.get("funcName");
    		String serviceReplyMq = (String)reqJson.get("serviceReplyMq");
    		reqJson.put("svcName", service);
    		reqJson.put("funcName", method);
    		if(reqJson.containsKey("serverId"))
    			reqJson.remove("serverId");
    		if(reqJson.containsKey("processId"))
    			reqJson.remove("processId");
    		JSONArray paramArr = new JSONArray();
    		for(int i=0; i<params.size(); i++){
    			functionParam p = params.get(i);
    			JSONObject param = new JSONObject();
    			String paramNum = "param" + ((Integer)(i+1)).toString();
    			param.put("name", paramNum);
    			param.put("type", p.type);
    			param.put("value", p.value.toString());
    			paramArr.add(param);
    		}
    		reqJson.put("params", paramArr);
    		String stack = (String)reqJson.get("stack");
    		stack = stack + "|" + currSvc + ":" + currFunc;
    		reqJson.put("stack", stack);
    		//System.out.println(reqJson.toJSONString());
    		this.connectToBroker();
    		this.send("svcQueue"+service, reqJson, serviceReplyMq);
			reply = this.listenAndFilter();
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        return reply.get("response").toString();
	}
	
	public functionParam encodeFunctionParams(String obj){
		functionParam out = new functionParam();
		out.type = obj.getClass().getSimpleName();
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(char obj){
		functionParam out = new functionParam();
		out.type = "char";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(int obj){
		functionParam out = new functionParam();
		out.type = "int";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(boolean obj){
		functionParam out = new functionParam();
		out.type = "boolean";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(byte obj){
		functionParam out = new functionParam();
		out.type = "byte";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(short obj){
		functionParam out = new functionParam();
		out.type = "short";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(long obj){
		functionParam out = new functionParam();
		out.type = "long";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(float obj){
		functionParam out = new functionParam();
		out.type = "float";
		out.value = obj;
		return out;
	}
	
	public functionParam encodeFunctionParams(double obj){
		functionParam out = new functionParam();
		out.type = "double";
		out.value = obj;
		return out;
	}
	
	public void doLogging(String logStr, String severity){
        try {
        	JSONParser parser = new JSONParser();
        	JSONObject reqJson = (JSONObject)parser.parse(new FileReader(tmpDir+"request.json"));
        	//System.out.println(reqJson);
        	doLogging(reqJson, "service", reqJson.get("serverId").toString(), 
        			reqJson.get("processId").toString(), (String)reqJson.get("funcName"), logStr, severity);
        }
        catch(Exception e){
        	e.printStackTrace();
        }  	
	}
	
	/*public static void main(String[] args) {
		coreServiceFramework obj = new coreServiceFramework();
		ArrayList<functionParam> list = new ArrayList<functionParam>();
		list.add(encodeFunctionParams("newVal"));
		list.add(encodeFunctionParams(123));
		System.out.println(obj.callService("Dummy", "func1", list));
		//obj.doLogging("Hello sexy khurana", "DUDE");
	}*/
}
