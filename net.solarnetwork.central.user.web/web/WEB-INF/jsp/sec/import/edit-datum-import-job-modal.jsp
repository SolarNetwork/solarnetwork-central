<form id="edit-datum-import-job-modal" class="modal fade edit-config import" action="<c:url value='/u/sec/import/jobs'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" style="width: 66%;" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='import.job.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal before">
		 		<p>
		 			<fmt:message key='import.job.edit.intro'/>
		 			<span class="update"><fmt:message key='import.job.update.intro'/></span>
		 		</p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='import.job.name.placeholder'/>"
		 					maxlength="64" required="required">
			 		</div>
		 		</div>
		 		<div class="form-group create">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.file.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="file" class="form-control" name="data" required="required">
		 				<span id="helpBlock" class="help-block"><fmt:message key='import.job.file.caption'/></span>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.timeZoneId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="timeZoneId" 
		 					placeholder="<fmt:message key='import.job.timeZoneId.placeholder'/>"
		 					maxlength="64">
		 				<span id="helpBlock" class="help-block"><fmt:message key='import.job.timeZoneId.caption'/></span>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.batchSize.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="number" class="form-control" name="batchSize" min="0"
		 					placeholder="<fmt:message key='import.job.batchSize.placeholder'/>">
		 				<span id="helpBlock" class="help-block"><fmt:message key='import.job.batchSize.caption'/></span>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.outputConfig.type.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control" name="serviceIdentifier" required="required">
		 				</select>
		 			</div>
		 		</div>
		 	</div>
		 	<div class="modal-body form-horizontal service-props-container before hidden">
		 	</div>
		 	<div class="modal-body upload hidden">
		 		<p><fmt:message key='import.job.upload.progress.intro'/></p>
				<div class="progress">
					<div class="progress-bar progress-bar-striped active" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;">
						<span class="amount">0</span>%
					</div>
				</div>
			</div>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='import.job.delete.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-danger pull-left delete-config">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='import.job.action.delete'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
 	<input type="hidden" name="id">
</form>
