$(document).ready(function() {
	'use strict';

	var exportConfigs = [];
	var adhocExportConfigs = [];
	var dataServices = [{
		id : 'net.solarnetwork.central.datum.export.standard.DefaultDatumExportDataFilterService'
	}];
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	var scheduleTypes = [];
	var aggregationTypes = [];

	var settingTemplates = $('#setting-templates');

	function populateExportConfigs(configs) {
		if ( typeof configs !== 'object' ) {
			configs = {};
		}
		configs.adhocDatumExportConfigs = populateAdhocDatumExportConfigs(adhocExportConfigs || []);
		configs.datumExportConfigs = populateDatumExportConfigs(configs.datumExportConfigs || []);
		configs.dataConfigs = populateDataConfigs(configs.dataConfigs || []);
		configs.destinationConfigs = populateDestinationConfigs(configs.destinationConfigs || []);
		configs.outputConfigs = populateOutputConfigs(configs.outputConfigs || [])
		return configs;
	}

	function populateAdhocDatumExportConfigs(configs, preserve) {
		var container = $('#adhoc-datum-export-list-container');
		var items = configs.map(function(adhocConfig) {
			var statusKey = 'q';
			var config = adhocConfig.config;
			if ( !(config.dataConfiguration && config.dataConfiguration.datumFilter && config.destinationConfiguration && config.outputConfiguration ) ) {
				return;
			}
			var task = adhocConfig.task;
			var model = {
				_contextItem: adhocConfig,
				id: config.id,
				name: config.name,
				startDate: moment(config.dataConfiguration.datumFilter.startDate).format('D MMM YYYY'),
				endDate: moment(config.dataConfiguration.datumFilter.endDate).format('D MMM YYYY'),
				destinationConfigName: config.destinationConfiguration.name,
				outputConfigName: config.outputConfiguration.name,
			};
			if ( config.dataConfiguration.datumFilter.nodeIds && config.dataConfiguration.datumFilter.nodeIds.length > 0 ) {
				model.nodes = config.dataConfiguration.datumFilter.nodeIds.join(', ');
			} else {
				model.nodes = '*';
			}
			if ( config.dataConfiguration.datumFilter.sourceIds && config.dataConfiguration.datumFilter.sourceIds.length > 0 ) {
				model.sources = config.dataConfiguration.datumFilter.sourceIds.join(', ');
			} else {
				model.sources = '*';
			}
			model.aggregation = SolarReg.Templates.serviceDisplayName(aggregationTypes,
					config.dataConfiguration.datumFilter.aggregationKey) || '';
			model.statusClass = 'info';
			if ( task ) {
				model.message = task.message;
				if ( task.completionDate ) {
					model.completed = moment(task.completionDate).format('D MMM YYYY HH:mm');
				}
				statusKey = task.statusKey ? task.statusKey : 'q';
				if ( task.statusKey === 'c' ) {
					model.statusClass = task.success ? 'success' : 'danger';
				}
			}
			model.status = 	container.find('.statuses').data('labelStatus'+statusKey.toUpperCase());
			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve, function(item, el) {
			el.find('.status').addClass('label-' +item.statusClass);
			if ( item.completed ) {
				el.find('.completed.hidden').removeClass('hidden');
			}
		});

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
				if ( configType === 'destination' ) {
					relatedConfig = SolarReg.findByName(exportConfigs.destinationConfigs, config.config.destinationConfiguration.name);
				} else if ( configType === 'output'  ) {
					relatedConfig = SolarReg.findByName(exportConfigs.outputConfigs, config.config.outputConfiguration.name);
				}
				if ( relatedConfig ) {
					SolarReg.Templates.setContextItem(link, relatedConfig);
				}
				// this cannot be edited without a config
				link.toggleClass('edit-link', !!relatedConfig);
			});
		});

		SolarReg.populateListCount(container, configs);
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

			btn.closest('li').find('.edit-link[data-config-type]').each(function(idx, el) {
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

		SolarReg.populateListCount(container, configs);
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

	function handleDeleteConfiguration(id, configs) {
		if ( id === undefined || !Array.isArray(configs) || configs.length < 1 ) {
			return;
		}
		var idx = configs.findIndex(function(el) {
			return (id == el.id);
		});
		if ( idx >= 0 ) {
			configs.splice(idx, 1);
		}
	}

	// ***** Edit ad hoc datum export job form
	$('#edit-adhoc-datum-export-config-modal').on('show.bs.modal', function(event) {
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
			SolarReg.storeServiceConfiguration(res, exportConfigs.adhocDatumExportConfigs);
			populateAdhocDatumExportConfigs([res], true);
		}, function serializeDataConfigForm(form) {
			var data = SolarReg.Settings.encodeServiceItemForm(form);
			if ( data.dataConfiguration.datumFilter ) {
				var filter = data.dataConfiguration.datumFilter;
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
		SolarReg.Settings.resetEditServiceForm(this, $('#datum-export-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				handleDeleteConfiguration(id, exportConfigs.adhocDatumExportConfigs);
				SolarReg.populateListCount($('#datum-export-list-container'), exportConfigs.adhocDatumExportConfigs);
			}
		});
	});

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
			SolarReg.storeServiceConfiguration(res, exportConfigs.datumExportConfigs);
			populateDatumExportConfigs([res], true);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#datum-export-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				handleDeleteConfiguration(id, exportConfigs.datumExportConfigs);
				SolarReg.populateListCount($('#datum-export-list-container'), exportConfigs.datumExportConfigs);
			}
		});
	});

	// ***** Edit data set form
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
			SolarReg.storeServiceConfiguration(res, exportConfigs.dataConfigs);
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
		SolarReg.Settings.resetEditServiceForm(this, $('#export-data-config-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				handleDeleteConfiguration(id, exportConfigs.dataConfigs);
			}
		});
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
			SolarReg.storeServiceConfiguration(res, exportConfigs.destinationConfigs);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-destination-config-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				handleDeleteConfiguration(id, exportConfigs.destinationConfigs);
			}
		});
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
			SolarReg.storeServiceConfiguration(res, exportConfigs.outputConfigs);
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				handleDeleteConfiguration(id, exportConfigs.outputConfigs);
			}
		});
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

	$('.export.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

	$('#datum-export-configs').first().each(function() {
		var loadCountdown = 7;

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

		// get ad hoc jobs
		$.getJSON(SolarReg.solarUserURL('/sec/export/adhoc'), function(json) {
			console.log('Got adhoc exports: %o', json);
			if ( json && json.success === true ) {
				adhocExportConfigs = json.data;
			}
			liftoff();
		});

		$('#datum-export-list-container').on('click', function(event) {
			console.log('Got click on %o export config: %o', event.target, event);
			var el = $(event.target),
				configType = el.data('config-type');
			if ( configType ) {
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
