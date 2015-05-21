<a id="top"></a>

<c:if test='${fn:length(pendingNodeOwnershipRequests) > 0}'>
	<section id="pending-transfer-requests">
		<h2><fmt:message key='my-nodes.pending-transfer-requests.header'/></h2>
		<p>
			<fmt:message key='my-nodes.pending-transfer-requests.intro'>
				<fmt:param>${fn:length(pendingNodeOwnershipRequests)}</fmt:param>
			</fmt:message>
		</p>
		<table class="table" id="pending-transfer-requests-table">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="my-nodes.transferOwnership.requester.label"/></th>
					<th><fmt:message key="my-nodes.transferOwnership.requestDate.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${pendingNodeOwnershipRequests}" var="transfer">
					<tr>
						<td>${transfer.node.id}</td>
						<td>${transfer.user.email}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${transfer.created}" pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>
							<button type="button" class="btn btn-small btn-default decide-ownership-transfer"
								data-user-id="${transfer.user.id}" data-node-id="${transfer.node.id}" 
								data-requester="${transfer.user.email}">
								<fmt:message key='my-nodes.transferOwnership.action.requestDecision'/>
							</button>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

<c:if test='${fn:length(pendingUserNodeConfirmationsList) > 0}'>
	<section id="pending">
		<h2><fmt:message key='my-nodes.pending-invite.header'/></h2>
		<p>
			<fmt:message key='my-nodes.pending-invite.intro'>
				<fmt:param>${fn:length(pendingUserNodeConfirmationsList)}</fmt:param>
			</fmt:message>
		</p>
		<div class="alert alert-info alert-dismissible" role="alert">
			<button type="button" class="close" data-dismiss="alert" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
			<fmt:message key='my-nodes.solarnode.link.help'/>
		</div>
		<table class="table">
			<thead>
				<tr>
					<th class="col-sm-2"><fmt:message key="user.nodeconf.created.label"/></th>
					<th class="col-sm-10"></th>
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
							<div class="btn-group">
								<a class="btn btn-default" href="<c:url value='/u/sec/my-nodes/invitation'/>?id=${userNodeConf.id}">
									<fmt:message key='my-nodes.view-invitation.link'/>
								</a>
								<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
									<span class="caret"></span>
									<span class="sr-only"><fmt:message key='toggle.dropdown.label'/></span>
								</button>
								<ul class="dropdown-menu" role="menu">
									<li>
										<a class="btn btn-danger" href="<c:url value='/u/sec/my-nodes/cancelInvitation'/>?id=${userNodeConf.id}">
											<i class="glyphicon glyphicon-trash"></i> <fmt:message key='my-nodes.cancel-invitation.link'/>
										</a>
									</li>
								</ul>
							</div>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

<c:if test='${fn:length(pendingUserNodeTransferList) > 0}'>
	<section id="pending-transfers">
		<h2>
			<fmt:message key='my-nodes.pending-transfer-ownership.header'/>
		</h2>
		<p class="intro">
			<fmt:message key='my-nodes.pending-transfer-ownership.intro'>
				<fmt:param value="${fn:length(pendingUserNodeTransferList)}"/>
			</fmt:message>
		</p>
		<table class="table" id="pending-transfer">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="my-nodes.transferOwnership.recipient.label"/></th>
					<th><fmt:message key="my-nodes.transferOwnership.requestDate.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${pendingUserNodeTransferList}" var="userNode">
					<tr class="node-row">
						<td>
							${userNode.node.id}
							<c:if test='${fn:length(userNode.name) gt 0}'> - ${userNode.name}</c:if>
							<c:if test='${fn:length(userNode.description) gt 0}'> (${userNode.description})</c:if>
						</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNode.node.created}" pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>${userNode.transfer.email}</td>
						<td>
							<joda:dateTimeZone value="GMT">
								<joda:format value="${userNode.transfer.created}" pattern="dd MMM yyyy"/> GMT
							</joda:dateTimeZone>
						</td>
						<td>
							<button type="button" data-action="<c:url value='/u/sec/my-nodes/cancelNodeTransferRequest'/>?userId=${userNode.user.id}&nodeId=${userNode.node.id}"
								title="<fmt:message key='my-nodes.transferOwnership.action.cancel'/>"
								class="btn btn-small btn-danger cancel-ownership-transfer"><i class="glyphicon glyphicon-remove"></i></button>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</section>
</c:if>

