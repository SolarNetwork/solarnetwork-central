// TODO: move this stuff into Global? Settings?

/**
 * Search for an object with a matching `id` property value.
 * 
 * @param {array} array the array of objects to search through
 * @param {string} identifier the `id` value to search for
 * @returns {object} the first object that has a matching `id` property
 */
SolarReg.findById = function findByIdentifier(array, identifier) {
	var result;
	if ( identifier && Array.isArray(array) ) {
		result = array.find(function(obj) {
			return obj.id === identifier;
		});
	}
	return result;
 };


SolarReg.Templates = {};

/**
* Get a display name for a service based on its identifier.
* 
* @param {Array} services the array of localized service infos to search
* @param {String} identifier the service ID to look for
* @returns {String} the localized name, or an empty string if not found
*/
SolarReg.Templates.serviceDisplayName =	function serviceDisplayName(services, identifier) {
   var service = SolarReg.findById(services, identifier);
   return (service ? service.localizedName : '');
};

/**
 * Populate an HTML `<select>` element with options based on localized service infos.
 * 
 * @param {Array} services array of localized service infos
 * @param {String} selector jQuery selector for the `<select>` element to update
 */
SolarReg.Templates.populateServiceSelectOptions = function populateServiceSelectOptions(services, selector) {
	services = Array.isArray(services) ? services : [];
	$(selector).each(function() {
		var select = this;
		services.forEach(function(service) {
			select.add(new Option(service.localizedName, service.id));
		});
	});
	return services;
};

/**
 * Create new DOM elements from a template DOM element for each of a parameter object.
 * 
 * @param {jQuery} container the container that holds the template item
 * @param {Array} items  the array of parameter objects to populate into cloned templates
 * @param {boolean} preserve `true` to only append items, do not clear out any existing items
 * @see #appendTemplateItem
 */
SolarReg.Templates.populateTemplateItems = function populateTemplateItems(container, items, preserve) {
	var itemTemplate = container.find('.template').first();
	var itemContainer = itemTemplate.parent();
	if ( !preserve ) {
		itemTemplate.nextAll().remove();
	}
	items.forEach(function(item) {
		var existing;
		if ( preserve && item._contextItem && item._contextItem.id ) {
			// look for existing row to update, rather than append
			existing = itemContainer.children().filter(function(i, e) {
				var ctx = $(e).data('context-item');
				return (ctx && ctx.id && ctx.id === item._contextItem.id);
			}).first();
		}
		if ( existing ) {
			SolarReg.Templates.replaceTemplateProperties(existing, item);
		} else {
			SolarReg.Templates.appendTemplateItem(itemContainer, itemTemplate, item);
		}
	});
	container.toggleClass('hidden', items.length < 1);
};

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
SolarReg.Templates.replaceTemplateProperties = function replaceTemplateProperties(el, obj, prefix) {
   var prop, sel;
   for ( prop in obj ) {
	   if ( !prop.startsWith('_') && obj.hasOwnProperty(prop) ) {
		   sel = "[data-tprop='" +(prefix || '') +prop +"']";
		   el.find(sel).addBack(sel).text(obj[prop]);
	   }
   }
   return el;
};

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
* @returns {jQuery} the newly inserted element
*/
SolarReg.Templates.appendTemplateItem = function appendTemplateItem(container, template, item) {
   if ( !(container && template && item) ) {
	   return;
   }
   var newItem = template.clone(true).removeClass('template');
   if ( item._contextItem ) {
	   newItem.data('context-item', item._contextItem);
   }
   SolarReg.Templates.replaceTemplateProperties(newItem, item).appendTo(container);
   newItem.find('a.edit-link').on('click', function(event) {
	   // don't handle this directly; assume will propagate and get handled at container level
	   event.preventDefault();
   });
   return newItem;
};

SolarReg.Templates.serviceInfoMessage = function serviceInfoMessage(service, key) {
	if ( !(service && service.localizedInfoMessages && key) ) {
		return '';
	}
	return (service.localizedInfoMessages[key] || '');
};

/**
 * Find the first available context item for a DOM element.
 * 
 * @param {element} el the element to find the context item for
 * @returns {Object} the found context item, or undefined 
 */
SolarReg.Templates.findContextItem = function findContextItem(el) {
	var el = $(el);
	var ctx = el.data('context-item');
	if ( ctx ) {
		return ctx;
	}
	return el.parents().filter(function(i, e) {
		return !!$(e).data('context-item');
	}).first().data('context-item');
};

SolarReg.Settings = {};

/**
 * Create a setting item form object suitable for using as template parameters for a setting item.
 * 
 * @param {object} service the service info
 * @param {object} setting the setting specifier
 * @param {item} [item] the current item value
 * @returns {object} a form item object
 */
SolarReg.Settings.serviceFormItem = function formItem(service, setting, item) {
	var result = {};
	if ( service && setting ) {
		result.id = setting.key;
		result.name = SolarReg.Templates.serviceInfoMessage(service, setting.key +'.key');
		result.description = SolarReg.Templates.serviceInfoMessage(service, setting.key +'.desc');
		if ( item && item.value ) {
			result.value = item.value;
		} else {
			result.value = setting.defaultValue || '';
		}
	}
	return result;
};

