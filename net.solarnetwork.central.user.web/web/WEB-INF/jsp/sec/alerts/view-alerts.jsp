<%--
	Input parameters:
	
		nodeDataAlerts 		- collection of UserAuth objects of type NodeStaleData
		nodeDataAlertTypes 	- collection of UserAlertType that represent node data alerts
		userNodes      		- collection of UserNode objects
 --%>
<p class="intro">
	<fmt:message key='alerts.intro'/>
</p>

<section id="node-data-alerts">
	<h2>
		<fmt:message key='alerts.node.data.header'/>
		<button type="button" id="add-node-data-button" class="btn btn-primary pull-right" data-target="#create-node-data-alert-modal" data-toggle="modal">
			<i class="glyphicon glyphicon-plus"></i> <fmt:message key='alerts.action.create'/>
		</button>
	</h2>
	<p class="intro">
		<fmt:message key='alerts.node.data.intro'>
			<fmt:param value="${fn:length(nodeDataAlerts)}"/>
		</fmt:message>
	</p>
	<c:if test="${not empty nodeDataAlerts}">
		<table class="table" id="node-data-alerts">
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
					<tr class="alert-row" data-node-id="${alert.nodeId}" data-alert-id="${alert.id}">
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
							<c:if test="${not empty alert.options.sourceIds}">
								<c:forEach items="${alert.options.sourceIds}" var="source" varStatus="itr">
									<c:if test="${not itr.first}">, </c:if>
									${source}
								</c:forEach>
							</c:if>
						</td>
						<td>
							<c:if test="${alert.options.age != null}">
								<fmt:formatNumber type="number" maxFractionDigits="0">${alert.options.age / 60}</fmt:formatNumber>
							</c:if>
						</td>
						<td>
							<span class="label${alert.status eq 'Active' 
								? ' label-success' : alert.status eq 'Disabled' 
								? ' label-default' : ' label-primary'}">
								<fmt:message key='alert.status.${alert.status}.label'/>
							</span>
						</td>
						<td>
						
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:if>
</section>

<%-- Modal views --%>

<form id="create-node-data-alert-modal" class="modal fade alert-form" action="<c:url value='/u/sec/alerts/add'/>" method="post">
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
						<input name='options["ageMinutes"]' type="number" min="10" required="required" value="30" class="form-control col-sm-3" id="create-node-data-alert-sources"/>
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
		 			<fmt:message key='alerts.action.create'/>
		 		</button>
		 	</div>
		</div>
	</div>
	<input type="hidden" name="type" value="NodeStaleData"/><%-- At the momemnt, this is the only supported type --%>
</form>