<section id="nodes">
	<h2>
		<fmt:message key='my-nodes.nodelist.header'/>
		<button type="button" id="invite-new-node-button" class="btn btn-primary pull-right" data-target="#invite-modal" data-toggle="modal">
			<i class="glyphicon glyphicon-plus"></i> <fmt:message key='my-nodes.inviteNode'/>
		</button>
	</h2>
	<p class="intro">
		<fmt:message key='my-nodes.intro'>
			<fmt:param value="${fn:length(userNodesList)}"/>
		</fmt:message>
	</p>
	<c:if test="${fn:length(userNodesList) > 0}">
		<table class="table" id="my-nodes-table">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="user.node.name.label"/></th>
					<th><fmt:message key="user.node.description.label"/></th>
					<th><fmt:message key="user.node.private.label"/></th>
					<th><fmt:message key="user.node.certificate.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userNodesList}" var="userNode">
					<tr class="node-row" data-node-id="${userNode.node.id}" data-user-id="${userNode.user.id}"
						<c:if test='${fn:length(userNode.name) gt 0}'>data-node-name="${userNode.name}"</c:if>
						>
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
						<td>
							<c:choose>
								<c:when test="${empty userNode.certificate}">
									<span class="label">
										<fmt:message key="user.node.certificate.unmanaged"/>
									</span>
								</c:when>
								<c:otherwise>
									<span class="label${userNode.certificate.status.value eq 'Active' 
										? ' label-success' : userNode.certificate.status.value eq 'Disabled' 
										? ' label-warning' : ' label-primary'}">
										<fmt:message key="user.node.certificate.status.${userNode.certificate.status.value}"/>
									</span>
								</c:otherwise>
							</c:choose>
						</td>
						<td>
							<div class="btn-group">
								<button type="button" class="btn btn-small btn-default edit-node" data-target="#edit-node-modal"
									data-user-id="${userNode.user.id}" data-node-id="${userNode.node.id}"
									><fmt:message key='my-nodes.action.edit'/></button>
								<button type="button" class="btn btn-small btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
									<span class="caret"></span>
									<span class="sr-only"><fmt:message key='toggle.dropdown.label'/></span>
								</button>
								<ul class="dropdown-menu dropdown-menu-right" role="menu">
									<li>
										<c:if test='${userNode.certificate.status.value eq "Active"}'>
											<a href="#" class="view-cert">
												<fmt:message key="user.node.certificate.action.view"/>
											</a>
										</c:if>
										<a href="#" class="transfer-ownership">
											<fmt:message key="user.node.action.transferOwnership"/>
										</a>
									</li>
								</ul>
							</div>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
</section>

<%-- Modal forms --%>

<form id="invite-modal" class="modal fade" action="<c:url value='/u/sec/my-nodes/new'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='my-nodes.inviteNode'/></h4>
		 	</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='my-nodes-invitation.create.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='my-nodes.invitation.securityPhrase.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="phrase" 
		 					placeholder="<fmt:message key='my-nodes.invitation.securityPhrase.label'/>"
		 					maxlength="128" required="required"/>
			 			<span class="help-block"><small><fmt:message key='my-nodes.invitation.securityPhrase.caption'/></small></span>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='my-nodes.invitation.tz.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="timeZone" id="invite-tz"
		 					placeholder="<fmt:message key='my-nodes.invitation.tz.placeholder'/>"
		 					maxlength="128" required="required"/>
		 			</div>
		 			<div class="col-sm-1">
		 				<span class="help-block" id="invite-tz-country"></span>
		 			</div>
		 		</div>
	 			<div id="tz-picker-container" class="tz-picker-container"></div>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<input type="hidden" name="country" id="invite-country"/>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='my-nodes.inviteNode'/></button>
		 	</div>
		 </div>
 	</div>
</form>
<form id="view-cert-modal" class="modal fade" action="<c:url value='/u/sec/my-nodes/cert'/>/0">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='my-nodes.cert.view.title'/></h4>
		 	</div>
		 	<div class="modal-body">
		 		<p><fmt:message key='my-nodes-cert.view.intro'/></p>
		 		<fieldset class="form-inline">
		 			<label for="view-cert-password"><fmt:message key='my-nodes.cert.view.password.label'/></label>
					<input class="span3 form-control" type="password" name="password" id="view-cert-password" />
		 		</fieldset>
		 		<pre class="cert hidden" id="modal-cert-container"></pre>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<a href="<c:url value='/u/sec/my-nodes/cert/0'/>?download=true" id="modal-cert-download" class="btn btn-default">
		 			<fmt:message key='my-nodes.cert.action.download'/>
		 		</a>
		 		<button type="submit" class="btn btn-primary">
		 			<fmt:message key='my-nodes-cert.action.view'/>
		 		</button>
		 	</div>
		 </div>
	</div>
