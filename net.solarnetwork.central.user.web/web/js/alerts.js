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

});
