/**
 * SolarNetwork base API.
 * 
 * @namespace SolarNetwork base API
 */
var SolarNetwork = {
	
	version : '0.0.1',
	host : '',
	
	/**
	 * Enumeration of possible datum types.
	 * @class
	 * @property {String} consumption consumption (power)
	 * @property {String} day day, e.g. sunrise, sunset, average temperature, etc.
	 * @property {String} power power generation
	 * @property {String} price power price
	 * @property {String} weather weather, e.g. temperature, humidity, etc.
	 */
	datumTypes : {
		consumption : "Consumption",
		day : "Day",
		power : "Power",
		price : "Price",
		weather : "Weather"
	},
	
	/**
	 * Enumeration of possible reporting aggregate types.
	 * @class
	 * @property {String} minute minute level aggregation
	 * @property {String} hour hour level aggregation
	 * @property {String} day day level aggregation
	 * @property {String} week week level aggregation
	 * @property {String} month month level aggregation
	 */
	aggregateTypes : {
		minute : "Minute",
		hour : "Hour",
		day : "Day",
		week : "Week",
		month : "Month"
	},
	
	/**
	 * Log a message to the console.
	 * 
	 * @param {String} msg the message to log
	 */
	log : function(msg) {
		if ( eval('typeof console') !== 'undefined' ) {
			console.log.apply(console, arguments);
		}
	},
	
	/**
	 * Log a debug-level message to the console.
	 * 
	 * @param {String} msg the message to log
	 */
	debug : function(msg) {
		// TODO: conditionally log based on config settings
		SolarNetwork.log.apply(this, arguments);
	},
	
	/**
	 * Log an error message.
	 * 
	 * @param {String} msg the message to log as an error
	 */
	error : function(msg) {
		SolarNetwork.log('!! ' +msg);
	},
	
	/**
	 * Test if an object is an Array.
	 * 
	 * @param {Object} obj the object to test
	 * @returns {Boolean} true if obj is an Array
	 */
	isArray : function(obj) {
		return Object.prototype.toString.apply(obj) === '[object Array]';
	},
	
	/**
	 * Test if an object is an Array and has at least one value.
	 * 
	 * @param {Object} obj the object to test
	 * @returns {Boolean} true if obj is an Array and has one more more values
	 */
	isNonEmptyArray : function(obj) {
		return Object.prototype.toString.apply(obj) === '[object Array]' && obj.length > 0;
	},
	
	/**
	 * Obtain an absolute URL for a SolarNetwork service relative URL.
	 * 
	 * @param {String} url a relative URL
	 * @returns {String} URL
	 */
	networkUrl : function(url) {
		return SolarNetwork.host + url;
	},
	
	/**
	 * Dispatch an event to an object, setting the <em>this</em> reference
	 * to the object, rather than the DOM element that triggered the event.
	 * 
	 * <p>The target object must be passed as event data on the <em>target</em>
	 * property. The function to call must be passed as event data on the
	 * <em>action</em> property.</p>
	 * 
	 * @param {Object} event the jQuery Event object
	 */
	dispatchEvent : function(event) {
		if ( event.data === undefined || event.data.target === undefined
				|| event.data.action === undefined ) {
			SolarNetwork.error("Event dispatch target not specefied on 'delegate' property.");
			return;
		}
		var action = event.data.target[event.data.action];
		if ( action === undefined || typeof action !== 'function' ) {
			SolarNetwork.error("Event dispatch function [" +event.data.action 
				+"] not available on target object [" +event.data.target +"]");
		}
		event.data.target[event.data.action].call(event.data.target, event);
	},
	
	/**
	 * Handle a JSON AJAX response.
	 * 
	 * <p>Parse a JSON string and optionally invoke a callback function.</p>
	 * 
	 * @param {String} data the JSON encoded data string
	 * @param {Object} target an optional calllback target (will be set to <em>this</em> in callback)
	 * @param {Function} callback an optional callback function
	 * @return the parsed JSON object
	 */
	handleJson : function(data, target, callback) {
		if ( !data ) {
			return undefined;
		}
		if ( data.length > 2 && data.charAt(0) === '(' ) {
			// remove wrapping parens for native JSON parsing
			data = data.slice(1, -1);
		}
		data = JSON.parse(data);
		if ( target && callback ) {
			callback.call(target, data);
		}
		return data;
	},
	
	/**
	 * Get the number of properties defined on an object.
	 * 
	 * @param obj the object to test
	 * @returns {Number} the number of properties
	 */
	propertyCount : function(obj) {
		if ( typeof obj !== 'object' ) {
			return 0;
		}
		var count = 0;
		var k = undefined;
		for ( k in obj ) {
			if ( obj.hasOwnProperty(k) ) {
				count++;
			}	
		}
	},
	
	whitespaceRegexp : /^\s+$/,
	
	/**
	 * Modify an array of form name/value pair objects, removing empty values.
	 * 
	 * <p>Each item in the array is expected to have 'name' and 'value' 
	 * properties. If the 'value' property is an empty string, that item will
	 * be removed from the array.</p>
	 * 
	 * @param array array of objects
	 */
	emptyStringRemove : function(array) {
		// iterate backwards, to remove as we go
		var idx = 0;
		var val = undefined;
		for ( var i = array.length; i > 0; i-- ) {
			idx = i - 1;
			val = array[idx].value;
			if ( val === '' || (typeof val === 'string' && val.match(SolarNetwork.whitespaceRegexp)) ) {
				array.splice(idx, 1);
			}
		}
	}

};
