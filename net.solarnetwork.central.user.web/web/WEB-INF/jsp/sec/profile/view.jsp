<p><fmt:message key='user.profile.intro'/></p>
<div class="row">
	<div class="col-sm-2">
		<strong><fmt:message key='user.profile.details.title'/></strong>
	</div>
	<div class="col-sm-8">
		${user.name}<br/>
		${user.email}<br/>
	</div>
	<div class="col-sm-2 text-right">
		<a class="btn btn-primary" href="<c:url value='/u/sec/profile/edit'/>"><fmt:message key='edit.label'/></a>
	</div>
</div>
