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
	
	$('#create-user-auth-token').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			form.modal('hide');
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#create-user-auth-token .modal-body > *:first-child', 'alert-error', statusText);
		}
	});
	
	$('.action-user-token').find('button').click(function(event) {
		//.user-token-change-status or .user-token-delete
		event.preventDefault();
		var button = $(this);
		var tokenId = button[0].form.elements['id'].value;
		if ( button.hasClass('user-token-delete') ) {
			var form = $('#delete-user-auth-token');
			form[0].elements['id'].value = tokenId;
			form.find('.container-token').text(tokenId);
			form.modal('show');
		} else if ( button.hasClass('user-token-change-status') ) {
			var newStatus = (button.data('status') === 'v' ? 'z' : 'v');
			$.post(button.data('action'), {id:tokenId, status:newStatus}, function(data) {
				document.location.reload(true);
			}, 'json');
		}
	});
	
	$('#delete-user-auth-token').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			form.modal('hide');
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#delete-user-auth-token .modal-body > *:first-child', 'alert-error', statusText);
		}
	});

	$('#create-data-auth-token').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			form.modal('hide');
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#create-data-auth-token .modal-body > *:first-child', 'alert-error', statusText);
		}
	});
	
	$('.action-data-token').find('button').click(function(event) {
		//.user-token-change-status or .user-token-delete
		event.preventDefault();
		var button = $(this);
		var tokenId = button[0].form.elements['id'].value;
		if ( button.hasClass('data-token-delete') ) {
			var form = $('#delete-data-auth-token');
			form[0].elements['id'].value = tokenId;
			form.find('.container-token').text(tokenId);
			form.modal('show');
		} else if ( button.hasClass('data-token-change-status') ) {
			var newStatus = (button.data('status') === 'v' ? 'z' : 'v');
			$.post(button.data('action'), {id:tokenId, status:newStatus}, function(data) {
				document.location.reload(true);
			}, 'json');
		}
	});
	
	$('#delete-data-auth-token').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			form.modal('hide');
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#delete-data-auth-token .modal-body > *:first-child', 'alert-error', statusText);
		}
	});
});
