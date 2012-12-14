<%@page contentType="text/html" pageEncoding="UTF-8" %>
<fieldset class="review">
	<div class="control-group<c:if test='${not empty userNameErrors}'> error</c:if>">
		<label class="control-label" for="user-name"><fmt:message key="user.name.label"/></label>
		<div class="controls">
			<span id="user-name" class="input-xlarge uneditable-input">${user.name}</span>
		</div>
	</div>
	<div class="control-group<c:if test='${not empty userEmailErrors}'> error</c:if>">
		<label class="control-label" for="user-email"><fmt:message key="user.email.label"/></label>
		<div class="controls">
			<span id="user-email" class="input-xlarge uneditable-input">${user.email}</span>
		</div>
	</div>
	<div class="control-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
		<label class="control-label" for="user-password"><fmt:message key="user.password.label"/></label>
		<div class="controls">
			<span id="user-password" class="input-xlarge uneditable-input">•••••</span>
		</div>
	</div>
</fieldset>
