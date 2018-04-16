// depends on SolarReg.Templates

SolarReg.Settings = {};

/**
 * Reset an edit service form for reuse.
 * 
 * @param {HTMLFormElement} form the form to reset
 * @param {jQuery} container a container element of existing list items to delete from, if the form closed after a delete action
 */
SolarReg.Settings.resetEditServiceForm = function resetEditServiceForm(form, container) {
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
			}).done(function() {
				modal.addClass('deleted');
				modal.modal('hide');
			}).fail(function(xhr, statusText, error) {
				modal.removeClass('danger');
				modal.find('.delete-confirm').addClass('hidden');
				submitBtn.prop('disabled', false);
				SolarReg.showAlertBefore(modal.find('.modal-body:first > *:first-child'), 'alert-warning', statusText);
			});
		} else {
			modal.modal('hide');
		}
	}
};

SolarReg.Settings.focusEditServiceForm = function focusEditServiceForm(event) {
	var form = event.target;
	$(form).find('input[type=text]').first().focus();
};

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
        formElement.find('.setting-form-element')
            .val(setting.secureTextEntry ? '' : formItem.value)
            .attr('name', 'serviceProperties.' + setting.key);
        var helpElement = formElement.find('.setting-help');
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
	}).done(function(json, statusText) {
		console.log('Saved output config: %o', json);
		if ( json && json.success === true && typeof onSuccess === 'function' ) {
			onSuccess(body, json.data);
		}
		modal.modal('hide');
	}).fail(function(xhr, statusText, error) {
		SolarReg.showAlertBefore(modal.find('.modal-body:first > *:first-child'), 'alert-warning', statusText);
	});
};
