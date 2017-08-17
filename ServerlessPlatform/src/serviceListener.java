import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class serviceListener {
	public String mqName;
	public String serverId;
	public String processId;
	public String serviceReplyMq; 
	final static String nfsDir = "/var/serverless/";
	final static String classDir = "file:///var/serverless/Services/";
	final static String tmpDir = "/tmp/";
	final static String defaultExchange = "amq.direct";

	public void setVariable() {
		mqName = System.getenv("QUEUE_NAME");
		serverId = System.getenv("SERVER_ID");
		processId = System.getenv("PROCESS_ID");
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

	public static void main(String[] args) {
		serviceListener listnerObj = new serviceListener();
		listnerObj.setVariable();
		Properties brokerProps = getBrokerDetails();

		/*Get a ConnectionFactory object */
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost((String)brokerProps.get("ip"));
		factory.setPort(Integer.parseInt((String)brokerProps.get("port")));
		factory.setUsername((String)brokerProps.get("user"));
		factory.setPassword((String)brokerProps.get("password"));
		/* Create a connection */
		Connection connection = null;
		try {
			connection = factory.newConnection();
			/* Create a channel over that TCP/IP connection */
			Channel channel = connection.createChannel();
			/* create a MQ which callService function of coreSvcF/W will use */
			listnerObj.serviceReplyMq = "svcQueueDocker"+listnerObj.processId+listnerObj.serverId;
			channel.queueDeclare(listnerObj.serviceReplyMq,true,false,true,null);
			channel.queueBind(listnerObj.serviceReplyMq,defaultExchange,listnerObj.serviceReplyMq);
			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
					Map<String, Object> headers = properties.getHeaders();
					if (headers==null || (headers.containsKey("machine") && headers.get("machine").toString().equals(listnerObj.serverId))) {
						JSONParser parser = new JSONParser();
						try {
							//channel.basicAck(envelope.getDeliveryTag(), false);
							String replyMqName = properties.getReplyTo();
							String correlationId = properties.getCorrelationId();
							JSONObject json = (JSONObject) parser.parse(new String(body, "UTF-8"));
							JSONObject reply = listnerObj.doWork(json);
							if(json.containsKey("command") &&
									json.get("command").toString().equals("stopListening")){
								HashMap<String, Object> headersMap = new HashMap<String,Object>();
								headersMap.put("machine",listnerObj.serverId);
								AMQP.BasicProperties props = new AMQP.BasicProperties
									.Builder()
									.correlationId(correlationId)
									.headers(headersMap)
									.build();
								channel.basicAck(envelope.getDeliveryTag(), false);
								channel.basicPublish(defaultExchange, replyMqName, props, reply.toString().getBytes("UTF-8"));
								channel.queueDelete(listnerObj.serviceReplyMq);
								System.exit(0);
							}
							else {
								AMQP.BasicProperties props = new AMQP.BasicProperties
										.Builder()
										.correlationId(correlationId)
										.build();
								channel.basicPublish(defaultExchange, replyMqName, props, reply.toString().getBytes("UTF-8"));
								channel.basicAck(envelope.getDeliveryTag(), false);
							}
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					else
						channel.basicNack(envelope.getDeliveryTag(), false, true);
				}
			};
			channel.basicConsume(listnerObj.mqName, false, consumer);
			while(true) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException _ignore) {}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	JSONObject doWork(JSONObject json){						
		URL classUrl;
		/* reply MQ msg */
		JSONObject reply = new JSONObject();
		try {
			if(json.containsKey("command")){	//command is to stop listening
				if(json.get("command").toString().equals("stopListening"))
					reply.put("containerId", this.processId);
			}
			else {
				json.put("serverId", this.serverId);
				json.put("processId", this.processId);
				//need to save this json to a file in /tmp/
				File file = new File(tmpDir+"request.json");
				Writer fileHandle = new BufferedWriter(new FileWriter(file));
				JSONObject tmpJson = new JSONObject(json);
				tmpJson.put("serviceReplyMq", serviceReplyMq);
				fileHandle.write(tmpJson.toString());
				fileHandle.close();

				classUrl = new URL(classDir+(String) json.get("svcName")+"/src/");
				URL[] classUrls = { classUrl };
				URLClassLoader classLoader = new URLClassLoader(classUrls);
				Class<?> cls = classLoader.loadClass((String) json.get("svcName"));
				Method[] methods = cls.getMethods();
				boolean successfullExec = false;

				reply.put("usrId", (String)json.get("usrId"));
				reply.put("reqId", (String)json.get("reqId"));

				for (Method method : methods) {
					if (method.getName().equals((String) json.get("funcName"))) { // if method name matches
						Class<?>[] paramTypes = method.getParameterTypes();
						JSONArray param_array = (JSONArray) json.get("params");
						if (paramTypes.length != param_array.size()) // if number of params doesnt match
							continue;
						else { // number of params matched, now check if corresponding types match
							Object[] argsObject = new Object[paramTypes.length];
							boolean typeMisMatch = false;
							for (int i = 0; i < paramTypes.length; i++) {
								JSONObject arg = (JSONObject) param_array.get(i);
								String argName = (String) arg.get("name");
								int idx = Integer.parseInt(argName.substring(argName.lastIndexOf('m')+1));
								String type = (String) arg.get("type");
								String value = (String)arg.get("value");
								if(!paramTypes[idx-1].getSimpleName().equals(type)) {
									typeMisMatch = true;
									System.out.println("type didnt match for idx "+idx+" given type "+type+" actual "+paramTypes[idx-1].getName());
									break;	//types didnt match
								}
								if (type.equals("int"))
									argsObject[idx-1] = Integer.parseInt(value);
								else if (type.equals("float"))
									argsObject[idx-1] = Float.parseFloat(value);
								else if (type.equals("long"))
									argsObject[idx-1] = Long.parseLong(value);
								else if (type.equals("double"))
									argsObject[idx-1] = Double.parseDouble(value);
								else if (type.equals("short"))
									argsObject[idx-1] = Short.parseShort(value);
								else if (type.equals("byte"))
									argsObject[idx-1] = Byte.parseByte(value);
								else if (type.equals("boolean"))
									argsObject[idx-1] = Boolean.parseBoolean(value);
								else if (type.equals("char"))
									argsObject[idx-1] = (value).charAt(0);
								else
									argsObject[idx-1] = value.toString();
							}
							if(typeMisMatch) break;
							//call the function
							Object instance = cls.newInstance();
							Object returnValue = method.invoke(instance, argsObject);
							if(!method.getReturnType().getName().equals("void"))
								reply.put("response", returnValue.toString());
							else
								reply.put("response", "void");
							System.out.println(reply.toString());
							successfullExec = true;
						}
						classLoader.close();
						if(successfullExec)
							break;
					}
				}
				if(!successfullExec)
					reply.put("response", "error: No such function");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return reply;
	}
}