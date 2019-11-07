// depends on SolarReg.Templates

SolarReg.Settings = {};

SolarReg.Settings.modalAlertBeforeSelector = '.modal-body:not(.hidden):first > *:first-child';

/**
 * Reset an edit service form for reuse.
 * 
 * @param {HTMLFormElement} form the form to reset
 * @param {jQuery} container a container element of existing list items to delete from, if the form closed after a delete action
 */
SolarReg.Settings.resetEditServiceForm = function resetEditServiceForm(form, container) {
	var id = (form.elements && form.elements['id'] ? form.elements['id'].value : undefined);
	var f = $(form);
	// look if we deleted an item, which will only be true if form in "danger" mode
	if ( id && container && f.hasClass('deleted') ) {
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
	if ( typeof form.reset === 'function' ) {
		form.reset();
	}
	f.find('input[type=hidden]').not('[name=_csrf]').val('');

	// clear delete status
	f.removeClass('danger').removeClass('deleted');
	f.find('button[type=submit]').prop('disabled', false);
	f.find('.delete-confirm').addClass('hidden');
	f.find('button.delete-config').addClass('hidden');

	// clear any context item
	SolarReg.Templates.setContextItem(f, null);
	
	// clean any alert
	f.find(SolarReg.Settings.modalAlertBeforeSelector +'.alert-warning').remove();
};

/**
 * Handle the delete action for an edit service item form.
 * 
 * @param {event} event the event that triggered the delete action
 */
SolarReg.Settings.handleEditServiceItemDeleteAction = function handleEditServiceItemDelete(event) {
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
			}).done(function(json) {
				if ( json && json.success === true ) {
					modal.addClass('deleted');
					modal.modal('hide');
				} else {
					var msg = (json && json.message ? json.message : 'Unknown error: ' +statusText);
					SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning', msg);
				}
			}).fail(function(xhr, statusText, error) {
				modal.removeClass('danger');
				modal.find('.delete-confirm').addClass('hidden');
				submitBtn.prop('disabled', false);
				SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning', statusText);
			});
		} else {
			modal.modal('hide');
		}
	}
};

SolarReg.Settings.focusEditServiceForm = function focusEditServiceForm(event) {
	var form = event.target;
	$(form).find('input[type=text]').first().trigger('focus');
};

/**
 * Generate a display model object out of a configuration object.
 * 
 * The returned object will contain:
 * 
 *  * `name` - the `config.name`
 *  * `type` - the localized name of the service matching `config.serviceIdentifier`
 *  * `serviceProperties` - result of calling {@link SolarReg.Settings.serviceConfigurationItemServiceProperties}
 * 
 * @param {object} config the configuration object to generate a display model object for
 * @param {array} services the array of possible services the configuration might adopt
 * @returns {object} a display model object
 */
SolarReg.Settings.serviceConfigurationItem = function serviceConfigurationItem(config, services) {
	if ( !(config && Array.isArray(services)) ) {
		return {};
	}
	var result = {
			_contextItem : config,
			name : config.name,
			type : SolarReg.Templates.serviceDisplayName(services, config.serviceIdentifier)
    };
    var serviceProps = (config.serviceProperties
                        ? SolarReg.Settings.serviceConfigurationItemServiceProperties(config, services)
                        : null);
	if ( serviceProps ) {
        result.serviceProperties = serviceProps;
    }
	return result;
};

/**
 * Get a service configuration display model item for the service properties of a service configuration.
 * 
 * This method will iterate over the `settingSpecifiers` array of the service with an `id` that matches
 * `config.serviceIdentifier`, and then populate localized keys and associated values for each non-empty
 * service property. If a setting has the `secureTextEntry` flag set to `true` then the value will be
 * rendered as a set of asterisk characters only.
 * 
 * @param {Object} config the service configuration to get a display model for
 * @param {Array} services the available services
 */
