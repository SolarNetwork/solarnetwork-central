<form id="edit-adhoc-datum-export-config-modal" class="modal fade edit-config export" action="<c:url value='/u/sec/export/adhocRef'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='export.adhocDatumExportConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='export.adhocDatumExportConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.adhocDatumExportConfig.name.label'/>
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
		 		
		 		<!-- Data filter config -->
				<div class="form-group">
					<label class="col-sm-3 control-label">
						<fmt:message key='export.dataConfig.nodes.label'/>
						${' '}
					</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" name="dataConfiguration.datumFilter.nodeIds" 
							placeholder="<fmt:message key='export.dataConfig.nodes.placeholder'/>"
							maxlength="128">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-3 control-label">
						<fmt:message key='export.dataConfig.sources.label'/>
						${' '}
					</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" name="dataConfiguration.datumFilter.sourceIds" 
							placeholder="<fmt:message key='export.dataConfig.sources.placeholder'/>"
							maxlength="256">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-3 control-label">
						<fmt:message key='export.dataConfig.aggregation.label'/>
						${' '}
					</label>
					<div class="col-sm-8">
						<select class="form-control export-data-aggregation-types" name="dataConfiguration.datumFilter.aggregationKey">
						</select>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-3 control-label" for="adhoc-datum-export-min-date">
						<fmt:message key='export.adhocDatumExportConfig.minDate.label'/>
					</label>
					<div class="col-sm-8">
						<input type="text" name="dataConfiguration.datumFilter.startDate" class="form-control" id="adhoc-datum-export-min-date"
							placeholder="<fmt:message key='datumDelete.minDate.placeholder'/>" required>
				 		<span id="helpBlock" class="help-block"><fmt:message key='export.adhocDatumExportConfig.minDate.caption'/></span>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-3 control-label" for="adhoc-datum-export-max-date">
						<fmt:message key='export.adhocDatumExportConfig.maxDate.label'/>
					</label>
					<div class="col-sm-8">
						<input type="text" name="dataConfiguration.datumFilter.endDate" class="form-control" id="adhoc-datum-export-max-date"
							placeholder="<fmt:message key='export.adhocDatumExportConfig.maxDate.placeholder'/>" required>
				 		<span id="helpBlock" class="help-block"><fmt:message key='export.adhocDatumExportConfig.maxDate.caption'/></span>
					</div>
				</div>

				<!-- Destination, format service config -->
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.destinationConfigurationId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="destinationConfigurationId" required>
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfig.outputConfigurationId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="outputConfigurationId" required>
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
