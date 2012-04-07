/**
 * SolarNetwork DRAS About API.
 * 
 * @class SolarNetwork DRAS About API
 * @constructor
 * @param {Object} config configuration parameters
 * @param {Number} elementId the ID of the table to populate
 */
SolarNetwork.DRAS.About = function(config) {
	config = typeof config === 'object' ? config : {elementId:config};
	
	var me = this;
	var elementId = undefined;
	var context = '';
	var aboutAppUrl = '/app.do'
	
	var init = function(cfg) {
		elementId = typeof cfg.elementId === 'string' ? cfg.elementId : undefined;
		context = typeof cfg.context === 'string' ? cfg.context : '';
		aboutAppUrl = typeof cfg.aboutAppUrl === 'string' ? cfg.aboutAppUrl : '/app.json';
	};
	
	var getAboutAppUrl = function() {
		return context + aboutAppUrl;
	};
	
	/**
	 * Update the about table.
	 */
	this.update = function() {
		$.getJSON(getAboutAppUrl(), function(data) {
			var table = $('#'+elementId + ' tbody').last();
			for ( var prop in data.app ) {
				var row = $('<tr></tr>');
				var info = data.app[prop];
				row.append($('<th></th>').text(info.title === undefined ? prop : info.title));
				row.append($('<td></td>').text(info.value));
				table.append(row);
			}
		});
	};

	init(config);
};
