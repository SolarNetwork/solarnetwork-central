<a id="top"></a>

<p class="intro">
	<fmt:message key='export.intro'/>
</p>

<div class="datum-export-getstarted hidden alert alert-info alert-dismissible" role="alert">
	<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
	<strong><fmt:message key='export.getstarted.title'/></strong>
	<fmt:message key='export.getstarted.intro'/>
</div>

<section id="datum-export-configs">
	<h2>
		<fmt:message key='export.datumExportList.header'/>
		<a href="#edit-datum-export-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.datumExportList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.datumExportList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<div id="datum-export-list-container" class="table configs hidden">
		<ol class="list-container">
		</ol>
		<ol class="hidden">
			<li class="template">
				<p>
					<fmt:message key='export.datumExportList.exportListItem'/>
				</p>
			</li>
		</ol>
	</div>
</section>

<section id="adhoc-datum-export-configs">
	<h2>
		<fmt:message key='export.adhocDatumExportList.header'/>
		<a href="#edit-adhoc-datum-export-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.adhocDatumExportList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.adhocDatumExportList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="adhoc-datum-export-list-container" class="table configs hidden">
		<thead class="statuses"
			data-label-status-q="<fmt:message key='export.adhocConfig.status.q.label'/>"
			data-label-status-p="<fmt:message key='export.adhocConfig.status.p.label'/>"
			data-label-status-e="<fmt:message key='export.adhocConfig.status.e.label'/>"
			data-label-status-c="<fmt:message key='export.adhocConfig.status.c.label'/>"
			>
			<tr>
				<th><fmt:message key='export.adhocConfig.name.label'/></th>
				<th><fmt:message key='export.adhocConfig.nodes.label'/></th>
				<th><fmt:message key='export.adhocConfig.sources.label'/></th>
				<th><fmt:message key='export.adhocConfig.aggregation.label'/></th>
				<th><fmt:message key='export.adhocConfig.status.label'/></th>
			</tr>
			<tr class="template">
				<td data-tprop="name"></td>
				<td data-tprop="nodes"></td>
				<td data-tprop="sources"></td>
				<td data-tprop="aggregation"></td>
				<td>
					<span class="label status" data-tprop="status"></span>
					<p class="completed hidden">
						<fmt:message key='export.adhocConfig.status.completed.label'/>
						<span> </span>
						<span data-tprop="completed"></span>
						<span> </span>
						<small class="help-block" data-tprop="message"></small>
					</p>
				</td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<section id="export-data-configs">
	<h2>
		<fmt:message key='export.dataConfigList.header'/>
		<a href="#edit-export-data-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.dataConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.dataConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="export-data-config-list-container" class="table configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='export.dataConfig.name.label'/></th>
				<th><fmt:message key='export.dataConfig.nodes.label'/></th>
				<th><fmt:message key='export.dataConfig.sources.label'/></th>
				<th><fmt:message key='export.dataConfig.aggregation.label'/></th>
			</tr>
			<%--
				The .template class defines the HTML structure for one object.
			 --%>
			<tr class="template">
				<td><a href="#" class="edit-link" data-tprop="name" data-edit-modal="#edit-export-data-config-modal"></a></td>
				<td data-tprop="nodes"></td>
				<td data-tprop="sources"></td>
				<td data-tprop="aggregation"></td>
			</tr>
		</thead>
		<%--
			The .list-container class defines where objects will be rendered into HTML.
		 --%>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<section id="export-output-configs">
	<h2>
		<fmt:message key='export.outputConfigList.header'/>
		<a href="#edit-export-output-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.outputConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.outputConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="export-output-config-list-container" class="table configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='export.outputConfig.name.label'/></th>
				<th><fmt:message key='export.outputConfig.type.label'/></th>
				<th><fmt:message key='export.outputConfig.compression.label'/></th>
			</tr>
			<tr class="template">
				<%--
					Within a .template, data-tprop attributes correspond to model object property names,
					whose text content will be replaced by the model object property value.
					
					The a.edit-link element will have its click event ignored; instead a click
					handler on the .list-container should be registered, and use the event.target
					property to verify the click happened
				 --%>
				<td><a href="#" class="edit-link" data-tprop="name" data-edit-modal="#edit-export-output-config-modal"></a></td>
				<td data-tprop="type"></td>
				<td data-tprop="compression"></td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<section id="export-destination-configs">
	<h2>
		<fmt:message key='export.destinationConfigList.header'/>
		<a href="#edit-export-destination-config-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.destinationConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.destinationConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="export-destination-config-list-container" class="table configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='export.destinationConfig.name.label'/></th>
				<th><fmt:message key='export.destinationConfig.type.label'/></th>
				<th><fmt:message key='export.serviceProps.label'/></th>
			</tr>
			<tr class="template">
				<td><a href="#" class="edit-link" data-tprop="name" data-edit-modal="#edit-export-destination-config-modal"></a></td>
				<td data-tprop="type"></td>
				<td>
					<%--
						The .service-props-container here serves as a nested template for dynamic service properties.
					 --%>
					<dl class="service-props-container">
					</dl>
					<dl class="service-props-template hidden">
						<dt class="template" data-tprop="serviceProperties.name"></dt>
						<dd class="template" data-tprop="serviceProperties.value"></dd>
					</dl>
				</td>
			</tr>
		</thead>
		<tbody class="list-container">
		</tbody>
	</table>
</section>

<%-- Modal forms --%>

<jsp:include page="edit-export-job-modal.jsp"/>
<jsp:include page="edit-export-job-date-modal.jsp"/>
<jsp:include page="edit-data-modal.jsp"/>
<jsp:include page="edit-destination-modal.jsp"/>
<jsp:include page="edit-output-modal.jsp"/>
<jsp:include page="edit-adhoc-export-job-modal.jsp"/>

<%-- Setting templates --%>

<div id="export-setting-templates" class="hidden">
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
				data-toggle="setting-toggle" aria-pressed="false"
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
