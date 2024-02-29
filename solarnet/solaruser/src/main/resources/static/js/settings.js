// depends on SolarReg.Templates

SolarReg.Settings = {};

SolarReg.Settings.modalAlertBeforeSelector = '.modal-body:not(.hidden):first > *:first-child';

/**
 * Reset an edit service form for reuse.
 *
 * @param {HTMLFormElement} form the form to reset
 * @param {jQuery} container a container element of existing list items to delete from, if the form closed after a delete action
 * @param {Function} [callback] an optional callback that will be invoked with the `id` form element value and a boolean
 *                              `deleted flag set to `true` if the form item has been deleted
 */
SolarReg.Settings.resetEditServiceForm = function resetEditServiceForm(form, container, callback) {
	const config = SolarReg.Templates.findContextItem(form);
	var id = (form.elements && form.elements['id'] 
		? form.elements['id'].value 
		: (config ? config.id : undefined));
	var f = $(form),
		deleted = f.hasClass('deleted'),
		idNum = Number(id);
	if ( id ) {
		if ( !Number.isNaN(idNum) && Number.isInteger(idNum) ) {
			id = idNum;
		}
		if ( container && deleted ) {
			var existing = SolarReg.Templates.findExistingTemplateItem(container, id);
			if ( existing.length > 0 ) {
				existing.remove();
				if ( container.children().length < 1 && !container.hasClass('show-empty') ) {
					// empty container; hide it
					SolarReg.Templates.findConfigurationContainer(container).addClass('hidden');
				}
			}
		}
		if ( callback ) {
			callback(id, deleted);
		}
	}

	// clear out dynamic settings
	f.find('.service-props-container').empty().addClass('hidden');

	// clear out dynamic lists
	f.find('.dynamic-list-container').empty();

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
 * Get a settings form URL serializer function.
 * 
 * @param {Object} [options] an object with optional configuration properties
 * @param {Function} [options.urlSerializer] an optional function to generate the submit URL; the `this` object will be the form;
 * 											 will be passed the form action and body parameters; defaults to appending a `/{id}`
 * 											 value where {id} is the value of the `id` form field, unless the form has a data
 * 											 attribute `settings-url-id-create-property` in which case that field's value will
 * 											 be used instead; if the form has a data attribute `settings-id-query-param` then
 * 											 the ID value will be added as a query parameter using the given name instead of
 * 											 appending the value to the path
 * @returns {Function} serializer function either from `options.urlSerializer` or the default implementation
 */
SolarReg.Settings.settingsFormUrlFunction = function settingsFormUrlFunction(options) {
	return (options && typeof options.urlSerializer === 'function'
		? options.urlSerializer
		: function defaultEditServiceItemUrlSerializer(action, params) {
			const form = this;
			var result = encodeURI(SolarReg.replaceTemplateParameters(decodeURI(action), params));
			var idVal = (form.elements['id'] ? form.elements['id'].value : '');
			if ( (idVal === undefined || idVal === '') && form.dataset.settingsUrlIdCreateProperty 
					&& form.elements[form.dataset.settingsUrlIdCreateProperty] ) {
				idVal = form.elements[form.dataset.settingsUrlIdCreateProperty].value;
			}
			if ( idVal !== '' ) {
				if ( form.dataset.settingsIdQueryParam ) {
					result += '?' + form.dataset.settingsIdQueryParam + '=';
				} else {
					result += '/';
				}
				result += encodeURIComponent(idVal);
			}
			return result;
		});
};

/**
 * Handle the delete action for a modal edit service item form.
 *
 * This will use an ajax `DELETE` request to submit the service form. By default the form action
 * will have `/{id}` appended to the URL. On success, the modal form will have a `deleted` class
 * added and it will be dismissed. Use the modal's `hidden.bs.modal` event as a hook to perform
 * additional actions.
 *
 * If a `settings-id-query-param` data attribute is set on the form element, then the URL will use
 * a query parameter of the given name to pass the ID value.
 *
 * @param {Event} event the event that triggered the delete action
 * @param {Object} [options] an object with optional configuration properties
 * @param {Function} [options.urlSerializer] an optional function to generate the submit URL; will be passed the form action and form;
 *                                           defaults to appending a `/{id}` value where {id} is the value of the `id`
 *                                           form field
 */
SolarReg.Settings.handleEditServiceItemDeleteAction = function handleEditServiceItemDeleteAction(event, options) {
	var modal = $(event.target).closest('.modal');
	var form = modal.get(0);
	var confirmEl = modal.find('.delete-confirm');
	var submitBtn = modal.find('button[type=submit]');
	var body = SolarReg.Settings.encodeServiceItemForm(form);
	var urlFn = SolarReg.Settings.settingsFormUrlFunction(options);
	if ( confirmEl && confirmEl.hasClass('hidden') ) {
		// show confirm
		confirmEl.removeClass('hidden');

		// disable submit button
		submitBtn.prop('disabled', true);

		// enable "danger" mode in modal
		modal.addClass('danger');
	} else {
		// perform delete
		const deleteUrl = urlFn.call(form, form.action, body);
		$.ajax({
			type: 'DELETE',
			url: deleteUrl,
			dataType: 'json',
			beforeSend: function(xhr) {
				SolarReg.csrf(xhr);
			}
		}).done(function(json) {
			if ( json && json.success === true ) {
				modal.addClass('deleted');
				modal.modal('hide');
			} else {
				var msg = SolarReg.formatResponseMessage(json);
				if ( !msg ) {
					msg = 'Unknown error.';
				}
				SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning', msg);
			}
		}).fail(function(xhr, statusText, error) {
			modal.removeClass('danger');
			modal.find('.delete-confirm').addClass('hidden');
			submitBtn.prop('disabled', false);
			SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning',
				SolarReg.extractResponseMessage(xhr, statusText, error));
		});
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

    if ( !(service && Array.isArray(service.settingSpecifiers)) ) {
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
		} else if (setting.defaultValue !== undefined) {
			result.value = ''+setting.defaultValue;
		} else {
			result.value = '';
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
		val,
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
		if ( name && obj && obj[component] !== undefined ) {
			val = Array.isArray(obj[component])
				? SolarReg.arrayAsDelimitedString(obj[component])
				: obj[component];
			if ( field.type == "date" ) {
				// make sure date formatted
				val = moment(val).format('YYYY-MM-DD');
			}
			$(field).val(val);
		}
	}
}

/**
 * Handle the configuration of a setting toggle button.
 *
 * @param {jQuery} btn the toggle button
 * @param {boolean} enabled {@constant true} if the button is enabled
 */
SolarReg.Settings.handleSettingToggleButtonChange = function handleSettingToggleButtonChange(btn, enabled) {
	if ( !btn ) {
		return;
	}
	btn.toggleClass('btn-success', enabled)
		.toggleClass('btn-default', !enabled)
		.toggleClass('active', enabled)
		.attr('aria-pressed', enabled )
		.button(enabled ? 'on' : 'off')
		.val(enabled ? 'true' : 'false');
}

/**
 * Configure a toggle button.
 *
 * @param {jQuery} btn the toggle button
 * @param {boolean} enabled {@constant true} if the button is enabled
 */
SolarReg.Settings.setupSettingToggleButton = function setupSettingToggleButton(btn, enabled) {
	if ( !(btn.data('toggle') === 'setting-toggle' && btn.data('on-text') && btn.data('off-text')) ) {
		return false;
	}
	SolarReg.Settings.handleSettingToggleButtonChange(btn, enabled); // initialize initial state
	btn.on('click', function() {
		var btn = $(this);
		SolarReg.Settings.handleSettingToggleButtonChange(btn, btn.val() !== 'true');
	});
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
		SolarReg.Settings.setupSettingToggleButton(fieldElement, formItem.value === 'true');
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
	if ( config && config.id !== undefined ) {
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
SolarReg.Settings.handleEditServiceItemAction = function handleEditServiceItemAction(event) {
	var button = $(event.target).closest('.edit-link,.action-link');
	if ( button.length < 1 ) {
		return;
	}
	var config = SolarReg.Templates.findContextItem(button);
	var modal;
	if ( button.hasClass('edit-link') ) {
		modal = $(button.data('editModal'));
	} else if ( button.hasClass('action-link') ) {
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
 * This method iterates over all form fields and maps them into object properties. The field
 * names are split on `.` characters into nested objects.  * A form field can be ignored by adding a `data-settings-ignore` attribute to that field,
 * with any non-empty value.
 *
 * The object property name can include a prefix by adding a `data-settings-prefix`
 * attribute to a form field.
 *
 * The object property can be split into an array by adding a `data-array-delim`
 * attribute with a regular expression value to split the form field with.
 *
 * The object property name can also be derived from a previous form element value, by
 * adding a `data-settings-name-field` attribute to that field. The attribute value is
 * the name of another HTML field that comes before this field (in DOM order).
 *
 * For example, given form fields like
 *
 * ```html
 * <input name="foo" value="bar">
 * <input name="info.name" value="Joe">
 * <input name="into.age" value="99">
 * <select name="answer">
 *   <option value="0">0</option>
 *   <option value="11">11</option>
 *   <option value="42" selected>42</option>
 * </select>
 * <input name="cilent-id" value="abc123" data-settings-prefix="props.">
 * <input name="key" value="car" data-settings-ignore="true">
 * <input name="val" value="mini" data-settings-name-field="key" data-settings-prefix="props.">
 * <input name="array" value="1,2,3" data-array-delim=",">
 * ```
 *
 * An object like the following would be generated:
 *
 * ```javascript
 * {foo:"bar", info:{name:"Joe", age:"99"}, answer:"42", props:{"client-id":abc123, car:"mini"}, array:["1","2","3"]}
 * ```
 *
 * @param {HTMLFormElement} form the form
 * @param {boolean} excludeEmptyProperties {@constant true} to omit empty form field values from the result
 * @returns {Object} the encoded object
 */
SolarReg.Settings.encodeServiceItemForm = function encodeServiceItemForm(form, excludeEmptyProperties) {
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
		// allow name to be derived from a reference to a previous field's value
		if ( field.dataset.settingsNameField ) {
			name = undefined;
			if ( field.value ) {
				for ( let b = i - 1; b >= 0; b -= 1 ) {
					let nameField = fields[b];
					if ( nameField.name === field.dataset.settingsNameField ) {
						if ( nameField && nameField.selectedOptions ) {
							// <select>
							if ( nameField.selectedOptions.length ) {
								name = (nameField.selectedOptions[0].hasAttribute('value')
									? nameField.selectedOptions[0].value
									: undefined)
							}
						} else {
							// <input>
							name = nameField.value;
						}
						break;
					}
				}
			}
		}
		if ( !name || field.dataset.settingsIgnore ) {
			continue;
		}
		if ( field.dataset.settingsPrefix ) {
			name = field.dataset.settingsPrefix + name;
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
			if ( !value && (excludeEmptyProperties || name === 'id') ) {
				// don't populate empty ID value or when excludeEmptyProperties === true
				continue;
			}
		}
		if ( value && field.dataset.arrayDelim ) {
			let delim = field.dataset.arrayDelim;
			try {
				delim = new RegExp(field.dataset.arrayDelim);
			} catch ( err ) {
				// ignore and use as normal string
			}
			value = value.split(delim);
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
 * The `POST` HTTP method will be used, unless the form has a `ajax-method` data attribute, in which case that value
 * will be used. For example to use `PUT` you would have:
 *
 * ```html
 * <form data-ajax-method="put">
 * ```
 *
 * If a `urlId` option is provided and the form has a value in its `id` element, and a `settings-update-method` data
 * attribute exists on the form, that value will be used. For example to `POST` on creation but `PUT` on update:
 *
 * ```html
 * <form method="post" data-settings-update-method="put">
 * ```
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
 * @param {boolean} [options.urlId] if `true` then if the form has an `id` input with a non-empty value, add the value to the submit URL after a `/`;
 * 									can also be provided via a `settings-url-id` data attribute on the form element
 * @param {function} [options.errorMessageGenerator] an optional function called after an error to generate the error message to show,
 *                                                   with (xhr, json, form, formData) arguments
 * @returns {jqXHR} the jQuery XHR object
 */
SolarReg.Settings.handlePostEditServiceForm = function handlePostEditServiceForm(event, onSuccess, serializer, options) {
	event.preventDefault();
	const form = event.target;
	const modal = $(form);
	var body = (typeof serializer === 'function'
		? serializer(form)
		: SolarReg.Settings.encodeServiceItemForm(form));
	const urlFn = SolarReg.Settings.settingsFormUrlFunction(options);
	const urlId = (!!form.dataset.settingsUrlId || (options && options.urlId));
	const submitUrl = urlFn.call(form, decodeURI(form.action), body);
	const origXhr = $.ajaxSettings.xhr;
	var xhrMethod = (form.dataset.ajaxMethod ? form.dataset.ajaxMethod.toUpperCase() : 'POST');
	if ( urlId && form.elements['id'] && form.elements['id'].value && form.dataset.settingsUpdateMethod ) {
		xhrMethod = form.dataset.settingsUpdateMethod.toUpperCase();
	}
	var jqXhrOpts = {
		method: xhrMethod,
		url: submitUrl,
		xhr: function() {
			var xhr = origXhr.apply(this, arguments);
			if ( options && typeof options.upload === 'function' && xhr.upload ) {
				xhr.upload.addEventListener('progress', options.upload);
			}
			if ( options && typeof options.download === 'function' ) {
				xhr.addEventListener('progress', options.download);
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
		console.debug('Save config result: %o', json);
		if ( json && json.success === true ) {
			if ( typeof onSuccess === 'function' ) {
				onSuccess(body, json.data);
			}
			modal.modal('hide');
		} else {
			var msg = (json && json.message ? json.message : 'Unknown error: ' +statusText);
			SolarReg.showAlertBefore(modal.find(SolarReg.Settings.modalAlertBeforeSelector), 'alert-warning', msg);
		}
	}).fail(function(xhr, statusText) {
		var json = {}, msg;
		if ( xhr.responseJSON ) {
			json = xhr.responseJSON;
		} else if ( xhr.responseText ) {
			json = JSON.parse(xhr.responseText);
		}
		if ( options && typeof options.errorMessageGenerator === 'function' ) {
			msg = options.errorMessageGenerator(xhr, json, form, body);
		}
		if ( !msg ) {
			msg = 'Error: ' +(json && json.message ? json.message : statusText);
		}
		var el = modal.find(SolarReg.Settings.modalAlertBeforeSelector);
		SolarReg.showAlertBefore(el, 'alert-warning', msg);
		modal.find('button[type=submit]').prop('disabled', false);
	});
};

/**
 * Handle an add or delete dynamic list item event such as `click`.
 *
 * This handler looks for a `dynamic-list-add` or `dynamic-list-delete` class on the event
 * target hierarchy.
 *
 * If an `add` element is found, then the following actions are taken:
 *
 *  1. Find the closest element with the `dynamic-list` class; this is the dynamic list
 *     "root" element.
 *  2. Within the "root" element, find an element with the `dynamic-list-item template`
 *     classes; this is the list item template to copy.
 *  3. Within the "root" element, find an element with the `dynamic-list-container` class;
 *     this is the container element to copy the template element into.
 *  4. Make a deep clone of the found template element and insert into the container
 *     element.
 *
 * If a `delete` element is found, then the closest ancester with a `dynamic-list-item`
 * class is removed.
 *
 * In general, the expected hierarchy of elements looks like this (actual element names
 * do not matter and are just for illustration, and arbitrary nesting of other elements
 * are allowed):
 *
 * ```
 * ─ div.dynamic-list
 *   ├── button.dynamic-list-add
 *   ├── div.dynamic-list-item.template
 *   │   └── button.dynamic-list-delete
 *   └── div.dynamic-list-container
 * ```
 *
 * @param {event} event the submit event that triggered form submission
 */
SolarReg.Settings.handleDynamicListAddOrDelete = function handleDynamicListAddOrDelete(event) {
	var target = $(event.target);
	if ( target.closest('.dynamic-list-delete').length ) {
		// handle dynamic list delete item
		event.preventDefault();
		target.closest('.dynamic-list-item').remove();
	} else if (target.closest('.dynamic-list-add').length ) {
		// handle dynamic list add item
		event.preventDefault();
		let listRoot = target.closest('.dynamic-list')
			, listTemplate = listRoot.find('.dynamic-list-item.template')
			, listContainer = listRoot.find('.dynamic-list-container');
		SolarReg.Templates.appendTemplateItem(listContainer, listTemplate, {});
	}
	return; // ensure undefined is returned to allow other event handlers to work
};

/**
 * Populate a dynamic list of key/value form element pairs based on the properties of a configuration object.
 *
 * @param {object} listConfig the list configuration object, whose properties will be turned into form elements
 * @param {jQuery} container a container element of existing list items to delete from, if the form closed after a delete action
 * @param {string} listRootClass the CSS class name of the list root element
 * @param {string} inputKeyName the form element name to use for the key element
 * @param {string} inputValueName the form element name to use for the value element
 */
SolarReg.Settings.populateDynamicListObjectKeyValues = function populateDynamicListObjectKeyValues(listConfig, container, listRootClass, inputKeyName, inputValueName) {
	if ( !listConfig ) {
		return;
	}
	let listRoot = container.find('.dynamic-list.'+listRootClass)
		, listTemplate = listRoot.find('.template')
		, listContainer = listRoot.find('.dynamic-list-container');

	for ( itemKey in listConfig ) {
		let itemVal = listConfig[itemKey];
		let newListItem = SolarReg.Templates.appendTemplateItem(listContainer, listTemplate, {});
		newListItem.find('*[name='+inputKeyName+']').val(itemKey);
		newListItem.find('*[name='+inputValueName+']').val(itemVal);
	}
};
