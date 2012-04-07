<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="global-error">
	<c:if test="${not empty rootCauseException}">
		<c:choose>
			<c:when test="${rootCauseException.reason eq 'REGISTRATION_ALREADY_CONFIRMED'}">
				<fmt:message key="registration.confirm.alreadyconfirmed"/>
			</c:when>
			<c:otherwise>
				<fmt:message key="registration.confirm.notconfirmed"/>
			</c:otherwise>
		</c:choose>
	</c:if>
</div>
