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
			<fmt:param value="${fn:length(userAlerts)}"/>
		</fmt:message>
	</p>
	<c:if test="${not empty userAlerts}">
		<table class="table" id="node-data-alerts">
			<thead>
				<tr>
					<th><fmt:message key="alert.nodeId.label"/></th>
					<th></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${userAlerts}" var="alert">
					<tr class="alert-row" data-node-id="${userAlert.nodeId}" data-alert-id="${userAlert.id}">
						<td>${userAlert.nodeId}</td>
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
								<c:set var="alertTypeLabel">alert.type.${alertType}.label</c:set>
								<c:set var="alertTypeCaption">alert.type.${alertType}.caption</c:set>
								<option value="${alertType}" title="<fmt:message key='${alertTypeCaption}'/>">
									<fmt:message key='${alertTypeLabel}'/>
								</option>
							</c:forEach>
						</select>
					</div>
				</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="create-node-data-alert-sources"><fmt:message key='alert.sourceIds.label'/></label>
					<div class="col-sm-10">
						<input name="name" type="text" maxlength="255" class="form-control" id="create-node-data-alert-sources"/>
						<span class="help-block"><fmt:message key="alert.sourceIds.caption"/></span>
					</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-2 control-label" for="create-node-data-alert-status"><fmt:message key='alert.status.label'/></label>
					<div class="col-sm-10" id="create-node-data-alert-status">
						<c:forEach items="${alertStatuses}" var="alertStatus" varStatus="itr">
							<c:set var="alertStatusLabel">alert.status.${alertStatus}.label</c:set>
							<c:set var="alertStatusCaption">alert.status.${alertStatus}.caption</c:set>
							<label class="radio-inline">
								<input type="radio" name="status" value="${alertStatus}" 
									<c:if test='${itr.first}'>checked</c:if>
									title="<fmt:message key='${alertStatusCaption}'/>"/>
								<c:out value=" "/>
								<fmt:message key='${alertStatusLabel}'/>
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
