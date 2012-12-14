<fieldset>
	<c:set var="userNameErrors"><form:errors path="name" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userNameErrors}'> error</c:if>">
		<c:set var="label"><fmt:message key="user.name.label"/></c:set>
		<label class="control-label" for="user-name">${label}</label>
		<div class="controls">
			<form:input path="name" id="user-name" maxlength="128" placeholder="${label}"/>
			<c:out value="${userNameErrors}" escapeXml="false"/>
		</div>
	</div>
	<c:set var="userEmailErrors"><form:errors path="email" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userEmailErrors}'> error</c:if>">
		<label class="control-label" for="user-email"><fmt:message key="user.email.label"/></label>
		<div class="controls">
			<c:set var="label"><fmt:message key="user.email.placeholder"/></c:set>
			<form:input path="email" maxlength="240" id="user-email" placeholder="${label}"/>
			<span class="help-inline"><fmt:message key="user.email.caption"/></span>
			<c:out value="${userEmailErrors}" escapeXml="false"/>
		</div>
	</div>
	<c:set var="userPasswordErrors"><form:errors path="password" cssClass="help-inline" element="span"/></c:set>
	<div class="control-group<c:if test='${not empty userPasswordErrors}'> error</c:if>">
		<label class="control-label" for="user-password"><fmt:message key="user.password.label"/></label>
		<div class="controls">
			<c:set var="label">
				<c:choose>
					<c:when test="${empty user.id}">
						<fmt:message key="user.password.placeholder"/>
					</c:when>
					<c:otherwise>
						<fmt:message key="user.password.edit.placeholder"/>
					</c:otherwise>
				</c:choose>
			</c:set>
			<form:password path="password" maxlength="255" id="user-password" placeholder="${label}"/>
			<c:out value="${userPasswordErrors}" escapeXml="false"/>
		</div>
	</div>
</fieldset>