</form>
<form id="edit-node-modal" class="modal fade page1" action="<c:url value='/u/sec/my-nodes/updateNode'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='my-nodes.edit-node.title'/></h4>
		 	</div>
		 	<div class="modal-body">
				<div class="hbox">
					<fieldset class="form-horizontal">
						<div class="form-group">
							<label class="col-sm-2 control-label" for="usernode-id"><fmt:message key="user.node.id.label"/></label>
							<div class="col-sm-10">
								<span class="uneditable-input span2 form-control" id="usernode-id"></span>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label" for="usernode-name"><fmt:message key="user.node.name.label"/></label>
							<div class="col-sm-10">
								<input name="name" type="text" maxlength="128" class="form-control" id="usernode-name"/>
								<span class="help-block"><fmt:message key="user.node.name.caption"/></span>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label" for="usernode-description"><fmt:message key="user.node.description.label"/></label>
							<div class="col-sm-10">
								<input name="description" type="text" maxlength="512" class="form-control" id="usernode-description"/>
								<span class="help-block"><fmt:message key="user.node.description.caption"/></span>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label" for="usernode-private"><fmt:message key="user.node.private.label"/></label>
							<div class="col-sm-10">
								<div class="checkbox">
									<label>
										<input name="requiresAuthorization" type="checkbox" value="true" id="usernode-private"/>
										<fmt:message key="user.node.private.caption"/>
									</label>
								</div>
							</div>
						</div>
						<div class="form-group">
							<label class="col-sm-2 control-label" for="usernode-location"><fmt:message key="user.node.location.label"/></label>
							<div class="col-sm-10">
								<span id="usernode-location"></span>
								<button type="button" class="btn btn-default change-location"><fmt:message key='change.label'/></button>
							</div>
						</div>
					</fieldset>
					<fieldset class="form-horizontal edit-location-tz">
						<p><fmt:message key='my-nodes.edit-node.choose-tz.intro'/></p>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-tz"><fmt:message key='location.tz.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.timeZoneId" id="edit-node-location-tz"
		 							placeholder="<fmt:message key='my-nodes.invitation.tz.placeholder'/>"
		 							maxlength="128" />
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-country"><fmt:message key='location.country.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.country" id="edit-node-location-country" maxlength="2" />
							</div>
				 		</div>
						<div class="tz-picker-container"></div>
					</fieldset>
					<fieldset class="form-horizontal" id="edit-node-location-details" 
						data-lookup-url="<c:url context="/solarquery" value='/api/v1/pub/location'/>">
						<p><fmt:message key='my-nodes.edit-node.choose-location.intro'/></p>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-postal-code"><fmt:message key='location.postalCode.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.postalCode" id="edit-node-location-postal-code"
		 							maxlength="128" />
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-locality"><fmt:message key='location.locality.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.locality" id="edit-node-location-locality"
		 							maxlength="128" />
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-state"><fmt:message key='location.state.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.stateOrProvince" id="edit-node-location-state"
		 							maxlength="128" />
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-region"><fmt:message key='location.region.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.region" id="edit-node-location-region"
		 							maxlength="128" />
							</div>
				 		</div>
				 		<p class="hidden" id="edit-node-location-search-no-match">
				 			<fmt:message key='my-nodes.edit-node.choose-location.nomatch'/>
				 		</p>
						<table class="table table-striped table-hover hidden" id="edit-node-location-search-results">
							<thead>
								<tr>
									<th><fmt:message key='location.country.label'/></th>
									<th><fmt:message key='location.state.label'/></th>
									<th><fmt:message key='location.region.label'/></th>
									<th><fmt:message key='location.locality.label'/></th>
									<th><fmt:message key='location.postalCode.label'/></th>
								</tr>
								<tr class="template">
									<td data-tprop="country"></td>
									<td data-tprop="stateOrProvince"></td>
									<td data-tprop="region"></td>
									<td data-tprop="locality"></td>
									<td data-tprop="postalCode"></td>
								</tr>
							</thead>
							<tbody>
							</tbody>
						</table>
					</fieldset>
					<fieldset class="form-horizontal" id="edit-node-location-private-details">
						<p><fmt:message key='my-nodes.edit-node.private-location.intro'/></p>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-street"><fmt:message key='location.address.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.street" id="edit-node-location-street"
		 							maxlength="256" />
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-latitude"><fmt:message key='location.lat.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.latitude" id="edit-node-location-latitude"
		 							maxlength="16"  aria-describedby="edit-node-locaiton-latitude-help"/>
		 						<span class="help-block" id="edit-node-locaiton-latitude-help"><fmt:message key='my-nodes.edit-node.choose-location-private.latlon.caption'/></span>
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-longitude"><fmt:message key='location.lon.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.longitude" id="edit-node-location-longitude"
		 							maxlength="16" aria-describedby="edit-node-locaiton-longitude-help"/>
		 						<span class="help-block" id="edit-node-locaiton-longitude-help"><fmt:message key='my-nodes.edit-node.choose-location-private.latlon.caption'/></span>
							</div>
				 		</div>
						<div class="form-group">
				 			<label class="col-sm-3 control-label" for="edit-node-location-elevation"><fmt:message key='location.elevation.label'/></label>
							<div class="col-sm-9">
								<input type="text" class="form-control" name="node.location.elevation" id="edit-node-location-elevation"
		 							maxlength="12"  aria-describedby="edit-node-locaiton-elevation-help"/>
		 						<span class="help-block" id="edit-node-locaiton-elevation-help"><fmt:message key='my-nodes.edit-node.choose-location-private.elevation.caption'/></span>
							</div>
				 		</div>
					</fieldset>
				</div>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="button" class="btn page2 page3 page4 btn-info" id="edit-node-page-back"><fmt:message key='back.label'/></button>
		 		<button type="submit" class="btn page1 btn-primary"><fmt:message key='save.label'/></button>
		 		<button type="button" class="btn page2 btn-primary" id="edit-node-select-tz"><fmt:message key='my-nodes.edit-node.choose-tz.action.select'/></button>
		 		<button type="button" class="btn page3 btn-primary" id="edit-node-select-location" disabled="disabled"><fmt:message key='my-nodes.edit-node.choose-location.action.select'/></button>
		 		<button type="button" class="btn page4 btn-primary" id="edit-node-select-location-private"><fmt:message key='my-nodes.edit-node.choose-location-private.action.select'/></button>
		 	</div>
		</div>
	</div>
	<input type="hidden" name="node.id"/>
	<input type="hidden" name="user.id"/>
	<input type="hidden" name="node.locationId"/>
