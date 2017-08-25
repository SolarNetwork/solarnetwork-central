<a id="top"></a>

<section id="outstanding-invoices" class="hidden">
	<h2>
		<fmt:message key='billing.outstandingInvoiceList.header'/>
	</h2>
	<p class="intro">
		<fmt:message key='billing.outstandingInvoiceList.intro'>
			<fmt:param value="${fn:length(outstandingInvoiceList)}"/>
		</fmt:message>
	</p>
	<c:if test="${fn:length(outstandingInvoiceList) > 0}">
		<table class="table" id="outstanding-invoice-list-table">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="user.node.name.label"/></th>
					<th><fmt:message key="user.node.description.label"/></th>
					<th><fmt:message key="user.node.private.label"/></th>
					<th><fmt:message key="user.node.certificate.label"/></th>
					<th></th>
				</tr>
				<tr class="template">
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
	</c:if>
</section>

<section id="invoices">
	<h2>
		<fmt:message key='billing.invoiceList.header'/>
	</h2>
	<p class="intro">
		<fmt:message key='billing.invoiceList.intro'>
			<fmt:param value="${fn:length(invoiceList)}"/>
		</fmt:message>
	</p>
	<c:if test="${fn:length(invoiceList) > 0}">
		<table class="table" id="invoice-list-table">
			<thead>
				<tr>
					<th><fmt:message key="user.node.id.label"/></th>
					<th><fmt:message key="user.node.created.label"/></th>
					<th><fmt:message key="user.node.name.label"/></th>
					<th><fmt:message key="user.node.description.label"/></th>
					<th><fmt:message key="user.node.private.label"/></th>
					<th><fmt:message key="user.node.certificate.label"/></th>
					<th></th>
				</tr>
				<tr class="template">
				</tr>
			</thead>
			<tbody>
			</tbody>
		</table>
	</c:if>
</section>
