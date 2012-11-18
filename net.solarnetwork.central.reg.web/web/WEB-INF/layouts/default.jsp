<!DOCTYPE html>
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<tiles:useAttribute name="navloc" scope="request"/>
<html lang="en">
	<tiles:insertAttribute name="head" />
	<body>
		<div class="container">
			<tiles:insertAttribute name="header" />
			<tiles:insertAttribute name="body" />
		</div>
	</body>
</html>
