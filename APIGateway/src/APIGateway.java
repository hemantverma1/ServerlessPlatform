import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.*;

@WebServlet(urlPatterns={"/AdminServlet"})
public class APIGateway extends HttpServlet {
	private static String ldapadminuname = null;
	private static String ldapadminpassword = null;
	private static final long serialVersionUID = 1L;

	public APIGateway() {
		super();
	}
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = request.getParameter("action");
		if(action==null)
			action="default";
		System.out.println("action = "+action);
		if(action.equals("adminLogin"))
		{
			ldapadminuname = request.getParameter("u");
			ldapadminpassword = request.getParameter("p");
			System.out.println(ldapadminuname+" "+ ldapadminpassword);
			try {
				if(AuthenticationService.authenticateJndi(ldapadminuname, ldapadminpassword)) {
					request.setAttribute("temp", "failed");
					RequestDispatcher rd=request.getRequestDispatcher("dashboard.jsp");		
					rd.forward(request, response);
				}
				else {
					request.setAttribute("returned", "failed");
					RequestDispatcher rd=request.getRequestDispatcher("admin.jsp");		
					rd.forward(request, response);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(action.equals("AddUser")) {
			String userName = request.getParameter("username");
			String password = request.getParameter("password");

			if(AuthenticationService.addUser(userName,password)) {
				userName = userName+":AddedSuccesfully";
				System.out.println(userName);
				request.setAttribute("addedOrNot", userName);
			}
			else {
				userName = userName+"AlreadyExists";
				request.setAttribute("addedOrNot", userName);
			}
			RequestDispatcher rd=request.getRequestDispatcher("control.jsp");		
			rd.forward(request, response);
			System.out.println("added");
		}
		else if(action.equals("ListUsers")){
			ArrayList<String> resp = AuthenticationService.userList();
			request.setAttribute("UserList", resp);
			RequestDispatcher rd=request.getRequestDispatcher("control.jsp");		
			rd.forward(request, response);
			System.out.println("List Shown");
		}
		else if (action.equals("DeleteUser")) {
			String toDeleteUser = request.getParameter("toDelete");
			System.out.println(toDeleteUser);
			AuthenticationService.deleteUser(toDeleteUser);
			RequestDispatcher rd=request.getRequestDispatcher("control.jsp");		
			rd.forward(request, response);
			System.out.println("deleted");
		}
		else if(action.equals("getStats")) {
			JSONObject monitoringReq = new JSONObject();
			monitoringReq.put("giveMachineStats", "All");
			communicateWithMQ mqHandler = new communicateWithMQ();
			mqHandler.connectToBroker();
			String retQ = mqHandler.createMq("svcQueueAPIAdminReply");
			mqHandler.send("svcQueueMonitoring", monitoringReq, retQ);
			JSONObject monitoringStats = mqHandler.listenAndFilter();
			mqHandler.disconnect();
			//System.out.println(monitoringStats.toString());
			try {
				request.setAttribute("jsonobj", monitoringStats);
				HashMap<String, ArrayList<String>> tempArray  = new HashMap<String, ArrayList<String>>(); 				 
				JSONArray queuearray =  mqHandler.getQueueDetails();
				for (int i = 0; i < queuearray.size(); i++) {
					JSONObject jo = (JSONObject) queuearray.get(i);
					if(jo!=null){
						ArrayList<String> temp = new ArrayList<String>();
						if(jo.containsKey("messages"))
								temp.add(jo.get("messages").toString());
						if(jo.containsKey("consumers"))
								temp.add(jo.get("consumers").toString());
						if(jo.containsKey("name"))
						tempArray.put(jo.get("name").toString(), temp);
					}
				}
				request.setAttribute("MQobj", tempArray);
				RequestDispatcher rd=request.getRequestDispatcher("monitor.jsp");		
				rd.forward(request, response);
				System.out.println("monitored");
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (action.equals("useCaseLogin"))
		{
			try {
				String uname = request.getParameter("email").toString().trim();
				String password = request.getParameter("pwd").toString();
				JSONObject androidOBJ = new JSONObject();
				PrintWriter out = response.getWriter();
				if(new AuthenticationService().authenticateUser(uname, password).get("authSuccessful")
						.toString().equals("true")) {
					androidOBJ.put("resp", "1");
					out.println(androidOBJ.toString());
					System.out.println("Authenticated: Sent : " + androidOBJ.toString());
				}
				else {
					androidOBJ.put("resp", "0");
					out.println(androidOBJ.toString());
					System.out.println("ERROR: Sent : " + androidOBJ.toString());
				}
			}catch (Exception e) {
				e.printStackTrace();
			}

		}
		else if (action.equals("useCaseSignUp")) {
			try {
				String userName = request.getParameter("email");
				String password = request.getParameter("pwd");
				JSONObject androidOBJ = new JSONObject();
				PrintWriter out = response.getWriter();
				if(AuthenticationService.addUser(userName,password)) {
					androidOBJ.put("resp", "1");
					out.println(androidOBJ.toString());			
					System.out.println("Added: Sent : " + androidOBJ.toString());
				}
				else {
					androidOBJ.put("resp", "0");
					out.println(androidOBJ.toString());
					System.out.println("ERROR: Sent : " + androidOBJ.toString());
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		else
			response.getWriter().append("Invalid request");
	}
}
