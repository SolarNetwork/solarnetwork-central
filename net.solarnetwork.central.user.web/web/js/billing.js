$(document).ready(function() {
	'use strict';
	
	$('#outstanding-invoices').each(function() {
		/*
		$.getJSON(SolarReg.solarUserURL('/sec/billing/systemInfo'), function(json) {
			console.log('Got billing info: %o', json);
		});
		*/
		$.getJSON(SolarReg.solarUserURL('/sec/billing/invoices/list?unpaid=true'), function(json) {
			console.log('Got unpaid invoices: %o', json);
		});
		return false; // break on each()
	});
	
});