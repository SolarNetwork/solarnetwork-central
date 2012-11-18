<fieldset>
	<c:set var="userNameErrors"><form:errors path="name" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userNameErrors}'> error</c:if>">
		<label class="control-label" for="user-name"><fmt:message key="user.name.label"/></label>
		<div class="controls">
			<form:input path="name" id="user-name" maxlength="128"/>
			<c:out value="${userNameErrors}" escapeXml="false"/>
		</div>
	</div>
	<c:set var="userEmailErrors"><form:errors path="email" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userEmailErrors}'> error</c:if>">
		<label class="control-label" for="user-email"><fmt:message key="user.email.label"/></label>
		<div class="controls">
			<form:input path="email" maxlength="240" id="user-email"/>
			<span class="help-inline"><fmt:message key="user.email.caption"/></span>
			<c:out value="${userEmailErrors}" escapeXml="false"/>
		</div>
	</div>
	<c:set var="userPasswordErrors"><form:errors path="password" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
		<label class="control-label" for="user-password"><fmt:message key="user.password.label"/></label>
		<div class="controls">
			<form:password path="password" maxlength="255" id="user-password"/>
			<c:out value="${userPasswordErrors}" escapeXml="false"/>
		</div>
	</div>
</fieldset>
