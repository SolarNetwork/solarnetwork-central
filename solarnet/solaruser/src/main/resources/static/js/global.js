'use strict';
/* Global SolarNetwork App Support */

var SolarReg = {
	/**
	 * Insert an alert element before a given element.
	 *
	 * @param {jQuery} el the element to insert the alert before
	 * @param {String} clazz an alert class to add
	 * @param {String} msg a message to show
	 * @returns {void}
	 */
	showAlertBefore: function(el, clazz, msg) {
	    $('<div class="alert'+(clazz.length > 0 ? ' ' +clazz : '')
	    		+'"><button type="button" class="close" data-dismiss="alert">\u00d7</button>'
	    		+msg +'</div>').insertBefore(el);
	},

	solarUserURL: function(relativeURL) {
		return $('meta[name=solarUserRootURL]').attr('content') + relativeURL;
	},

	solarUserPublicURL: function(relativeURL) {
		return $('meta[name=solarUserRootPublicURL]').attr('content').replace(/\/$/, '') + relativeURL;
	},

	csrfData: (function() {
		var csrf = $("meta[name='csrf']").attr("content"),
			header = $("meta[name='csrf_header']").attr("content");
		return {token:csrf,headerName:header};
	}()),
	
	/**
	 * A case-insensitive, numeric-aware natural sort collator.
	 * 
	 * @type {Intl.Collator}
	 */
	naturalSortCollator: new Intl.Collator(undefined, {numeric: true, sensitivity: 'base'}),

};

/**
 * Comparison function using "natural sort".
 *
 * @param {string} left the element to insert the alert before
 * @param {string} right an alert class to add
 * @returns {number} -1, 0, or 1 if `left` is less than, equal to, or greater than `right`
 */
SolarReg.naturalSort = SolarReg.naturalSortCollator.compare;

/**
 * Get the CSRF token value or set the token as a request header on an XHR object.
 *
 * @param {XMLHttpRequest} [xhr] The XHR object to set the CSR request header on.
 * @return The CSRF value.
 */
SolarReg.csrf = function(xhr) {
	 if ( xhr && typeof xhr.setRequestHeader === 'function' ) {
		 xhr.setRequestHeader(SolarReg.csrfData.headerName, SolarReg.csrfData.token);
	 }
	 return SolarReg.csrfData.token;
};

/**
 * Search for an object with a matching `id` property value.
 *
 * @param {Iterable} array the iterable list to search through
 * @param {String} identifier the `id` value to search for
 * @returns {Object} the first object that has a matching `id` property
 */
SolarReg.findByIdentifier = function findByIdentifier(array, identifier) {
	if ( identifier && array ) {
		for ( const el of array ) {
			if ( el.id === identifier ) {
				return el;
			}
		}
	}
	return undefined;
 };

/**
 * Search for an object with a matching `name` property value.
 *
 * @param {Iterable} array the iterable list of objects to search through
 * @param {String} name the `name` value to search for
 * @returns {Object} the first object that has a matching `name` property
 */
SolarReg.findByName = function findByName(array, name) {
	if ( name && array ) {
		for ( const el of array ) {
			if ( el.name === identifier ) {
				return el;
			}
		}
	}
	return undefined;
 };

 /**
  * Split a string into an array of numbers.
  *
  * This method will filter out any values that are not numbers.
  *
  * @param {string} string the string to split into numbers
  * @param {RegExp} [delimiter] the regular expression to split with; defaults to comma with optional surrounding whitespace
  * @returns {Array.<Number>}
  */
SolarReg.splitAsNumberArray = function splitAsNumberArray(string, delimiter) {
	 delimiter = (delimiter || /\s*,\s*/);
	 if ( !string ) {
		 return [];
	 }
	 return string.split(delimiter)
		 .map(function(id) { return Number(id); })
		 .filter(function(id) { return !isNaN(id); });
 };

 /**
  * Delete empty string properties from an object.
  *
  * @param {Object} obj the object to remove empty string properties from
  * @param {boolean} recurse {@constant true} to recurse into nested objects
  * @returns {void}
  */
 SolarReg.deleteEmptyProperties = function deleteEmptyProperties(obj, recurse) {
	 var prop;
	 if ( typeof obj !== 'object' ) {
		 return;
	 }
	 for ( prop in obj ) {
		if ( !Object.prototype.hasOwnProperty.call(obj, prop) ) {
			continue;
		}
		if ( typeof obj[prop] === 'string' && obj[prop].trim().length < 1 ) {
			delete obj[prop];
		} else if ( recurse && typeof obj[prop] === 'object' ) {
			deleteEmptyProperties(obj[prop], true);
		}
	 }
 };

