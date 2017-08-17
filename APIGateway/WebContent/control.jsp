<%@page import="java.util.ArrayList"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
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
</head>

<body>

	<!-- <form id="my_form" method="post" action="AdminServlet">
      <input type="hidden" name="action" value="Dashboard"/>
</form>
 -->

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
					<h4>LDAP Control Mode</h4>

				</div>

				<div class="row">
					<div class="col-sm-5">
						<div class="well">
							<h2>LDAP Add User</h2>
							<form method="post" name="controlform" action="AdminServlet">
								Add User: <input type="text" name="username"
									placeholder="Enter User name" /> </br> Password: <input
									type="password" name="password" placeholder="Password" /> <input
									type="submit" name="action" value="AddUser" />
							</form>
							<%
								if (request.getAttribute("addedOrNot") != null) {
							%>
							<input type="hidden"
								value=<%=request.getAttribute("addedOrNot")%> id="temp">
							<script type="text/javascript">
								alert(document.getElementById("temp").value);
							</script>
							<%
								}
							%>

						</div>
					</div>
					<div class="col-sm-5">
						<div class="well">
							<h2>LDAP Delete User</h2>
							<form method="post" action="AdminServlet">
								<input type="submit" name="action" value="ListUsers" /></br>
							</form>

							<%
								if (request.getAttribute("UserList") != null) {
							%>
							<form method="post" action="AdminServlet">
								<SELECT name="toDelete" MULTIPLE style="max-width: 90%;">
									<h2>Users</h2>
									<%
										ArrayList<String> users = (ArrayList<String>) request.getAttribute("UserList");
											for (String name : users) {
									%>
									<OPTION>
										<%=name%>
										<%
											}
										%>
									
								</SELECT>&nbsp; <input type="submit" name="action" value="DeleteUser" />
							</form>
							<%
								}
							%>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

</body>
</html>
