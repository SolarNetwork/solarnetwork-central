$(document).ready(function() {
	'use strict';
	
	function renderInvoiceTableRows(tbody, templateRow, results) {
		var i, len, tr, invoice, prop, cell;
		tbody.empty();
		if ( results.length > 0 ) {
			for ( i = 0, len = results.length; i < len; i += 1 ) {
				tr = templateRow.clone(true);
				tr.removeClass('template');
				invoice = results[i];
				tr.data('invoice', invoice);
				for ( prop in invoice ) {
					if ( invoice.hasOwnProperty(prop) ) {
						cell = tr.find("[data-tprop='" +prop +"']");
						cell.text(invoice[prop]);
					}
				}
				tbody.append(tr);
			}
		}
	}
	
	$('#outstanding-invoices').each(function() {
		/*
		$.getJSON(SolarReg.solarUserURL('/sec/billing/systemInfo'), function(json) {
			console.log('Got billing info: %o', json);
		});
		*/
		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/list?unpaid=true'), function(json) {
			console.log('Got unpaid invoices: %o', json);
			var section = $('#outstanding-invoices');
			var haveUnpaid = json && json.data && json.data.results.length > 0;
			if ( haveUnpaid ) {
				var table = $('#outstanding-invoice-list-table');
				var templateRow = table.find('tr.template');
				var tbody = table.find('tbody');
				renderInvoiceTableRows(tbody, templateRow, json.data.results);
			}
			section.toggleClass('hidden', !haveUnpaid);
		});
		return false; // break on each()
	});
	
});