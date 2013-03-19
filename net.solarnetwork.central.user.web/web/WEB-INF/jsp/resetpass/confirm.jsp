<p class="intro">
	<fmt:message key="user.resetpassword.confirm.intro"/>
</p>

<c:url value="/u/resetPassword/reset" var="postUrl"/>
<form:form modelAttribute="form" cssClass="form-horizontal" action="${postUrl}" method="post">

	<fieldset>
		<c:set var="userPasswordErrors"><form:errors path="password" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
			<label class="control-label" for="user-password"><fmt:message key="user.password.label"/></label>
			<div class="controls">
				<c:set var="label"><fmt:message key="user.password.placeholder"/></c:set>
				<form:password path="password" maxlength="240" id="user-password" placeholder="${label}"/>
				<c:out value="${userPasswordErrors}" escapeXml="false"/>
			</div>
		</div>
		<c:set var="userPasswordErrors"><form:errors path="password" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
			<label class="control-label" for="user-password"><fmt:message key="user.password.again.label"/></label>
			<div class="controls">
				<form:password path="passwordConfirm" maxlength="240" id="user-password" />
				<c:out value="${userPasswordErrors}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<form:hidden path="confirmationCode"/>
		<form:hidden path="username"/>
		<input type="submit" class="btn btn-primary" value="<fmt:message key='user.resetpassword.confirm.submit.label'/>" />
	</div>

</form:form>
