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
	<a class="btn btn-primary" href="#invite-modal" role="button" data-toggle="modal">
		<i class="icon-plus icon-white"></i> <fmt:message key='my-nodes.inviteNode'/>
	</a>
	<form id="invite-modal" class="modal hide fade" action="<c:url value='/u/sec/my-nodes/new'/>">
	 	<div class="modal-header">
	 		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	 		<h3><fmt:message key='my-nodes.inviteNode'/></h3>
	 	</div>
	 	<div class="modal-body">
	 		<div>
		 		<p><fmt:message key='my-nodes-invitation.create.intro'/></p>
	 			<label class="control-label" for="secret-phrase"><fmt:message key='my-nodes.invitation.securityPhrase.label'/></label>
	 			<input type="text" class="span6" name="phrase" 
	 				placeholder="<fmt:message key='my-nodes.invitation.securityPhrase.label'/>"
	 				maxlength="128"/>
	 			<span class="help-block"><fmt:message key='my-nodes.invitation.securityPhrase.caption'/></span>
	 		</div>
	 	</div>
	 	<div class="modal-footer">
	 		<a href="#" class="btn" data-dismiss="modal" aria-hidden="true"><fmt:message key='close.label'/></a>
	 		<button type="submit" class="btn btn-primary"><fmt:message key='my-nodes.inviteNode'/></button>
	 	</div>
	 </form>
</section>
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
					<th></th>
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
						<td>
							<a class="btn" href="<c:url value='/u/sec/my-nodes/invitation'/>?id=${userNodeConf.id}">
								<fmt:message key='my-nodes.view-invitation.link'/>
							</a>
							<a class="btn btn-danger" href="<c:url value='/u/sec/my-nodes/cancelInvitation'/>?id=${userNodeConf.id}">
								<i class="icon-trash icon-white"></i> <fmt:message key='my-nodes.cancel-invitation.link'/>
							</a>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

