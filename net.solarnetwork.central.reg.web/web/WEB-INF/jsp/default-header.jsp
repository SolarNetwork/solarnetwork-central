
<div class="navbar">
	<div class="navbar-inner">
		<a class="brand" href="<c:url value='/index.do'/>">
			<img src="<c:url value='/img/logo.svg'/>" alt="<fmt:message key='app.name'/>" width="214" height="30"/>	
		</a>
		<ul class="nav">
			<li ${navloc == 'home' ? 'class="active"' : ''}><a href="<c:url value='/index.do'/>"><fmt:message key='link.home'/></a></li>
			<sec:authorize ifNotGranted="ROLE_USER">
				<li  ${navloc == 'login' ? 'class="active"' : ''}>
					<a href="<c:url value='/login.do'/>"><fmt:message key="link.login"/></a>
				</li>
			</sec:authorize>
		</ul>
		<sec:authorize ifAnyGranted="ROLE_USER">
			<p class="pull-right navbar-text">
				<fmt:message key='nav.label.principal'>
					<fmt:param><sec:authentication property="principal.username" /></fmt:param>
				</fmt:message>
			</p>
		</sec:authorize>
	</div>
</div>
