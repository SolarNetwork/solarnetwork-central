$(document).ready(function() {
	'use strict';
	
	var invoicePagination = {
		page: 0,
		total: 0,
		pageSize: 10,
	};

	var latestInvoice;
	
	function resetInvoiceDetails(modal) {
		$(modal).find('table.invoice-items').addClass('hidden').find('tbody').empty();
		$(modal).find('.invoice-items-loading').removeClass('hidden');
	}
	
	function replaceTemplateProperties(el, obj, prefix) {
		var prop, sel;
		for ( prop in obj ) {
			if ( obj.hasOwnProperty(prop) ) {
				sel = "[data-tprop='" +(prefix || '') +prop +"']";
				el.find(sel).addBack(sel).text(obj[prop]);
			}
		}
	}
	
	function populateInvoiceItemDetails(table, invoice) {
		var i, len, j, len2,
			items = (invoice ? invoice.localizedInvoiceItems : null),
			haveItems = (Array.isArray(items) && items.length > 0),
			taxItems = (invoice ? invoice.localizedTaxInvoiceItemsGroupedByDescription : null),
			haveTaxItems = (Array.isArray(taxItems) && taxItems.length > 0),
			tbody = table.find('tbody'),
			tfoot = table.find('tfoot'),
			templateRow = table.find('.invoice-item.template'),
			taxTemplateRow = table.find('.invoice-item-tax.template'),
			tr, 
			item,
			prop,
			usageList,
			usageTemplateRow,
			usageRecords,
			haveUsageRecords,
			usageRecord,
			li;
		tbody.empty();
		tfoot.empty();
		if ( haveItems ) {
			for ( i = 0, len = items.length; i < len; i += 1 ) {
				item = items[i];
				if ( item.itemType === 'TAX' ) {
					// display overall tax later
					continue;
				}
				tr = templateRow.clone(true);
				tr.removeClass('template');
				tr.data('invoiceItem', item);
				replaceTemplateProperties(tr, item);
				
				// look for metadata
				if ( item.metadata ) {
					replaceTemplateProperties(tr, item.metadata, 'metadata.');
				}
				
				usageRecords = item.localizedInvoiceItemUsageRecords;
				haveUsageRecords = (Array.isArray(usageRecords) && usageRecords.length > 0);
				if ( haveUsageRecords ) {
					usageList = tr.find('.usage-records');
					usageTemplateRow = usageList.find('.usage-record.template');
					for ( j = 0, len2 = usageRecords.length; j < len2; j += 1 ) {
						usageRecord = usageRecords[j];
						li = usageTemplateRow.clone(true);
						li.removeClass('template');
						replaceTemplateProperties(li, usageRecord);
						usageList.append(li);
					}
					usageTemplateRow.remove();
					usageList.removeClass('hidden');
				}
				
				tbody.append(tr);
			}
		}
		if ( haveTaxItems ) {
			for ( i = 0, len = taxItems.length; i < len; i += 1 ) {
				item = taxItems[i];
				tr = taxTemplateRow.clone(true);
				tr.removeClass('template');
				tr.data('invoiceItem', item);
				replaceTemplateProperties(tr, item);
				tfoot.append(tr);
			}
		}
		table.toggleClass('hidden', !haveItems);
	}
	
	function showInvoiceDetails(event) {
		event.preventDefault();
		
		var invoice = $(this).closest('tr').data('invoice');
		console.log('Showing invoice %o', invoice);
		
		var modal = $('#invoice-details-modal');
		var container = modal.find('.invoice-details');
		var prop, cell;
		for ( prop in invoice ) {
			if ( invoice.hasOwnProperty(prop) ) {
				cell = container.find("[data-tprop='" +prop +"']").text(invoice[prop]);
				if ( prop === 'localizedAmount' ) {
					cell.toggleClass('label-danger', invoice.balance > 0);
					cell.toggleClass('label-success', !(invoice.balance > 0));
				}
			}
		}
		
		modal.find('a.invoice-render').attr('href', SolarReg.solarUserURL('/sec/billing/invoices/' +invoice.id +'/render/pdf'));

		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/' +invoice.id), function(json) {
			console.log('Got invoice detail: %o', json);
			populateInvoiceItemDetails(modal.find('table.invoice-items'), (json ? json.data : null));
			modal.find('.invoice-items-loading').addClass('hidden');
		});
		
		modal.modal('show');
	}
	
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
						if ( cell.is('a') ) {
							cell.on('click', showInvoiceDetails);
						}
						cell.text(invoice[prop]);
					}
				}
				tbody.append(tr);
			}
		}
	}
	
	function renderInvoiceTable(table, pagination, displayCount, json) {
		var haveRows = json && json.data && json.data.results && json.data.results.length > 0;
		var total = (json.data ? json.data.totalResults : returned);
		var container, prop, cell;
		var table = $(table);
		if ( haveRows ) {
			var templateRow = table.find('tr.template');
			var tbody = table.find('tbody');
			renderInvoiceTableRows(tbody, templateRow, json.data.results);
		}
		if ( json.data ) {
			container = table.find('tfoot');
			if ( container.length ) {
				for ( prop in json.data ) {
					container.find("[data-tprop='" +prop +"']").text(json.data[prop]);
				}
			}
		}
		if ( pagination ) {
			var offset = (json.data ? json.data.startingOffset : 0);
			var returned = (haveRows ? json.data.results.length : 0);
			var pageCount = Math.ceil(total / invoicePagination.pageSize);
			var page = (offset / invoicePagination.pageSize);
			var haveMore = (offset + returned < total);
			
			var pageNav = $(pagination);
			var paginationListItems = pageNav.find('li');
			paginationListItems.not(':first').not(':last').not('.template').remove();
			
			var prevItem = paginationListItems.first();
			prevItem.toggleClass('disabled', !(offset > 0));
			
			var nextItem = paginationListItems.last();
			nextItem.toggleClass('disabled', !haveMore);

			var templateItem = paginationListItems.filter('.template');
			
			var i, len, pageItem;
			for ( i = 0, len = pageCount; i < len; i += 1 ) {
				pageItem = templateItem.clone(true).removeClass('template');
				if ( i === page ) {
					pageItem.addClass('active');
				}
				pageItem.find("[data-tprop='pageNumber']").text(i+1);
				pageItem.find('a').attr('href', '#'+i);
				pageItem.insertBefore(nextItem);
			}
			
			invoicePagination.total = total;
			invoicePagination.page = page;
			
			pageNav.toggleClass('hidden', pageCount < 2);
		}
		$(displayCount).text(total);
		return haveRows;
	}

	function renderPreviewInvoiceUsageTableRows(tbody, totalRow, templateRow, item) {
		var i, len, tr, prop, cell, itemUsage, usage;
		tbody.empty();
		if ( !Array.isArray(item.localizedInvoiceItemUsageRecords) && item.localizedInvoiceItemUsageRecords.length > 0 ) {
			return;
		}
		itemUsage = item.localizedInvoiceItemUsageRecords[0]; // only take first usage
		if ( !Array.isArray(itemUsage.localizedUsageTiers) ) {
			return;
		}
		for ( i = 0, len = itemUsage.localizedUsageTiers.length; i < len; i += 1 ) {
			tr = templateRow.clone(true);
			tr.removeClass('template');
			usage = itemUsage.localizedUsageTiers[i];
			tr.data('usage', usage);
			for ( prop in usage ) {
				if ( usage.hasOwnProperty(prop) ) {
					cell = tr.children("[data-tprop='" +prop +"']");
					cell.text(usage[prop]);
				}
			}
			tbody.append(tr);
		}

		for ( prop in itemUsage ) {
			if ( itemUsage.hasOwnProperty(prop) ) {
				cell = totalRow.children("[data-tprop='" +prop +"']");
				cell.text(itemUsage[prop]);
			}
		}
	}

	function renderPreviewInvoiceTableRows(tbody, templateRow, invoice) {
		var i, len, tr, prop, cell, item;

		tbody.empty();
		if ( !invoice.id ) {
			return;
		}
		for ( i = 0, len = invoice.localizedNonTaxInvoiceItems.length; i < len; i += 1 ) {
			tr = templateRow.clone(true);
			tr.removeClass('template');
			item = invoice.localizedNonTaxInvoiceItems[i];
			tr.data('item', item);
			for ( prop in item ) {
				if ( item.hasOwnProperty(prop) ) {
					cell = tr.children("[data-tprop='" +prop +"']");
					cell.text(item[prop]);
				}
			}
			renderPreviewInvoiceUsageTableRows(tr.find('tbody'), tr.find('tfoot tr'), tr.find('tr.template'), item);		
			tbody.append(tr);
		}
	}

	function renderPreviewInvoiceTaxItems(tbody, templateRow, taxItems) {
		var i, len, tr, taxItem;
		if ( !Array.isArray(taxItems) ) {
			return;
		}
		for ( i = 0, len = taxItems.length; i < len; i += 1 ) {
			tr = templateRow.clone(true);
			tr.removeClass('template');
			taxItem = taxItems[i];
			tr.data('item', taxItem);
			replaceTemplateProperties(tr, taxItem);
			tbody.append(tr);
		}
	}

	function renderPreviewInvoiceTotals(table, invoice) {
		var foo,
			taxItems = (invoice ? invoice.localizedTaxInvoiceItemsGroupedByDescription : null),
			haveTaxItems = (Array.isArray(taxItems) && taxItems.length > 0),
			totalRows = table.children('thead,tfoot').children('tr').not('.template');
		if ( haveTaxItems ) {
			renderPreviewInvoiceTaxItems(table.children('tbody'), table.find('tr.template'), taxItems);
		}
		replaceTemplateProperties(totalRows, invoice);
	}

	function renderPreviewInvoiceTable(table, json) {
		var haveRows = json && json.data && json.data.id;
		table = $(table);
		if ( haveRows ) {
			var templateRow = table.children('thead').children('tr.template');
			var tbody = table.children('tbody');
			var tallyTable = table.children('tfoot').find('table');
			renderPreviewInvoiceTableRows(tbody, templateRow, json.data);
			renderPreviewInvoiceTotals(tallyTable, json.data);
		}
	}
	
	function loadInvoicePage(pageNum) {
		console.log('Want page %d', pageNum);
		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/list?unpaid=false&offset=' 
				+(pageNum * invoicePagination.pageSize)
				+'&max=' +invoicePagination.pageSize), function(json) {
			console.log('Got invoices: %o', json);
			var havePaid = renderInvoiceTable('#invoice-list-table', '#invoice-list-pagination', 
					'.invoiceListCount', json);
			$('#invoices').toggleClass('hidden', !havePaid);
		});
	}
	
	$('#outstanding-invoices').each(function() {
		
		// setup pagination links
		var paginationListItems = $('#invoice-list-pagination li');
		paginationListItems.first().find('a').on('click', function(event) {
			event.preventDefault();
			loadInvoicePage(invoicePagination.page - 1);
		});
		paginationListItems.filter('.template').find('a').on('click', function(event) {
			event.preventDefault();
			var page = +this.hash.substring(1);
			loadInvoicePage(page);
		});
		paginationListItems.last().find('a').on('click', function(event) {
			event.preventDefault();
			loadInvoicePage(invoicePagination.page + 1);
		});
		
		// reset invoice details modal on close
		$('#invoice-details-modal').on('hidden.bs.modal', function(event) {
			resetInvoiceDetails(this);
		});

		// get current usage
		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/preview'), function(json) {
			console.log('Got preview invoice: %o', json);
			renderPreviewInvoiceTable('#upcoming-invoice-table', json);
			$('#upcoming-invoice').toggleClass('hidden', !json.data);
		});
		
		// get unpaid invoices
		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/list?unpaid=true'), function(json) {
			console.log('Got unpaid invoices: %o', json);
			var haveUnpaid = renderInvoiceTable('#outstanding-invoice-list-table', null, 
					'.outstandingInvoiceListCount', json);
			$('#outstanding-invoices').toggleClass('hidden', !haveUnpaid);
		});
		
		// get all invoices
		loadInvoicePage(0);

		return false; // break on each()
	});
	
});