SolarReg.Settings.serviceConfigurationItemServiceProperties = function serviceConfigurationItemServiceProperties(config, services) {
    if ( !config.serviceProperties ) {
        return null;
    }
    var service = SolarReg.findByIdentifier(services, config.serviceIdentifier),
        infoMessages = (service ? service.localizedInfoMessages : null),
        serviceProps = config.serviceProperties,
        result = {};

    if ( !Array.isArray(service.settingSpecifiers) ) {
        return null;
    }
    service.settingSpecifiers.forEach(function(setting) {
        var type = setting.type,
            key = setting.key,
            msgKey,
            name,
            val;
        if ( !(type && key) ) {
            return;
        }

        msgKey = key+'.key';
        name = infoMessages[msgKey] || key;
        val = serviceProps[key];
        if ( val ) {
            result[name] = (setting.secureTextEntry ? '*****' : val);
        }
    });
    return result;
};

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
		if ( config && config[setting.key] !== undefined ) {
			result.value = config[setting.key];
		} else {
			result.value = ''+setting.defaultValue || '';
		}
	}
	return result;
};

/**
 * Populate form fields with corresponding values from a configuration object.
 * 
 * This method will iterate over all form fields, and look for a corresponding property in `config`
 * with the same name. Names will be split on `.` characters, such that nested objects within
 * `config` will be inspected for the associated value.
 * 
 * For example given a configuration object like `{filter: {ids: [1, 2, 3]}}` then a form field
 * named `filter.ids` would resolve to the array `[1, 2, 3]`.
 * 
 * Array values will be passed to {@link SolarReg.arrayAsDelimitedString} before being set
 * as a form field value.
 * 
 * @param {HTMLFormElement} form the form
 * @param {object} config the current configuration settings
 */
