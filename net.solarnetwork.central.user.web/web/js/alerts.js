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

});
