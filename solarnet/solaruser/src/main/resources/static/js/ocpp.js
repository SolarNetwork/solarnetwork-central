$(document).ready(function() {
	'use strict';

	$('#ocpp-management').first().each(function ocppManagement() {
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
	
				return model;
			});
			SolarReg.Templates.populateTemplateItems(chargersContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, chargerConfigs, chargersContainer);
		}

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

				if ( data.userId === "" ) {
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

		$('#ocpp-credentials-container .list-container').on('click', function(event) {
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		$('#ocpp-credential-password-modal').on('hidden.bs.modal', function(event) {
			// clear out credentials
			$(this).find('*[data-tprop]').text('');
		});

		/* ============================
		   OCPP entity delete
		   ============================ */
		$('.ocpp.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 2;
			var chargerConfs = [];
			var credentialConfs = [];
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateChargerConfigs(chargerConfs);
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
