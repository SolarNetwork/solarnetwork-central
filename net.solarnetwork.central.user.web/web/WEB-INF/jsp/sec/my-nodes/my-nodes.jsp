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
					<th><fmt:message key="user.node.private.label"/></th>
					<%--th><fmt:message key="user.node.certificate.label"/></th--%>
					<th></th>
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
						<td>
							<span class="label${userNode.requiresAuthorization ? '' : ' label-success'}">
								<fmt:message key="user.node.private.${userNode.requiresAuthorization}"/>
							</span>
						</td>
						<%--td>
							<c:if test="${not empty userNode.certificate}">
								<span class="label${userNode.certificate.status.value eq 'Active' 
									? ' label-success' : userNode.certificate.status.value eq 'Disabled' 
									? ' label-important' : ''}">
									<fmt:message key="user.node.certificate.status.${userNode.certificate.status.value}"/>
								</span>
								<c:if test="${userNode.certificate.status.value eq 'Active'}">
									<a href="<c:url value='/u/sec/my-nodes/cert'/>?id=${userNode.certificate.id}" class="btn btn-small view-cert">
										<fmt:message key="user.node.certificate.action.view"/>
									</a>
								</c:if>
							</c:if>
						</td--%>
						<td>
							<button type="button" class="btn btn-small edit-node" data-target="#edit-node-modal"
								data-user-id="${userNode.user.id}" data-node-id="${userNode.node.id}"
								><fmt:message key='my-nodes.action.edit'/></button>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
	<a class="btn btn-primary" href="#invite-modal" data-toggle="modal">
		<i class="icon-plus icon-white"></i> <fmt:message key='my-nodes.inviteNode'/>
	</a>
	<form id="invite-modal" class="modal hide fade" action="<c:url value='/u/sec/my-nodes/new'/>" method="post">
	 	<div class="modal-header">
	 		<button type="button" class="close" data-dismiss="modal">&times;</button>
	 		<h3><fmt:message key='my-nodes.inviteNode'/></h3>
	 	</div>
	 	<div class="modal-body form-inline">
	 		<div>
		 		<p><fmt:message key='my-nodes-invitation.create.intro'/></p>
	 			<label>
	 				<fmt:message key='my-nodes.invitation.securityPhrase.label'/>
	 				${' '}
	 				<input type="text" class="span4" name="phrase" 
	 					placeholder="<fmt:message key='my-nodes.invitation.securityPhrase.label'/>"
	 					maxlength="128" required="required"/>
	 			</label> 			
	 			<span class="help-block"><small><fmt:message key='my-nodes.invitation.securityPhrase.caption'/></small></span>
	 		</div>
	 		<div>
	 			<label class="control-label">
	 				<fmt:message key='my-nodes.invitation.tz.label'/>
	 				${' '}
	 				<input type="text" class="span4" name="timeZone" id="invite-tz"
	 					placeholder="<fmt:message key='my-nodes.invitation.tz.placeholder'/>"
	 					maxlength="128" required="required"/>
	 				<span class="help" id="invite-tz-country"></span>
	 			</label>
	 			<div id="tz-picker-container" class="tz-picker-container"></div>
	 		</div>
	 	</div>
	 	<div class="modal-footer">
	 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
	 		<input type="hidden" name="country" id="invite-country"/>
	 		<button type="submit" class="btn btn-primary"><fmt:message key='my-nodes.inviteNode'/></button>
	 	</div>
	</form>
	<div id="view-cert-modal" class="modal hide fade">
	 	<div class="modal-header">
	 		<button type="button" class="close" data-dismiss="modal">&times;</button>
	 		<h3><fmt:message key='my-nodes.cert.view.title'/></h3>
	 	</div>
	 	<div class="modal-body">
	 		<p><fmt:message key='my-nodes-cert.view.intro'/></p>
	 		<pre class="cert" id="modal-cert-container"></pre>
	 	</div>
	 	<div class="modal-footer">
	 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
	 		<a href="<c:url value='/u/sec/my-nodes/cert'/>?download=true&id=0" id="modal-cert-download" class="btn btn-primary">
	 			<fmt:message key='my-nodes.cert.action.download'/>
	 		</a>
	 	</div>
	</div>
	<form id="edit-node-modal" class="modal hide fade page1" action="<c:url value='/u/sec/my-nodes/updateNode'/>" method="post">
	 	<div class="modal-header">
	 		<button type="button" class="close" data-dismiss="modal">&times;</button>
	 		<h3><fmt:message key='my-nodes.edit-node.title'/></h3>
	 	</div>
	 	<div class="modal-body">
			<div class="hbox">
				<fieldset class="form-horizontal">
					<div class="control-group">
						<label class="control-label" for="usernode-id"><fmt:message key="user.node.id.label"/></label>
						<div class="controls">
							<span class="uneditable-input span2" id="usernode-id"></span>
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="usernode-name"><fmt:message key="user.node.name.label"/></label>
						<div class="controls">
							<input name="name" type="text" maxlength="128" class="span3" id="usernode-name"/>
							<span class="help-block"><fmt:message key="user.node.name.caption"/></span>
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="usernode-description"><fmt:message key="user.node.description.label"/></label>
						<div class="controls">
							<input name="description" type="text" maxlength="512" class="span3" id="usernode-description"/>
							<span class="help-block"><fmt:message key="user.node.description.caption"/></span>
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="usernode-private"><fmt:message key="user.node.private.label"/></label>
						<div class="controls">
							<input name="requiresAuthorization" type="checkbox" value="true" id="usernode-private"/>
							<span class="help-block"><fmt:message key="user.node.private.caption"/></span>
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="usernode-location"><fmt:message key="user.node.location.label"/></label>
						<div class="controls">
							<span id="usernode-location"></span>
							<button type="button" class="btn change-location"><fmt:message key='change.label'/></button>
						</div>
					</div>
				</fieldset>
				<fieldset class="form-horizontal edit-location-tz">
					<p><fmt:message key='my-nodes.edit-node.choose-tz.intro'/></p>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-tz"><fmt:message key='location.tz.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.timeZoneId" id="edit-node-location-tz"
	 							placeholder="<fmt:message key='my-nodes.invitation.tz.placeholder'/>"
	 							maxlength="128" />
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-country"><fmt:message key='location.country.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.timeZoneId" id="edit-node-location-country" maxlength="128" />
						</div>
			 		</div>
					<div class="tz-picker-container"></div>
				</fieldset>
			</div>
	 	</div>
	 	<div class="modal-footer">
	 		<button type="button" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></button>
	 		<button type="button" class="btn page2 btn-info" id="edit-node-page-back"><fmt:message key='back.label'/></button>
	 		<button type="button" class="btn page2 btn-primary" id="edit-node-select-tz"><fmt:message key='my-nodes.edit-node.choose-tz.action.select'/></button>
	 		<button type="submit" class="btn page1 btn-primary"><fmt:message key='save.label'/></button>
	 	</div>
		<input type="hidden" name="node.id"/>
		<input type="hidden" name="user.id"/>
		<input type="hidden" name="node.locationId"/>
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
					<th><fmt:message key="user.nodeconf.created.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${pendingUserNodeConfirmationsList}" var="userNodeConf">
					<tr>
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