SolarReg.Settings.setupCoreSettings = function setupCoreSettings(form, config) {
	if ( !(form && config) ) {
		return;
	}
	var i, len,
		fields = form.elements,
		field,
		name,
		components,
		component,
		obj,
		j, jLen;
	for ( i = 0, len = fields.length; i < len; i += 1 ) {
		field = fields.item(i);
		name = field.name;
		obj = config;
		components = name.split('.');
		component = name;
		for ( j = 1, jLen = components.length; j < jLen && obj; j += 1 ) {
			obj = obj[components[j-1]];
			component = components[j];
		}
		if ( name && obj && obj[component] ) {
			$(field).val(Array.isArray(obj[component]) 
				? SolarReg.arrayAsDelimitedString(obj[component]) 
				: obj[component]);
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
		var templateElement = SolarReg.Templates.appendTemplateItem(container, template, formItem);
		var fieldElement = templateElement.find('.setting-form-element');
        fieldElement
            .val(setting.secureTextEntry ? '' : formItem.value)
			.attr('name', 'serviceProperties.' + setting.key);
		if ( fieldElement.data('toggle') === 'setting-toggle' && fieldElement.data('on-text') && fieldElement.data('off-text') ) {
			var handleButton = function handleButton(btn, enabled) {
				btn.toggleClass('btn-success', enabled)
					.toggleClass('active', enabled)
					.attr('aria-pressed', enabled )
					.button(enabled ? 'on' : 'off')
					.val(enabled ? 'true' : 'false');
			}
			handleButton(fieldElement, formItem.value === 'true'); // initialize initial state
			fieldElement.on('click', function() {
				var btn = $(this);
				handleButton(btn, btn.val() !== 'true');
			});
		}
        var helpElement = templateElement.find('.setting-help');
        if ( helpElement.data('toggle') === 'popover' ) {
            helpElement.attr('data-content', formItem.description);
            helpElement.on('click', function(event) {
                event.preventDefault();
                $(event.currentTarget).popover('show');
            });
        } else {
            helpElement.html(formItem.description);
        }
        helpElement.toggleClass('hidden', !formItem.description);
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
	var service = (config ? SolarReg.findByIdentifier(services, config.serviceIdentifier) : services[0]);
	var container = modal.find('.service-props-container').first();
	SolarReg.Templates.populateServiceSelectOptions(services, 'select[name=serviceIdentifier]');
	SolarReg.Settings.setupCoreSettings(modal.get(0), config);
	SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
	if ( config && config.id ) {
		modal.find('button.delete-config').removeClass('hidden');
	}
};

/**
 * Handle a click on a service item, to display a edit form or action modal.
 * 
 * This function will examine the `event.target` for an `edit-link` class.
 * If found, then a modal defined by the CSS selector found on the `edit-modal`
 * data attribute will be opened. If `edit-link` is not available, then it
 * will look for an `action-link` class. If found, then a modal defined by the
 * CSS selector found on the `action-modal` data attribute will be opened.
 * 
 * For any opened modal, {@link SolarReg.Templates.setContextItem()} will be
 * called on it before opening, setting whatever context item is returned by
 * passing the event target to {@link SolarReg.Templates.findContextItem()}.
 * 
 * @param {Event} event the event that triggered the action
 */
SolarReg.Settings.handleEditServiceItemAction = function handleEditAction(event) {
	if ( !(event.target && event.target.classList) ) {
		return;
	}
	var config = SolarReg.Templates.findContextItem(event.target);
	var button = $(event.target);
	var modal;
	if ( event.target.classList.contains('edit-link') ) {
		modal = $(button.data('edit-modal'));
	} else if ( event.target.classList.contains('action-link') ) {
		modal = $(button.data('action-modal'));
	}
	if ( modal && modal.modal ) {
		SolarReg.Templates.setContextItem(modal, config);
		modal.modal('show');
		event.preventDefault();
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
			if ( field.selectedOptions.length < 1 ) {
				continue;
			}
			// handle <option> without any value attribute as null value
			value = (field.selectedOptions[0].hasAttribute('value')
				? field.selectedOptions[0].value
				: null);
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
 * @param {function} [serializer] a callback to invoke to serialize the form data; will be passed the form object and must return
 *                                an object that will be serialized into JSON via `JSON.stringify` or a FormData object
 * @param {object} [options] an object with optional configuration properties
 * @param {function} [options.download] an optional download progress event callback function
 * @param {function} [options.upload] an optional upload progress event callback function
 * @param {function} [options.urlSerializer] an optional function to generate the submit URL; will be passed the form action (decoded) and the serialized body content;
 *                                           defaults to `SolarReg.replaceTemplateParameters`
 * @param {function} [options.urlId] if `true` then if the form as an `id` input with a non-empty value, add the value to the submit URL after a `/`
 * @returns {jqXHR} the jQuery XHR object
 */
SolarReg.Settings.handlePostEditServiceForm = function handlePostEditServiceForm(event, onSuccess, serializer, options) {
	event.preventDefault();
	var form = event.target;
	var modal = $(form);
	var body = (typeof serializer === 'function' 
		? serializer(form) 
		: SolarReg.Settings.encodeServiceItemForm(form));
	var urlFn = (options && typeof options.urlSerializer === 'function' 
		? options.urlSerializer 
		: SolarReg.replaceTemplateParameters);
	var action = (options && options.urlId && form.elements['id'] && form.elements['id'].value
		? form.action + '/' + encodeURIComponent(form.elements['id'].value)
		: form.action);
	var submitUrl = encodeURI(urlFn(decodeURI(action), body));
	var origXhr = $.ajaxSettings.xhr;
	var jqXhrOpts = {
		type: 'POST',
		url: submitUrl,
		xhr: function() {
			var xhr = origXhr.apply(this, arguments);
			if ( options && typeof options.upload === 'function' && xhr.upload ) {
				xhr.upload.addEventListener('progress', options.upload);
			}
			if ( options && typeof options.download === 'function' ) {
				xhr.addEventListener('progress', options.upload);
			}
			return xhr;
		},
		beforeSend: function(xhr) {
			SolarReg.csrf(xhr);
		}
	};
	modal.find('button[type=submit]').prop('disabled', true);
	if ( body instanceof FormData ) {
		jqXhrOpts.data = body;
		jqXhrOpts.contentType = false;
		jqXhrOpts.processData = false;
	} else {
		if ( body ) {
			jqXhrOpts.data = JSON.stringify(body);
			jqXhrOpts.contentType = 'application/json; charset=utf-8';
		}
		jqXhrOpts.dataType = 'json';
	}
	return $.ajax(jqXhrOpts).done(function(json, statusText) {
		console.log('Save config result: %o', json);
		if ( json && json.success === true ) {
			if ( typeof onSuccess === 'function' ) {
				onSuccess(body, json.data);
			}
			modal.modal('hide');
		} else {
			var msg = (json && json.message ? json.message : 'Unknown error: ' +statusText);
			SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning', msg);
		}
	}).fail(function(xhr, statusText, error) {
		var json = {};
		if ( xhr.responseJSON ) {
			json = xhr.responseJSON;
		} else if ( xhr.responseText ) {
			json = JSON.parse(xhr.responseText);
		}
		var msg = 'Error: ' +(json && json.message ? json.message : statusText);
		var el = modal.find(SolarReg.Settings.modalAlertBeforeSelector);
		SolarReg.showAlertBefore(el, 'alert-warning', msg);
	});
};
