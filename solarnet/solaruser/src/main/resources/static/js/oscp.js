$(document).ready(function() {
	'use strict';

	$('#oscp-management').first().each(function oscpManagement() {
		/* ============================
		   Capacity Groups
		   ============================ */
		const groupsContainer = $('#oscp-groups-container');
		const groupConfigs = [];
		const groupConfigsMap = new Map();

		
		/* ============================
		   Settings
		   ============================ */
		const settingsContainer = $('#oscp-settings-container');
		const settingConfigs = []; // only ever one of these, use array for consistency with Settings/Templates functions
		const groupSettingConfigsMap = new Map();

		function settingModel(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			if ( config.groupId === undefined ) {
				config.id = 1; // assign arbitrary ID for default settings
				if ( config.publishToSolarIn === undefined ) {
					config.publishToSolarIn = true;
				}
				if ( config.publishToSolarFlux === undefined ) {
					config.publishToSolarFlux = true;
				}
				if ( config.sourceIdTemplate === undefined ) {
					config.sourceIdTemplate = '/oscp/{role}/{action}/{cp}/{co}/{cgIdentifier}';
				}
			} else {
				config.id = config.groupId;
				model.groupId = config.groupId;
			}
			model.id = config.id;
			model.nodeId = config.nodeId;
			model.publishToSolarIn = config.publishToSolarIn;
			model.publishToSolarFlux = config.publishToSolarFlux;
			model.sourceIdTemplate = config.sourceIdTemplate;
			return model;
		}

		function populateSettingConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			var items = configs.map(settingModel);
			SolarReg.Templates.populateTemplateItems(settingsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, settingConfigs, settingsContainer);
		}

		function populateGroupSettingConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				groupSettingConfigsMap.clear();
			}
			var groupItems = [];
			configs.forEach(config => {
				var model = settingModel(config);
				groupSettingConfigsMap.set(model.groupId, model);
				if ( preserve ) {
					let groupItem = groupConfigsMap.get(model.groupId);
					if ( groupItem ) {
						groupItem.settings = model._contextItem;
						groupItem.sourceIdTemplate = model.sourceIdTemplate;
						groupItem.publishToSolarIn = !!model.publishToSolarIn;
						groupItem.publishToSolarFlux = !!model.publishToSolarFlux;
						groupItems.push(groupItem);
					}
				}
				return model;
			});
			if ( groupItems.length > 0 ) {
				SolarReg.Templates.populateTemplateItems(groupsContainer, groupItems, true, (item, el) => {
					let settingsEditContainer = el.find('.settings-container').removeClass('hidden').parent();
					SolarReg.Templates.setContextItem(settingsEditContainer, item.settings);
				});
			}
		}

		$('#oscp-settings-edit-modal').on('show.bs.modal', function(event) {
			// handle both default and capacity-group-specific settings
			var modal = $(event.target),
				config = SolarReg.Templates.findContextItem(modal);
			if ( !config ) {
				config = (settingConfigs.length > 0 ? settingConfigs[0] : undefined);
				SolarReg.Templates.setContextItem(modal, config);
				modal.attr('action', modal.data('action'));
			} else {
				modal.attr('action', modal.data('action-group'));
			}
			modal.find('.group').toggleClass('hidden', config.groupId === undefined);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarIn]'), !!config.publishToSolarIn);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarFlux]'), !!config.publishToSolarFlux);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				if ( res.groupId === undefined ) {
					populateSettingConfigs([res], true);
				} else {
					populateGroupSettingConfigs([res], true);
				}
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true),
					config = SolarReg.Templates.findContextItem(form);

				if ( !config || config.groupId === undefined ) {
					delete data.groupId;
				}

				delete data.id;

				if ( !data.userId ) {
					// use actor user ID, i.e. for new settings
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#oscp-settings-container .list-container'), (id, deleted) => {
				if ( deleted ) {
					groupSettingConfigsMap.delete(id);
					let groupItem = groupConfigsMap.get(id);
					if ( groupItem ) {
						delete groupItem.settings;
						delete groupItem.nodeId;
						delete groupItem.sourceIdTemplate;
						delete groupItem.publishToSolarIn;
						delete groupItem.publishToSolarFlux;
						SolarReg.Templates.populateTemplateItems(groupsContainer, [groupItem], true, (item, el) => {
							let settingsEditContainer = el.find('.settings-container').addClass('hidden').parent();
							SolarReg.Templates.setContextItem(settingsEditContainer, {id:id,groupId:id});
						});
					}
				}
			});
		})
		.find('button.toggle').each(function() {
			var toggle = $(this);
			SolarReg.Settings.setupSettingToggleButton(toggle, false);
		});

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 2;
			var settingConfs = [];
			var groupSettingConfs = [];
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateSettingConfigs(settingConfs);
					populateGroupSettingConfigs(groupSettingConfs);
				}
			}

			// get settings
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/settings'), function(json) {
				console.debug('Got OSCP settings: %o', json);
				if ( json && json.success === true && (typeof json.data === 'object') ) {
					settingConfs = [json.data];
				} else {
					settingConfs = [{}];
				}
				liftoff();
			});

			// list all group settings
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-groups/settings'), function(json) {
				console.debug('Got OSCP group settings: %o', json);
				if ( json && json.success === true ) {
					groupSettingConfs = json.data;
				}
				liftoff();
			});

		})();

	});
});
