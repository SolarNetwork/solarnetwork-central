<c:if test="${not empty param.login_error}">
	<div class="alert alert-warning">
		<fmt:message key="login.error"/>
	</div>
</c:if>

<p class="intro">
	<fmt:message key="login.intro">
		<fmt:param><c:url value="/register.do"/></fmt:param>
		<fmt:param><c:url value="/u/resetPassword"/></fmt:param>
	</fmt:message>
</p>

<form action="<c:url value='/login'/>" class="form-horizontal login-form" method="post">
	<fieldset>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="login-username"><fmt:message key="user.email.label"/></label>
			<div class="col-sm-4">
				<input class="form-control" type="text" name="username" id="login-username" maxlength="240" 
					placeholder="<fmt:message key='user.email.placeholder'/>"
					aria-describedby="login-username-help"
					value="<c:if test='${not empty param.login_error}'>${SPRING_SECURITY_LAST_USERNAME}</c:if>"/>
				<span class="help-block" id="login-username-help"><fmt:message key='user.email.caption'/></span>
			</div>
		</div>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="login-password"><fmt:message key="user.password.label"/></label>
			<div class="col-sm-4">
				<input class="form-control" type="password" name="password" id="login-password" maxlength="255"
					placeholder="<fmt:message key='user.password.placeholder'/>"/>
			</div>
		</div>
	</fieldset>
	<div class="form-group">
		<div class="col-sm-offset-2 col-sm-10">
			<button type="submit" class="btn btn-primary"><fmt:message key='action.login'/></button>
		</div>
	</div>
	<sec:csrfInput/>
</form>
