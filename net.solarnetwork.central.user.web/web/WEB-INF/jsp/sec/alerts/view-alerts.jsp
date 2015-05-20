<%--
	Input parameters:
	
		nodeDataAlerts 		- collection of UserAuth objects of type NodeStaleData
		nodeDataAlertTypes 	- collection of UserAlertType that represent node data alerts
		userNodes      		- collection of UserNode objects
 --%>
<p class="intro">
	<fmt:message key='alerts.intro'/>
</p>

<section class="node-data-alerts">
	<h2>
		<fmt:message key='alerts.node.data.header'/>
		<button type="button" id="add-node-data-button" class="btn btn-primary pull-right">
			<i class="glyphicon glyphicon-plus"></i> <fmt:message key='alerts.action.create'/>
		</button>
	</h2>
	<p class="intro">
		<fmt:message key='alerts.node.data.intro'>
			<fmt:param value="${fn:length(nodeDataAlerts)}"/>
		</fmt:message>
	</p>
	<c:if test="${not empty nodeDataAlerts}">
		<table class="table" id="node-data-alerts"
			data-action-situation='<c:url value="/u/sec/alerts/situation"/>'>
			<thead>
				<tr>
					<th><fmt:message key="alert.nodeId.label"/></th>
					<th><fmt:message key="alert.type.label"/></th>
					<th><fmt:message key="alert.options.sourceIds.label"/></th>
					<th><fmt:message key="alert.options.ageMinutes.heading"/></th>
					<th><fmt:message key="alert.status.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${nodeDataAlerts}" var="alert">
					<tr class="alert-row${alert.situation != null ? ' alert-danger' : ''}">
						<td>
							<c:choose>
								<c:when test="${alert.nodeId != null}">
									${alert.nodeId}
								</c:when>
								<c:otherwise>
									<fmt:message key='alert.nodeId.any'/>
								</c:otherwise>
							</c:choose>
						</td>
						<td>
							<fmt:message key='alert.type.${alert.type}.label'/>
						</td>
						<td>
							<c:set var="alertSources">
								<c:if test="${not empty alert.options.sourceIds}">
									<c:forEach items="${alert.options.sourceIds}" var="source" varStatus="itr">
										<c:if test="${not itr.first}">, </c:if>
										${source}
									</c:forEach>
								</c:if>
							</c:set>
							<c:out value="${alertSources}"/>
						</td>
						<td>
							<c:set var="alertAge">
								<c:if test="${alert.options.age != null}">
									<fmt:formatNumber type="number" maxFractionDigits="0" >${alert.options.age / 60}</fmt:formatNumber>
								</c:if>
							</c:set>
							<c:out value="${alertAge}"/>
						</td>
						<td>
							<span class="label${alert.status eq 'Active' 
								? ' label-success' : alert.status eq 'Disabled' 
								? ' label-default' : ' label-primary'}">
								<fmt:message key='alert.status.${alert.status}.label'/>
							</span>
						</td>
						<td>
							<button type="button" class="btn btn-small btn-default edit-alert"
								data-node-id="${alert.nodeId}" data-alert-id="${alert.id}"
								data-alert-type="${alert.type}" data-alert-status="${alert.status}"
								data-sources="${alertSources}" data-age="${alertAge}">
								<fmt:message key='alerts.action.edit'/>
							</button>
							<c:if test="${alert.situation != null}">
								<button type="button" class="btn btn-small btn-danger view-situation"
									data-alert-id="${alert.id}">
									<span class="glyphicon glyphicon-alert" aria-hidden="true"></span>
									<fmt:message key='alerts.action.situation.view'/>
								</button>
							</c:if>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
</section>

<%-- Modal views --%>

