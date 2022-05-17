$(document).ready(function() {
	'use strict';

	$('#ocpp-management').first().each(function ocppManagement() {
		/* ============================
		   Authorizations
		   ============================ */
		const authsContainer = $('#ocpp-auths-container');
		const authConfigs = [];

		function populateAuthConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			var items = configs.map(function(config) {
				var model = SolarReg.Settings.serviceConfigurationItem(config, []);
				model.id = config.id;
				model.createdDisplay = moment(config.created).format('D MMM YYYY');
				model.token = config.token;
				model.expiryDateDisplay = (config.expiryDate ? moment(config.expiryDate).format('D MMM YYYY') : '');
				model.enabled = config.enabled;
	
				return model;
			});
			SolarReg.Templates.populateTemplateItems(authsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, authConfigs, authsContainer);
		}

		$('#ocpp-auths-container .list-container').on('click', function(event) {
			// edit auth
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#ocpp-auth-edit-modal').on('show.bs.modal', function(event) {
			var config = SolarReg.Templates.findContextItem(this),
				enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange($(this).find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				populateAuthConfigs([res], true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( data.expiryDate ) {
					// make sure encoded as ISO timestamp
					data.expiryDate = moment(data.expiryDate).toISOString();
				}

				if ( !data.userId ) {
					// use actor user ID, i.e. for new auths
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				errorMessageGenerator: function(xhr, json, form) {
					var msg;
					if ( json ) {
						if ( json.code === 'DAO.00101' ) {
							// assume this means the given identifier is a duplicate
							msg = form.elements['token'].dataset['errorDuplicateText'];
						}
					}
					return msg;
				}
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-auths-container .list-container'), (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, authConfigs, authsContainer);
			});
		})
		.find('button.toggle').each(function() {
			var toggle = $(this);
			SolarReg.Settings.setupSettingToggleButton(toggle, false);
		});
   
		/* ============================
		   Chargers
		   ============================ */
		const chargersContainer = $('#ocpp-chargers-container');
		const chargerConfigs = [];
		const chargerConfigsMap = new Map();

		function populateChargerConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				chargerConfigsMap.clear();
			}
			var items = configs.map(function(config) {
				var model = SolarReg.Settings.serviceConfigurationItem(config, []);
				Object.assign(model, config.info); // copy info props directly onto model
				model.identifier = model.id; // rename info.id
				model.id = config.id;
				model.createdDisplay = moment(config.created).format('D MMM YYYY');
				model.nodeId = config.nodeId;
				model.enabled = config.enabled;
				model.registrationStatus = config.registrationStatus;
				model.connectorCount = config.connectorCount;
				var settings = chargerSettingConfigsMap.get(config.id);
				if ( settings ) {
					model.settings = settings._contextItem;
					model.sourceIdTemplate = settings.sourceIdTemplate;
					model.publishToSolarIn = settings.publishToSolarIn;
					model.publishToSolarFlux = settings.publishToSolarFlux;
				}
				chargerConfigsMap.set(config.id, model);
				return model;
			});
			SolarReg.Templates.populateTemplateItems(chargersContainer, items, preserve, (item, el) => {
				let settingsEditContainer = el.find('.settings-container').toggleClass('hidden', item.settings === undefined).parent();
				// even if item does not have settings, provide a context item with the charger ID so editing works
				let settingConfig = (item.settings ? item.settings : {id:item.id,chargePointId:item.id});
				SolarReg.Templates.setContextItem(settingsEditContainer, settingConfig);
			});
			SolarReg.saveServiceConfigurations(configs, preserve, chargerConfigs, chargersContainer);
		}

		$('#ocpp-chargers-container .list-container').on('click', function(event) {
			// edit charger or charger settings
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#ocpp-charger-edit-modal').on('show.bs.modal', function(event) {
			var config = SolarReg.Templates.findContextItem(this),
				enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange($(this).find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				populateChargerConfigs([res], true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( !data.userId ) {
					// use actor user ID, i.e. for new chargers
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				errorMessageGenerator: function(xhr, json, form) {
					var msg;
					if ( json ) {
						if ( json.code === 'DAO.00101' ) {
							// assume this means the given identifier is a duplicate
							msg = form.elements['info.id'].dataset['errorDuplicateText'];
						} else if ( json.message === 'UNKNOWN_OBJECT' ) {
							// assume this means the given node ID is not valid
							msg = form.elements['nodeId'].dataset['errorInvalidText'];
						}
					}
					return msg;
				}
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-chargers-container .list-container'), (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, chargerConfigs, chargersContainer);
				if ( deleted ) {
					chargerConfigsMap.delete(id);
					chargerSettingConfigsMap.delete(id);
				}
			});
		})
		.find('button.toggle').each(function() {
			var toggle = $(this);
			SolarReg.Settings.setupSettingToggleButton(toggle, false);
		});

		/* ============================
		   Connectors
		   ============================ */
		const connectorsContainer = $('#ocpp-connectors-container');
		const connectorConfigs = [];

		function setupConnectorId(config) {
			if ( config.id ) {
				return;
			}
			config.id = config.chargePointId + '.' + config.connectorId;
			return config;
		}

		function populateConnectorConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			var items = configs.map(function(config) {
				var model = SolarReg.Settings.serviceConfigurationItem(setupConnectorId(config), []);
				Object.assign(model, config.info); // copy info props directly onto model
				model.id = config.id;
				model.chargePointId = config.chargePointId;
				model.createdDisplay = moment(config.created).format('D MMM YYYY');
				if ( model.timestamp ) {
					model.timestampDisplay = moment(model.timestamp).format('D MMM YYYY');
				}	
				return model;
			});
			SolarReg.Templates.populateTemplateItems(connectorsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, connectorConfigs, connectorsContainer);
		}

		$('#ocpp-connectors-container .list-container').on('click', function(event) {
			// edit connector
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#ocpp-connector-edit-modal').on('show.bs.modal', function(event) {
			var config = SolarReg.Templates.findContextItem(this),
				enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange($(this).find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				populateConnectorConfigs([res], true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( !data.userId ) {
					// use actor user ID, i.e. for new connectors
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				errorMessageGenerator: function(xhr, json, form) {
					var msg;
					if ( json ) {
						if ( json.code === 'DAO.00105' ) {
							// assume this means the given charger ID is not valid
							msg = form.elements['chargePointId'].dataset['errorInvalidText'];
						}
					}
					return msg;
				}
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-connectors-container .list-container'), (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, connectorConfigs, connectorsContainer);
			});
		})
		.find('button.toggle').each(function() {
			var toggle = $(this);
			SolarReg.Settings.setupSettingToggleButton(toggle, false);
		});

		/* ============================
		   Credentials
		   ============================ */
		const credentialsContainer = $('#ocpp-credentials-container');
		const credentialConfigs = [];
	
		function populateCredentialConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			var items = configs.map(function(config) {
				var model = SolarReg.Settings.serviceConfigurationItem(config, []);
				model.id = config.id;
				model.createdDisplay = moment(config.created).format('D MMM YYYY');
				model.username = config.username;
	
				var allowedChargePoints = SolarReg.arrayAsDelimitedString(config.allowedChargePoints);
				model.allowedChargePoints = allowedChargePoints || '*';
	
				return model;
			});
			SolarReg.Templates.populateTemplateItems(credentialsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, credentialConfigs, credentialsContainer);
		}
	
		$('#ocpp-credentials-container .list-container').on('click', function(event) {
			// edit credentials
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#ocpp-credential-password-modal').on('hidden.bs.modal', function(event) {
			// clear out credentials
			$(this).find('*[data-tprop]').text('');
		});

		$('#ocpp-credential-edit-modal').on('show.bs.modal', function(event) {
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				if ( res.password ) {
					// server returned a password, so show that to the user once and delete from config
					const pwModal = $('#ocpp-credential-password-modal');
					SolarReg.Templates.replaceTemplateProperties(pwModal.find('table'), res);
					pwModal.modal('show');
					delete res.password;
				}
				populateCredentialConfigs([res], true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form);

				// preserve existing password (or auto-assign for new credential)
				if ( data.password === "" ) {
					delete data.password;
				}

				var allowedChargePoints = (data.allowedChargePoints ? data.allowedChargePoints.split(/\s*,\s*/) : []);
				if ( allowedChargePoints.length ) {
					data.allowedChargePoints = allowedChargePoints;
				} else {
					delete data.allowedChargePoints;
				}

				if ( !data.userId ) {
					// use actor user ID, i.e. for new credentials
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-credentials-container .list-container'), (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, credentialConfigs, credentialsContainer);
			});
		});

		/* ============================
		   Settings
		   ============================ */
		const settingsContainer = $('#ocpp-settings-container');
		const settingConfigs = []; // only ever one of these, use array for consistency with Settings/Templates functions
		const chargerSettingConfigsMap = new Map();

		function settingModel(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			if ( config.chargePointId === undefined ) {
				config.id = 1; // assign arbitrary ID for default settings
				if ( config.publishToSolarIn === undefined ) {
					config.publishToSolarIn = true;
				}
				if ( config.publishToSolarFlux === undefined ) {
					config.publishToSolarFlux = true;
				}
				if ( config.sourceIdTemplate === undefined ) {
					config.sourceIdTemplate = '/ocpp/{chargePointId}/{connectorId}/{location}';
				}
			} else {
				config.id = config.chargePointId;
				model.chargePointId = config.chargePointId;
			}
			model.id = config.id;
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

		function populateChargerSettingConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				chargerSettingConfigsMap.clear();
			}
			var chargerItems = [];
			configs.forEach(config => {
				var model = settingModel(config);
				chargerSettingConfigsMap.set(model.chargePointId, model);
				if ( preserve ) {
					let chargerItem = chargerConfigsMap.get(model.chargePointId);
					if ( chargerItem ) {
						chargerItem.settings = model._contextItem;
						chargerItem.sourceIdTemplate = model.sourceIdTemplate;
						chargerItem.publishToSolarIn = !!model.publishToSolarIn;
						chargerItem.publishToSolarFlux = !!model.publishToSolarFlux;
						chargerItems.push(chargerItem);
					}
				}
				return model;
			});
			if ( chargerItems.length > 0 ) {
				SolarReg.Templates.populateTemplateItems(chargersContainer, chargerItems, true, (item, el) => {
					let settingsEditContainer = el.find('.settings-container').removeClass('hidden').parent();
					SolarReg.Templates.setContextItem(settingsEditContainer, item.settings);
				});
			}
		}

		$('#ocpp-settings-edit-modal').on('show.bs.modal', function(event) {
			// handle both default and charger-specific settings
			var modal = $(event.target),
				config = SolarReg.Templates.findContextItem(modal);
			if ( !config ) {
				config = (settingConfigs.length > 0 ? settingConfigs[0] : undefined);
				SolarReg.Templates.setContextItem(modal, config);
				modal.attr('action', modal.data('action'));
			} else {
				modal.attr('action', modal.data('action-charger'));
			}
			modal.find('.charger').toggleClass('hidden', config.chargePointId === undefined);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarIn]'), !!config.publishToSolarIn);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarFlux]'), !!config.publishToSolarFlux);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				if ( res.chargePointId === undefined ) {
					populateSettingConfigs([res], true);
				} else {
					populateChargerSettingConfigs([res], true);
				}
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true),
					config = SolarReg.Templates.findContextItem(form);

				if ( !config || config.chargePointId === undefined ) {
					delete data.chargePointid;
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
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-settings-container .list-container'), (id, deleted) => {
				if ( deleted ) {
					chargerSettingConfigsMap.delete(id);
					let chargerItem = chargerConfigsMap.get(id);
					if ( chargerItem ) {
						delete chargerItem.settings;
						delete chargerItem.sourceIdTemplate;
						delete chargerItem.publishToSolarIn;
						delete chargerItem.publishToSolarFlux;
						SolarReg.Templates.populateTemplateItems(chargersContainer, [chargerItem], true, (item, el) => {
							let settingsEditContainer = el.find('.settings-container').addClass('hidden').parent();
							SolarReg.Templates.setContextItem(settingsEditContainer, {id:id,chargePointId:id});
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
		   OCPP entity delete
		   ============================ */
		$('.ocpp.edit-config button.delete-config').on('click', function(event) {
			var options = {};
			var form = $(event.target).closest('form').get(0);
			if ( form && form.elements['connectorId'] ) {
				// connectors have a (chargePointId, connectorId) ID value
				options.urlSerializer = action => {
					return action + '/' + encodeURIComponent(form.elements['chargePointId'].value)
						+ '/' + encodeURIComponent(form.elements['connectorId'].value);
				};
			} else if ( form && form.elements['chargePointId'] && form.elements['sourceIdTemplate'] ) {
				// charger settings use /chargers/X/settings path
				options.urlSerializer = action => {
					return action.replace(/\/settings$/, '/' + form.elements['chargePointId'].value + '/settings');
				};
			}
			SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
		});

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 6;
			var settingConfs = [];
			var chargerConfs = [];
			var chargerSettingConfs = [];
			var authConfs = [];
			var credentialConfs = [];
			var connectorConfs = [];
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateSettingConfigs(settingConfs);
					populateAuthConfigs(authConfs);
					populateCredentialConfigs(credentialConfs);
					populateConnectorConfigs(connectorConfs);

					populateChargerSettingConfigs(chargerSettingConfs);
					populateChargerConfigs(chargerConfs);
				}
			}

			// get settings
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/settings'), function(json) {
				console.debug('Got OCPP settings: %o', json);
				if ( json && json.success === true && Array.isArray(json.data) ) {
					settingConfs = [json.data];
				} else {
					settingConfs = [{}];
				}
				liftoff();
			});

			// list all chargers
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/chargers'), function(json) {
				console.debug('Got OCPP chargers: %o', json);
				if ( json && json.success === true ) {
					chargerConfs = json.data;
				}
				liftoff();
			});

			// list all charger settings
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/chargers/settings'), function(json) {
				console.debug('Got OCPP charger settings: %o', json);
				if ( json && json.success === true ) {
					chargerSettingConfs = json.data;
				}
				liftoff();
			});

			// list all authorizations
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/authorizations'), function(json) {
				console.debug('Got OCPP authorizations: %o', json);
				if ( json && json.success === true ) {
					authConfs = json.data;
				}
				liftoff();
			});
			
			// list all credentials
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/credentials'), function(json) {
				console.debug('Got OCPP credentials: %o', json);
				if ( json && json.success === true ) {
					credentialConfs = json.data;
				}
				liftoff();
			});
			
			// list all connectors
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/connectors'), function(json) {
				console.debug('Got OCPP connectors: %o', json);
				if ( json && json.success === true ) {
					connectorConfs = json.data;
				}
				liftoff();
			});
		})();
	});
});
