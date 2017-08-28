<a id="top"></a>

<p class="intro">
	<fmt:message key='billing.intro'/>
</p>

<section id="outstanding-invoices" class="hidden">
	<h2>
		<fmt:message key='billing.outstandingInvoiceList.header'/>
	</h2>
	<p class="intro">
		<fmt:message key='billing.outstandingInvoiceList.intro'>
			<fmt:param value="0"/>
		</fmt:message>
	</p>
	<table class="table unpaid" id="outstanding-invoice-list-table">
		<thead>
			<tr>
				<th><fmt:message key="billing.invoice.date.label"/></th>
				<th><fmt:message key="billing.invoice.balance.label"/></th>
			</tr>
			<tr class="template">
				<td data-tprop="localizedDate"></td>
				<td><span class="label label-danger" data-tprop="localizedBalance"></span></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>

<section id="invoices" class="hidden">
	<h2>
		<fmt:message key='billing.invoiceList.header'/>
	</h2>
	<p class="intro">
		<fmt:message key='billing.invoiceList.intro'>
			<fmt:param value="0"/>
		</fmt:message>
	</p>
	<table class="table" id="invoice-list-table">
		<thead>
			<tr>
				<th><fmt:message key="billing.invoice.date.label"/></th>
				<th><fmt:message key="billing.invoice.balance.label"/></th>
			</tr>
			<tr class="template">
				<td data-tprop="localizedDate"></td>
				<td><span class="label label-danger" data-tprop="localizedBalance"></span></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>
