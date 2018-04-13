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
 * Find an existing template item element in a list of items.
 * 
 * @param {jQuery} container the container of the list of items to search
 * @param {*} itemId the item ID to look for
 * @returns {jQuery} the jQuery selector for the first matching item
 */
SolarReg.Templates.findExistingTemplateItem = function findExistingTemplateItem(container, itemId) {
	return container.children().filter(function(i, e) {
		var ctx = $(e).data('context-item');
		// note the loose == here, to handle numbers that might be handed in as strings from form elements
		return (ctx && ctx.id && ctx.id == itemId);
	}).first();
};

/**
 * Create new DOM elements from a template DOM element for each of a parameter object.
 * 
 * The `container` will be searched for an element with class `list-container`, and 
 * if found that element will be where the new DOM elements will be appended. If not 
 * found, the parent of `container` will be used.
 * 
 * @param {jQuery} container the container that holds the template item
 * @param {Array} items  the array of parameter objects to populate into cloned templates
 * @param {boolean} preserve `true` to only append items, do not clear out any existing items
 * @see #appendTemplateItem
 */
SolarReg.Templates.populateTemplateItems = function populateTemplateItems(container, items, preserve) {
	var itemTemplate = container.find('.template').first();
	var itemContainer = container.find('.list-container').first();
	if ( itemContainer.length < 1 ) {
		itemContainer = itemTemplate.parent();
	} 
	if ( !preserve ) {
		itemTemplate.nextAll().remove();
	}
	items.forEach(function(item) {
		var existing;
		if ( preserve && item._contextItem && item._contextItem.id ) {
			// look for existing row to update, rather than append
			existing = SolarReg.Templates.findExistingTemplateItem(itemContainer, item._contextItem.id);
		}
		if ( existing && existing.length > 0 ) {
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

/**
 * Prepare an edit service form for display.
 * 
 * @param {HTMLFormElement} form the form to reset
 * @param {array} services array of possible service infos for this form
 * @param {jQuery} settingTemplates the container for setting templates
 */
SolarReg.Templates.prepareEditServiceForm = function prepareEditServiceForm(form, services, settingTemplates) {
	var clicked = event.relatedTarget;
	if ( form && clicked ) {
		var config = SolarReg.Templates.findContextItem(clicked);
		var service = (config ? SolarReg.findById(services, config.serviceIdentifier) : null);
		var container = $(form).find('.service-props-container');
		SolarReg.Settings.setupServiceCoreSettings(service, form, config);
		SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
	}
};

/**
 * Search for the nearest configuration container for an element.
 * 
 * The configuration container is the closest element (anscestor or self) that 
 * contains the class `configs`. If not found, `el` itself will be returned.
 * 
 * @param {jQuery} el the element to find the configuration container for
 */
SolarReg.Templates.findConfigurationContainer = function findConfigurationContainer(el) {
	if ( el.hasClass('configs') ) {
		return el;
	}
	var result = el.closest('.configs');
	return (result.length > 0 ? result : el);
}

/**
 * Reset an edit service form for reuse.
 * 
 * @param {HTMLFormElement} form the form to reset
 * @param {jQuery} container a container element of existing list items to delete from, if the form closed after a delete action
 */
SolarReg.Templates.resetEditServiceForm = function resetEditServiceForm(form, container) {
	var id = form.elements['id'].value;
	var f = $(form);
	// look if we deleted an item, which will only be true if form in "danger" mode
	if ( id && f.hasClass('deleted') ) {
		var existing = SolarReg.Templates.findExistingTemplateItem(container, id);
		if ( existing.length > 0 ) {
			existing.remove();
			if ( container.children().length < 1 ) {
				// empty container; hide it
				SolarReg.Templates.findConfigurationContainer(container).addClass('hidden');
			}
		}
	}

	// reset form and modal for next item
	form.reset();
	f.find('input[type=hidden]').val('');

	// clear delete status
	f.removeClass('danger').removeClass('deleted');
	f.find('button[type=submit]').prop('disabled', false);
	f.find('.delete-confirm').addClass('hidden');
	f.find('button.delete-config').addClass('hidden');
};

/**
 * Handle the delete action for an edit service form.
 * 
 * @param {HTMLButtonElement} deleteBtn the delete button that was activated
 */
SolarReg.Templates.handleEditServiceFormDelete = function handleEditServiceFormDelete(deleteBtn) {
	var modal = $(deleteBtn).closest('.modal');
	var confirmEl = modal.find('.delete-confirm');
	var submitBtn = modal.find('button[type=submit]');
	if ( confirmEl && confirmEl.hasClass('hidden') ) {
		// show confirm
		confirmEl.removeClass('hidden');

		// disable submit button
		submitBtn.prop('disabled', true);

		// enable "danger" mode in modal
		modal.addClass('danger');
	} else {
		// perform delete
		var id = modal.get(0).elements['id'].value;
		if ( id ) {
			var action = modal.attr('action') + '/' + encodeURIComponent(id);
			$.ajax({
				type: 'DELETE',
				url: action,
				dataType: 'json',
				beforeSend: function(xhr) {
					SolarReg.csrf(xhr);
				}
			}).done(function() {
				modal.addClass('deleted');
				modal.modal('hide');
			}).fail(function(xhr, statusText, error) {
				modal.removeClass('danger');
				modal.find('.delete-confirm').addClass('hidden');
				submitBtn.prop('disabled', false);
				SolarReg.showAlertBefore(modal.find('.modal-body > *:first-child'), 'alert-warning', statusText);
			});
		} else {
			modal.modal('hide');
		}
	}
};

/**
 * Show a form to edit a service configuration.
 * 
 * @param {jQuery} modal the modal form
 * @param {array} services array of possible service infos for this form
 * @param {jQuery} settingTemplates the container for setting templates
 * @param {object} [config] the configuration
 */
SolarReg.Templates.showEditServiceForm = function showEditServiceForm(modal, services, settingTemplates, config) {
	if ( !(modal && Array.isArray(services) && config) ) {
		return;
	}
	var service = SolarReg.findById(services, config.serviceIdentifier);
	var container = modal.find('.service-props-container').first();
	SolarReg.Settings.setupServiceCoreSettings(service, modal.get(0), config);
	SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
	if ( config && config.id ) {
		modal.find('button.delete-config').removeClass('hidden');
	}
	modal.modal('show');
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
	if ( !(service && form && config) ) {
		return;
	}
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

	function renderJobsList(configs) {
		
	}

	$('#edit-export-destination-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Templates.prepareEditServiceForm(event.target, destinationServices, settingTemplates);
	});

	$('#edit-export-output-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Templates.prepareEditServiceForm(event.target, outputServices, settingTemplates);
	}).on('submit', function(event) {
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
				SolarReg.showAlertBefore('#edit-export-output-config-modal .modal-body > *:first-child', 'alert-warning', statusText);
			}
		});
		event.preventDefault();
		return false;
	}).on('hidden.bs.modal', function() {
		SolarReg.Templates.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	$('#export-output-config-list-container').on('click', function(event) {
		console.log('click: %o', event);
		if ( event.target && event.target.classList && event.target.classList.contains('edit-link') ) {
			var config = SolarReg.Templates.findContextItem(event.target);
			SolarReg.Templates.showEditServiceForm($('#edit-export-output-config-modal'), outputServices, settingTemplates, config);
		}
	});

	$('.modal button.delete-config').on('click', function(event) {
		SolarReg.Templates.handleEditServiceFormDelete(event.target);
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