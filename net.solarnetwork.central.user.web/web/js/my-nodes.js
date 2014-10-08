$(document).ready(function() {
	$('a.view-cert').click(function(event) {
		var a = this;
		var id = a.href.match(/\d+$/)[0];
		event.preventDefault();
		$.getJSON(a.href, function(data) {
			$('#modal-cert-container').text(data.pemvalue);
			var downLink = $('#modal-cert-download');
			downLink.attr('href',  downLink.attr('href').replace(/\d+$/, id));
			$('#view-cert-modal').modal('show');
		});
	});
	
	$('#edit-node-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			form.modal('hide');
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-error', statusText);
		}
	});
	
	$('#invite-modal').on('show', function() {
		var form = $(this);
		var url = form.attr('action').replace(/\/[^\/]+$/, '/tzpicker.html');
		var tzcontainer = $('#tz-picker-container');
		if ( tzcontainer.children().length == 0 ) {
			tzcontainer.load(url, function() {
				var picker = $('#timezone-image');
				picker.timezonePicker({
					target : '#invite-tz',
					countryTarget : '#invite-country',
					change : function() {
						$('#invite-tz-country').text($('#invite-country').val());
					}
				});
				picker.timezonePicker('detectTimezone');
			});
		}
	});
});
