$(document).ready(function() {
	'use strict';

	function populateCredentialConfigs(configs, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		var container = $('#ocpp-credentials-container');
		var items = configs.map(function(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			model.id = config.id;
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			model.username = config.username;

			var allowedChargePoints = SolarReg.arrayAsDelimitedString(config.allowedChargePoints);
			model.allowedChargePoints = allowedChargePoints || '*';

			return model;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve);
		container.closest('section').find('.listCount').text(configs.length);
		return configs;
	}

	$('#ocpp-management').first().each(function ocppManagement() {
		var credentialConfigs = [];
	
		/* ============================
		   Edit credentials form
		   ============================ */
		$('#ocpp-credential-edit-modal').on('show.bs.modal', function(event) {
			var config = SolarReg.Templates.findContextItem(this);
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				populateCredentialConfigs([res], true);
				// TODO: show auto-generated password if returned in res.password
				//SolarReg.storeServiceConfiguration(res, expireConfigs.dataConfigs);
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

				return data;
			});
			return false;
		})
		.on('hidden.bs.modal', function() {
			SolarReg.Settings.resetEditServiceForm(this, $('#ocpp-credentials-container .list-container'));
		});

		$('#ocpp-credentials-container .list-container').on('click', function(event) {
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		/* ============================
		   OCPP entity delete
		   ============================ */
		$('.ocpp.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 1;
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					populateCredentialConfigs(credentialConfigs);
				}
			}
	
			// list all configs
			$.getJSON(SolarReg.solarUserURL('/sec/ocpp/credentials'), function(json) {
				console.debug('Got OCPP credentials: %o', json);
				if ( json && json.success === true ) {
					credentialConfigs = json.data;
				}
				liftoff();
			});
		})();
	});
});
