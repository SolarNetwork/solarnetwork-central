<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>
<!DOCTYPE HTML>
<html>
	<tiles:useAttribute name="tab"/>

	<head>
		<tiles:insertAttribute name="head" />
	</head>
	<body>
		<jsp:include page="/WEB-INF/jsp/includes/banner.jsp"/>
	
		<div id="tabs" class="ui-tabs ui-widget ui-widget-content ui-corner-all bodyPanel">
			<header>
				<jsp:include page="/WEB-INF/jsp/includes/tabs.jsp">
					<jsp:param name="tab" value="${tab}" /> 
				</jsp:include>
			</header>

			<section class="content">
				<tiles:insertAttribute name="body" />
			</section>
		</div>
	</body>
</html>
