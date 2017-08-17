import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * Servlet implementation class ServiceGateway
 */
@WebServlet("/service/*")
public class ServiceGateway extends HttpServlet {
	private static final long serialVersionUID = 1L;
	final static int timeOut = 10000;
	communicateWithMQ mqHandler;
	String replyMq;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ServiceGateway() {
		super();
		mqHandler =  new communicateWithMQ(); 
		mqHandler.connectToBroker();
		replyMq = mqHandler.createMq("svcQueueServiceGatewayReply");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		String svcName = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/')+1);
		JSONObject svcReq = null;
		JSONParser parser = new JSONParser();
		try {
			String usrReq = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
			System.out.println("Request Came: "+usrReq);
			svcReq = (JSONObject) parser.parse(usrReq);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		String authToken = null;
		System.out.println(svcReq.toString());
		String reqId = UUID.randomUUID().toString();
		if(svcReq.containsKey("command")){
			if(svcReq.get("command").toString().equals("login")){
				if(svcReq.containsKey("usrId") && svcReq.containsKey("passwd")){
					JSONObject authResponse = doAuthentication(svcReq, reqId);
					System.out.println(authResponse.toString());
					if(!authResponse.get("authSuccessful").toString().equals("true")){
						//reply un-authenticated user
						response.getWriter().append("Invalid user");
						return;
					}
					//fully authenticated user - notify authorization service
					authToken =  authResponse.get("token").toString(); 
					notifyAuthorization(svcReq, reqId, authToken);
					response.getWriter().append(authToken);
				}
				else{
					//reply error
					response.getWriter().append("Provide userId and passwd");
					return;
				}
			}
			else if(svcReq.get("command").toString().equals("logout")) {
				System.out.println("Logging out");
				authToken = svcReq.get("authToken").toString();
				doLogout(svcReq, reqId, authToken);
			}
			else{
				//error
				response.getWriter().append("Wrong command argument");
				return;
			}
		}
		else{
			if(!svcReq.containsKey("authToken")){
				if(svcReq.containsKey("usrId") && svcReq.containsKey("passwd")){
					JSONObject authResponse = doAuthentication(svcReq, reqId);
					System.out.println(authResponse.toString());
					if(!authResponse.get("authSuccessful").toString().equals("true")){
						//reply un-authenticated user
						response.getWriter().append("Invalid user");
						return;
					}
					//fully authenticated user - notify authorization service
					authToken =  authResponse.get("token").toString(); 
					notifyAuthorization(svcReq, reqId, authToken);
				}
				else{
					//reply error
					response.getWriter().append("Provide userId and passwd");
					return;
				}
				svcReq.remove("passwd");
			}
			else
				authToken = svcReq.get("authToken").toString();
			String authzationResponse = doAuthorization(svcReq, reqId, authToken, svcName);
			if(!authzationResponse.equals("true")){
				response.getWriter().append("Unauthorized access");
				return;
			}
			svcReq.put("svcName", svcName);
			svcReq.put("timeOut", timeOut);
			svcReq.put("reqId", reqId);
			svcReq.put("stack", "API");
			System.out.println(svcReq.toString());
			mqHandler.send("svcQueueLoadRouter", svcReq, replyMq);
			JSONObject reqResponse = mqHandler.listenAndFilter();
			reqResponse.put("authToken", authToken);
			reqResponse.remove("reqId");
			response.getWriter().append(reqResponse.toString());
		}
	}
	
	@SuppressWarnings("unchecked")
	JSONObject doAuthentication(JSONObject svcReq, String reqId){
		JSONObject reply = null;
		JSONObject authReq = new JSONObject();
		communicateWithMQ mqHandler = new communicateWithMQ();
		mqHandler.connectToBroker();
		String replyMq = mqHandler.createMq("svcQueueServiceGatewayAuthnReply");

		authReq.put("svcName", "AuthenticationService");
		authReq.put("funcName", "authenticateUser");
		authReq.put("usrId", svcReq.get("usrId").toString());
		authReq.put("reqId", reqId);

		JSONArray params = new JSONArray();
		JSONObject tmp = new JSONObject();
		tmp.put("name", "param1");	//usrId
		tmp.put("type", "String");
		tmp.put("value", svcReq.get("usrId").toString());
		params.add(tmp);
		JSONObject tmp1 = new JSONObject();
		tmp1.put("name", "param2");	//passwd
		tmp1.put("type", "String");
		tmp1.put("value", svcReq.get("passwd").toString());
		params.add(tmp1);
		authReq.put("params", params);
		System.out.println(authReq);
		mqHandler.send("svcQueueAuthenticationService", authReq, replyMq);
		JSONParser parser = new JSONParser();
		try {
			String str = mqHandler.listenAndFilter().get("response").toString();
			mqHandler.disconnect();
			System.out.println("Response from Auth svc "+str);
			reply = (JSONObject) parser.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return reply;
	}
	
	@SuppressWarnings("unchecked")
	String doAuthorization(JSONObject svcReq, 
			String reqId, String authToken, String svcName){
		//do Authorization
		JSONObject authorzationReq = new JSONObject();
		authorzationReq.put("svcName", "AuthorizationService");
		authorzationReq.put("funcName", "authorizeUser");
		authorzationReq.put("usrId", svcReq.get("usrId").toString());
		authorzationReq.put("reqId", reqId);
		JSONArray params = new JSONArray();
		JSONObject tmp = new JSONObject();
		tmp.put("name", "param1");	//service
		tmp.put("type", "String");
		tmp.put("value", svcName);
		params.add(tmp);
		JSONObject tmp1 = new JSONObject();
		tmp1.put("name", "param2");	//token
		tmp1.put("type", "String");
		tmp1.put("value", authToken);
		params.add(tmp1);
		authorzationReq.put("params", params);
		System.out.println(authorzationReq.toString());
		communicateWithMQ mqHandler = new communicateWithMQ();
		mqHandler.connectToBroker();
		String replyMq = mqHandler.createMq("svcQueueServiceGatewayAuthrReply");
		mqHandler.send("svcQueueAuthorizationService", authorzationReq, replyMq);
		String reply = mqHandler.listenAndFilter().get("response").toString();
		mqHandler.disconnect();
		System.out.println(reply);
		return reply;
	}
	
	@SuppressWarnings("unchecked")
	void doLogout(JSONObject svcReq, 
		String reqId, String authToken){
		JSONObject logoutReq = new JSONObject();
		logoutReq.put("svcName", "AuthorizationService");
		logoutReq.put("funcName", "deleteTokenEntry");
		logoutReq.put("usrId", svcReq.get("usrId").toString());
		logoutReq.put("reqId", reqId);
		JSONArray params = new JSONArray();
		JSONObject tmp = new JSONObject();
		tmp.put("name", "param1");	//service
		tmp.put("type", "String");
		tmp.put("value", authToken);
		params.add(tmp);
		logoutReq.put("params", params);
		System.out.println(logoutReq.toString());
		communicateWithMQ mqHandler = new communicateWithMQ();
		mqHandler.connectToBroker();
		String replyMq = mqHandler.createMq("svcQueueServiceGatewayAuthLReply");
		mqHandler.send("svcQueueAuthorizationService", logoutReq, replyMq);
		mqHandler.listenAndFilter();
		mqHandler.disconnect();
	}
	
	@SuppressWarnings("unchecked")
	void notifyAuthorization(JSONObject svcReq, String reqId, String authToken){
		JSONObject notifyAuthorization = new JSONObject();
		notifyAuthorization.put("svcName", "AuthorizationService");
		notifyAuthorization.put("funcName", "saveTokenDetails");
		notifyAuthorization.put("usrId", svcReq.get("usrId").toString());
		notifyAuthorization.put("reqId", reqId);		
		JSONArray params = new JSONArray();
		JSONObject tmp = new JSONObject();
		tmp.put("name", "param1");	//token
		tmp.put("type", "String");
		tmp.put("value", authToken);
		params.add(tmp);
		JSONObject tmp1 = new JSONObject();
		tmp1.put("name", "param2");	//usrId
		tmp1.put("type", "String");
		tmp1.put("value", svcReq.get("usrId").toString());
		params.add(tmp1);
		JSONObject tmp2 = new JSONObject();
		tmp2.put("name", "param3");	//timestamp
		tmp2.put("type", "String");
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
		tmp2.put("value", timeStamp);
		params.add(tmp2);
		notifyAuthorization.put("params", params);
		communicateWithMQ mqHandler = new communicateWithMQ();
		mqHandler.connectToBroker();
		String replyMq = mqHandler.createMq("svcQueueServiceGatewayAuthNRReply");
		mqHandler.send("svcQueueAuthorizationService", notifyAuthorization, replyMq);
		mqHandler.listenAndFilter(); //wait till your request gets serviced off
		mqHandler.disconnect();
	}
}