<!DOCTYPE html>
<%@ page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<tiles:useAttribute name="navloc" scope="request"/>
<html lang="en">
	<tiles:insertAttribute name="head" />
	<meta name="solarUserRootURL" content="<c:url value='/u'/>">
	<body>
		<c:import url="/WEB-INF/jsp/navbar.jsp"/>
		<tiles:insertAttribute name="header" />
		<div class="container">
			<c:if test="${not empty statusMessageKey}">
				<div class="alert alert-success">
					<c:choose>
						<c:when test="${fn:startsWith(statusMessageKey, 'registration.')}">
							<fmt:message key="${statusMessageKey}">
								<fmt:param><sec:authentication property="principal.username" /></fmt:param>
							</fmt:message>
						</c:when>
						<c:otherwise>
							<fmt:message key="${statusMessageKey}">
								<c:if test="${not empty statusMessageParam0}">
									<fmt:param value="${statusMessageParam0}"/>
								</c:if>
							</fmt:message>
						</c:otherwise>
					</c:choose>
				</div>
				<c:remove var="statusMessageKey" scope="session"/>
				<c:remove var="statusMessageParam0" scope="session"/>
			</c:if>
			<c:if test="${not empty errorMessageKey}">
				<div class="alert alert-warning">
					<fmt:message key="${errorMessageKey}">
						<c:if test="${not empty errorMessageKey}">
							<fmt:param value="${errorMessageKeyParam0}"/>
						</c:if>
					</fmt:message>
				</div>
				<c:remove var="errorMessageKey" scope="session"/>
				<c:remove var="errorMessageKeyParam0" scope="session"/>
			</c:if>
			<tiles:insertAttribute name="body" />
		</div>
		<tiles:insertAttribute name="footer" />
	</body>
</html>
