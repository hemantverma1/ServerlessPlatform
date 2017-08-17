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
				<li class="active"><a href="admin.jsp" onclick="alert('Logged out successfully!!!');">Logout</a></li>
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
					<li class="active"><a href="admin.jsp" onclick="alert('Logged out successfully!!!');">Logout</a></li>

				</ul>
				<br>
			</div>
			<br>

			<div class="col-sm-9">
				<div class="well">
					<h4>Welcome to Admin Dashboard</h4>

				</div>



			</div>
		</div>
	</div>

</body>
</html>