/**
 * Split a string into an array of numbers.
 *
 * This method will filter out any values that are not numbers.
 *
 * @param {array} array the array to join into a string
 * @param {string} [delimiter] the string to join elements with; defaults to comma and space
 * @param {string} [emptyValue] the value to display if the array is empty or undefined;
 *                              defaults to an empty string
 * @returns {string}
 */
SolarReg.arrayAsDelimitedString = function arrayAsDelimitedString(array, delimiter, emptyValue) {
	// allow the empty string delimiter
	delimiter = (delimiter !== undefined ? delimiter : ', ');
	if ( !(Array.isArray(array) && array.length > 0) ) {
		return emptyValue !== undefined ? emptyValue : '';
	}
	return array.join(delimiter);
};


/**
 * Replace template variables on a string with corresponding values from a parameter object.
 *
 * The template variables take the form of `{x}` where `x` is the parameter name.
 *
 * @param {string} str the string to replace template values on
 * @param {object} params the substitution parameter values
 * @returns {string} the string with parameters replaced
 */
SolarReg.replaceTemplateParameters = function replaceTemplateParameters(str, params) {
	var re = /\{(\w+)\}/g;
	return str.replace(re, function(match, p1) {
		return params[p1] || '';
	});
};

/**
 * Either update an existing or add a new service configuration to an array of configurations.
 *
 * If an existing object in `configurations` has an `id` that matches `config.id` then
 * that element's properties will be replaced by those on `config`. Otherwise `config`
 * will be appended to `configurations`.
 *
 * @param {Object} config the configuration to save
 * @param {Array} configurations the array of existing configurations
 */
SolarReg.storeServiceConfiguration = function storeServiceConfiguration(config, configurations) {
	if ( !(config && config.id && Array.isArray(configurations)) ) {
		return;
	}
	var prop, existing;
	existing = SolarReg.findByIdentifier(configurations, config.id);
	if ( existing ) {
		// found an existing configuration; so update the properties on that
		// to match the new configuration
		for ( prop in config ) {
			if ( !config.hasOwnProperty(prop)  ) {
				continue;
			}
			existing[prop] = config[prop];
		}
		for ( prop in existing ) {
			if ( !config.hasOwnProperty(prop) ) {
				delete existing[prop];
			}
		}
	} else {
		configurations.push(config);
	}
};

/**
 * Save service configurations to an array.
 *
 * @param {Array.<Object>} updates the configurations that have been updated
 * @param {boolean} preserve {@constant true} to update existing configurations, {@constant false} to replace all existing configurations
 * @param {Array.<Object>} configs the service configurations array which will be updated
 * @param {jQuery} [container] the container whose closest `section` while have a  `.listCount` element updated with the final configuration count
 * @returns {Array.<Object>} the {@code configs} array
 */
 SolarReg.saveServiceConfigurations = function saveServiceConfigurations(updates, preserve, configs, container) {
	if ( !Array.isArray(updates) ) {
		return;
	}
	if ( preserve ) {
		for ( const config of updates ) {
			SolarReg.storeServiceConfiguration(config, configs);
		}
	} else {
		configs.length = 0;
		configs.push(...updates);
	}
	if ( container ) {
		SolarReg.populateListCount(container, configs);
	}
	return configs;
};

/**
* Handle removing a deleted configuration from an array of service configurations.
*
* @param {Number|String} deletedId the deleted config ID
* @param {Array.<Object>} configs the array of configurations to delete from
* @param {jQuery} container the container to update the config count in
*/
SolarReg.deleteServiceConfiguration = function deleteServiceConfiguration(deletedId, configs, container) {
	if ( !(deletedId && Array.isArray(configs)) ) {
		return;
	}
	const idx = configs.findIndex(function(el) {
		return (el.id === deletedId);
	});
	if ( idx >= 0 ) {
		configs.splice(idx, 1);
	}
	SolarReg.populateListCount(container, configs);
};

/**
 * Update counter elements with a `listCount` class with the count of items in an array.
 *
 * @param {jQuery} container the element to search for `.listCount` elements in
 * @param {Array.<Object>} configurations the array whose lenght to use
 */
 SolarReg.populateListCount = function populateListCount(container, configurations) {
	if ( !container ) {
		return;
	}
	container.closest('section').find('.listCount').text(
		Array.isArray(configurations) ? configurations.length : 0);
};

