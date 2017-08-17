import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class communicateWithMQ{
	final static String defaultExchange = "amq.direct"; 
	final static String nfsDir = "/var/serverless/";
	String replyMqName;
	String ip;
	int port;
	String user;
	String password;
	Connection connection=null;
	Channel channel=null;

	public String getReplyMq(){
		return replyMqName;
	}

	public communicateWithMQ(){
		Properties brokerProps = getBrokerDetails();
		ip = (String)brokerProps.get("ip");
		port = Integer.parseInt((String)brokerProps.get("port"));
		user = (String)brokerProps.get("user");
		password = (String)brokerProps.get("password");
		/*superUser = (String)brokerProps.get("superUser");
		superUserPasswd = (String)brokerProps.get("superUserPasswd");*/
	}

	private static Properties getBrokerDetails(){
		Properties properties = new Properties();
		try{
			InputStream inputStream = new FileInputStream(nfsDir + "brokerDetails.cfg");
			properties.load(inputStream);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return properties;
	}

	public void connectToBroker(){
		try {
			/*Get a ConnectionFactory object */
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(ip);
			factory.setPort(port);
			factory.setUsername(user);
			factory.setPassword(password);
			/* Create a connection */
			connection = factory.newConnection();
			/* Create a channel over that TCP/IP connection */
			channel = connection.createChannel();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public void disconnect(){
		try{
			connection.close();	
			channel=null;
			connection=null;
		}
		catch(Exception e){}
	}

	/*creates a durable(lives MQ broker restart, exclusive(only one consumer), autoDelete(as soon as
	 * last consumer exits/disconnects, queue vanishes off)  
	 */
	public String createMq(String mqName){
		String retQ = null;
		try{
			boolean notDeclared = true;
			while(notDeclared){
				String uniqueId = UUID.randomUUID().toString();
				uniqueId = uniqueId.substring(uniqueId.lastIndexOf('-') + 1);
				retQ = mqName+uniqueId;
				try{
					channel.queueDeclare(retQ,true,true,true,null);
					notDeclared = false;
				}
				catch(IOException e){}
			}
			channel.queueBind(retQ,defaultExchange,retQ);
			return retQ;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public String createMqFixedName(String mqName){
		try {
			channel.queueDeclare(mqName,true,true,true,null);
			channel.queueBind(mqName,defaultExchange,mqName);
			return mqName;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}


	public int send(String mqName, JSONObject message, String replyMq){
		/*Used when you want to send a new msg to the specified MQ
		 *Side effect: correlation Id and replyMq name will be stored in 
		 * this object only
		 */
		try {
			if(channel==null)
				connectToBroker();
			String routingKey = mqName;
			AMQP.BasicProperties props = new AMQP.BasicProperties
					.Builder()
					.replyTo(replyMq)
					.build();
			channel.basicPublish(defaultExchange, routingKey, props, message.toString().getBytes("UTF-8"));
			replyMqName = replyMq;
		}
		catch(Exception e){
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public int reply(JSONObject message){ 
		/*Used when a request has already been listened 
		through this object and then if you want to revert for that  request*/
		try{
			String routingKey = replyMqName;
			AMQP.BasicProperties props = new AMQP.BasicProperties();
			channel.basicPublish(defaultExchange, routingKey, props, message.toString().getBytes("UTF-8"));
		}
		catch(Exception e){
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public int sendWithHeader(String mqName, JSONObject message, String replyMq, Map<String, Object> headersMap){
		/*Used when you want to send a new msg to the specified MQ with header-fields set in the msg headers
		 *Side effect: correlation Id and replyMq name will be stored in 
		 * this object only
		 */
		try {
			if(channel==null)
				connectToBroker();
			String routingKey = mqName;
			AMQP.BasicProperties props = new AMQP.BasicProperties
					.Builder()
					.replyTo(replyMq)
					.headers(headersMap)
					.build();
			channel.basicPublish(defaultExchange, routingKey, props, message.toString().getBytes("UTF-8"));
			replyMqName = replyMq;
		}
		catch(Exception e){
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public JSONObject listenAndConsume(String mqName){
		/*Used when a consumer simply wants to listen and consume a msg from
		 * the specified MQ. Side effects: correlation Id and replyMq name will be stored in 
		 * this object only
		 */
		JSONObject ret = null;
		try{
			if(channel==null)
				connectToBroker();
			JSONParser parser = new JSONParser();
			GetResponse body = null;
			while(true){
				body = channel.basicGet(mqName, true);
				if(body!=null)
					break;
			}
			replyMqName = body.getProps().getReplyTo();
			ret = (JSONObject)parser.parse(new String(body.getBody(), "UTF-8"));
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return ret;
	}

	public JSONObject listenAndFilter(){
		return listenAndConsume(replyMqName);
	}

	/*CAUTION: FOLLOWING code will create a queue if you try to 
	 * find queuedepth of a non-existing queue */
	public int queueDepth(String mqName){
		try{
			return channel.queueDeclarePassive(mqName).getMessageCount();
		}
		catch(Exception e){
			e.printStackTrace();
			return 0;
		}
	}

	public JSONArray getQueueDetails() {
		JSONArray retArr = null;
		try {
			/* http://127.0.0.1:15672/api/queues */
			URL url = new URL ("http://"+ip+":15672/api/queues");
			byte[] message = "admin:admin".getBytes(StandardCharsets.UTF_8);
			String encoding = Base64.getEncoder().encodeToString(message);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setRequestProperty  ("Authorization", "Basic " + encoding);
			InputStream content = (InputStream)connection.getInputStream();
			BufferedReader in = new BufferedReader (new InputStreamReader (content));
			String line;
			String output = "";
			while ((line = in.readLine()) != null)
				output += line;
			JSONParser parser = new JSONParser();
			retArr = (JSONArray) parser.parse(output);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return retArr;
	}

	public HashMap<String, Long> getQueuedepths(){
		JSONArray queues = getQueueDetails();
		HashMap<String, Long> hash = new HashMap<String, Long>();
		try{
			for(int i=0; i<queues.size(); i++)
				hash.put((String)((JSONObject)queues.get(i)).get("name"), (Long)((JSONObject)queues.get(i)).get("messages"));
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return hash;
	}
}