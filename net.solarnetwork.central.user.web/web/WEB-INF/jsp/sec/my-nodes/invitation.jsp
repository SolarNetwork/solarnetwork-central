<p class="intro">
	<fmt:message key="my-nodes.invitation.intro"/>
</p>
<pre class="well">${details.confirmationKey}</pre>
<h3></h3>
<p class="alert alert-info">
	<strong><fmt:message key='my-nodes.invitation.securityPhrase.label'/>:</strong>
	${details.securityPhrase}
</p>
<div>
	<a href="<c:url value='/u/sec/my-nodes'/>" class="btn btn-primary"><fmt:message key='back.label'/></a>
</div>