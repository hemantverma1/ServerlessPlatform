import org.json.simple.JSONObject;

public class RouterWorker implements Runnable {
	public String getSvcQueue(String svc){
		return "svcQueue"+svc;
	}
    @SuppressWarnings("unchecked")
	public void run() {
    	communicateWithMQ mqHandler = new communicateWithMQ();
    	mqHandler.connectToBroker();
    	String replyMq = mqHandler.createMq("svcQueueLoadRouterReply");
    	while(true){
    		JSONObject jsonReq = mqHandler.listenAndConsume("svcQueueLoadRouter");
    		String returnMq = mqHandler.getReplyMq();
    		if(jsonReq.containsKey("svcName")){
    			String svcName = (String)jsonReq.get("svcName");
    			mqHandler.send(getSvcQueue(svcName), jsonReq, replyMq);
    			mqHandler.send(returnMq, mqHandler.listenAndFilter(), null);
    		}
    		else{
    			JSONObject json = new JSONObject();
    			json.put("response", "error: no such service");
    			mqHandler.reply(json);
    		}    		
    	}
    }
}