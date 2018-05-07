<%--
	The .edit-config class attached to .modal activates "modal configuration edit form" mode.
 --%>
<form id="edit-datum-export-date-modal" class="modal fade edit-config" action="<c:url value='/u/sec/export/configs/{id}/date'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='export.datumExportConfigDate.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='export.datumExportConfigDate.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfigDate.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="name" readonly>
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='export.datumExportConfigDate.date.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="startingExportDate" 
		 					placeholder="<fmt:message key='export.datumExportConfigDate.date.placeholder'/>"
		 					maxlength="16" required="required">
			 		</div>
		 		</div>
		 	</div>
		 	<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
	<input type="hidden" name="id"/>
</form>
