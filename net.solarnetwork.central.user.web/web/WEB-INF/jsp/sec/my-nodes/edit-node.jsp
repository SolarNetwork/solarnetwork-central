<spring:nestedPath path="userNode">
	<form:hidden path="node.id"/>
	<form:hidden path="user.id"/>
	<fieldset>
		<c:set var="errors"><form:errors path="name" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<c:set var="label"><fmt:message key="user.node.id.label"/></c:set>
			<label class="control-label" for="usernode-id">${label}</label>
			<div class="controls">
				<span class="uneditable-input span3" id="usernode-id">${userNode.id}</span>
			</div>
		</div>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<c:set var="label"><fmt:message key="user.node.name.label"/></c:set>
			<label class="control-label" for="usernode-name">${label}</label>
			<div class="controls">
				<form:input path="name" id="usernode-name" maxlength="128"/>
				<span class="help-block"><fmt:message key="user.node.name.caption"/></span>
				<c:out value="${errors}" escapeXml="false"/>
			</div>
		</div>
		<c:set var="errors"><form:errors path="description" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<label class="control-label" for="usernode-description"><fmt:message key="user.node.description.label"/></label>
			<div class="controls">
				<form:input path="description" maxlength="512" cssClass="span6" id="usernode-description"/>
				<span class="help-block"><fmt:message key="user.node.description.caption"/></span>
				<c:out value="${errors}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
</spring:nestedPath>
