<form id="edit-node-event-hook-modal" class="modal fade edit-config node-event" action="<c:url value='/u/sec/event/node/hooks'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='node-event.hookConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='node-event.hookConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='node-event.hookConfig.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='node-event.hookConfig.name.placeholder'/>"
		 					maxlength="64" required="required"/>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='node-event.hookConfig.topic.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control node-event-topic-types" name="topic" required="required">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='node-event.hookConfig.nodes.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="nodeIds" 
		 					placeholder="<fmt:message key='node-event.hookConfig.nodes.placeholder'/>"
		 					maxlength="128">
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='node-event.hookConfig.sources.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="sourceIds" 
		 					placeholder="<fmt:message key='node-event.hookConfig.sources.placeholder'/>"
		 					maxlength="256">
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='node-event.hookConfig.service.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="serviceIdentifier" required="required">
		 				</select>
		 			</div>
		 		</div>
		 	</div>
		 	<div class="modal-body form-horizontal service-props-container hidden">
		 	</div>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='node-event.hookConfig.delete.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-danger pull-left delete-config hidden">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='node-event.hook.action.delete'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
	<input type="hidden" name="id"/>
</form>
