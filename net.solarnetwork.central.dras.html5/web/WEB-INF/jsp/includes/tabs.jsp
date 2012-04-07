<ul
	class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">
	<li class="ui-state-default ui-corner-top ${param.tab == 'home' ? 'ui-tabs-selected ui-state-active' : '' }">
		<a href="<c:url value='/u/home.do'/>"><fmt:message key="tab.home" /> </a>
	</li>
	<li class="ui-state-default ui-corner-top ${param.tab == 'users' ? 'ui-tabs-selected ui-state-active' : '' }">
		<a href="<c:url value='/u/users.do'/>"><fmt:message key="tab.users" /></a>
	</li>
	<li class="ui-state-default ui-corner-top ${param.tab == 'programs' ? 'ui-tabs-selected ui-state-active' : '' }">
		<a href="<c:url value='/u/programs.do'/>"><fmt:message key="tab.programs" /></a>
	</li>
	<li class="ui-state-default ui-corner-top ${param.tab == 'events' ? 'ui-tabs-selected ui-state-active' : '' }">
		<a href="<c:url value='/u/events.do'/>"><fmt:message key="tab.events" /></a>
	</li>
	<sec:authorize url="/u/reports.do">
		<li class="ui-state-default ui-corner-top ${param.tab == 'reports' ? 'ui-tabs-selected ui-state-active' : '' }">
			<a href="<c:url value='/u/reports.do'/>"><fmt:message key="tab.reports" /></a>
		</li>
	</sec:authorize>
	<li class="ui-state-default ui-corner-top ${param.tab == 'about' ? 'ui-tabs-selected ui-state-active' : '' }">
		<a href="<c:url value='/u/about.do'/>"><fmt:message key="tab.about" /></a>
	</li>
	<li class="ui-state-default ui-corner-top ${param.tab == 'logout' ? 'ui-tabs-selected ui-state-active' : '' } logout">
		<a href="<c:url value='/u/logout.do'/>"><fmt:message key="tab.logout" /></a>
	</li>
</ul>
