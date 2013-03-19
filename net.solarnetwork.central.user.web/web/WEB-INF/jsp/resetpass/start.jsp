<p class="intro">
	<fmt:message key="user.resetpassword.intro"/>
</p>

<form class="form-horizontal" method="get" action="<c:url value='/u/resetPassword/generate'/>">

	<fieldset>
		<div class="control-group">
			<label class="control-label" for="user-email"><fmt:message key="user.email.label"/></label>
			<div class="controls">
				<c:set var="label"><fmt:message key="user.email.placeholder"/></c:set>
				<input type="text" name="email" maxlength="240" id="user-email" placeholder="${label}"/>
				<span class="help-inline"><fmt:message key="user.email.caption"/></span>
			</div>
		</div>
	</fieldset>

	<div class="form-actions">
		<input type="submit" class="btn btn-primary" value="<fmt:message key='user.resetpassword.submit.label'/>"/>
	</div>

</form>
