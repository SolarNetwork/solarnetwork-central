<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="intro">
	<fmt:message key="registration.confirm.confirmed">
		<fmt:param>${user.name}</fmt:param>
	</fmt:message>
</div>
<p>
	<fmt:message key="registration.confirm.login">
		<fmt:param><c:url value='/u/my-nodes'/></fmt:param>
		<fmt:param>${user.email}</fmt:param>
	</fmt:message>
</p>
