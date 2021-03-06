<a id="top"></a>

<p class="intro">
	<fmt:message key='billing.intro'/>
</p>

<section id="upcoming-invoice">
	<h2>
		<fmt:message key='billing.upcomingInvoice.header'/>
		<a class="btn btn-info pull-right" href="<c:url value='/u/sec/billing/invoices/preview/render/pdf'/>"><fmt:message key='print.label'/></a>
	</h2>
	<p class="intro">
		<fmt:message key='billing.upcomingInvoice.intro'/>
	</p>
	<table class="invoice table" id="upcoming-invoice-table">
		<thead>
			<tr class="template">
				<th data-tprop="localizedDescription" class="col-sm-3"></th>
				<td class="col-sm-9">
					<table class="table table-condensed tally">
						<thead>
							<tr>
								<th class="col-sm-2"><fmt:message key='billing.usage.tier'/></th>
								<th class="col-sm-5"><fmt:message key='billing.usage.quantity'/></th>
								<th class="col-sm-5"><fmt:message key='billing.usage.cost'/></th>
							</tr>
							<tr class="template">
								<td data-tprop="localizedDescription"></td>
								<td data-tprop="localizedQuantity"></td>
								<td data-tprop="localizedCost"></td>
							</tr>
						</thead>
						<tbody class="tiers">
						</tbody>
						<tfoot>
							<tr>
								<th></th>
								<th data-tprop="localizedAmount"></th>
								<th data-tprop="localizedCost"></th>
							</tr>
						</tfoot>
					</table>
				</td>
			</tr>
		</thead>
		<tbody>
		</tbody>
		<tfoot>
			<tr>
				<td></td>
				<td>
					<table class="table table-condensed tally">
						<thead class="borderless">
							<tr class="template">
								<th></th>
								<th data-tprop="localizedDescription"></th>
								<th data-tprop="localizedAmount"></th>
							</tr>
							<tr>
								<th class="col-sm-9"></th>
								<th class="col-sm-1"><fmt:message key='billing.invoice.totalBeforeTax.label'/></th>
								<th class="col-sm-2" data-tprop="localizedNonTaxAmount"></th>
							</tr>
						</thead>
						<tbody class="borderless">
						</tbody>
						<tfoot class="borderless">
							<tr>
								<th></th>
								<th><fmt:message key='billing.invoice.totalWithTax.label'/></th>
								<th data-tprop="localizedAmount"></th>
							</tr>
						</tfoot>
					</table>
				</td>
			</tr>
		</tfoot>
	</table>
</section>

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
				<th><fmt:message key="billing.invoice.number.label"/></th>
				<th><fmt:message key="billing.invoice.date.label"/></th>
				<th><fmt:message key="billing.invoice.balance.label"/></th>
			</tr>
			<tr class="template">
				<td><a href="#" data-tprop="invoiceNumber"></a></td>
				<td data-tprop="localizedDate"></td>
				<td><span class="label label-danger" data-tprop="localizedBalance"></span></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
		<tfoot>
			<tr>
				<th colspan="2" class="ledger-amount-label"><fmt:message key="billing.invoice.balance.total.label"/></th>
				<th data-tprop="localizedTotalBalance"></th>
			</tr>
		</tfoot>
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
	<nav aria-label="<fmt:message key='billing.invoice.pagination.label'/>" id="invoice-list-pagination" class="hidden">
		<ul class="pagination">
			<li><a href="#" aria-label="<fmt:message key='billing.invoice.pagination.prev.label'/>"><span aria-hidden="true">&laquo;</span></a></li>
			<li class="template"><a href="#"><span data-tprop="pageNumber">1</span> <span class="sr-only hidden"><fmt:message key='billing.invoice.pagination.current.label'/></span></a></li>
			<li><a href="#" aria-label="<fmt:message key='billing.invoice.pagination.next.label'/>"><span aria-hidden="true">&raquo;</span></a></li>
		</ul>
	</nav>
	<table class="table" id="invoice-list-table">
		<thead>
			<tr>
				<th><fmt:message key="billing.invoice.number.label"/></th>
				<th><fmt:message key="billing.invoice.date.label"/></th>
				<th><fmt:message key="billing.invoice.amount.label"/></th>
			</tr>
			<tr class="template">
				<td><a href="#" data-tprop="invoiceNumber"></a></td>
				<td data-tprop="localizedDate"></td>
				<td><span class="label label-success" data-tprop="localizedAmount"></span></td>
			</tr>
		</thead>
		<tbody>
		</tbody>
	</table>
