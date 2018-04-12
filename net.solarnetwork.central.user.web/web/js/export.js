$(document).ready(function() {
	'use strict';
	
	var exportConfigs = {};
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	
	/**
	 * Get a display name for a service based on its identifier.
	 * 
	 * @param {Array} services the array of localized service infos to search
	 * @param {String} identifier the service ID to look for
	 * @returns {String} the localized name, or an empty string if not found
	 */
	function serviceDisplayName(services, identifier) {
		var service;
		if ( identifier && Array.isArray(services) ) {
			service = services.find(function(service) {
				return service.id === identifier;
			});
		}
		return (service ? service.localizedName : '');
	}
	
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
	
	function populateExportConfigs(configs) {
		if ( typeof configs !== 'object' ) {
			configs = {};
		}
		configs.datumExportConfigs = populateDatumExportConfigs(configs.datumExportConfigs);
		configs.dataConfigs = populateDataConfigs(configs.dataConfigs);
		configs.destintationConfigs = populateDestinationConfigs(configs.destintationConfigs);
		configs.outputConfigs = populateOutputConfigs(configs.outputConfigs)
		return configs;
	}
	
	function populateDatumExportConfigs(configs) {
		configs = Array.isArray(configs) ? configs : [];
		// TODO
		return configs;
	}
	
	function populateDataConfigs(configs) {
		configs = Array.isArray(configs) ? configs : [];
		// TODO
		return configs;
	}
	
	function populateDestinationConfigs(configs) {
		configs = Array.isArray(configs) ? configs : [];
		// TODO
		return configs;
	}
	
	function populateOutputConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-output-config-list-container');
		var itemTemplate = container.find('.template').first();
		var itemContainer = itemTemplate.parent();
		var items = configs.map(function(config) {
			return {
					_config : config,
					name : config.name,
					type : serviceDisplayName(outputServices, config.serviceIdentifier),
					compression : serviceDisplayName(compressionTypes, config.compressionTypeKey)
			};
		});
		if ( !preserve ) {
			itemTemplate.nextAll().remove();
		}
		items.forEach(function(item) {
			appendTemplateItem(itemContainer, itemTemplate, item);
		});
		container.toggleClass('hidden', configs.length < 1);
		return configs;
	}
	
	function appendTemplateItem(container, template, item) {
		if ( !(container && template && item) ) {
			return;
		}
		var newItem = template.clone(true).removeClass('template');
		replaceTemplateProperties(newItem, item).appendTo(container);
	}
	
	function replaceTemplateProperties(el, obj, prefix) {
		var prop, sel;
		for ( prop in obj ) {
			if ( !prop.startsWith('_') && obj.hasOwnProperty(prop) ) {
				sel = "[data-tprop='" +(prefix || '') +prop +"']";
				el.find(sel).addBack(sel).text(obj[prop]);
			}
		}
		return el;
	}
	
	function renderJobsList(configs) {
		
	}

	$('#edit-export-output-config-modal').on('submit', function(event) {
		var form = event.target;
		var body = JSON.stringify({
			name : form.elements['name'].value,
			compressionTypeKey : form.elements['compressionTypeKey'].selectedOptions[0].value,
			serviceIdentifier : form.elements['serviceIdentifier'].value
		});
		$.ajax({
			type: 'POST',
			url: form.action,
			contentType: "application/json; charset=utf-8",
			data : body,
			dataType: 'json',
			beforeSend: function(xhr) {
				SolarReg.csrf(xhr);
            },
			success: function(json) {
				console.log('Saved output config: %o', json);
				if ( json && json.success === true ) {
					populateOutputConfigs([json.data], true);
					if ( Array.isArray(exportConfigs.outputConfigs) ) {
						exportConfigs.outputConfigs.push(json.data);
					}
				}
				$(form).modal('hide');
			},
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#edit-export-output-config-modal .modal-body > *:first-child', 'alert-error', statusText);
			}
		});
		event.preventDefault();
		return false;
	}).on('hidden.bs.modal', function() {
		// reset form and modal for next item
		this.reset();
		var form = $(this);
		form.find('input[type=hidden]').value = '';
	});

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
			if ( json && json.success === true ) {
				exportConfigs = populateExportConfigs(json.data);
			}
		});
	});
	
});