</form>
<form id="transfer-ownership-modal" class="modal fade" action="<c:url value='/u/sec/my-nodes/requestNodeTransfer'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='my-nodes.transferOwnership.title'/></h4>
		 	</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='my-nodes.transferOwnership.intro'/></p>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="transfer-ownership-node"><fmt:message key="user.node.id.label"/></label>
					<div class="col-sm-10">
						<p id="transfer-ownership-node" class="form-control-static"></p>
					</div>
				</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="transfer-ownership-recipient"><fmt:message key='my-nodes.transferOwnership.recipient.label'/></label>
					<div class="col-sm-10">
						<input class="form-control" type="text" name="recipient" maxlength="240" id="transfer-ownership-recipient"
							required="required"
							placeholder="<fmt:message key='my-nodes.transferOwnership.recipient.placeholder'/>"
							aria-describedby="transfer-ownership-recipient-help"
							 />
						<span class="help-block" id="transfer-ownership-recipient-help"><fmt:message key='my-nodes.transferOwnership.recipient.caption'/></span>
					</div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<button type="submit" class="btn btn-primary">
		 			<fmt:message key='my-nodes.transferOwnership.action.submit'/>
		 		</button>
		 	</div>
		 </div>
	</div>
	<input type="hidden" name="nodeId"/>
	<input type="hidden" name="userId"/>
</form>
<form id="decide-transfer-ownership-modal" class="modal fade" action="<c:url value='/u/sec/my-nodes/confirmNodeTransferRequest'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='my-nodes.transferOwnership.requestDecision.title'/></h4>
		 	</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='my-nodes.transferOwnership.requestDecision.intro'/></p>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="transfer-ownership-request-node"><fmt:message key="user.node.id.label"/></label>
					<div class="col-sm-10">
						<p id="transfer-ownership-request-node" class="form-control-static"></p>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="transfer-ownership-request-requester"><fmt:message key="my-nodes.transferOwnership.requester.label"/></label>
					<div class="col-sm-10">
						<p id="transfer-ownership-request-requester" class="form-control-static"></p>
					</div>
				</div>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<button type="button" class="btn btn-danger submit">
		 			<fmt:message key='my-nodes.transferOwnership.action.decline'/>
		 		</button>
		 		<button type="button" class="btn btn-success submit" data-accept="true">
		 			<fmt:message key='my-nodes.transferOwnership.action.accept'/>
		 		</button>
		 	</div>
		 </div>
	</div>
	<input type="hidden" name="nodeId"/>
	<input type="hidden" name="userId"/>
	<input type="hidden" name="accept" value="false"/>
</form>
