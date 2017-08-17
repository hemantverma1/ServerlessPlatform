<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="org.json.simple.*"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
<head>
	<title>Welcome Admin</title>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<link rel="stylesheet"
	href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
	<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
	<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
	<style>
		/* Set height of the grid so .sidenav can be 100% (adjust as needed) */
		.row.content {
			height: 550px
		}

		/* Set gray background color and 100% height */
		.sidenav {
			background-color: #f1f1f1;
			height: 100%;
		}

		/* On small screens, set height to 'auto' for the grid */
		@media screen and (max-width: 767px) {
			.row.content {
				height: auto;
			}
		}
	</style>
	<style>
		table {
			font-family: arial, sans-serif;
			border-collapse: collapse;
			width: 100%;
		}

		td, th {
			border: 1px solid #dddddd;
			text-align: left;
			padding: 8px;
		}

		tr:nth-child(even) {
			background-color: #dddddd;
		}
	</style>
</head>

<body>

	<nav class="navbar navbar-inverse visible-xs">
		<div class="container-fluid">
			<div class="navbar-header">
				<button type="button" class="navbar-toggle" data-toggle="collapse"
				data-target="#myNavbar">
				<span class="icon-bar"></span> <span class="icon-bar"></span> <span
				class="icon-bar"></span>
			</button>

		</div>
		<div class="collapse navbar-collapse" id="myNavbar">
			<ul class="nav navbar-nav">

				<li class="active"><a href="dashboard.jsp">Home</a></li>
				<li class="active"><a href="monitor.jsp">Monitor</a></li>
				<li class="active"><a href="control.jsp">Control</a></li>
				<li class="active"><a href="team.html">About Us</a></li>
				<li class="active"><a href="admin.jsp"
					onclick="alert('Logged out successfully!!!');">Logout</a></li>

				</ul>
			</div>
		</div>
	</nav>

	<div class="container-fluid">
		<div class="row content">
			<div class="col-sm-3 sidenav hidden-xs">
				<h2>Administration</h2>
				<ul class="nav nav-pills nav-stacked">
					<li class="active"><a href="dashboard.jsp">Home</a></li>
					<li class="active"><a href="monitor.jsp">Monitor</a></li>
					<li class="active"><a href="control.jsp">Control</a></li>
					<li class="active"><a href="team.html">About Us</a></li>
					<li class="active"><a href="admin.jsp"
						onclick="alert('Logged out successfully!!!');">Logout</a></li>

					</ul>
					<br>
				</div>
				<br>

				<div class="col-sm-9">
					<div class="well">
						<h4>Monitoring Mode</h4>
						<form method="post" action="AdminServlet">
							<input type="submit" name="action" value="getStats" />
						</form>
					</div>

					<%
					if (request.getAttribute("jsonobj") != null) {
					JSONObject obj = (JSONObject) request.getAttribute("jsonobj");
					JSONArray jsonArray = (JSONArray)obj.get("machine");
					%>

					<div class="row">
						<div class="col-sm-6">
							<div class="well">
								<h2>Server Level Stats</h2>
								<table>
									<tr>
										<th>Machine_Name</th>
										<th>CPU_Usage</th>
										<th>RAM_USAGE</th>
										<th>#Processes</th>
									</tr>

									<%
									for (int i = 0; i < jsonArray.size(); i++) {
									JSONObject jo = (JSONObject) jsonArray.get(i);
									%>
									<tr>
										<td><%=jo.get("machine").toString()%></td>
										<td><%=jo.get("cpu").toString()%></td>
										<td><%=jo.get("ram").toString()%></td>
										<td><%=jo.get("processes").toString()%></td>
									</tr>


									<%
								}

								jsonArray = (JSONArray)obj.get("svc");
								%>
							</table>

						</div>
					</div>

					<div class="col-sm-6">
						<div class="well">
							<h2>Service Level Stats</h2>
							<table>
								<tr>
									<th>Service_Name</th>
									<th>#Service_instances</th>

								</tr>
								<%
								for (int i = 0; i < jsonArray.size(); i++) {
								JSONObject jo = (JSONObject) jsonArray.get(i);
								%>
								<tr>
									<td><%=jo.get("name").toString()%></td>
									<td><%=jo.get("instances").toString()%></td>

								</tr>

								<%
							}
							%>
						</table>
					</div>
				</div>
				<%
			}
			%>
		</div>
		<%
		if (request.getAttribute("MQobj") != null) {
						//ArrayList<String> obj = (ArrayList<String>)request.getAttribute("MQobj");
						HashMap<String, ArrayList<String>> obj = (HashMap<String, ArrayList<String>>) request
						.getAttribute("MQobj");
						%>
						<div class="row">
							<div class="col-sm-6">
								<div class="well">
									<h2>Rabbit MQ Stats</h2>
									<table>
										<tr>
											<th>List Of Queues Running</th>
											<th>Messages in Queue</th>
											<th>Consumers</th>
										</tr>
										<%
										for (HashMap.Entry<String, ArrayList<String>> entry : obj.entrySet()) {
										String key = entry.getKey();
										ArrayList<String> value = entry.getValue();
										%>
										<tr>
											<td><%=key%></td>
											<%
											for (String aString : value) {
											%>
											<td><%=aString%></td>
											<%
										}
										%>
									</tr>
									<%
								}
								%>
							</table>
						</div>
					</div>
				</div>
				<%
			}
			%>
		</div>
	</body>
	</html>
