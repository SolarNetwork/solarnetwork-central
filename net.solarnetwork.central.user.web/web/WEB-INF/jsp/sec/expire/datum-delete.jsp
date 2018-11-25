<section>
	<h2>
		<fmt:message key='datumDelete.header'/>
	</h2>
	<p class="intro">
		<fmt:message key='datumDelete.intro'/>
	</p>
	<form id="datum-delete-form" class="col-sm-9 form-horizontal" action="<c:url value='/u/sec/expire/datum-delete'/>" method="post">
		<div class="form-group">
			<label class="col-sm-2 control-label" for="datum-delete-node-ids">
				<fmt:message key='datumDelete.nodes.label'/>
			</label>
			<div class="col-sm-10">
				<input type="text" name="nodeIds" class="form-control" id="datum-delete-node-ids"
					placeholder="<fmt:message key='datumDelete.nodes.placeholder'/>">
		 		<span id="helpBlock" class="help-block"><fmt:message key='datumDelete.nodes.caption'/></span>
			</div>
		</div>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="datum-delete-source-ids">
				<fmt:message key='datumDelete.sources.label'/>
			</label>
			<div class="col-sm-10">
				<input type="text" name="sourceIds" class="form-control" id="datum-delete-source-ids"
					placeholder="<fmt:message key='datumDelete.sources.placeholder'/>">
		 		<span id="helpBlock" class="help-block"><fmt:message key='datumDelete.sources.caption'/></span>
			</div>
		</div>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="datum-delete-min-date">
				<fmt:message key='datumDelete.minDate.label'/>
			</label>
			<div class="col-sm-10">
				<input type="text" name="localStartDate" class="form-control" id="datum-delete-min-date"
					placeholder="<fmt:message key='datumDelete.minDate.placeholder'/>" required>
		 		<span id="helpBlock" class="help-block"><fmt:message key='datumDelete.minDate.caption'/></span>
			</div>
		</div>
		<div class="form-group">
			<label class="col-sm-2 control-label" for="datum-delete-max-date">
				<fmt:message key='datumDelete.maxDate.label'/>
			</label>
			<div class="col-sm-10">
				<input type="text" name="localEndDate" class="form-control" id="datum-delete-max-date"
					placeholder="<fmt:message key='datumDelete.maxDate.placeholder'/>" required>
		 		<span id="helpBlock" class="help-block"><fmt:message key='datumDelete.maxDate.caption'/></span>
			</div>
		</div>
		<div class="form-group">
			<div class="col-sm-offset-2 col-sm-10">
				<button type="submit" class="btn btn-primary"><fmt:message key='datumDelete.action.preview'/></button>
			</div>
		</div>
	 	<sec:csrfInput/>
	</form>
</section>

<form id="datum-delete-preview-modal" class="modal fade edit-config" action="<c:url value='/u/sec/expire/datum-delete/confirm'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='datumDelete.preview.title'/></h4>
			</div>
		 	<div class="modal-body">
		 		<p><fmt:message key='datumDelete.preview.intro'/></p>
		 		<div class="progress waiting">
					<div class="progress-bar progress-bar-striped active" role="progressbar" style="width: 100%"></div>
				</div>
				<p class="waiting"><fmt:message key='onemomentplease.label'/></p>
		 		<table class="expire-preview-counts tally table ready hidden">
		 			<tbody>
		 				<tr>
		 					<th><fmt:message key='expire.datumMonthlyCount.label'/></th><td data-tprop="datumMonthlyCountDisplay"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key='expire.datumDailyCount.label'/></th><td data-tprop="datumDailyCountDisplay"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key='expire.datumHourlyCount.label'/></th><td data-tprop="datumHourlyCountDisplay"></td>
		 				</tr>
		 				<tr>
		 					<th><fmt:message key='expire.datumCount.label'/></th><td data-tprop="datumCountDisplay"></td>
		 				</tr>
		 			</tbody>
		 			<tfoot>
		 				<tr>
		 					<th><fmt:message key='expire.datumTotalCount.label'/></th><th data-tprop="datumTotalCountDisplay"></th>
		 				</tr>
		 			</tfoot>
		 		</table>
		 	</div>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='datumDelete.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
		 		<button type="button" class="btn btn-danger pull-left delete-config">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='datumDelete.action.delete'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 	</div>
		 </div>
 	</div>
 	<input type="hidden" name="nodeIds">
 	<input type="hidden" name="sourceIds">
 	<input type="hidden" name="localStartDate">
 	<input type="hidden" name="localEndDate">
 	<sec:csrfInput/>
</form>