/**
 * Extract an error message from a JSON response object.
 *
 * @param {Object} json the JSON response with a message and optional code properties
 * @returns {String} error message
 */
SolarReg.formatResponseMessage = function formatResponseMessage(json) {
	var result = (json ? json.message : "");
	if ( result && json.code ) {
		result += " (" +json.code +")";
	}
	return result;
};

/**
 * Extract an error message from an XHR error response.
 *
 * @param {XMLHttpRequest} xhr the request
 * @returns {String} error message
 */
SolarReg.extractResponseMessage = function extractResponseMessage(xhr, statusText, error) {
	var result = "";
	if ( xhr.responseJSON ) {
		result = SolarReg.formatResponseMessage(xhr.responseJSON);
	}
	if ( !result ) {
		try {
			json = JSON.parse(xhr.responseText);
			result = SolarReg.formatResponseMessage(json);
		} catch (ex) {
			// ignore
		}
	}
	if ( !result ) {
		if ( error ) {
			result = error;
		} else if ( statusText ) {
			result = statusText;
		} else {
			result = "Unknown error.";
		}
	}
	return result;
};

/**
 * Extract i18n data attributes from a DOM element.
 *
 * All data attributes whose name starts with `i18n-` will be returned.
 *
 * @param {DOMElement} el the element to extract the i18n data attributes from
 * @returns {Object} the data attributes
 */
SolarReg.i18nData = function i18nData(el) {
	if ( !el ) {
		return;
	}
	let data = el.dataset,
		result = {},
		val;
	for ( let prop in data ) {
		if ( prop.startsWith('i18n') ) {
			val = data[prop];
			if ( !val ) {
				continue;
			}
			prop = prop.substring(4);
			prop = prop.charAt(0).toLowerCase() + prop.substring(1);
			result[prop] = val;
		}
	}
	return result;
};

/**
 * Toggle the visibility of all `.page-loading` (to hidden) and `.page-loaded` (to visible) elements.
 */
SolarReg.showPageLoaded = function showPageLoaded() {
	$('.page-loading').addClass('hidden');
	$('.page-loaded').removeClass('hidden');
};

/**
 * Copy all enumerable properties one one object into another, as long as they do not already
 * exist in the destination object.
 * 
 * @param {object} dest the destination object to copy properties to
 * @param {src} object the source t copy properties from
 */
SolarReg.fill = function fill(dest, src) {
	if (!(dest && src)) {
		return;
	}
	for (let prop in src) {
		if (dest[prop] === undefined ) {
			dest[prop] = src[prop];
		}
	}
};

$(document).ready(function() {
	$('body').on('hidden', '.modal.dynamic', function () {
		$(this).removeData('modal');
	});

	$('[data-toggle="tooltip"]').tooltip();

	/* ==================
	   Global i18n
	   ================== */
	SolarReg.i18n = SolarReg.i18nData(document.querySelector('body'));

	/* ==================
	   Copy form support
	   ================== */
	$('button.copy').on('click', function copyFieldValue() {
		var btn = $(this),
			copyable = btn.closest('.copyable-container').find('.copyable');
		if ( copyable.length ) {
			copyable[0].select();
			try {
				let success = document.execCommand('copy');
				if ( success ) {
					btn.trigger('copied', [SolarReg.i18n.copiedTooltip || 'Copied!']);
				} else {
					btn.trigger('copied', [SolarReg.i18n.copyFailedTooltip || 'Failed!']);
				}
			} catch ( err ) {
				btn.trigger('copied', [SolarReg.i18n.copyFailedTooltip || 'Failed!']);
			}
			}
	}).on('copied', function handleCopyResult(event, message) {
		$(this).attr('title', message)
			.tooltip('fixTitle')
			.tooltip('show')
			.attr('title', SolarReg.i18n.copyTooltip)
			.tooltip('fixTitle');
	});

}).ajaxComplete(function(event, xhr, ajaxOptions) {
	// look for X-LoginFormPage header to handle auto client-side redirect to login page
	if ( xhr.readyState == 4 && "true" === xhr.getResponseHeader("X-LoginFormPage") ) {
		window.location.href = SolarReg.solarUserPublicURL('/login');
	}
});
