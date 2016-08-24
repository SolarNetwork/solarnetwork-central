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
		 				<tr>
		 					<th><fmt:message key="alert.situation.info.label"/></th>
		 					<td class="alert-situation-info">
		 						<dl class="dl-horizontal">
		 							<dt><fmt:message key="alert.situation.info.nodeId.label"/></dt>
		 							<dd class="alert-situation-info-nodeId"></dd>
		 							
		 							<dt><fmt:message key="alert.situation.info.sourceId.label"/></dt>
		 							<dd class="alert-situation-info-sourceId"></dd>
		 						</dl>
		 					</td>
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
		 		<button type="button" class="btn btn-danger pull-left" id="alert-situation-resolve" data-csrf="${_csrf.token}">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='alerts.action.resolve'/>
		 		</button>
		 		<a href="#" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></a>
		 	</div>
		</div>
	</div>
</div>
