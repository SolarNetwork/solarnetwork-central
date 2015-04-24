<p class="intro">
	<fmt:message key="user.resetpassword.intro"/>
</p>

<form class="form-horizontal" method="get" action="<c:url value='/u/resetPassword/generate'/>">

	<fieldset>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="reset-pass-email"><fmt:message key="user.email.label"/></label>
			<div class="col-sm-4">
				<c:set var="label"><fmt:message key="user.email.placeholder"/></c:set>
				<input class="form-control" type="text" name="email" maxlength="240" id="reset-pass-email" placeholder="${label}"
					aria-describedby="reset-password-email-help"
					/>
				<span class="help-block" id="reset-password-email-help"><fmt:message key="user.email.caption"/></span>
			</div>
		</div>
	</fieldset>

	<div class="form-group">
		<div class="col-sm-offset-2 col-sm-10">
			<input type="submit" class="btn btn-primary" value="<fmt:message key='user.resetpassword.submit.label'/>"/>
		</div>
	</div>

</form>
