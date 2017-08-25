$(document).ready(function() {
	'use strict';
	
	$('#outstanding-invoices').each(function() {
		$.getJSON(SolarReg.solarUserURL('/sec/billing/systemInfo'), function(json) {
			console.log('Got billing info: %o', json);
		});
		return false; // break on each()
	});
	
});