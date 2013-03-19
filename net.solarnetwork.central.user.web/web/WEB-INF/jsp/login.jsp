<c:if test="${not empty param.login_error}">
	<div class="alert alert-error">
		<fmt:message key="login.error"/>
	</div>
</c:if>

<p class="intro">
	<fmt:message key="login.intro">
		<fmt:param><c:url value="/register.do"/></fmt:param>
		<fmt:param><c:url value="/u/resetPassword"/></fmt:param>
	</fmt:message>
</p>

<form action="<c:url value='/j_spring_security_check'/>" class="form form-horizontal login-form" method="post">
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="login-username"><fmt:message key="user.email.label"/></label>
			<div class="controls">
				<input type="text" name="j_username" id="login-username" maxlength="240" 
					placeholder="<fmt:message key='user.email.placeholder'/>"
					value="<c:if test='${not empty param.login_error}'>${SPRING_SECURITY_LAST_USERNAME}</c:if>"/>
				<span class="help-inline"><fmt:message key='user.email.caption'/></span>
			</div>
		</div>
		<div class="control-group">
			<label class="control-label" for="login-password"><fmt:message key="user.password.label"/></label>
			<div class="controls">
				<input type="password" name="j_password" id="login-password" maxlength="255"
					placeholder="<fmt:message key='user.password.placeholder'/>"/>
			</div>
		</div>
	</fieldset>
	<div class="form-actions">
		<button type="submit" class="btn btn-primary"><fmt:message key='action.login'/></button>
	</div>
</form>
