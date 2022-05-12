$(document).ready(function() {
	'use strict';

	/**
	 * Handle removing a deleted configuration from a service form delete action.
	 * @param {Number|String} deletedId the deleted config ID
	 * @param {Array} configs the array of configurations to delete from
	 * @param {jQuery} container the container to update the config count in
	 */
	function handleDeletedConfig(deletedId, configs, container) {
		if ( deletedId && Array.isArray(configs) ) {
			var idx = configs.findIndex(function(el) {
				return (el.id == deletedId);
			});
			if ( idx >= 0 ) {
				configs.splice(idx, 1);
			}
			container.closest('section').find('.listCount').text(configs.length);
		}
	}

	$('#ocpp-management').first().each(function ocppManagement() {
		var credentialConfigs = [];
	
		/* ============================
		   Credentials
		   ============================ */
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
			if ( !preserve ) {
				credentialConfigs = configs;
			}
			container.closest('section').find('.listCount').text(credentialConfigs.length);
			return configs;
		}
	
		$('#ocpp-credential-edit-modal').on('show.bs.modal', function(event) {
			var config = SolarReg.Templates.findContextItem(this);
			SolarReg.Settings.prepareEditServiceForm($(event.target), [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function(event) {
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				credentialConfigs.push(res);
				populateCredentialConfigs([res], true);
				var pwModal = $('#ocpp-credential-password-modal');
				SolarReg.Templates.replaceTemplateProperties(pwModal.find('table'), res);
				pwModal.modal('show');
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
				handleDeletedConfig(deleted ? id : null, credentialConfigs, $('#ocpp-credentials-container'));
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