<form id="create-node-data-alert-modal" class="modal fade alert-form" action="<c:url value='/u/sec/alerts/save'/>" method="post">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='alerts.node.data.create.title'/></h4>
		 	</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='alerts.node.data.create.intro'/></p>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="create-node-data-alert-node-id"><fmt:message key="alert.nodeId.label"/></label>
					<div class="col-sm-10">
						<select name="nodeId" class="form-control" id="create-node-data-alert-node-id">
							<option value=""><fmt:message key='alert.nodeId.any'/></option>
							<c:forEach items="${userNodes}" var="userNode">
								<option value="${userNode.node.id}">${userNode.idAndName}</option>
							</c:forEach>
						</select>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="create-node-data-alert-type"><fmt:message key="alert.type.label"/></label>
					<div class="col-sm-10 checkbox">
						<select name="type" class="form-control" id="create-node-data-alert-type">
							<c:forEach items="${nodeDataAlertTypes}" var="alertType">
								<option value="${alertType}" title="<fmt:message key='alert.type.${alertType}.caption'/>">
									<fmt:message key='alert.type.${alertType}.label'/>
								</option>
							</c:forEach>
						</select>
					</div>
				</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="create-node-data-alert-age"><fmt:message key='alert.options.ageMinutes.label'/></label>
					<div class="col-sm-10">
						<input name='options["ageMinutes"]' type="number" min="10" required="required" value="30" class="form-control col-sm-3" id="create-node-data-alert-age"/>
						<span class="help-block"><fmt:message key="alert.options.ageMinutes.caption"/></span>
					</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="create-node-data-alert-sources"><fmt:message key='alert.options.sourceIds.label'/></label>
					<div class="col-sm-10">
						<input name='options["sources"]' type="text" maxlength="255" class="form-control" id="create-node-data-alert-sources"/>
						<span class="help-block"><fmt:message key="alert.options.sourceIds.caption"/></span>
					</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="create-node-data-alert-status"><fmt:message key='alert.status.label'/></label>
					<div class="col-sm-10" id="create-node-data-alert-status">
						<c:forEach items="${alertStatuses}" var="alertStatus" varStatus="itr">
							<label class="radio-inline">
								<input type="radio" name="status" value="${alertStatus}" 
									<c:if test='${itr.first}'>checked</c:if>
									title="<fmt:message key='alert.status.${alertStatus}.caption'/>"/>
								<c:out value=" "/>
								<fmt:message key='alert.status.${alertStatus}.label'/>
							</label>
						</c:forEach>
						<div class="help-block alert-status-help"></div>
					</div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 		<button type="submit" class="btn btn-primary before">
		 			<fmt:message key='alerts.action.save'/>
		 		</button>
		 	</div>
		</div>
	</div>
	<input type="hidden" name="id" value=""/>
</form>

<div id="alert-situation-modal" class="modal fade alert-situation-form">
	<div class="modal-dialog">
		<div class="modal-content">
		 	<div class="modal-header bg-danger">
		 		<button type="button" class="close" data-dismiss="modal">&times;</button>
		 		<h4 class="modal-title"><fmt:message key='alerts.situation.view.title'/></h4>
		 	</div>
		 	<div class="modal-body">
		 		<%-- Note: when we have more than one alert type, use JS to toggle the visibility of alert-type elements
		 				   based on the type of the alert being displayed. This allows i18n messages to be rendered for 
		 				   all types.
		 		--%>
		 		<p class="alert-type alert-type-NodeStaleData"><fmt:message key='alerts.situation.view.NodeStaleData.intro'/></p>
		 		<table class="table">
		 			<tbody>
		 				<tr>
		 					<th><fmt:message key="alert.type.label"/></th>
		 					<td class="alert-situation-type"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key="alert.situation.created.label"/></th>
		 					<td>
		 						<span class="alert-situation-created"></span>
		 						<div class="help-block"><fmt:message key='alert.situation.created.caption'/></div>
		 					</td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key="alert.nodeId.label"/></th>
		 					<td class="alert-situation-node"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key="alert.options.ageMinutes.heading"/></th>
		 					<td class="alert-situation-age"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key="alert.options.sourceIds.label"/></th>
		 					<td class="alert-situation-sources"></td>
		 				</tr>
		 				<tr class="notified">
		 					<th><fmt:message key="alert.situation.notified.label"/></th>
		 					<td>
		 						<span class="alert-situation-notified"></span>
		 						<div class="help-block"><fmt:message key='alert.situation.notified.caption'/></div>
		 					</td>
		 				</tr>
		 			</tbody>
		 		</table>
		 		<p class="alert-type alert-type-NodeStaleData"><fmt:message key='alerts.situation.resolve.caption'/></p>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-danger pull-left" id="alert-situation-resolve">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='alerts.action.resolve'/>
		 		</button>
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 	</div>
		</div>
	</div>
</div>