</section>

<%-- Modal forms --%>

<div class="modal fade" tabindex="-1" role="dialog" id="invoice-details-modal">
	<div class="modal-dialog" role="document">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-label="<fmt:message key='close.label'/>"><span aria-hidden="true">&times;</span></button>
				<h4 class="modal-title"><fmt:message key='billing.invoice.details.title'/></h4>
			</div>
			<div class="modal-body container-fluid">
		 		<div class="row">
		 			<div class="col-xs-2">
		 				<p><b><fmt:message key='billing.invoice.number.label'/></b></p>
			 		</div>
		 			<div class="col-xs-6">
		 				<p><b><fmt:message key='billing.invoice.date.label'/></b></p>
			 		</div>
		 			<div class="col-xs-4">
		 				<p><b><fmt:message key='billing.invoice.amount.label'/></b></p>
			 		</div>
		 		</div>
		 		<div class="row invoice-details">
		 			<div class="col-xs-2">
		 				<p data-tprop="invoiceNumber"></p>
			 		</div>
		 			<div class="col-xs-6">
		 				<p data-tprop="localizedDate"></p>
			 		</div>
		 			<div class="col-xs-4">
		 				<p><span class="label" data-tprop="localizedAmount"></span></p>
			 		</div>
		 		</div>
			</div>
			<div class="modal-body invoice-items-loading">
				<div class="progress">
					<div class="progress-bar progress-bar-striped active" role="progressbar" style="width: 100%">
						<em><fmt:message key='billing.invoice.details.loading'/></em>
	    			</div>
    			</div>
			</div>
			<table class="invoice-items hidden table table-striped">
				<thead>
					<tr>
						<th><fmt:message key='billing.invoice.item.planName.label'/></th>
						<th><fmt:message key='billing.invoice.item.node.label'/></th>
						<th><fmt:message key='billing.invoice.item.dateRange.label'/></th>
						<th><fmt:message key='billing.invoice.item.usage.label'/></th>
						<th><fmt:message key='billing.invoice.item.amount.label'/></th>
					</tr>
					<tr class="invoice-item template">
						<td data-tprop="localizedDescription"></td>
						<td data-tprop="metadata.nodeId"></td>
						<td><span  data-tprop="localizedStartDate"></span> - <span data-tprop="localizedEndDate"></span></td>
						<td>
							<dl class="usage-records hidden">
								<dt class="usage-record template" data-tprop="localizedUnitType"></dt>
								<dd class="usage-record template" data-tprop="localizedAmount"></dd>
							</dl>
						</td>
						<td><span class="label label-default" data-tprop="localizedAmount"></span></td>
					</tr>
					<tr class="invoice-item-tax template">
						<td colspan="3"></td>
						<td data-tprop="localizedDescription"></td>
						<td><span class="label label-default" data-tprop="localizedAmount"></span></td>
					</tr>
				</thead>
				<tbody>
				</tbody>
				<tfoot>
				</tfoot>
			</table>
			<div class="modal-footer">
				<a target="sn_invoice_print" class="btn btn-info pull-left invoice-render" href="<c:url value='/u/sec/billing/invoices/{invoiceId}/render'/>"><fmt:message key='print.label'/></a>
				<button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key='close.label'/></button>
			</div>
		</div>
	</div>
</div>
