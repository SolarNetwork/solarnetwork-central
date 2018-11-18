$(document).ready(function() {
	'use strict';
	
	var expireConfigs = {};
	var dataServices = [{
		id : 'net.solarnetwork.central.user.expire.standard.DefaultUserExpireDataFilterService'
	}];
	var outputServices = [];
	var destinationServices = [];
	var compressionTypes = [];
	var scheduleTypes = [];
	var aggregationTypes = [];

	var settingTemplates = $('#expire-setting-templates');
	
	function populateExpireConfigs(configs) {
		if ( typeof configs !== 'object' ) {
			configs = {};
		}
		configs.dataConfigs = populateDataConfigs(configs.dataConfigs);
		return configs;
	}
	
	function populateDataConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#expire-data-config-list-container');
		var items = configs.map(function(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, dataServices);
			var filter = config.datumFilter || {};
			model.aggregation = SolarReg.Templates.serviceDisplayName(aggregationTypes, filter.aggregationKey) || '';
			var nodeIds = SolarReg.arrayAsDelimitedString(filter.nodeIds);
			model.nodes = nodeIds || '*';
			var sourceIds = SolarReg.arrayAsDelimitedString(filter.sourceIds);
			model.sources = sourceIds || '*';
			model.expireDays = config.expireDays || 730;
			model.active = config.active ? '\u2713' : '';
			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		container.closest('section').find('.listCount').text(configs.length);
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

	function handleToggleButton(btn, enabled) {
		btn.toggleClass('btn-success', enabled)
			.toggleClass('active', enabled)
			.attr('aria-pressed', enabled )
			.button(enabled ? 'on' : 'off')
			.val(enabled ? 'true' : 'false');
	}
	
	// ***** Edit data policy form
	$('#edit-expire-data-config-modal').on('show.bs.modal', function(event) {
		var config = SolarReg.Templates.findContextItem(this),
			active = (config && config.active === true ? true : false);
		handleToggleButton($(this).find('button[name=active]'), active);
		SolarReg.Settings.prepareEditServiceForm($(event.target), dataServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, dataServices);
	})
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			populateDataConfigs([res], true);
			SolarReg.storeServiceConfiguration(res, expireConfigs.dataConfigs);
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
		SolarReg.Settings.resetEditServiceForm(this, $('#expire-data-config-list-container .list-container'));
	})
	.find('button[name=active]').each(function() {
		var fieldElement = $(this);
		handleToggleButton(fieldElement, false); // initialize initial state
		fieldElement.on('click', function() {
			var btn = $(this);
			handleToggleButton(btn, btn.val() !== 'true');
		});
	});
	
	// ***** Preview data policy modal
	$('#expire-data-config-preview-modal').on('show.bs.modal', function() {
		var config = SolarReg.Templates.findContextItem(this);
		$(this).find('.config-name').text(config ? config.name : '');
	})
	.on('shown.bs.modal', function(event) {
		var me = this,
			config = SolarReg.Templates.findContextItem(this);
		if ( config && config.id ) {
			$.getJSON(SolarReg.solarUserURL('/sec/expire/configs/data/' +encodeURIComponent(config.id) +'/preview'), function(json) {
				if ( !(json && json.success) ) {
					return;
				}
				var configNow = SolarReg.Templates.findContextItem(me),
					modal = $(me),
					counts = json.data,
					prop;
				
				// format numbers with commas for clarity (not i18n I know :)
				for ( prop in counts ) {
					if ( !isNaN(Number(counts[prop])) ) {
						counts[prop+'Display'] = counts[prop].toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
					}
				}
				if ( configNow && configNow.id === config.id ) {
					SolarReg.Templates.replaceTemplateProperties(modal.find('.expire-preview-counts'), counts);
					modal.find('.ready').removeClass('hidden').end()
						.find('.waiting').addClass('hidden');
				}
			});
		}
	})
	.on('hidden.bs.modal', function() {
		var modal = $(this);
		modal.find('.ready').addClass('hidden').end()
			.find('.waiting').removeClass('hidden');
	});

	$('#expire-data-config-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, dataServices, settingTemplates);
	});
	
	$('.expire.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

	$('#expire-data-configs').first().each(function() {
		var loadCountdown = 2;

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				populateExpireConfigs(expireConfigs);
				$('.datum-expire-getstarted').toggleClass('hidden', 
					expireConfigs.dataConfigs && expireConfigs.dataConfigs.length > 0);
			}
		}

		// get available aggregation services
		$.getJSON(SolarReg.solarUserURL('/sec/expire/services/aggregation'), function(json) {
			console.log('Got expire aggregation types: %o', json);
			if ( json && json.success === true ) {
				aggregationTypes = SolarReg.Templates.populateServiceSelectOptions(json.data, 'select.expire-data-aggregation-types');
			}
			liftoff();
		});
		
		// list all configs
		$.getJSON(SolarReg.solarUserURL('/sec/expire/configs'), function(json) {
			console.log('Got expire configurations: %o', json);
			if ( json && json.success === true ) {
				expireConfigs = json.data;
			}
			liftoff();
		});
	});
	
});