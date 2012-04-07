<div class="login">
	<form method="POST" name="login" action="j_security_check">
		<table style="vertical-align: middle;">
			<tr>
				<td><fmt:message key="login.username.label" /></td>
				<td>
					<input type="text" name="j_username" id="first.responder" />
				</td>
			</tr>
			<tr>
				<td><fmt:message key="login.password.label" /></td>
				<td>
					<input type="password" name="j_password" />
				</td>
			</tr>
			<tr>
				<td></td>
				<td>
					<input type="submit" value="<fmt:message key='login.login.label'/>" />
				</td>
			</tr>
		</table>
	</form>
</div>
<script>
	document.getElementById("first.responder").focus();
</script>