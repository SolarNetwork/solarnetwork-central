<form id="edit-export-output-config-modal" class="modal fade" action="<c:url value='/u/sec/export/configs/output'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='export.outputConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='export.outputConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.outputConfig.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='export.outputConfig.name.placeholder'/>"
		 					maxlength="64" required="required"/>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.outputConfig.type.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control export-output-services" name="serviceIdentifier" required="required">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.outputConfig.compression.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control export-output-compression-types" name="compressionTypeKey" required="required">
		 				</select>
		 			</div>
		 		</div>
		 	</div>
		 	<div class="modal-body form-horizontal service-props-container hidden">
		 	</div>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='export.deleteConfiguration.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-danger pull-left delete-config hidden">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='export.deleteConfiguration.label'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
	<input type="hidden" name="id"/>
 	<sec:csrfInput/>
</form>
