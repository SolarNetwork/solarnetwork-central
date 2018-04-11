$(document).ready(function() {
	'use strict';
	
	$('#datum-export-configs').first().each(function() {
		// list all configs
		$.getJSON(SolarReg.solarUserURL('/sec/export/configs'), function(json) {
			console.log('Got export configurations: %o', json);
			// TODO
		});
	});
	
});