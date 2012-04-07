<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<h1><fmt:message key="loggedout.title" /></h1>

<p>
	<fmt:message key="loggedout.msg"/>
	<a href="<c:url value='/u/home.do'/>"><fmt:message key="loggedout.login.msg"/></a>
</p>
