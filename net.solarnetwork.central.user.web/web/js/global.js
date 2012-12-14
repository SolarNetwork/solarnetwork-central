/* Global SolarNetwork App Support */
var SolarReg = {
	showAlertBefore: function(el, clazz, msg) {
	    $('<div class="alert'+(clazz.length > 0 ? ' ' +clazz : '')
	    		+'"><button type="button" class="close" data-dismiss="alert">×</button>'
	    		+msg +'</div>').insertBefore(el);
	}
};

$(document).ready(function() {
	$('body').on('hidden', '.modal.dynamic', function () {
		$(this).removeData('modal');
	});
});
