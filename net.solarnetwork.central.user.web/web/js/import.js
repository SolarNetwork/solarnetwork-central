$(document).ready(function() {
	'use strict';
	
	var inputServices = [];
	
	var settingTemplates = $('#import-setting-templates');

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

	// ***** Edit output format form
	$('#edit-datum-import-job-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), inputServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, inputServices);
	})
	.on('submit', function(event) {
		// TODO use FormData to post multi-part form
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this, $('#export-output-config-list-container .list-container'));
	});

	$('#datum-import-jobs').first().each(function() {
		var loadCountdown = 2;

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				// TODO start the party
			}
		}

		// get available input services
		$.getJSON(SolarReg.solarUserURL('/sec/import/services/input'), function(json) {
			console.log('Got import input services: %o', json);
			if ( json && json.success === true ) {
				inputServices = json.data;
			}
			liftoff();
		});

		// list all jobs for user
		$.getJSON(SolarReg.solarUserURL('/sec/import/jobs'), function(json) {
			console.log('Got import jobs: %o', json);
			if ( json && json.success === true ) {
				// TODO render job statuses
			}
			liftoff();
		});
	});
});