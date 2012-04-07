<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<div class="intro">
	To add a new node, you must invite that node to associate with your
	account. Copy the following invitation code and paste it into your
	node's setup screen (http://solarnode.localhost/setup):
</div>
<div class="copycode">
	${nodeAssociation.confirmationKey}
</div>
<div class="button-group">
	<form:form>
		<input type="submit" name="_eventId_done" value="<fmt:message key='continue.label'/>" />
	</form:form>
</div>
