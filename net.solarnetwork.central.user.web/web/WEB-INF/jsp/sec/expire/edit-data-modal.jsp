<%--
	The .edit-config class attached to .modal activates "modal configuration edit form" mode.
 --%>
<form id="edit-expire-data-config-modal" class="modal fade edit-config" action="<c:url value='/u/sec/expire/configs/data'/>" method="post" tabindex="-1" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='expire.dataConfig.edit.title'/></h4>
			</div>
		 	<div class="modal-body form-horizontal">
		 		<p><fmt:message key='expire.dataConfig.edit.intro'/></p>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='expire.dataConfig.name.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<%--
		 					Form field names correspond to model object property names.
		 				 --%>
		 				<input type="text" class="form-control" name="name" 
		 					placeholder="<fmt:message key='expire.dataConfig.name.placeholder'/>"
		 					maxlength="64" required="required">
			 		</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='expire.dataConfig.nodes.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="datumFilter.nodeIds" 
		 					placeholder="<fmt:message key='expire.dataConfig.nodes.placeholder'/>"
		 					maxlength="128">
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='expire.dataConfig.sources.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="text" class="form-control" name="datumFilter.sourceIds" 
		 					placeholder="<fmt:message key='expire.dataConfig.sources.placeholder'/>"
		 					maxlength="256">
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='expire.dataConfig.aggregation.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<select class="form-control expire-data-aggregation-types" name="datumFilter.aggregationKey">
		 				</select>
		 			</div>
		 		</div>
		 		<div class="form-group">
		 			<label class="col-sm-3 control-label">
		 				<fmt:message key='expire.dataConfig.expireDays.label'/>
		 				${' '}
		 			</label>
		 			<div class="col-sm-8">
		 				<input type="number" class="form-control" name="expireDays" 
		 					placeholder="<fmt:message key='expire.dataConfig.expireDays.placeholder'/>"
		 					min="1" max="36500" required>
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
		 			<fmt:message key='expire.deleteConfiguration.confirm.intro'/>
		 		</p>
		 	</div>
		 	<div class="modal-footer">
			 	<%--
			 		The .delete-config button defines the button used to initiate the delete action.
			 	 --%>
		 		<button type="button" class="btn btn-danger pull-left delete-config hidden">
		 			<span class="glyphicon glyphicon-trash"></span>
		 			<fmt:message key='expire.deleteConfiguration.label'/>
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
 	<input type="hidden" name="serviceIdentifier" value="net.solarnetwork.central.user.expire.standard.DefaultUserExpireDataFilterService"/>
	<input type="hidden" name="id"/>
</form>
