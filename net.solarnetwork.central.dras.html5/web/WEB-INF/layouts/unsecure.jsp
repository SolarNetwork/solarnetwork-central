<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<!DOCTYPE HTML>
<html>
	<head>
		<tiles:insertAttribute name="head" />
	</head>
	<body>
		<div class="loginPanel ui-corner-all">
			<header>
				<jsp:include page="/WEB-INF/jsp/includes/banner.jsp"/>
			</header>
			
			<div class="clear"></div>
		
			<section class="content">
				<tiles:insertAttribute name="body" />
			</section>
		</div>
	</body>
</html>
