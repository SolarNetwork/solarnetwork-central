<p class="intro">
	<fmt:message key='my-nodes.intro'>
		<fmt:param value="${fn:length(userNodesList)}"/>
	</fmt:message>
</p>
<section id="nodes">
	<h2><fmt:message key='my-nodes.nodelist.header'/></h2>
	<c:if test="${fn:length(userNodesList) > 0}">
		<table class="table">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="user.node.name.label"/></th>
					<th><fmt:message key="user.node.description.label"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userNodesList}" var="userNode">
					<tr>
						<td>${userNode.node.id}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNode.node.created}"
									 pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>${userNode.name}</td>
						<td>${userNode.description}</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
	<a class="btn btn-primary" href="<c:url value='/u/sec/invite-node'/>">
		<i class="icon-plus icon-white"></i> <fmt:message key='my-nodes.inviteNode'/>
	</a>
</section>
<div class="intro">
	
</div>
<c:if test="${fn:length(pendingUserNodeConfirmationsList) > 0}">
	<section id="pending">
		<h2><fmt:message key='my-nodes.pending-invite.header'/></h2>
		<p>
			<fmt:message key='my-nodes.pending-invite.intro'>
				<fmt:param>${fn:length(pendingUserNodeConfirmationsList)}</fmt:param>
			</fmt:message>
		</p>
		<table class="table">
			<thead>
				<tr>
					<th><fmt:message key="user.nodeconf.nodeId.label"/></th>
					<th><fmt:message key="user.nodeconf.created.label"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${pendingUserNodeConfirmationsList}" var="userNodeConf">
					<tr>
						<td>${userNodeConf.nodeId}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNodeConf.created}"
									 pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

