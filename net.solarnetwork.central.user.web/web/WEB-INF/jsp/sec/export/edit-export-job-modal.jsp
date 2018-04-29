<%--
	The .edit-config class attached to .modal activates "modal configuration edit form" mode.
 --%>
<form id="edit-datum-export-config-modal" class="modal fade edit-config" action="<c:url value='/u/sec/export/configs'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='export.datumExportConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='export.datumExportConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<%--
		 					Form field names correspond to model object property names.
		 				 --%>
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='export.datumExportConfig.name.placeholder'/>"
		 					maxlength="64" required="required"/>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.dataConfigurationId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="dataConfigurationId">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.destinationConfigurationId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="destinationConfigurationId">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.outputConfigurationId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="outputConfigurationId">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.scheduleKey.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control export-output-schedule-types" name="scheduleKey">
		 				</select>
		 			</div>
		 		</div>
		 	</div>
		 	<%--
		 		The .service-props-container class defines where dynamic setting form elements will be rendered.
		 	 --%>
		 	<div class="modal-body form-horizontal service-props-container hidden">
		 	</div>
		 	<%--
		 		The .delete-confirm class will be shown/hidden via the .hidden class to confirm the delete action.
		 	 --%>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='export.deleteConfiguration.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
			 	<%--
			 		The .delete-config button defines the button used to initiate the delete action.
			 	 --%>
		 		<button type="button" class="btn btn-danger pull-left delete-config hidden">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='export.deleteConfiguration.label'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
				<%--
					The submit button performs the create/update save action.
				 --%>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
 	<%--
 		The id field stores the primary key of the configuration to update, and must be cleared when
 		creating a new object.
 	 --%>
	<input type="hidden" name="id"/>
</form>
