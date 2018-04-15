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
			return SolarReg.Settings.serviceConfigurationItem(config, destinationServices);
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}
	
	function populateOutputConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-output-config-list-container');
		var items = configs.map(function(config) {
			var item = SolarReg.Settings.serviceConfigurationItem(config, outputServices);
			item.compression = SolarReg.Templates.serviceDisplayName(compressionTypes, config.compressionTypeKey);
			return item;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		return configs;
	}

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
	function storeServiceConfiguration(config, configurations) {
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
	}

	function renderJobsList(configs) {
		
	}

	// ***** Edit destination form
	$('#edit-export-destination-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), destinationServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDestinationConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.destintationConfigs);
		});
		return false;
	}).on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-destination-config-list-container .list-container'));
	});

	$('#export-destination-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, destinationServices, settingTemplates);
	});


	// ***** Edit output format form
	$('#edit-export-output-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), outputServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateOutputConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.outputConfigs);
		});
		return false;
	}).on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	$('#export-output-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, outputServices, settingTemplates);
	});

	$('.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

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