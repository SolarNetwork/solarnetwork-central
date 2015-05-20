$(document).ready(function() {
	'use strict';
	
	function setupAlertStatusHelp(radio) {
		var help = $(radio).attr('title');
		$(radio.form).find('.alert-status-help').text(help);
	}
	
	$('.alert-form input[name=status]').change(function(event) {
		setupAlertStatusHelp(this);
	}).filter(':checked').each(function() {
		// make sure the form starts with the first selected element
		setupAlertStatusHelp(this);
	});
	
	$('#create-node-data-alert-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#create-node-data-alert-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	});
	
	$('#add-node-data-button').on('click', function(event) {
		var form = $('#create-node-data-alert-modal');
		form.get(0).reset(); // doesn't reset hidden fields
		form.get(0).elements['id'].value = '';
		form.modal('show');
	});

	$('#node-data-alerts').on('click', 'button.edit-alert', function(event) {
		event.preventDefault();
		var btn = $(this);
		var alertId = btn.data('alert-id'),
			nodeId = btn.data('node-id'),
			alertType = btn.data('alert-type'),
			alertStatus = btn.data('alert-status'),
			alertSources = btn.data('sources'),
			alertAge = btn.data('age');
		var form = $('#create-node-data-alert-modal');
		$('#create-node-data-alert-node-id').val(nodeId);
		$('#create-node-data-alert-type').val(alertType);
		form.find('input[type=radio][value=' + alertStatus + ']').prop('checked', true);
		$('#create-node-data-alert-sources').val(alertSources);
		$('#create-node-data-alert-age').val(alertAge);
		form.get(0).elements['id'].value = alertId;
		form.modal('show');
	});
});
