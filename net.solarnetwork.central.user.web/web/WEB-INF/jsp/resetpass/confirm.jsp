<p class="intro">
	<fmt:message key="user.resetpassword.confirm.intro"/>
</p>

<c:url value="/u/resetPassword/reset" var="postUrl"/>
<form:form modelAttribute="form" cssClass="form-horizontal" action="${postUrl}" method="post">

	<fieldset>
		<c:set var="userPasswordErrors"><form:errors path="password" cssClass="alert alert-warning" element="span"/></c:set>
		<div class="form-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
			<label class="col-sm-2 control-label" for="reset-pass-password"><fmt:message key="user.password.label"/></label>
			<div class="col-sm-4">
				<c:set var="label"><fmt:message key="user.password.placeholder"/></c:set>
				<form:password path="password" maxlength="240" id="reset-pass-password" placeholder="${label}" cssClass="form-control"/>
				<c:out value="${userPasswordErrors}" escapeXml="false"/>
			</div>
		</div>
		<c:set var="userPasswordErrors"><form:errors path="password" cssClass="alert alert-warning" element="span"/></c:set>
		<div class="form-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
			<label class="col-sm-2 control-label" for="reset-pass-password-again"><fmt:message key="user.password.again.label"/></label>
			<div class="col-sm-4">
				<form:password path="passwordConfirm" maxlength="240" id="reset-pass-password-again" cssClass="form-control"/>
				<c:out value="${userPasswordErrors}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
	<div class="form-group">
		<div class="col-sm-offset-2 col-sm-10">
			<input type="submit" class="btn btn-primary" value="<fmt:message key='user.resetpassword.confirm.submit.label'/>" />
		</div>
	</div>

	<form:hidden path="confirmationCode"/>
	<form:hidden path="username"/>
</form:form>
