
// helper global variable
var I18N = undefined;

/**
 * SolarNetwork DRAS About API.
 * 
 * @class SolarNetwork DRAS About API
 * @constructor
 * @param {Object} config configuration parameters
 * @param {Number} elementId the ID of the table to populate
 */
SolarNetwork.DRAS.Messages = function(config) {
	config = typeof config === 'object' ? config : {};
	
	var me = this;
	var context = '';
	var contextUi = '';
	var msgUrl = '/msgs.json';
	var messages = {};
	var callback = undefined;
	var loaded = false;
	
	var init = function(cfg) {
		elementId = typeof cfg.elementId === 'string' ? cfg.elementId : undefined;
		context = typeof cfg.context === 'string' ? cfg.context : '';
		contextUi = typeof cfg.contextUi === 'string' ? cfg.contextUi : '';
		msgUrl = typeof cfg.msgUrl === 'string' ? cfg.msgUrl : msgUrl;
		callback = typeof cfg.callback === 'function' ? cfg.callback : undefined;
	};
	
	var getAppMsgUrl = function() {
		return context + msgUrl;
	};
	
	var getUiMsgUrl = function() {
		return contextUi + msgUrl;
	};
	
	/**
	 * Test if the messages have been loaded.
	 * 
	 * @returns {Boolean} true if message loaded
	 */
	this.isLoaded = function() {
		return loaded;
	};

	/**
	 * Look for a message in either the UI or App bundle.
	 * 
	 * @param key {String} the message key to look for
	 * @returns the associated message, or undefined if not available
	 */
	this.msg = function(key) {
		if ( messages.ui[key] !== undefined ) {
			return messages.ui[key];
		}
		return messages.app[key];
	};
	
	/**
	 * Update the about table.
	 */
	this.update = function() {
		var count = 0;
		var handleComplete = function() {
			count++;
			if ( count > 1 ) {
				// we're done, notify callback
				loaded = true;
				SolarNetwork.debug('Loaded %d app and %d UI i18n messages', 
						SolarNetwork.propertyCount(messages.app),
						SolarNetwork.propertyCount(messages.ui));
				if ( I18N === undefined ) {
					// set up global reference to self
					I18N = me;
				}
				if ( callback !== undefined ) {
					callback.call(me);
				}
			}
		};
		$.getJSON(getAppMsgUrl(), function(data) {
			messages.app = data.messages;
			handleComplete();
		});
		$.getJSON(getUiMsgUrl(), function(data) {
			messages.ui = data.messages;
			handleComplete();
		});
	};

	init(config);
	this.update();
};
