<%@page contentType="text/html" pageEncoding="UTF-8" %>
<fieldset class="review">
	<div class="form-group<c:if test='${not empty userNameErrors}'> error</c:if>">
		<label class="col-sm-2 control-label" for="user-name"><fmt:message key="user.name.label"/></label>
		<div class="col-sm-10">
			<p id="user-name" class="form-control-static">${user.name}</p>
		</div>
	</div>
	<div class="form-group<c:if test='${not empty userEmailErrors}'> error</c:if>">
		<label class="col-sm-2 control-label" for="user-email"><fmt:message key="user.email.label"/></label>
		<div class="col-sm-10">
			<p id="user-email" class="form-control-static">${user.email}</p>
		</div>
	</div>
	<div class="form-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
		<label class="col-sm-2 control-label" for="user-password"><fmt:message key="user.password.label"/></label>
		<div class="col-sm-10">
			<p id="user-password" class="form-control-static">•••••</p>
		</div>
	</div>
</fieldset>
