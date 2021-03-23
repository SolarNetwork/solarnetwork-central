<a id="top"></a>

<p class="intro">
	<fmt:message key='import.intro'/>
</p>

<section id="datum-import-jobs">
	<h2>
		<fmt:message key='import.jobList.header'/>
		<a href="#edit-datum-import-job-modal" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='import.jobList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='import.jobList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table id="datum-import-job-list-container" class="table table-body-borderless configs hidden">
		<thead>
			<tr>
				<th><fmt:message key='import.job.id.label'/></th>
				<th><fmt:message key='import.job.groupKey.label'/></th>
				<th><fmt:message key='import.job.name.label'/></th>
				<th><fmt:message key='import.job.timeZoneId.label'/></th>
				<th><fmt:message key='import.job.batchSize.label'/></th>
				<th><fmt:message key='import.job.state.label'/></th>
				<th><fmt:message key='import.job.percentComplete.label'/></th>
				<th></th>
			</tr>
		</thead>
		<tbody class="template">
			<tr>
				<td>
					<a href="#" class="edit-link" data-tprop="shortId" data-edit-modal="#edit-datum-import-job-modal"></a>
					<div data-tprop="submitDateDisplay"></div>
				</td>
				<td data-tprop="groupKeyDisplay"></td>
				<td data-tprop="name"></td>
				<td data-tprop="timeZoneId"></td>
				<td data-tprop="batchSize"></td>
				<td>
					<span data-tprop="state"></span>
					<div class="complete hidden" data-tprop="completionDateDisplay"></div>
				</td>
				<td>
					<p>
						<span class="label label-danger success-error hidden"><fmt:message key='import.job.success.error'/></span>
						<span class="label label-success success-ok hidden"><fmt:message key='import.job.success.ok'/></span>
					</p>
					<div class="progress hidden">
						<div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="min-width: 2em;">
							<span data-tprop="progressAmount">0</span>%
						</div>
					</div>
					<div class="running"  style="font-size: 86%">
						<p class="running hidden"><b><fmt:message key='import.job.loadedCount.label'/>:</b> <span data-tprop="loadedCount"></span></p>
						<p class="running hidden"><b><fmt:message key='import.job.duration.label'/>:</b> <span data-tprop="duration"></span></p>
						<p class="running hidden"><b><fmt:message key='import.job.eta.label'/>:</b> <span data-tprop="eta"></span></p>
					</div>
					<p class="success-error hidden" data-tprop="messageHtml"></p>
					<p class="complete hidden">
						<fmt:message key='import.job.loadedCount.label'/>
						${' '}
						<span data-tprop="loadedCount"></span>
						${' '}
						<fmt:message key='in.label'/>
						${' '}
						<span data-tprop="duration"></span>
					</p>
				</td>
				<td class="col-sm-2 text-right">
					<div class="btn-group preview">
						<button type="button" class="btn btn-small btn-default action-link"
							 data-action-modal="#preview-datum-import-job-modal"><fmt:message key='import.job.action.preview'/></button>
						<button type="button" class="btn btn-small btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">
							<span class="caret"></span>
							<span class="sr-only"><fmt:message key='toggle.dropdown.label'/></span>
						</button>
						<ul class="dropdown-menu dropdown-menu-right" role="menu">
							<li>
								<a href="#" class="action-link" data-action-modal="#confirm-datum-import-job-modal"><fmt:message key="import.job.action.confirm"/></a>
							</li>
						</ul>
					</div>
				</td>
			</tr>
			<tr class="rule">
				<td colspan="7">
					<dl class="service-props-container hbox" style="font-size: 86%">
					</dl>
					<dl class="service-props-template hidden">
						<dt class="template" data-tprop="serviceProperties.name"></dt>
						<dd class="template" data-tprop="serviceProperties.value"></dd>
					</dl>
				</td>
			</tr>
		</tbody>
	</table>
</section>

<%-- Modal forms --%>

<jsp:include page="edit-datum-import-job-modal.jsp"/>
<jsp:include page="preview-datum-import-job-modal.jsp"/>
<jsp:include page="confirm-datum-import-job-modal.jsp"/>

<%-- Setting templates --%>

<div id="import-setting-templates" class="hidden">
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
