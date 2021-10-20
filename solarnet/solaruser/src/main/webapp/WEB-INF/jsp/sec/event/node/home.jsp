<a id="top"></a>

<p class="intro">
	<fmt:message key='node-event.intro'/>
</p>

<section id="node-event-hooks">
	<h2>
		<fmt:message key='node-event.hookList.header'/>
		<a href="#edit-node-event-hook-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='node-event.hook.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='node-event.hookList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="node-event-hook-list-container" class="table configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='node-event.hookConfig.name.label'/></th>
				<th><fmt:message key='node-event.hookConfig.topic.label'/></th>
				<th><fmt:message key='node-event.hookConfig.nodes.label'/></th>
				<th><fmt:message key='node-event.hookConfig.sources.label'/></th>
				<th><fmt:message key='node-event.serviceProperties.label'/></th>
			</tr>
			<tr class="template">
				<td><a href="#" class="edit-link" data-tprop="name" data-edit-modal="#edit-node-event-hook-modal"></a></td>
				<td><span data-tprop="topicName"></span><br><span class="label label-default" data-tprop="topic"></span></td>
				<td data-tprop="nodes"></td>
				<td data-tprop="sources"></td>
				<td>
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

<jsp:include page="edit-hook-modal.jsp"/>

<%-- Setting templates --%>

<div id="node-event-setting-templates" class="hidden">
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
