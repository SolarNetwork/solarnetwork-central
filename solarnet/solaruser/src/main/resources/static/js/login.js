$(document).ready(function() {
	// main login form
	$('#login-username').trigger('select').trigger('focus');
	
	// reset pass form
	$('#reset-pass-email').trigger('select').trigger('focus');
	
	// reset pass confirm
	$('#reset-pass-password').trigger('select').trigger('focus');
});
