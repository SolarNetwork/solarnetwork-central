<p class="intro">
	<fmt:message key='my-nodes.intro'>
		<fmt:param value="${fn:length(userNodesList)}"/>
	</fmt:message>
</p>
<section id="nodes">
	<h2><fmt:message key='my-nodes.nodelist.header'/></h2>
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
										? ' label-important' : ''}">
										<fmt:message key="user.node.certificate.status.${userNode.certificate.status.value}"/>
									</span>
									<c:if test="${userNode.certificate.status.value eq 'Active'}">
										<button type="button" data-node-id="${userNode.node.id}" class="btn btn-small view-cert">
											<fmt:message key="user.node.certificate.action.view"/>
										</button>
									</c:if>
								</c:otherwise>
							</c:choose>
						</td>
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
	<form id="view-cert-modal" class="modal hide fade" action="<c:url value='/u/sec/my-nodes/cert'/>/0">
	 	<div class="modal-header">
	 		<button type="button" class="close" data-dismiss="modal">&times;</button>
	 		<h3><fmt:message key='my-nodes.cert.view.title'/></h3>
	 	</div>
	 	<div class="modal-body">
	 		<p><fmt:message key='my-nodes-cert.view.intro'/></p>
	 		<fieldset class="form-inline">
	 			<label for="view-cert-password"><fmt:message key='my-nodes.cert.view.password.label'/></label>
				<input class="span3" type="password" name="password" id="view-cert-password" />
	 		</fieldset>
	 		<pre class="cert hidden" id="modal-cert-container"></pre>
	 	</div>
	 	<div class="modal-footer">
	 		<a href="#" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></a>
	 		<a href="<c:url value='/u/sec/my-nodes/cert/0'/>?download=true" id="modal-cert-download" class="btn">
	 			<fmt:message key='my-nodes.cert.action.download'/>
	 		</a>
	 		<button type="submit" class="btn btn-primary">
	 			<fmt:message key='my-nodes-cert.action.view'/>
	 		</button>
	 	</div>
	</form>
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
							<input name="name" type="text" maxlength="128" class="span5" id="usernode-name"/>
							<span class="help-block"><fmt:message key="user.node.name.caption"/></span>
						</div>
					</div>
					<div class="control-group">
						<label class="control-label" for="usernode-description"><fmt:message key="user.node.description.label"/></label>
						<div class="controls">
							<input name="description" type="text" maxlength="512" class="span5" id="usernode-description"/>
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
							<input type="text" class="span1" name="node.location.country" id="edit-node-location-country" maxlength="2" />
						</div>
			 		</div>
					<div class="tz-picker-container"></div>
				</fieldset>
				<fieldset class="form-horizontal" id="edit-node-location-details" 
					data-lookup-url="<c:url context="/solarquery" value='/api/v1/pub/location'/>">
					<p><fmt:message key='my-nodes.edit-node.choose-location.intro'/></p>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-postal-code"><fmt:message key='location.postalCode.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.postalCode" id="edit-node-location-postal-code"
	 							maxlength="128" />
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-locality"><fmt:message key='location.locality.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.locality" id="edit-node-location-locality"
	 							maxlength="128" />
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-state"><fmt:message key='location.state.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.stateOrProvince" id="edit-node-location-state"
	 							maxlength="128" />
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-region"><fmt:message key='location.region.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.region" id="edit-node-location-region"
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
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-street"><fmt:message key='location.address.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.street" id="edit-node-location-street"
	 							maxlength="256" />
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-latitude"><fmt:message key='location.lat.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.latitude" id="edit-node-location-latitude"
	 							maxlength="16"  aria-describedby="edit-node-locaiton-latitude-help"/>
	 						<span class="help-block" id="edit-node-locaiton-latitude-help"><fmt:message key='my-nodes.edit-node.choose-location-private.latlon.caption'/></span>
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-longitude"><fmt:message key='location.lon.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.longitude" id="edit-node-location-longitude"
	 							maxlength="16" aria-describedby="edit-node-locaiton-longitude-help"/>
	 						<span class="help-block" id="edit-node-locaiton-longitude-help"><fmt:message key='my-nodes.edit-node.choose-location-private.latlon.caption'/></span>
						</div>
			 		</div>
					<div class="control-group">
			 			<label class="control-label" for="edit-node-location-elevation"><fmt:message key='location.elevation.label'/></label>
						<div class="controls">
							<input type="text" class="span3" name="node.location.elevation" id="edit-node-location-elevation"
	 							maxlength="12"  aria-describedby="edit-node-locaiton-elevation-help"/>
	 						<span class="help-block" id="edit-node-locaiton-elevation-help"><fmt:message key='my-nodes.edit-node.choose-location-private.elevation.caption'/></span>
						</div>
			 		</div>
				</fieldset>
			</div>
	 	</div>
	 	<div class="modal-footer">
	 		<button type="button" class="btn" data-dismiss="modal"><fmt:message key='close.label'/></button>
	 		<button type="button" class="btn page2 page3 page4 btn-info" id="edit-node-page-back"><fmt:message key='back.label'/></button>
	 		<button type="submit" class="btn page1 btn-primary"><fmt:message key='save.label'/></button>
	 		<button type="button" class="btn page2 btn-primary" id="edit-node-select-tz"><fmt:message key='my-nodes.edit-node.choose-tz.action.select'/></button>
	 		<button type="button" class="btn page3 btn-primary" id="edit-node-select-location" disabled="disabled"><fmt:message key='my-nodes.edit-node.choose-location.action.select'/></button>
	 		<button type="button" class="btn page4 btn-primary" id="edit-node-select-location-private"><fmt:message key='my-nodes.edit-node.choose-location-private.action.select'/></button>
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

