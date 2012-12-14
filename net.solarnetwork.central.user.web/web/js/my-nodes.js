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
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-error', statusText);
		}
	});
});
