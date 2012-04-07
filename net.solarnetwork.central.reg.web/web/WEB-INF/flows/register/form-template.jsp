<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="tiles" uri="http://tiles.apache.org/tags-tiles" %>

<c:if test="${not empty rootCauseException}">
	<div class="global-error">
		<c:choose>
			<c:when test="${not empty rootCauseException.reason}">
				<fmt:message key="registration.error.auth.${rootCauseException.reason}">
					<fmt:param value="rootCauseException.email"/>
				</fmt:message>
			</c:when>
			<c:otherwise>
				<fmt:message key="error.unexpected">
					<fmt:param value="rootCauseException.message"/>
				</fmt:message>
			</c:otherwise>
		</c:choose>
	</div>
</c:if>

<tiles:insertAttribute name="intro" />

<form:form modelAttribute="user" cssClass="registration-form">
	
	<tiles:insertAttribute name="detail" />
	
	<div class="button-group">
	 <tiles:insertAttribute name="actions" />
	</div>

</form:form>

<tiles:insertAttribute name="conclusion" />
