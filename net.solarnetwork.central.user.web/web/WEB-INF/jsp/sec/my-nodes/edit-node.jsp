<spring:nestedPath path="userNode">
	<form:hidden path="node.id"/>
	<form:hidden path="user.id"/>
	<fieldset>
		<div class="control-group">
			<label class="control-label" for="usernode-id"><fmt:message key="user.node.id.label"/></label>
			<div class="controls">
				<span class="uneditable-input span2" id="usernode-id">${userNode.id}</span>
			</div>
		</div>
		<c:set var="errors"><form:errors path="name" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<label class="control-label" for="usernode-name"><fmt:message key="user.node.name.label"/></label>
			<div class="controls">
				<form:input path="name" maxlength="128" cssClass="span3" id="usernode-name"/>
				<span class="help-block"><fmt:message key="user.node.name.caption"/></span>
				<c:out value="${errors}" escapeXml="false"/>
			</div>
		</div>
		<c:set var="errors"><form:errors path="description" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<label class="control-label" for="usernode-description"><fmt:message key="user.node.description.label"/></label>
			<div class="controls">
				<form:input path="description" maxlength="512" cssClass="span3" id="usernode-description"/>
				<span class="help-block"><fmt:message key="user.node.description.caption"/></span>
				<c:out value="${errors}" escapeXml="false"/>
			</div>
		</div>
		<c:set var="errors"><form:errors path="requiresAuthorization" cssClass="help-inline" element="span"/></c:set>
		<div class="control-group<c:if test='${not empty errors}'> error</c:if>">
			<label class="control-label" for="usernode-private"><fmt:message key="user.node.private.label"/></label>
			<div class="controls">
				<form:checkbox path="requiresAuthorization" id="usernode-private"/>
				<span class="help-block"><fmt:message key="user.node.private.caption"/></span>
				<c:out value="${errors}" escapeXml="false"/>
			</div>
		</div>
	</fieldset>
</spring:nestedPath>
