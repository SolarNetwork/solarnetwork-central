/* Global SolarNetwork App Support */
var SolarReg = {
	showAlertBefore: function(el, clazz, msg) {
	    $('<div class="alert'+(clazz.length > 0 ? ' ' +clazz : '')
	    		+'"><button type="button" class="close" data-dismiss="alert">×</button>'
	    		+msg +'</div>').insertBefore(el);
	},
	
	solarUserURL : function(relativeURL) {
		return $('meta[name=solarUserRootURL]').attr('content') + relativeURL;
	}

};

$(document).ready(function() {
	$('body').on('hidden', '.modal.dynamic', function () {
		$(this).removeData('modal');
	});
	
	$('a.logout').on('click', function(event) {
		event.preventDefault();
		$('#logout-form').get(0).submit();
	});
});
