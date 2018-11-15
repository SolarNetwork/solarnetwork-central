<div id="preview-datum-import-job-modal" class="modal fade edit-config" tabindex="-1" role="dialog">
	<div class="modal-dialog" style="width: 90%;" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='import.job.preview.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal before">
		 		<p><fmt:message key='import.job.preview.intro'/></p>
		 		<table id="datum-import-preview-list-container" class="table">
					<thead>
						<tr>
							<th><fmt:message key='import.job.preview.datum.nodeId.label'/></th>
							<th><fmt:message key='import.job.preview.datum.sourceId.label'/></th>
							<th><fmt:message key='import.job.preview.datum.date.label'/></th>
							<th><fmt:message key='import.job.preview.datum.instantaneous.label'/></th>
							<th><fmt:message key='import.job.preview.datum.accumulating.label'/></th>
							<th><fmt:message key='import.job.preview.datum.status.label'/></th>
							<th><fmt:message key='import.job.preview.datum.tags.label'/></th>
						</tr>
						<tr class="template">
							<td data-tprop="nodeId"></td>
							<td data-tprop="sourceId"></td>
							<td data-tprop="created"></td>
							<td>
								<dl class="instantaneous-sample-container">
								</dl>
								<dl class="template hidden">
									<dt class="template" data-tprop="name"></dt>
									<dd class="template" data-tprop="value"></dd>
								</dl>
							</td>
							<td>
								<dl class="accumulating-sample-container">
								</dl>
								<dl class="template hidden">
									<dt class="template" data-tprop="name"></dt>
									<dd class="template" data-tprop="value"></dd>
								</dl>
							</td>
							<td>
								<dl class="status-sample-container">
								</dl>
								<dl class="template hidden">
									<dt class="template" data-tprop="name"></dt>
									<dd class="template" data-tprop="value"></dd>
								</dl>
							</td>
							<td class="tag-list"></td>
						</tr>
					</thead>
					<tbody class="list-container">
					</tbody>
		 		</table>
		 	</div>
		 	<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 	</div>
		 </div>
 	</div>
</div>
