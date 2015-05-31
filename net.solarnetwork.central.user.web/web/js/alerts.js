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
	
	function nodeSourcesURL(nodeId) {
		return $('#create-node-data-alert-modal').data('action-node-sources').replace(/\/\d+(?=\/)/, '/'+nodeId);
	}
	
	function populateSourceList(nodeId, sourceListContainer) {
		if ( nodeId === '' ) {
			sourceListContainer.addClass('hidden');
		} else {
			$.getJSON(nodeSourcesURL(nodeId), function(json) {
				if ( json && json.success === true && Array.isArray(json.data) && json.data.length > 0 ) {
					sourceListContainer.find('.sources').text(json.data.join(', '));
					sourceListContainer.removeClass('hidden');
				} else {
					sourceListContainer.addClass('hidden');
				}
			}).fail(function(data, statusText, xhr) {
				// just hide the list
				sourceListContainer.addClass('hidden');
			});;
		}
	}
	
	$('.alert-form select[name=nodeId]').change(function(event) {
		populateSourceList($(this).val(), $('#create-node-data-alert-sources-list'));
	});
	
	$('#create-node-data-alert-modal').on('submit', function(event) {
		event.preventDefault();
		var form = this;
		var url = $(form).attr('action');
		var data = {};
		data.id = form.elements['id'].value;
		data.nodeId = $(form.elements['nodeId']).val();
		data.type = $(form.elements['type']).val();
		data.status = $(form).find('input[name=status]:checked').val();
		data.options = {
			ageMinutes : form.elements['option-age-minutes'].value,
			sources : form.elements['option-sources'].value,
			windows : [ // currently we only support one window, so we hard-code the array
			           { 
			        	   timeStart : form.elements['option-window-time-start'].value, 
			        	   timeEnd : form.elements['option-window-time-end'].value
			           }
			           ]
		};
		$.ajax({
			type : 'POST',
			url : url,
			dataType : 'json',
			contentType : 'application/json',
			data : JSON.stringify(data),
			success: function(json, status, xhr, form) {
				document.location.reload(true);
			},
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#create-node-data-alert-modal .modal-body > *:first-child', 'alert-warning', statusText);
			}
		});
	}).on('shown.bs.modal', function() {
		populateSourceList($('#create-node-data-alert-node-id').val(), $('#create-node-data-alert-sources-list'));
	});
	
	$('#add-node-data-button').on('click', function(event) {
		var form = $('#create-node-data-alert-modal');
		form.get(0).reset(); // doesn't reset hidden fields
		form.get(0).elements['id'].value = '';
		form.find('button.action-delete').hide();
		form.modal('show');
	});
	
	function populateAlertSituationValues(root, alert, nodeName) {
		// make use of the i18n type/status
		var type = (alert && alert.type && SolarReg.userAlertTypes
				? SolarReg.userAlertTypes[alert.type]
				: '');
		var date = (alert && alert.options && alert.options.situationDate
				? alert.options.situationDate
				: '');
		var node = (nodeName ? nodeName : alert.nodeId);
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
		return SolarReg.solarUserURL('/sec/alerts/situation');
	}
	
	/**
	 * View a single alert situation via a click event. The event target must define a data 
	 * attribute <code>alert-id</code> for the ID of the alert to view, and a modal 
	 * dialog with an ID <code>alert-situation-modal</code> to display the alert details.
	 */
	SolarReg.viewAlertSituation = function(event, nodeName) {
		event.preventDefault();
		var btn = $(this);
		var alertId = btn.data('alert-id');
		var url = alertSituationBaseURL() + '/' + encodeURIComponent(alertId);
		$.getJSON(url, function(json) {
			var modal = $('#alert-situation-modal'),
				alert = json.data,
				name = (nodeName ? nodeName : $('#create-node-data-alert-node-id').find('option[value="'+(alert.nodeId ? alert.nodeId : '')+'"]').text());
			if ( json.success === true && alert !== undefined ) {
				populateAlertSituationValues(modal, alert, name);
				$('#alert-situation-resolve').data('alert-id', json.data.id);
			}
			modal.modal('show');
		}).fail(function(data, statusText, xhr) {
			SolarReg.showAlertBefore('#alert-situation-modal .modal-body > *:first-child', 'alert-warning', 
					'Error getting alert situation details. ' +statusText);
		});
	};
	
	$('#node-data-alerts').on('click', 'button.edit-alert', function(event) {
		event.preventDefault();
		var btn = $(this);
		var alertId = btn.data('alert-id'),
			nodeId = btn.data('node-id'),
			alertType = btn.data('alert-type'),
			alertStatus = btn.data('alert-status'),
			alertSources = btn.data('sources'),
			alertAge = btn.data('age'),
			alertWindowStart = btn.data('window-time-start'),
			alertWindowEnd = btn.data('window-time-end');
		var form = $('#create-node-data-alert-modal');
		$('#create-node-data-alert-node-id').val(nodeId);
		$('#create-node-data-alert-type').val(alertType);
		form.find('input[type=radio][value=' + alertStatus + ']').prop('checked', true);
		$('#create-node-data-alert-sources').val(alertSources);
		$('#create-node-data-alert-age').val(alertAge);
		$('#create-node-data-alert-window-time-start').val(alertWindowStart);
		$('#create-node-data-alert-window-time-end').val(alertWindowEnd);
		form.get(0).elements['id'].value = alertId;
		form.find('button.action-delete').show();
		form.modal('show');
	}).on('click', 'button.view-situation', SolarReg.viewAlertSituation);
	
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
	
	(function() {
		var situationCountContainers = $('.alert-situation-count');
		if ( situationCountContainers.length > 0 ) {
			$.getJSON(SolarReg.solarUserURL('/sec/alerts/user/situation/count'), function(json) {
				var count = 0;
				if ( json && json.data ) {
					count = Number(json.data);
					if ( isNaN(count) ) {
						count = 0;
					}
				}
				situationCountContainers.text(count > 0 ? count : '');
			});
		}
	}());
});