/**
 * 
 * @param {object} service the service info 
 * @param {HTMLFormElement} form the form
 * @param {object} config the current configuration settings
 */
SolarReg.Settings.setupServiceCoreSettings = function setupServiceCoreSettings(service, form, config) {
	var i, len,
		fields = form.elements,
		field,
		name;
	for ( i = 0, len = fields.length; i < len; i += 1 ) {
		field = fields.item(i);
		name = field.name;
		if ( name && config[name] ) {
			$(field).val(config[name]);
		}
	}
}

/**
 * 
 * @param {object} service the service info
 * @param {*} container 
 * @param {*} templates 
 * @param {*} item 
 */
SolarReg.Settings.renderServiceInfoSettings = function renderServiceInfoSettings(service, container, templates, item) {
	if ( !(container && templates && service && Array.isArray(service.settingSpecifiers)) ) {
		return;
	}
	service.settingSpecifiers.forEach(function(setting) {
		var type = setting.type;
		if ( !type ) {
			return;
		}
		var templateSel = ".template[data-setting-type='" +type +"']";
		var template = templates.find(templateSel);
		if ( !template.length ) {
			return;
		}
		var formItem = SolarReg.Settings.serviceFormItem(service, setting, item);
		var formElement = SolarReg.Templates.appendTemplateItem(container, template, formItem);
		formElement.find('.setting-form-element').val(formElement.value);
	});
};

$(document).ready(function() {
	'use strict';
	
	var exportConfigs = {};
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];

	var settingTemplates = $('#export-setting-templates');
	
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
	
	function populateDestinationConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-destination-config-list-container');
		var items = configs.map(function(config) {
			return {
					_contextItem : config,
					name : config.name,
					type : SolarReg.Templates.serviceDisplayName(destinationServices, config.serviceIdentifier)
			};
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}
	
	function populateOutputConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-output-config-list-container');
		var items = configs.map(function(config) {
			return {
					_contextItem : config,
					name : config.name,
					type : SolarReg.Templates.serviceDisplayName(outputServices, config.serviceIdentifier),
					compression : SolarReg.Templates.serviceDisplayName(compressionTypes, config.compressionTypeKey)
			};
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}

	function editService(modalSelector, services, config) {
		if ( !(modalSelector && Array.isArray(services) && config) ) {
			return;
		}
		var modal = $(modalSelector);
		var service = SolarReg.findById(services, config.serviceIdentifier);
		var container = modal.find('.service-props-container').first();
		SolarReg.Settings.setupServiceCoreSettings(service, modal.get(0), config);
		SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
		modal.modal('show');
	}

	function renderJobsList(configs) {
		
	}

	$('#edit-export-destination-config-modal').on('show.bs.modal', function(event) {
		var form = event.target;
		var clicked = event.relatedTarget;
		if ( form && clicked ) {
			var config = SolarReg.Templates.findContextItem(clicked);
			var service = SolarReg.findById(destinationServices, config.serviceIdentifier);
			var container = $(form).find('.service-props-container');
			SolarReg.Settings.setupServiceCoreSettings(service, form, config);
			SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
		}
	});

	$('#edit-export-output-config-modal').on('show.bs.modal', function(event) {
		var form = event.target;
		var clicked = event.relatedTarget;
		if ( form && clicked ) {
			var config = SolarReg.Templates.findContextItem(clicked);
			var service = SolarReg.findById(outputServices, config.serviceIdentifier);
			var container = $(form).find('.service-props-container');
			SolarReg.Settings.setupServiceCoreSettings(service, form, config);
			SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
		}
	});

	$('#edit-export-output-config-modal').on('submit', function(event) {
		var form = event.target;
		var body = {
			name : form.elements['name'].value,
			compressionTypeKey : form.elements['compressionTypeKey'].selectedOptions[0].value,
			serviceIdentifier : form.elements['serviceIdentifier'].value
		};
		if ( form.elements['id'].value ) {
			body.id = form.elements['id'].value;
		}
		$.ajax({
			type: 'POST',
			url: form.action,
			contentType: "application/json; charset=utf-8",
			data : JSON.stringify(body),
			dataType: 'json',
			beforeSend: function(xhr) {
				SolarReg.csrf(xhr);
            },
			success: function(json) {
				console.log('Saved output config: %o', json);
				if ( json && json.success === true ) {
					populateOutputConfigs([json.data], true);
					if ( !body.id && Array.isArray(exportConfigs.outputConfigs) ) {
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

	$('#export-output-config-list-container').on('click', function(event) {
		console.log('click: %o', event);
		if ( event.target && event.target.classList && event.target.classList.contains('edit-link') ) {
			var config = SolarReg.Templates.findContextItem(event.target);
			editService('#edit-export-output-config-modal', outputServices, config);
		}
	});

	$('#datum-export-configs').first().each(function() {
		// get available output services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/output'), function(json) {
			console.log('Got export output services: %o', json);
			if ( json && json.success === true ) {
				outputServices = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-output-services');
			}
		});

		// get available destination services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/destination'), function(json) {
			console.log('Got export destination services: %o', json);
			if ( json && json.success === true ) {
				destinationServices = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-destination-services');
			}
		});

		// get available compression services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/compression'), function(json) {
			console.log('Got export compression types: %o', json);
			if ( json && json.success === true ) {
				compressionTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-output-compression-types');
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