$(document).ready(function() {
	'use strict';
	
	var hookConfigs = [];
	var hookServices = [];
	var topicTypes = [];

	var settingTemplates = $('#setting-templates');

	function populateHookConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#node-event-hook-list-container');
		var items = configs.map(function(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, hookServices);
			model.topic = config.topic;
			model.topicName = SolarReg.Templates.serviceDisplayName(topicTypes, config.topic);

			// convert nodeIds array into delimited string for text field input
			var nodeIds = SolarReg.arrayAsDelimitedString(config.nodeIds);
			model.nodes = nodeIds || '*';
			// convert sourceIds array into delimited string for text field input
			var sourceIds = SolarReg.arrayAsDelimitedString(config.sourceIds);
			model.sources = sourceIds || '*';
			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		SolarReg.populateListCount(container, hookConfigs);
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

	// ***** Edit hook form
	$('#edit-node-event-hook-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), hookServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, hookServices);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			SolarReg.storeServiceConfiguration(res, hookConfigs);
			populateHookConfigs([res], true);
		}, function serializeDataConfigForm(form) {
			var data = SolarReg.Settings.encodeServiceItemForm(form);
			if ( data.nodeIds !== undefined ) {
				// convert nodeIds delimited string into array of numbers
				var nodeIds =  SolarReg.splitAsNumberArray(data.nodeIds);
				if ( nodeIds.length ) {
					data.nodeIds = nodeIds;
				} else {
					delete data.nodeIds;
				}
			}
			if ( data.sourceIds !== undefined ) {
				// convert sourceIds delimited string into array of strings
				var sourceIds = data.sourceIds.split(/\s*,\s*/);
				if ( sourceIds.length ) {
					data.sourceIds = sourceIds;
				} else {
					delete data.sourceIds;
				}
			}
			return data;
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#node-event-hook-list-container .list-container'), function(id, deleted) {
			if ( deleted ) {
				var idx = hookConfigs.findIndex(function(el) {
					return (id == el.id);
				});
				if ( idx >= 0 ) {
					hookConfigs.splice(idx, 1);
				}
				SolarReg.populateListCount($('#node-event-hook-list-container'), hookConfigs);
			}
		});
	});

	$('#node-event-hook-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, hookServices, settingTemplates);
	});
	
	$('.node-event.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

	$('#node-event-hooks').first().each(function() {
		var loadCountdown = 3;

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				populateHookConfigs(hookConfigs);
			}
		}

		// get available event topic services
		$.getJSON(SolarReg.solarUserURL('/sec/event/node/topics'), function(json) {
			console.log('Got node event topics: %o', json);
			if ( json && json.success === true ) {
				topicTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.node-event-topic-types');
			}
			liftoff();
		});
		
		// get available hook services
		$.getJSON(SolarReg.solarUserURL('/sec/event/node/hook/services'), function(json) {
			console.log('Got hook output services: %o', json);
			if ( json && json.success === true ) {
				hookServices = json.data;
			}
			liftoff();
		});

		// list all configs
		$.getJSON(SolarReg.solarUserURL('/sec/event/node/hooks'), function(json) {
			console.log('Got hook configurations: %o', json);
			if ( json && json.success === true ) {
				hookConfigs = json.data;
			}
			liftoff();
		});

	});
	
});