<form id="edit-export-destination-config-modal" class="modal fade" action="<c:url value='/u/sec/export/configs/destination'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='export.destinationConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='export.destinationConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.destinationConfig.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='export.destinationConfig.name.placeholder'/>"
		 					maxlength="64" required="required"/>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.destinationConfig.type.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control export-destination-services" name="serviceIdentifier" required="required">
		 				</select>
		 			</div>
		 		</div>
		 	</div>
		 	<div class="modal-body form-horizontal service-props-container hidden">
		 	</div>
		 	<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<input type="hidden" name="id"/>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
 	<sec:csrfInput/>
</form>
