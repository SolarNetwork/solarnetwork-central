$(document).ready(function() {
	'use strict';
	
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
	
	function setupEditUserNodeFields(form, userNode) {
		var node = userNode.node;
		if ( !node ) {
			node = {}
		}
		var loc = userNode.nodeLocation;
		if ( !loc ) {
			loc = {};
		}
		var user = userNode.user;
		if ( !user ) {
			user = {};
		}
		$('#usernode-id').text(node.id);
		$('#usernode-name').val(userNode.name);
		$('#usernode-description').val(userNode.description);
		$('#usernode-private').prop('checked', userNode.requiresAuthorization);
		
		var locDisplay = [];
		if ( loc.name ) {
			locDisplay.push(loc.name);
		}
		if ( loc.country ) {
			locDisplay.push(loc.country);
		}
		if ( loc.timeZoneId ) {
			locDisplay.push(loc.timeZoneId);
		}
		if ( locDisplay.length > 0 ) {
			$('#usernode-location').text(locDisplay.join(', '));
		}

		form.find("input[name='node.id']").val(node.id || '');
		form.find("input[name='user.id']").val(user.id || '')
		form.find("input[name='node.locationId']").val(node.locationId || '');
	}
	
	$('button.edit-node').on('click', function(event) {
		var btn = $(this);
		var form = $(btn.data('target'));
		var url = form.attr('action').replace(/\/[^\/]+$/, '/node');
		var req = {userId : btn.data('user-id'), nodeId : btn.data('node-id') };
		setupEditUserNodeFields(form, {node : {id : req.nodeId}, user : {id : req.userId}});
		$.getJSON(url, req, function(json) {
			setupEditUserNodeFields(form, json.data);
		}).fail(function(data, statusText, xhr) {
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-error', statusText);
		});
		form.modal('show');
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
					changeHandler : function(tzName, countryName, offset) {
						$('#invite-tz-country').text(countryName);
					}
				});
				picker.timezonePicker('detectLocation');
			});
		}
	});
});
