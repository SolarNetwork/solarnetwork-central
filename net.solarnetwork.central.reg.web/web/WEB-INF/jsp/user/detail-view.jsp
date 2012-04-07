<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<fieldset>
	<div class="field">
		<label for="user.name"><fmt:message key="user.name.label"/></label>
		<div class="output">${user.name}</div>
	</div>
	<div class="field">
		<label for="user.email"><fmt:message key="user.email.label"/></label>
		<div class="output">${user.email}</div>
	</div>
	<div class="field">
		<label for="user.password"><fmt:message key="user.password.label"/></label>
		<div class="output">*****</div>
	</div>
</fieldset>
