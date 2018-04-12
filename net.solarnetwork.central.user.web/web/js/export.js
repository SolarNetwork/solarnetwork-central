$(document).ready(function() {
	'use strict';
	
	var exportConfigs = {};
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	
	/**
	 * Replace the textual content of DOM elements who have data attributes matching template parameter names.
	 * 
	 * This method replaces parameters in a DOM structure, based on parameter names encoded as HTML
	 * `data-tprop` attributes. For any element with a matching `data-tprop` value, its content will 
	 * be replaced by the parameter value.
	 * 
	 * Imagine a DOM structure like:
	 * 
	 * ```html
	 * <tr><td data-tprop="name"></td></tr>
	 * ```
	 * 
	 * Then calling this method like `replaceTemplateProperties($(trEl), {name:"Hello, world."})` will change the DOM
	 * to this:
	 * 
	 * ```html
	 * <tr><td data-tprop="name">Hello, world.</td></tr>
	 * ```
	 * 
	 * **Note** that parameter names starting with `_` are skipped.
	 * 
	 * @param {jQuery} el the element to perform replacements on
	 * @param {object} obj the object whose properties act as template parameters
	 * @param {string} [prefix] an optional prefix to prepend to each template parameter
	 */
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
	
	/**
	 * Populate an HTML `<select>` element with options based on localized service infos.
	 * 
	 * @param {Array} services array of localized service infos
	 * @param {String} selector jQuery selector for the `<select>` element to update
	 */
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
	
	/**
	 * Create new DOM elements from a template DOM element for each of a parameter object.
	 * 
	 * @param {jQuery} container the container that holds the template item
	 * @param {Array} items  the array of parameter objects to populate into cloned templates
	 * @param {boolean} preserve `true` to only append items, do not clear out any existing items
	 * @see #appendTemplateItem
	 */
	function populateTemplateItems(container, items, preserve) {
		var itemTemplate = container.find('.template').first();
		var itemContainer = itemTemplate.parent();
		if ( !preserve ) {
			itemTemplate.nextAll().remove();
		}
		items.forEach(function(item) {
			appendTemplateItem(itemContainer, itemTemplate, item);
		});
		container.toggleClass('hidden', items.length < 1);
	}
	
	/**
	 * Clone a template element, replace template parameters in the clone, and append
	 * the clone to a container element.
	 * 
	 * The special `_contextItem` parameter value, if provided, will be stored on 
	 * the cloned element as a data attribute named `context-item`.
	 * 
	 * @param {jQuery} container the container element to append the new item into
	 * @param {jQuery} template the template to clone
	 * @param {object} item the parameter object 
	 */
	function appendTemplateItem(container, template, item) {
		if ( !(container && template && item) ) {
			return;
		}
		var newItem = template.clone(true).removeClass('template');
		if ( item._contextItem ) {
			newItem.data('context-item', item._contextItem);
		}
		replaceTemplateProperties(newItem, item).appendTo(container);
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
		var items = configs.map(function(config) {
			return {
					_contextItem : config,
					name : config.name,
					type : serviceDisplayName(outputServices, config.serviceIdentifier),
					compression : serviceDisplayName(compressionTypes, config.compressionTypeKey)
			};
		});
		populateTemplateItems(container, items, preserve);
		return configs;
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
				$('.datum-export-getstarted').toggleClass('hidden', exportConfigs.length > 0);
			}
		});
	});
	
});