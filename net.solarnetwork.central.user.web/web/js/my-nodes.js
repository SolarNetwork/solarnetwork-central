$(document).ready(function() {
	'use strict';
	
	var tzPicker;
	
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
			$('#edit-node-location-country').val(loc.country);
		}
		if ( loc.timeZoneId ) {
			locDisplay.push(loc.timeZoneId);
			$('#edit-node-location-tz').val(loc.timeZoneId);
		}
		if ( locDisplay.length > 0 ) {
			$('#usernode-location').text(locDisplay.join(', '));
		}

		form.find("input[name='node.id']").val(node.id || '');
		form.find("input[name='user.id']").val(user.id || '')
		form.find("input[name='node.locationId']").val(node.locationId || '');
	}
	
	$('#nodes').on('click', 'button.edit-node', function(event) {
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
		editNodeShowPage(form, 1);
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
	}).data('page', 1);
	
	function editNodeShowPage(form, newPage) {
		var currPage = form.data('page');
		if ( currPage === newPage || newPage < 1 ) {
			return;
		}
		if ( newPage > currPage ) {
			while ( currPage < newPage ) {
				form.removeClass('page'+currPage);
				currPage += 1;
			}
		} else {
			while ( currPage > newPage ) {
				form.removeClass('page'+currPage);
				currPage -= 1;
			}
		}
		form.data('page', newPage);
		form.addClass('page'+newPage);
	}
	
	function selectTzPickerArea(tzcontainer) {
		var timeZoneId = $('#edit-node-location-tz').val();
		var country = $('#edit-node-location-country').val();
		if ( timeZoneId && country ) {
			tzcontainer.find("area[data-timezone='"+timeZoneId+"'][data-country="+country+']').trigger('click');
		}
	}
	
	$('#edit-node-modal button.change-location').on('click', function(event) {
		var form = $(this).parents('form').first();
		var pageContainer = form.find('.modal-body .hbox');
		var pickerUrl = form.attr('action').replace(/\/[^\/]+$/, '/tzpicker.html');
		var tzcontainer = form.find('.tz-picker-container');
		if ( tzcontainer.children().length == 0 ) {
			tzcontainer.load(pickerUrl, function() {
				var picker = tzcontainer.find('.timezone-image');
				picker.timezonePicker({
					target : '#edit-node-location-tz',
					countryTarget : '#edit-node-location-country',
					changeHandler : function(tzName, countryName, offset) {
						
						// TODO$('#invite-tz-country').text(countryName);
					}
				});
				selectTzPickerArea(tzcontainer);
			});
		} else {
			selectTzPickerArea(tzcontainer);
		}
		editNodeShowPage(form, 2);
	});
	
	$('#edit-node-page-back').on('click', function(event) {
		var form = $(this).parents('form').first();
		editNodeShowPage(form, form.data('page') - 1);
	});
	
	$('#edit-node-select-tz').on('click', function(event) {
		
	});
	
	$('#invite-modal').on('show', function() {
		var form = $(this);
		var url = form.attr('action').replace(/\/[^\/]+$/, '/tzpicker.html');
		var tzcontainer = $('#tz-picker-container');
		if ( tzcontainer.children().length == 0 ) {
			tzcontainer.load(url, function() {
				var picker = tzcontainer.find('.timezone-image');
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
