$(document).ready(function() {
	'use strict';

	$('#oscp-management').first().each(function oscpManagement() {
		/* ============================
		   Globals
		   ============================ */
		const i18n = SolarReg.i18nData(this);


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

		$('#oscp-settings-edit-modal').on('show.bs.modal', function handleModalShow(event) {
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
		.on('submit', function handleModalFormSubmit(event) {
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
		.on('hidden.bs.modal', function handleModalHidden() {
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
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

		
		/* ============================
		   System Token
		   ============================ */

	   function showSystemToken(config, type) {
			if ( config && config.token ) {
				$('#system-token-name').text(config.name);
				$('#system-token').val(config.token);
				$('#oscp-system-token-modal')
					.attr('data-system-type', type)
					.find('.system-type').text(i18n['systemType'+type.charAt(0).toUpperCase()+type.substring(1)])
					.end()
					.modal('show');
				delete config.token;
			}
		}

		$('#oscp-system-token-modal').on('hidden.bs.modal', function handleModalHidden() {
			// clear the token value
			$('#system-token').val('');
		});

		/* ============================
		   Capacity Providers
		   ============================ */
		const cpsContainer = $('#oscp-cps-container');
		const cpConfigs = [];
		const cpConfigsMap = new Map();

		function populateCpConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				cpConfigsMap.clear();
			}
			var items = configs.map(function(config) {
				var model = SolarReg.Settings.serviceConfigurationItem(config, []);
				config.id = config.configId; // assumed by setttings.js methods
				model.id = config.configId;				
				model.createdDisplay = moment(config.created).format('D MMM YYYY');
				model.baseUrl = config.baseUrl;
				model.enabled = config.enabled;
				model.registrationStatus = config.registrationStatus;
				if ( config.settings && Array.isArray(config.settings.measurementStyles) ) {
					model.measurementStyles = config.settings.measurementStyles;
					model.measurementStylesDisplay = config.settings.measurementStyles.join(', ');
				}
				if ( config.serviceProps ) {
					if ( config.serviceProps['oauth-token-url'] ) {
						model.oauthTokenUrl = config.serviceProps['oauth-token-url'];
					}
					if ( config.serviceProps['oauth-client-id'] ) {
						model.oauthClientId = config.serviceProps['oauth-client-id'];
					}
					if ( config.serviceProps['http-headers'] ) {
						model.httpHeaders = config.serviceProps['http-headers'];
					}
					if ( config.serviceProps['url-paths'] ) {
						model.urlPaths = config.serviceProps['url-paths'];
					}
				}
				cpConfigsMap.set(config.id, model);
				return model;
			});
			SolarReg.Templates.populateTemplateItems(cpsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, cpConfigs, cpsContainer);
		}

		$('#oscp-cps-container .list-container').on('click', function(event) {
			// edit cp or cp settings
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#oscp-cp-edit-modal').on('show.bs.modal', function handleModalShow(event) {
			var el = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(el.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(el, [], []);

			if ( !(config && config.serviceProps) ) {
				return;
			}

			// populate dynamic HTTP Headers list
			SolarReg.Settings.populateDynamicListObjectKeyValues(config.serviceProps['http-headers'], el, 
				'http-headers', 'httpHeaderName', 'httpHeaderValue');

			// populate dynamic URL Paths list
			SolarReg.Settings.populateDynamicListObjectKeyValues(config.serviceProps['url-paths'], el, 
				'url-paths', 'urlPathAction', 'urlPathPath');
			
			return;
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function handleCpModalFormSubmit(event) {
			const modal = $(this);
			SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
				populateCpConfigs([res], true);
				// save result as modal context, to possibly show token modal
				SolarReg.Templates.setContextItem(modal, res);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( !data.userId ) {
					// use actor user ID, i.e. for new cps
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				urlId: true
			});
			return false;
		})
		.on('hidden.bs.modal', function handleModalHidden() {
			const config = SolarReg.Templates.findContextItem(this);
			SolarReg.Settings.resetEditServiceForm(this, $('#oscp-cps-container .list-container'), (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, cpConfigs, cpsContainer);
				if ( deleted ) {
					cpConfigsMap.delete(id);
				}
			});
			if ( config && config.token ) {
				// token provided; show value after short delay to allow animation to finish
				setTimeout(function() {
					showSystemToken(config, 'cp');
				}, 200);
			}
		})
		.on('click', SolarReg.Settings.handleDynamicListAddOrDelete)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});


		/* ============================
		   OSCP entity delete
		   ============================ */
		$('.oscp.edit-config button.delete-config').on('click', function(event) {
			var options = {};
			var form = $(event.target).closest('form').get(0);
			if ( form && form.elements['groupId'] && form.elements['sourceIdTemplate'] ) {
				// group settings use /capacity-groups/X/settings path
				options.urlSerializer = action => {
					return action.replace(/\/settings$/, '/' + form.elements['groupId'].value + '/settings');
				};
			}
			SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
		});

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 3;
			var settingConfs = [];
			var groupSettingConfs = [];
			var cpConfs = [];

			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateSettingConfigs(settingConfs);
					populateGroupSettingConfigs(groupSettingConfs);
					populateCpConfigs(cpConfs);
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

			// list all capacity providers
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-providers'), function(json) {
				console.debug('Got OSCP capacity providers: %o', json);
				if ( json && json.success === true ) {
					cpConfs = json.data;
				}
				liftoff();
			});

		})();

	});
});
