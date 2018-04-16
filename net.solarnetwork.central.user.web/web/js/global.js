/* Global SolarNetwork App Support */

var SolarReg = {
	showAlertBefore: function(el, clazz, msg) {
	    $('<div class="alert'+(clazz.length > 0 ? ' ' +clazz : '')
	    		+'"><button type="button" class="close" data-dismiss="alert">×</button>'
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

$(document).ready(function() {
	$('body').on('hidden', '.modal.dynamic', function () {
		$(this).removeData('modal');
	});
	
	$('a.logout').on('click', function(event) {
		event.preventDefault();
		$('#logout-form').get(0).submit();
	});
});
