$(document).ready(function() {
	'use strict';
	
	var exportConfigs = {};
	var dataServices = [{
		id : 'net.solarnetwork.central.datum.export.standard.DefaultDatumExportDataFilterService'
	}];
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	var scheduleTypes = [];
	var aggregationTypes = [];

	var settingTemplates = $('#export-setting-templates');
	
	function populateExportConfigs(configs) {
		if ( typeof configs !== 'object' ) {
			configs = {};
		}
		configs.datumExportConfigs = populateDatumExportConfigs(configs.datumExportConfigs);
		configs.dataConfigs = populateDataConfigs(configs.dataConfigs);
		configs.destinationConfigs = populateDestinationConfigs(configs.destinationConfigs);
		configs.outputConfigs = populateOutputConfigs(configs.outputConfigs)
		return configs;
	}

	function populateDatumExportConfigs(configs, preserve) {
		var container = $('#datum-export-list-container');
		var items = configs.map(function(config) {
			var relatedConfig;
			var model = {
				_contextItem: config,
				id: config.id,
				name: config.name,
				schedule: SolarReg.Templates.serviceDisplayName(scheduleTypes, config.scheduleKey),
				date: config.startingExportDate,
				dataConfigId: config.dataConfigurationId,
				destinationConfigId: config.destinationConfigurationId,
				outputConfigId: config.outputConfigurationId,
			};
			if ( model.schedule ) {
				model.schedule = model.schedule.toLowerCase();
			}
			if ( config.dataConfigurationId ) {
				relatedConfig = SolarReg.findByIdentifier(exportConfigs.dataConfigs, config.dataConfigurationId);
				if ( relatedConfig ) {
					model.dataConfigName = relatedConfig.name;
				}
			}
			if ( !model.dataConfigName ) {
				model.dataConfigName = '?';
			}
			if ( config.destinationConfigurationId ) {
				relatedConfig = SolarReg.findByIdentifier(exportConfigs.destinationConfigs, config.destinationConfigurationId);
				if ( relatedConfig ) {
					model.destinationConfigName = relatedConfig.name;
				}
			}
			if ( !model.destinationConfigName ) {
				model.destinationConfigName = '?';
			}
			if ( config.outputConfigurationId ) {
				relatedConfig = SolarReg.findByIdentifier(exportConfigs.outputConfigs, config.outputConfigurationId);
				if ( relatedConfig ) {
					model.outputConfigName = relatedConfig.name;
				}
			}
			if ( !model.outputConfigName ) {
				model.outputConfigName = '?';
			}
			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);

		// attach actual service configuration to each link
		container.find('a.edit-link:not(*[data-config-type])').each(function(idx, el) {
			var btn = $(el),			
				config = SolarReg.Templates.findContextItem(btn);
			if ( !config ) {
				return;
			}

			btn.parent().find('.edit-link[data-config-type]').each(function(idx, el) {
				var link = $(el),
					configType = link.data('config-type'),
					relatedConfig;
				if ( configType === 'data' ) {
					relatedConfig = SolarReg.findByIdentifier(exportConfigs.dataConfigs, config.dataConfigurationId);
				} else if ( configType === 'destination' ) {
					relatedConfig = SolarReg.findByIdentifier(exportConfigs.destinationConfigs, config.destinationConfigurationId);
				} else if ( configType === 'output'  ) {
					relatedConfig = SolarReg.findByIdentifier(exportConfigs.outputConfigs, config.outputConfigurationId);
				} else if ( configType === 'date' ) {
					relatedConfig = config;
				}
				if ( relatedConfig ) {
					SolarReg.Templates.setContextItem(link, relatedConfig);
				}
				// this cannot be edited without a config
				link.toggleClass('edit-link', !!relatedConfig);
			});
		});

		container.closest('section').find('.listCount').text(configs.length);
		return configs;
	}
	
	function populateDataConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-data-config-list-container');
		var items = configs.map(function(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, dataServices);
			var filter = config.datumFilter || {};
			model.aggregation = SolarReg.Templates.serviceDisplayName(aggregationTypes, filter.aggregationKey) || '';
			var nodeIds = SolarReg.arrayAsDelimitedString(filter.nodeIds);
			model.nodes = nodeIds || '*';
			var sourceIds = SolarReg.arrayAsDelimitedString(filter.sourceIds);
			model.sources = sourceIds || '*';
			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		container.closest('section').find('.listCount').text(configs.length);
		return configs;
	}
	
	function populateDestinationConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#export-destination-config-list-container');
		var items = configs.map(function(config) {
			return SolarReg.Settings.serviceConfigurationItem(config, destinationServices);
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		container.closest('section').find('.listCount').text(configs.length);
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
		container.closest('section').find('.listCount').text(configs.length);
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

	function handleServiceIdentifierChange(event, services) {
		var target = event.target;
		console.log('change event on %o: %o', target, event);
		if ( target.name === 'serviceIdentifier' ) {
			var service = SolarReg.findByIdentifier(services, $(event.target).val());
			var modal = $(target.form);
			var container = modal.find('.service-props-container').first();
			SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, modal.data('context-item'));
		}
	}

	// ***** Edit datum export job form
	$('#edit-datum-export-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), [], settingTemplates);
		SolarReg.Templates.populateServiceSelectOptions(exportConfigs.dataConfigs, 'select[name=dataConfigurationId]');
		SolarReg.Templates.populateServiceSelectOptions(exportConfigs.destinationConfigs, 'select[name=destinationConfigurationId]');
		SolarReg.Templates.populateServiceSelectOptions(exportConfigs.outputConfigs, 'select[name=outputConfigurationId]');
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, []);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDatumExportConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.datumExportConfigs);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#datum-export-list-container .list-container'));
	});

	// ***** Edit data format form
	$('#edit-export-data-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), dataServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, dataServices);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDataConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.dataConfigs);
		}, function serializeDataConfigForm(form) {
			var data = SolarReg.Settings.encodeServiceItemForm(form);
			if ( data.datumFilter ) {
				var filter = data.datumFilter;
				var nodeIds =  SolarReg.splitAsNumberArray(filter.nodeIds);
				if ( nodeIds.length ) {
					filter.nodeIds = nodeIds;
				} else {
					delete filter.nodeIds;
				}
				var sourceIds = (filter.sourceIds ? filter.sourceIds.split(/\s*,\s*/) : []);
				if ( sourceIds.length ) {
					filter.sourceIds = sourceIds;
				} else {
					delete filter.sourceIds;
				}
			}
			return data;
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-data-config-list-container .list-container'));
	});

	$('#export-data-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, dataServices, settingTemplates);
	});
	
	// ***** Edit destination form
	$('#edit-export-destination-config-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), destinationServices, settingTemplates);
	}).on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, destinationServices);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDestinationConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.destinationConfigs);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
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
	.on('change', function(event) {
		handleServiceIdentifierChange(event, outputServices);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateOutputConfigs([res], true);
			storeServiceConfiguration(res, exportConfigs.outputConfigs);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	// ***** Edit job date form
	$('#edit-datum-export-date-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), [], settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('submit', function(event) {
		var form = event.target;
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			var config = SolarReg.Templates.findContextItem(form);
			if ( config ) {
				config.startingExportDate = res;
				populateDatumExportConfigs([config], true);
			}
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		// TODO SolarReg.Settings.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	$('#export-output-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, outputServices, settingTemplates);
	});

	$('.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

	$('#datum-export-configs').first().each(function() {
		var loadCountdown = 6;

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				populateExportConfigs(exportConfigs);
				$('.datum-export-getstarted').toggleClass('hidden', 
					exportConfigs.datumExportConfigs && exportConfigs.datumExportConfigs.length > 0);
			}
		}

		// get available compression services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/compression'), function(json) {
			console.log('Got export compression types: %o', json);
			if ( json && json.success === true ) {
				compressionTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-output-compression-types');
			}
			liftoff();
		});

		// get available schedule services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/schedule'), function(json) {
			console.log('Got export schedule types: %o', json);
			if ( json && json.success === true ) {
				scheduleTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-output-schedule-types');
			}
			liftoff();
		});
		
		// get available aggregation services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/aggregation'), function(json) {
			console.log('Got export aggregation types: %o', json);
			if ( json && json.success === true ) {
				aggregationTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.export-data-aggregation-types');
			}
			liftoff();
		});
		
		// get available output services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/output'), function(json) {
			console.log('Got export output services: %o', json);
			if ( json && json.success === true ) {
				outputServices = json.data;
			}
			liftoff();
		});

		// get available destination services
		$.getJSON(SolarReg.solarUserURL('/sec/export/services/destination'), function(json) {
			console.log('Got export destination services: %o', json);
			if ( json && json.success === true ) {
				destinationServices = json.data;
			}
			liftoff();
		});

		// list all configs
		$.getJSON(SolarReg.solarUserURL('/sec/export/configs'), function(json) {
			console.log('Got export configurations: %o', json);
			if ( json && json.success === true ) {
				exportConfigs = json.data;
			}
			liftoff();
		});

		$('#datum-export-list-container').on('click', function(event) {
			console.log('Got click on %o export config: %o', event.target, event);
			var target = event.target,
				el = $(event.target),
				configType = el.data('config-type');
			if ( configType ) {
				var config = SolarReg.Templates.findContextItem(el);
				if ( configType === 'data' ) {
					SolarReg.Settings.handleEditServiceItemAction(event, dataServices, settingTemplates);
				} else if ( configType === 'destination' ) {
					SolarReg.Settings.handleEditServiceItemAction(event, destinationServices, settingTemplates);
				} else if ( configType === 'output'  ) {
					SolarReg.Settings.handleEditServiceItemAction(event, outputServices, settingTemplates);
				} else if ( configType === 'date' ) {
					SolarReg.Settings.handleEditServiceItemAction(event, [], settingTemplates);
				}
			} else {
				SolarReg.Settings.handleEditServiceItemAction(event, [], settingTemplates);
			}
			event.preventDefault();
		});
	});
	
});