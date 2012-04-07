<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<h2><fmt:message key="login.title" /></h2>

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
			<td>
				<input type="submit" value="<fmt:message key='login.login.label'/>" />
			</td>
		</tr>
	</table>
</form>
<script>
	document.getElementById("first.responder").focus();
</script>
