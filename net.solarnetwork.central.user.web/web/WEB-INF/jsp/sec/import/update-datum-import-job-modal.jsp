<%--
	The .update-config class attached to .modal activates "modal configuration edit form" mode.
 --%>
<form id="update-datum-import-job-modal" class="modal fade edit-config import" action="<c:url value='/u/sec/import/jobs'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='import.job.update.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal before">
		 		<p><fmt:message key='import.job.update.intro'/></p>
		 		<!--  TODO -->
		 	</div>
		 	<%--
		 		The .delete-confirm class will be shown/hidden via the .hidden class to confirm the delete action.
		 	 --%>
		 	<div class="modal-body delete-confirm hidden">
		 		<p class="alert alert-danger">
		 			<fmt:message key='import.job.delete.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
			 	<%--
			 		The .delete-config button defines the button used to initiate the delete action.
			 	 --%>
		 		<button type="button" class="btn btn-danger pull-left delete-config">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='import.job.action.delete'/>
		 		</button>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
				<%--
					The submit button performs the create/update save action.
				 --%>
		 		<button type="submit" class="btn btn-primary"><fmt:message key='save.label'/></button>
		 	</div>
		 </div>
 	</div>
	<input type="hidden" name="id"/>
</form>
