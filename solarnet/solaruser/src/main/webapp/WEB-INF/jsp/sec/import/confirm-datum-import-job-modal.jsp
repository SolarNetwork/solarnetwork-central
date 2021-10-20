<form id="confirm-datum-import-job-modal" class="modal fade edit-config import" action="<c:url value='/u/sec/import/jobs/{id}/confirm'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='import.job.confirm.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal before">
		 		<p><fmt:message key='import.job.confirm.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<p class="form-control-static" data-tprop="name"></p>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.timeZoneId.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<p class="form-control-static" data-tprop="timeZoneId"></p>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='import.job.batchSize.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<p class="form-control-static" data-tprop="batchSize"></p>
			 		</div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="submit" class="btn btn-warning"><fmt:message key='import.job.action.confirm'/></button>
		 	</div>
		 </div>
 	</div>
 	<input type="hidden" name="id">
</form>
