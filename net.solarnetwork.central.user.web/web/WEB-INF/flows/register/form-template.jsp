<c:if test="${not empty rootCauseException}">
	<div class="alert alert-warning">
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

<form:form modelAttribute="user" cssClass="form-horizontal registration-form">
	<tiles:insertAttribute name="detail" />
</form:form>

<tiles:insertAttribute name="conclusion" />
