$(document).ready(function() {
	'use strict';
	
	var exportConfigs = [];
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	
	function populateServiceSelectOptions(services, selector) {
		services = Array.isArray(services) ? services : [];
		$(selector).each(function() {
			var select = this;
			services.forEach(function(service) {
				select.add(new Option(service.localizedName, service.id));
			});
		});
		return services;
	}
	
	function renderJobsList(configs) {
		
	}
	
	$('#datum-export-configs').first().each(function() {
		// get available output services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/output'), function(json) {
			console.log('Got export output services: %o', json);
			if ( json && json.success === true ) {
				outputServices = populateServiceSelectOptions(json.data, 'select.export-output-services');
			}
		});

		// get available destination services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/destination'), function(json) {
			console.log('Got export destination services: %o', json);
			if ( json && json.success === true ) {
				destinationServices = populateServiceSelectOptions(json.data, 'select.export-destination-services');
			}
		});

		// get available compression services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/compression'), function(json) {
			console.log('Got export compression types: %o', json);
			if ( json && json.success === true ) {
				compressionTypes = populateServiceSelectOptions(json.data, 'select.export-output-compression-types');
			}
		});

		// list all configs
		$.getJSON(SolarReg.solarUserURL('/sec/export/configs'), function(json) {
			console.log('Got export configurations: %o', json);
			// TODO
		});
	});
	
});