// TODO: move this stuff into Global? Settings?

/**
 * Search for an object with a matching `id` property value.
 * 
 * @param {array} array the array of objects to search through
 * @param {string} identifier the `id` value to search for
 * @returns {object} the first object that has a matching `id` property
 */
SolarReg.findById = function findById(array, identifier) {
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
	var prop, sel, val,
		sPropKey, sPropVal, sPropItem,
		sPropContainer = el.find('.service-props-container'),
		sPropTemplate = sPropContainer.find('.template');
	for ( prop in obj ) {
		if ( prop.startsWith('_') || !obj.hasOwnProperty(prop) ) {
			continue;
		}
		val = obj[prop];
		if ( prop === 'serviceProperties' && sPropContainer.length > 0 ) {
			for ( sPropKey in val ) {
				if ( !val.hasOwnProperty(sPropKey) ) {
					continue;
				}
				sPropVal = val[sPropKey];
				sPropItem = sPropTemplate.clone(true).removeClass('template');
				sel = "[data-tprop='" +(prefix || '') +"serviceProperties.name']";
				sPropItem.find(sel).addBack(sel).text(sPropKey);
				sel = "[data-tprop='" +(prefix || '') +"serviceProperties.value']";
				sPropItem.find(sel).addBack(sel).text(sPropVal);
				sPropContainer.append(sPropItem);
			}
		} else {
			sel = "[data-tprop='" +(prefix || '') +prop +"']";
			el.find(sel).addBack(sel).text(val);
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

	// clear out dynamic settings
	f.find('.service-props-container').empty().addClass('hidden');

	// reset form and modal for next item
	form.reset();
	f.find('input[type=hidden]').val('');

	// clear delete status
	f.removeClass('danger').removeClass('deleted');
	f.find('button[type=submit]').prop('disabled', false);
	f.find('.delete-confirm').addClass('hidden');
	f.find('button.delete-config').addClass('hidden');

	// clear any context item
	f.removeData('context-item');
};

/**
 * Handle the delete action for an edit service item form.
 * 
 * @param {event} event the event that triggered the delete action
 */
SolarReg.Templates.handleEditServiceItemDeleteAction = function handleEditServiceItemDelete(event) {
	var deleteBtn = event.target;
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

SolarReg.Templates.focusEditServiceForm = function focusEditServiceForm(event) {
	var form = event.target;
	$(form).find('input[type=text]').first().focus();
};

SolarReg.Templates.serviceConfigurationItem = function serviceConfigurationItem(config, services) {
	if ( !(config && Array.isArray(services)) ) {
		return {};
	}
	var result = {
			_contextItem : config,
			name : config.name,
			type : SolarReg.Templates.serviceDisplayName(services, config.serviceIdentifier)
	};
	if ( config.serviceProperties ) {
		result.serviceProperties = config.serviceProperties;
	}
	return result;
};

SolarReg.Settings = {};

/**
 * Create a setting item form object suitable for using as template parameters for a setting item.
 * 
 * @param {object} service the service info
 * @param {object} setting the setting specifier
 * @param {config} [config] the current configuration settings, if available
 * @returns {object} a form item object
 */
SolarReg.Settings.serviceFormItem = function formItem(service, setting, config) {
	var result = {};
	if ( service && setting ) {
		result.id = setting.key;
		result.name = SolarReg.Templates.serviceInfoMessage(service, setting.key +'.key');
		result.description = SolarReg.Templates.serviceInfoMessage(service, setting.key +'.desc');
		if ( config && config[setting.key] ) {
			result.value = config[setting.key];
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
 * Render a form item for each setting defined on a service.
 * 
 * Each form item template must have a `.template` class and a `data-setting-type` attribute
 * that matches the setting type that template supports.
 * 
 * @param {object} service the service info whose settings should be rendered as form items
 * @param {jQuery} container the container to render all form item HTML into
 * @param {jQuery} templates a container of setting form item templates, one for each type of supported setting
 * @param {object} [item] the current model object, if available, to populate the initial form values with
 */
SolarReg.Settings.renderServiceInfoSettings = function renderServiceInfoSettings(service, container, templates, item) {
	if ( !(container && templates && service && Array.isArray(service.settingSpecifiers)) ) {
		return;
	}
	container.empty();
	service.settingSpecifiers.forEach(function(setting) {
		var type = setting.type;
		if ( !type ) {
			return;
		}

		var options = [];
		if ( setting.secureTextEntry ) {
			options.push('secureTextEntry');
		}
		if ( options.length > 0 ) {
			options.sort();
			type += '|' + options.join(',');
		}

		var templateSel = ".template[data-setting-type='" +type +"']";
		var template = templates.find(templateSel);
		if ( !template.length ) {
			return;
		}
		var formItem = SolarReg.Settings.serviceFormItem(service, setting, (item ? item.serviceProperties : null));
		var formElement = SolarReg.Templates.appendTemplateItem(container, template, formItem);
		formElement.find('.setting-form-element').val(formItem.value).attr('name', 'serviceProperties.' + setting.key);
	});
	container.toggleClass('hidden', service.settingSpecifiers.length < 1);
};

/**
 * Prepare a form to edit a service configuration.
 * 
 * @param {jQuery} modal the modal form
 * @param {array} services array of possible service infos for this form
 * @param {jQuery} settingTemplates the container for setting templates
 * @param {object} [config] the configuration
 */
SolarReg.Settings.prepareEditServiceForm = function prepareEditServiceForm(modal, services, settingTemplates) {
	if ( !(modal && Array.isArray(services)) ) {
		return;
	}
	var config = SolarReg.Templates.findContextItem(modal);
	var service = (config ? SolarReg.findById(services, config.serviceIdentifier) : services[0]);
	var container = modal.find('.service-props-container').first();
	SolarReg.Settings.setupServiceCoreSettings(service, modal.get(0), config);
	SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
	if ( config && config.id ) {
		modal.find('button.delete-config').removeClass('hidden');
	}
};

SolarReg.Settings.handleEditServiceItemAction = function handleEditAction(event, services, settingTemplates) {
	console.log('click: %o', event);
	if ( event.target && event.target.classList && event.target.classList.contains('edit-link') ) {
		var config = SolarReg.Templates.findContextItem(event.target);
		var button = $(event.target);
		var modal = $(button.data('edit-modal'));
		modal.data('context-item', config);
		modal.modal('show');
	}
};

/**
 * Encode a service item configuration form into an object, suitable for encoding into JSON
 * for posting to SolarNetwork.
 * 
 * @param {HTMLFormElement} form the form
 * @returns {object} the encoded object
 */
SolarReg.Settings.encodeServiceItemForm = function encodeServiceItemForm(form) {
	var body = {},
		fields = form.elements;
	var i, iLen,
		j, jLen,
		field,
		name,
		value,
		components,
		component,
		bodyPart;
	
	for ( i = 0, iLen = fields.length; i < iLen; i += 1 ) {
		field = fields.item(i);
		name = field.name;
		if ( !name ) {
			continue;
		}
		components = name.split('.');
		if ( field.selectedOptions ) {
			// <select>
			value = field.selectedOptions[0].value;
		} else {
			// <input>
			value = field.value || '';
			if ( name === 'id' && !value ) {
				// don't populate empty ID value
				continue;
			}
		}

		bodyPart = body;
		for ( j = 0, jLen = components.length - 1; j < jLen; j += 1 ) {
			component = components[j];
			if ( !bodyPart[component] ) {
				bodyPart[component] = {};
			}
			bodyPart = bodyPart[component];
		}
		component = components[components.length - 1];
		bodyPart[component] = value;
	}

	return body;
};

/**
 * Handle the submit event for a edit service form.
 * 
 * @param {event} event the submit event that triggered form submission
 * @param {function} onSuccess a callback to invoke on success; will be passed the upload body object and the response body object
 * @returns {jqXHR} the jQuery XHR object
 */
SolarReg.Settings.handlePostEditServiceForm = function handlePostEditServiceForm(event, onSuccess) {
	var form = event.target;
	var modal = $(form);
	var body = SolarReg.Settings.encodeServiceItemForm(form);
	event.preventDefault();
	return $.ajax({
		type: 'POST',
		url: form.action,
		contentType: "application/json; charset=utf-8",
		data : JSON.stringify(body),
		dataType: 'json',
		beforeSend: function(xhr) {
			SolarReg.csrf(xhr);
		}
	}).done(function() {
		console.log('Saved output config: %o', json);
		if ( json && json.success === true && typeof onSuccess === 'function' ) {
			onSuccess(body, json.data);
		}
		modal.modal('hide');
	}).fail(function(xhr, statusText, error) {
		SolarReg.showAlertBefore(modal.find('.modal-body > *:first-child'), 'alert-warning', statusText);
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
			return SolarReg.Templates.serviceConfigurationItem(config, destinationServices);
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}
	
	function populateOutputConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-output-config-list-container');
		var items = configs.map(function(config) {
			var item = SolarReg.Templates.serviceConfigurationItem(config, destinationServices);
			item.compression = SolarReg.Templates.serviceDisplayName(compressionTypes, config.compressionTypeKey);
			return item;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}

	function renderJobsList(configs) {
		
	}

	// ***** Edit destination form
	$('#edit-export-destination-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), destinationServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Templates.focusEditServiceForm)
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDestinationConfigs([res], true);
			if ( !req.id && Array.isArray(exportConfigs.destintationConfigs) ) {
				exportConfigs.destintationConfigs.push(res);
			}
		});
		return false;
	}).on('hidden.bs.modal', function() {
		SolarReg.Templates.resetEditServiceForm(this, $('#export-destination-config-list-container .list-container'));
	});

	$('#export-destination-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, destinationServices, settingTemplates);
	});


	// ***** Edit output format form
	$('#edit-export-output-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceItem($(event.target), outputServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Templates.focusEditServiceForm)
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateOutputConfigs([res], true);
			if ( !req.id && Array.isArray(exportConfigs.outputConfigs) ) {
				exportConfigs.outputConfigs.push(res);
			}
		});
		return false;
	}).on('hidden.bs.modal', function() {
		SolarReg.Templates.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	$('#export-output-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, outputServices, settingTemplates);
	});

	$('.edit-config button.delete-config').on('click', SolarReg.Templates.handleEditServiceItemDeleteAction);

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