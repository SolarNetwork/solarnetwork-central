<%--
	Input parameters:
	
		userAlerts - collection of UserAuth objects
 --%>
<p class="intro">
	<fmt:message key='alerts.intro'/>
</p>

<section id="node-data-alerts">
	<h2>
		<fmt:message key='alerts.node.data.header'/>
		<button type="button" id="add-node-data-button" class="btn btn-primary pull-right" data-target="#add-node-data-modal" data-toggle="modal">
			<i class="glyphicon glyphicon-plus"></i> <fmt:message key='alerts.action.add'/>
		</button>
	</h2>
	<p class="intro">
		<fmt:message key='alerts.node.data.intro'>
			<fmt:param value="${fn:length(userAlerts)}"/>
		</fmt:message>
	</p>
	<c:if test="${fn:length(userAlerts) > 0}">
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
