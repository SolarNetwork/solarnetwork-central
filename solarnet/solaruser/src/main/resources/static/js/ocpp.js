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
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-auths-container .list-container'), function(id, deleted) {
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

		function populateChargerConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
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
	
				return model;
			});
			SolarReg.Templates.populateTemplateItems(chargersContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, chargerConfigs, chargersContainer);
		}

		$('#ocpp-chargers-container .list-container').on('click', function(event) {
			// edit charger
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
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-chargers-container .list-container'), function(id, deleted) {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, chargerConfigs, chargersContainer);
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
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-credentials-container .list-container'), function(id, deleted) {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, credentialConfigs, credentialsContainer);
			});
		});

		/* ============================
		   OCPP entity delete
		   ============================ */
		$('.ocpp.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 3;
			var chargerConfs = [];
			var authConfs = [];
			var credentialConfs = [];
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateChargerConfigs(chargerConfs);
					populateAuthConfigs(authConfs);
					populateCredentialConfigs(credentialConfs);
				}
			}

			// list all chargers
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/chargers'), function(json) {
				console.debug('Got OCPP chargers: %o', json);
				if ( json && json.success === true ) {
					chargerConfs = json.data;
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
		})();
	});
});
