<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<fieldset>
	<c:set var="userNameErrors"><form:errors path="name" cssClass="error" element="span"/></c:set>
	<div class="field input<c:if test='${not empty userNameErrors}'> error</c:if>">
		<label for="user.name"><fmt:message key="user.name.label"/></label>
		<div>
			<form:input path="name" id="user-name" maxlength="128"/>
		</div>
		<c:out value="${userNameErrors}" escapeXml="false"/>
	</div>
	<c:set var="userEmailErrors"><form:errors path="email" cssClass="error" element="span"/></c:set>
	<div class="field input<c:if test='${not empty userEmailErrors}'> error</c:if>">
		<label for="user.email"><fmt:message key="user.email.label"/></label>
		<div>
			<form:input path="email" maxlength="240"/>
			<div class="caption"><fmt:message key="user.email.caption"/></div>
		</div>
		<c:out value="${userEmailErrors}" escapeXml="false"/>
	</div>
	<c:set var="userPasswordErrors"><form:errors path="password" cssClass="error" element="span"/></c:set>
	<div class="field input<c:if test='${not empty userPasswordErrors}'> error</c:if>">
		<label for="user.password"><fmt:message key="user.password.label"/></label>
		<div>
			<form:password path="password" maxlength="255"/>
		</div>
		<c:out value="${userPasswordErrors}" escapeXml="false"/>
	</div>
</fieldset>
