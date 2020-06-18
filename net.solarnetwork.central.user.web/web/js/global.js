'use strict';
/* Global SolarNetwork App Support */

var SolarReg = {
	showAlertBefore: function(el, clazz, msg) {
	    $('<div class="alert'+(clazz.length > 0 ? ' ' +clazz : '')
	    		+'"><button type="button" class="close" data-dismiss="alert">\u00d7</button>'
	    		+msg +'</div>').insertBefore(el);
	},
	
	solarUserURL : function(relativeURL) {
		return $('meta[name=solarUserRootURL]').attr('content') + relativeURL;
	},

	csrfData : (function() {
		var csrf = $("meta[name='csrf']").attr("content"),
			header = $("meta[name='csrf_header']").attr("content");
		return {token:csrf,headerName:header};
	}()),
	};

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
 * @param {array} array the array of objects to search through
 * @param {string} identifier the `id` value to search for
 * @returns {object} the first object that has a matching `id` property
 */
SolarReg.findByIdentifier = function findByIdentifier(array, identifier) {
	var result;
	if ( identifier && Array.isArray(array) ) {
		result = array.find(function(obj) {
			return obj.id === identifier;
		});
	}
	return result;
 };

 /**
  * Split a string into an array of numbers.
  * 
  * This method will filter out any values that are not numbers.
  * 
  * @param {string} string the string to split into numbers
  * @param {RegExp} [delimiter] the regular expression to split with; defaults to comma with optional surrounding whitespace
  * @returns {array<Number>}
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
  * Split a string into an array of numbers.
  * 
  * This method will filter out any values that are not numbers.
  * 
  * @param {array} array the array to join into a string
  * @param {string} [delimiter] the string to join elements with; defaults to comma and space
  * @returns {string}
  */
SolarReg.arrayAsDelimitedString = function arrayAsDelimitedString(array, delimiter) {
	// allow the empty string delimiter
	delimiter = (delimiter !== undefined ? delimiter : ', ');
	if ( !(Array.isArray(array) && array.length > 0) ) {
		return '';
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
	var i, len, prop, existing;
	for ( i = 0, len = configurations.length; i < len; i += 1 ) {
		existing = configurations[i];
		if ( config.id === existing.id ) {
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
			return;
		}
	}
	configurations.push(config);
};

/**
 * Update counter elements with a `listCount` class with the count of items in an array.
 * 
 * @param {jQuery} container the element to search for `.listCount` elements in
 * @param {Array} configurations the array whose lenght to use
 */
SolarReg.populateListCount = function populateListCount(container, configurations) {
	if ( !container ) {
		return;
	}
	container.closest('section').find('.listCount').text(
		Array.isArray(configurations) ? configurations.length : 0);
}

$(document).ready(function() {
	$('body').on('hidden', '.modal.dynamic', function () {
		$(this).removeData('modal');
	});
	
	$('a.logout').on('click', function(event) {
		event.preventDefault();
		$('#logout-form').get(0).submit();
	});
});
