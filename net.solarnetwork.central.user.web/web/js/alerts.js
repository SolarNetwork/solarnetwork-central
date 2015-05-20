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
	
	function populateAlertSituationValues(root, alert) {
		// make use of the i18n type/status values available in the edit form
		var type = (alert && alert.type 
				? $('#create-node-data-alert-type').find('option[value='+alert.type+']').text()
				: '');
		var date = (alert && alert.options && alert.options.situationDate
				? alert.options.situationDate
				: '');
		var node = $('#create-node-data-alert-node-id').find('option[value="'+(alert && alert.nodeId ? alert.nodeId : '')+'"]').text();
		var age = (alert && alert.options && alert.options.age ? (alert.options.age / 60).toFixed(0) : '1');
		var sources = (alert && alert.options && alert.options.sources ? alert.options.sources : '');
		root.find('.alert-situation-type').text(type);
		root.find('.alert-situation-created').text(date);
		root.find('.alert-situation-node').text(node);
		root.find('.alert-situation-age').text(age);
		root.find('.alert-situation-sources').text(sources);
		
		if ( alert && alert.options.situationNotificationDate ) {
			root.find('.alert-situation-notified').text(alert.options.situationNotificationDate);
			root.find('.notified').show();
		} else {
			root.find('.notified').hide();
		}
	}

	function alertSituationBaseURL() {
		return $('#node-data-alerts').data('action-situation');
	}
	
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
	}).on('click', 'button.view-situation', function(event) {
		event.preventDefault();
		var btn = $(this);
		var alertId = btn.data('alert-id');
		var url = alertSituationBaseURL() + '/' + encodeURIComponent(alertId);
		$.getJSON(url, function(json) {
			console.log('hi: ' +json.data);
			var modal = $('#alert-situation-modal');
			if ( json.success === true && json.data !== undefined ) {
				populateAlertSituationValues(modal, json.data);
				$('#alert-situation-resolve').data('alert-id', json.data.id);
			}
			modal.modal('show');
		}).fail(function(data, statusText, xhr) {
			SolarReg.showAlertBefore('#alert-situation-modal .modal-body > *:first-child', 'alert-warning', 
					'Error getting alert situation details. ' +statusText);
		});
	});
	
	$('#alert-situation-resolve').on('click', function(event) {
		event.preventDefault();
		var alertId = $(this).data('alert-id');
		if ( alertId !== undefined ) {
			var url = alertSituationBaseURL() + '/' + encodeURIComponent(alertId) + '/resolve';
			$.post(url, {status:'Resolved'}, function(data) {
				document.location.reload(true);
			}, 'json');
		}
	});
});
