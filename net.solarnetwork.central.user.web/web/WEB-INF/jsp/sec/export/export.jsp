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
		<a href="#create-datum-export-config" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.datumExportList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.datumExportList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<ol class="table configs" id="datum-export-list-container">
		<li class="template">
			<p>
				<fmt:message key='export.datumExportList.exportListItem'/>
			</p>
		</li>
	</ol>
</section>

<section id="export-data-configs">
	<h2>
		<fmt:message key='export.dataConfigList.header'/>
		<a href="#create-export-data-config" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.dataConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.dataConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table class="table configs" id="export-data-config-list-container hidden">
		<thead>
			<tr>
				<th><fmt:message key="export.dataConfig.name.label"/></th>
				<th><fmt:message key="export.dataConfig.nodes.label"/></th>
				<th><fmt:message key="export.dataConfig.sources.label"/></th>
				<th><fmt:message key="export.dataConfig.aggregation.label"/></th>
			</tr>
			<tr class="template">
				<td><a href="#" data-tprop="name"></a></td>
				<td data-tprop="nodes"></td>
				<td data-tprop="sources"></td>
				<td data-tprop="aggregation"></td>
			</tr>
		</thead>
		<tbody>
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
	<table class="table configs" id="export-output-config-list-container hidden">
		<thead>
			<tr>
				<th><fmt:message key="export.outputConfig.name.label"/></th>
				<th><fmt:message key="export.outputConfig.type.label"/></th>
				<th><fmt:message key="export.outputConfig.filename.label"/></th>
				<th><fmt:message key="export.outputConfig.compressed.label"/></th>
			</tr>
			<tr class="template">
				<td><a href="#" data-tprop="name"></a></td>
				<td data-tprop="type"></td>
				<td data-tprop="filename"></td>
				<td data-tprop="compressed"></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>

<section id="export-dest-configs">
	<h2>
		<fmt:message key='export.destConfigList.header'/>
		<a href="#create-export-dest-config" class="btn btn-primary pull-right" data-toggle="modal" 
			title="<fmt:message key='export.destConfigList.action.create'/>">
			<i class="glyphicon glyphicon-plus"></i>
		</a>
	</h2>
	<p class="intro">
		<fmt:message key='export.destConfigList.intro'>
			<fmt:param value='0'/>
		</fmt:message>
	</p>
	<table class="table configs" id="export-dest-config-list-container hidden">
		<thead>
			<tr>
				<th><fmt:message key="export.destConfig.name.label"/></th>
				<th><fmt:message key="export.destConfig.type.label"/></th>
			</tr>
			<tr class="template">
				<td><a href="#" data-tprop="name"></a></td>
				<td data-tprop="type"></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>

<%-- Modal forms --%>

<jsp:include page="edit-output-modal.jsp"/>
<%--@include file="/WEB-INF/jsp/sec/export/create-output-modal.jsp" --%>
