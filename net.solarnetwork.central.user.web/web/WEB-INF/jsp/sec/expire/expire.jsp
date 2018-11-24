<a id="top"></a>

<p class="intro">
	<fmt:message key='expire.intro'/>
</p>

<div class="datum-expire-getstarted hidden alert alert-info alert-dismissible" role="alert">
	<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
	<strong><fmt:message key='expire.getstarted.title'/></strong>
	<fmt:message key='expire.getstarted.intro'/>
</div>

<section id="expire-data-configs">
	<h2>
		<fmt:message key='expire.dataConfigList.header'/>
		<a href="#edit-expire-data-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='expire.dataConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='expire.dataConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="expire-data-config-list-container" class="table configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='expire.dataConfig.name.label'/></th>
				<th><fmt:message key='expire.dataConfig.nodes.label'/></th>
				<th><fmt:message key='expire.dataConfig.sources.label'/></th>
				<th><fmt:message key='expire.dataConfig.aggregation.label'/></th>
				<th><fmt:message key='expire.dataConfig.expireDays.label'/></th>
				<th><fmt:message key='expire.dataConfig.active.label'/></th>
				<th><fmt:message key='expire.dataConfig.preview.label'/></th>
			</tr>
			<%--
				The .template class defines the HTML structure for one object.
			 --%>
			<tr class="template">
				<td><a href="#" class="edit-link" data-tprop="name" data-edit-modal="#edit-expire-data-config-modal"></a></td>
				<td data-tprop="nodes"></td>
				<td data-tprop="sources"></td>
				<td data-tprop="aggregation"></td>
				<td data-tprop="expireDays"></td>
				<td><span class="label label-success" data-tprop="active"></span></td>
				<td><a href="#"><span class="glyphicon glyphicon-eye-open action-link" aria-hidden="true" 
					data-action-modal="#expire-data-config-preview-modal"></span></a>
			</tr>
		</thead>
		<%--
			The .list-container class defines where objects will be rendered into HTML.
		 --%>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<c:if test="${not empty datumDeleteBiz}">
	<jsp:include page="datum-delete.jsp"/>
</c:if>

<%-- Modal forms --%>

<jsp:include page="edit-data-modal.jsp"/>

<div id="expire-data-config-preview-modal" class="modal config-action fade" role="dialog">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='expire.dataConfig.preview.title'/></h4>
			</div>
		 	<div class="modal-body">
		 		<p><fmt:message key='expire.dataConfig.preview.intro'/></p>
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
		 	<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
		 	</div>
		 </div>
 	</div>
</div>

<%-- Setting templates --%>

<div id="expire-setting-templates" class="hidden">
	<%--
		The setting-type data property must match a setting UID + any options as |opt1,opt2.
		
		Supported options:
			secureTextEntry : for password fields
	 --%>
	<div class="form-group template" data-setting-type="net.solarnetwork.settings.TextFieldSettingSpecifier">
		<label class="col-sm-3 control-label" data-tprop="name">
			${' '}
		</label>
		<div class="col-sm-7">
			<%--
				The .setting-form-element class must be attached to the setting form field that will hold the value to upload.
			 --%>
			<input type="text" class="form-control setting-form-element" name="__unnamed" maxlength="255">
		</div>
		<div class="col-sm-1 form-control-static">
			<a tabindex="-1" title="<fmt:message key='settings.info.label'/>" class="setting-help" role="button" data-toggle="popover" data-trigger="focus" data-html="true" data-container="body">
				<span class="glyphicon glyphicon glyphicon-info-sign" aria-hidden="true"></span>
			</a>
		</div>
	</div>
	<div class="form-group template" data-setting-type="net.solarnetwork.settings.TextFieldSettingSpecifier|secureTextEntry">
		<label class="col-sm-3 control-label" data-tprop="name">
			${' '}
		</label>
		<div class="col-sm-7">
			<input type="password" placeholder="<fmt:message key='settings.secureTextEntry.placeholder'/>" class="form-control setting-form-element" name="__unnamed" maxlength="255">
		</div>
		<div class="col-sm-1 form-control-static">
			<a tabindex="-1" title="<fmt:message key='settings.info.label'/>" class="setting-help" role="button" data-toggle="popover" data-trigger="focus" data-html="true" data-container="body">
				<span class="glyphicon glyphicon-info-sign" aria-hidden="true"></span>
			</a>
		</div>
	</div>	
	<div class="form-group template" data-setting-type="net.solarnetwork.settings.ToggleSettingSpecifier">
		<label class="col-sm-3 control-label" data-tprop="name">
			${' '}
		</label>
		<div class="col-sm-7">
			<button type="button" name="__unnamed" class="form-control setting-form-element toggle btn btn-default" 
				data-toggle="setting-toggle" aria-pressed="false" autocomplete="off"
				data-on-text="<fmt:message key='settings.toggle.enabled'/>"
				data-off-text="<fmt:message key='settings.toggle.disabled'/>">
				<fmt:message key='settings.toggle.disabled'/>
			</button>
		</div>
		<div class="col-sm-1 form-control-static">
			<a tabindex="-1" title="<fmt:message key='settings.info.label'/>" class="setting-help" role="button" data-toggle="popover" data-trigger="focus" data-html="true" data-container="body">
				<span class="glyphicon glyphicon glyphicon-info-sign" aria-hidden="true"></span>
			</a>
		</div>
	</div>
</div>
