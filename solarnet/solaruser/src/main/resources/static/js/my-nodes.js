$(document).ready(function() {
	'use strict';
	
	var tzPicker;
	var dynamicSearchTimer;
	
	$('#my-nodes-table').on('click', 'a.view-cert', function(event) {
		event.preventDefault();
		
		var btn = $(this);
		var id = btn.parents('.node-row').data('node-id');
		var form = $('#view-cert-modal');
		var downLink = $('#modal-cert-download').get(0);
		var renewLink = $('#modal-cert-renew').get(0);

		form.attr('action', form.attr('action').replace(/\d+$/, id));
		downLink.pathname = downLink.pathname.replace(/\d+$/, id);
		renewLink.pathname = renewLink.pathname.replace(/\d+$/, id);

		form.modal('show');
	}).on('click', 'a.transfer-ownership', function(event) {
		event.preventDefault();
		
		var btn = $(this);
		var nodeRow = btn.parents('.node-row').first();
		var nodeId = nodeRow.data('node-id');
		var nodeName = nodeRow.data('node-name');
		var userId = nodeRow.data('user-id');
		var form = $('#transfer-ownership-modal');

		form.find("input[name='nodeId']").val(nodeId || '');
		form.find("input[name='userId']").val(userId || '');
		
		$('#transfer-ownership-node').text(nodeId + (nodeName ? ' - ' + nodeName : ''));
		
		form.modal('show');
	}).on('click', 'a.archive', function(event) {
		event.preventDefault();
		
		var btn = $(this);
		var nodeRow = btn.parents('.node-row').first();
		var nodeId = nodeRow.data('node-id');
		var nodeName = nodeRow.data('node-name');
		var form = $('#archive-node-modal');

		form.find("input[name='nodeIds']").val(nodeId || '');
		form.find(".node-name-label").text(nodeId + (nodeName ? ' - ' + nodeName : ''));
		
		form.modal('show');
	}).on('click', 'button.view-situation', function(event) {
		// use call(this) to preserve button as 'this' object
		var nodeRow = $(this).parents('.node-row').first();
		var nodeId = nodeRow.data('node-id');
		var nodeName = nodeRow.data('node-name');
		if ( nodeName ) {
			nodeName = nodeId + ' - ' + nodeName;
		}
		SolarReg.viewAlertSituation.call(this, event, nodeName);
	});
	
	$('#pending-transfer').on('click', 'button.cancel-ownership-transfer', function(event) {
		event.preventDefault();
		var btn = $(this);
		var url = btn.data('action'),
			userId = btn.data('user-id'),
			nodeId = btn.data('node-id');
		$.post(url, { userId:userId, nodeId:nodeId, _csrf:SolarReg.csrf() }, function(json) {
			document.location.reload(true);
		}).fail(function(data, statusText, xhr) {
			SolarReg.showAlertBefore('#top', 'alert-warning', statusText);
		});
	});
	
	$('#pending-transfer-requests-table').on('click', 'button.decide-ownership-transfer', function(event) {
		event.preventDefault();
		var btn = $(this);
		var nodeId = btn.data('node-id');
		var userId = btn.data('user-id');
		var requester = btn.data('requester');
		var form = $('#decide-transfer-ownership-modal');
		
		form.find("input[name='nodeId']").val(nodeId || '');
		form.find("input[name='userId']").val(userId || '');
		form.find("input[name='accept']").val('false');
		
		$('#transfer-ownership-request-node').text(nodeId);
		$('#transfer-ownership-request-requester').text(requester);

		form.modal('show');
	});
	
	function updateCertDisplayDetails(json) {
		var dateFormat = 'dddd, D MMM YYYY, h:mm a',
			validUntil = moment(json.certificateValidUntilDate),
			renewAfter = moment(json.certificateRenewAfterDate),
			renewAfterMsg,
			timeLeft;
		
		$('#modal-cert-container').text(json.pemValue);
		$('#view-cert-serial-number').text(json.certificateSerialNumber);
		$('#view-cert-subject').text(json.certificateSubjectDN);
		$('#view-cert-issuer').text(json.certificateIssuerDN);
		$('#view-cert-valid-from').text(moment(json.certificateValidFromDate).format(dateFormat));
		$('#view-cert-valid-until').text(validUntil.format(dateFormat));
		
		if ( json.certificateRenewAfterDate ) {
			renewAfterMsg = renewAfter.format(dateFormat);
			if ( renewAfter.isAfter() ) {
				renewAfterMsg += ' (in ' +renewAfter.diff(moment(), 'days') + ' days)';
			} else if ( validUntil.isAfter() ) {
				timeLeft = validUntil.diff(moment(), 'days');
				if ( timeLeft > 0 ) {
					renewAfterMsg +=  ' (' + timeLeft + ' days left before expires)';
				} else {
					timeLeft = validUntil.diff(moment(), 'hours');
					renewAfterMsg +=  ' (' + timeLeft + ' hours left before expires)';
				}
			}
			$('#view-cert-renew-after').text(renewAfterMsg).parent().show();
		} else {
			$('#view-cert-renew-after').parent().hide();
		}
	}
	
	$('#view-cert-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			var renewAfter = moment(json.certificateRenewAfterDate);
			
			updateCertDisplayDetails(json);
			
			if ( renewAfter.isAfter() === false ) {
				$('#modal-cert-renew').removeClass('hidden');
			}
			
			$('#view-cert-modal .cert').removeClass('hidden');
			$('#view-cert-modal .nocert').addClass('hidden');
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#view-cert-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	}).on('shown.bs.modal', function() {
		$('#view-cert-password').focus();
	}).on('hidden.bs.modal', function() {
		document.location.reload(true);
	});
	
	$('#modal-cert-renew').on('click', function(event) {
		event.preventDefault();
		var btn = $(event.target),
			url = btn.attr('href'),
			pass = $('#view-cert-password').val();
		$.ajax({
			type: 'POST',
			url: url,
			data: {password:pass},
			dataType: 'json',
			beforeSend: function(xhr) {
				SolarReg.csrf(xhr);
            },
			success: function(json, status, xhr) {
				$('#view-cert-modal .renewed').removeClass('hidden');
				$('#modal-cert-renew').addClass('hidden');
				updateCertDisplayDetails(json);
			},
			error: function(xhr, status, statusText){
				SolarReg.showAlertBefore('#view-cert-modal .modal-body > *:first-child', 'alert-warning', statusText);
			}
		});
	});
	
	$('#transfer-ownership-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			$('#transfer-ownership-modal').modal('hide');
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#transfer-ownership-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	}).on('shown.bs.modal', function() {
		$('#transfer-ownership-recipient').focus();
	}).on('hidden.bs.modal', function() {
		document.location.reload(true);
	});
	
	$('#decide-transfer-ownership-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			$('#decide-transfer-ownership-modal').modal('hide');
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#decide-transfer-ownership-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	}).on('hidden.bs.modal', function() {
		document.location.reload(true);
	}).find('button.submit').on('click', function(event) {
		var btn = $(this);
		var form = $('#decide-transfer-ownership-modal');
		form.find('input[name="accept"]').val(btn.data('accept') ? 'true' : 'false');
		form.submit();
	});
	
	$('#archive-node-modal').ajaxForm({
		dataType: 'json',
		success: function(json, status, xhr, form) {
			document.location.reload(true);
		},
		error: function(xhr, status, statusText) {
			SolarReg.showAlertBefore('#archive-node-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	});
	
	function setupEditUserNodeLocationDisplay(loc) {
		var locDisplay = [], text = '';
		if ( loc.street ) {
			locDisplay.push(loc.street);
		}
		if ( loc.locality ) {
			locDisplay.push(loc.locality);
		}
		if ( loc.region ) {
			locDisplay.push(loc.region);
		}
		if ( loc.stateOrProvince ) {
			locDisplay.push(loc.stateOrProvince);
		}
		if ( loc.postalCode ) {
			locDisplay.push(loc.postalCode);
		}
		if ( loc.country ) {
			locDisplay.push(loc.country);
		}
		if ( locDisplay.length > 0 ) {
			text = locDisplay.join(', ');
		}
		if ( loc.latitude !== undefined && loc.longitude !== undefined ) {
			if ( text.length > 0 ) {
				text += ' (';
			}
			text += Number(loc.latitude).toFixed(3) + ', ' + Number(loc.longitude).toFixed(3);
			if ( loc.elevation ) {
				text += ' @ ' +loc.elevation + 'm';
			}
			text += ')';
		}
		$('#usernode-location').text(text);
	}
	
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
		
		setupEditUserNodeLocationDisplay(loc);
		setupEditUserLocationFields(loc);

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
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-warning', statusText);
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
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-warning', statusText);
		}
	}).data('page', 1).on('show', function() {
		dynamicSearchTimer = undefined;
		$('#edit-node-location-search-results').addClass('hidden');
	});
	
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
		var destPage = form.data('page') - 1;
		$('#edit-node-location-search-results').toggleClass('hidden', destPage !== 3);
		editNodeShowPage(form, destPage);
	});
	
	$('#edit-node-select-tz').on('click', function(event) {
		var form = $(this).parents('form').first();
		$('#edit-node-select-location').attr('disabled', 'disabled');
		editNodeShowPage(form, 3);
		if ( dynamicSearchTimer === undefined ) {
			searchForLocationDetails();
		}
	});
	
	$('#edit-node-location-search-results').on('click', 'tr', function(event) {
		var me = $(this);
		var loc = me.data('location');
		if ( me.hasClass('success') === false ) {
			me.parent().find('tr.success').removeClass('success');
			me.addClass('success');
		}
		setupEditUserLocationFields(loc);
	});
	
	function showLocationSearchResults(results) {
		var table = $('#edit-node-location-search-results');
		var templateRow = table.find('tr.template');
		var tbody = table.find('tbody');
		var form = $('#edit-node-modal');
		var i, len, tr, loc, prop, cell;
		tbody.empty();
		if ( results.length > 0 ) {
			for ( i = 0, len = results.length; i < len; i += 1 ) {
				tr = templateRow.clone(true);
				tr.removeClass('template');
				loc = results[i];
				tr.data('location', loc);
				for ( prop in loc ) {
					if ( loc.hasOwnProperty(prop) ) {
						cell = tr.find("[data-tprop='" +prop +"']");
						cell.text(loc[prop]);
					}
				}
				tbody.append(tr);
			}
			table.removeClass('hidden');
			$('#edit-node-location-search-no-match').addClass('hidden');
		} else {
			table.addClass('hidden');
			$('#edit-node-location-search-no-match').removeClass('hidden'); // no matches, allow saving
		}
		$('#edit-node-select-location').removeAttr('disabled');
	}
	
	function setupEditUserLocationFields(location) {
		if ( !location ) {
			location = {};
		}
		var form = $('#edit-node-modal');
		var elements = form.get(0).elements;
		var criteria = ['country', 'timeZoneId', 'region', 'stateOrProvince', 'locality', 'postalCode', 
		                'street', 'latitude', 'longitude', 'elevation'];
		var input;
		criteria.forEach(function(prop) {
			input = elements['node.location.'+prop];
			if ( input ) {
				$(input).val(location[prop]);
			}
		});
		input = elements['node.locationId'];
		if ( input ) {
			$(input).val(location.id);
		}
	}
	
	function handleLocationDetailsChange(event) {
		if ( dynamicSearchTimer ) {
			clearTimeout(dynamicSearchTimer);
		}
		dynamicSearchTimer = setTimeout(searchForLocationDetails, 300);
	}
	
	function searchForLocationDetails() {
		var form = $('#edit-node-modal');
		var elements = form.get(0).elements;
		var url = $('#edit-node-location-details').data('lookup-url');
		var criteria = ['timeZoneId', 'country', 'region', 'stateOrProvince', 'locality', 'postalCode'];
		var req = {}, input;
		criteria.forEach(function(prop) {
			input = elements['node.location.'+prop];
			if ( input ) {
				input = $(input);
				if ( input.val().length > 0 ) {
					req['location.'+prop] = input.val();
				}
			}
		});
		$.getJSON(url, req, function(json) {
			if ( json.success == true && json.data && Array.isArray(json.data.results) ) {
				showLocationSearchResults(json.data.results);
			}
		}).fail(function(xhr, statusText, data) {
			SolarReg.showAlertBefore('#edit-node-modal .modal-body > *:first-child', 'alert-warning', statusText);
		});
	}
	
	$('#edit-node-location-details').on('keyup', 'input', handleLocationDetailsChange);
	
	$('#edit-node-select-location').on('click', function() {
		var form = $('#edit-node-modal');
		$('#edit-node-location-search-results').addClass('hidden');
		editNodeShowPage(form, 4);
	});
	
	function numberOrUndefined(value) {
		var result;
		if ( typeof value === 'number' ) {
			result = value;
		} else if ( value === '' ) {
			// empty string to undefined;
		} else {
			result = Number(value);
			if ( isNaN(result) ) {
				result = undefined;
			}
		}
		return result;
	}
	
	$('#edit-node-select-location-private').on('click', function() {
		var form = $('#edit-node-modal');
		editNodeShowPage(form, 1);
		setupEditUserNodeLocationDisplay({
			name : $('#edit-node-location-name').val(),
			country : $('#edit-node-location-country').val(),
			stateOrProvince : $('#edit-node-location-state').val(),
			region : $('#edit-node-location-region').val(),
			locality : $('#edit-node-location-locality').val(),
			postalCode : $('#edit-node-location-postal-code').val(),
			street : $('#edit-node-location-street').val(),
			latitude : numberOrUndefined($('#edit-node-location-latitude').val()),
			longitude : numberOrUndefined($('#edit-node-location-longitude').val()),
			elevation : numberOrUndefined($('#edit-node-location-elevation').val()),
			timeZoneId : $('#edit-node-location-tz').val()
		});
	});
	
	function makeInviteCertCreateVisible(form, visible) {
		form.find('.cert-create').toggleClass('hidden', !visible);
		var btn = $('#node-cert-create');
		btn.toggleClass('btn-default', !visible)
			.toggleClass('btn-warning', visible);
		if ( visible ) {
			btn.data('default-title', btn.text());
			btn.text(btn.attr('data-confirm-title'));
		} else  if ( btn.data('default-title') ) {
			btn.text(btn.data('default-title'));
			btn.removeData('default-title');
		}
	}
	
	$('#invite-modal').on('show.bs.modal', function() {
		var form = $(this);
		var url = form.attr('action').replace(/\/[^\/]+$/, '/tzpicker.html');
		var tzcontainer = $('#tz-picker-container');
		
		makeInviteCertCreateVisible(form, false);

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
	
	$('#node-cert-create').on('click', function() {
		var btn = $(this),
			form = $(this.form),
			pwdInput = $(this.form.elements['phrase']),
			tzInput = $(this.form.elements['timeZone']),
			countryInput = $(this.form.elements['country']);
		if ( btn.hasClass('btn-warning') ) {
			// verify fields
			if ( !pwdInput.val() ) {
				pwdInput.trigger('focus');
				return;
			} else if ( !(tzInput.val() && countryInput.val()) ) {
				tzInput.trigger('focus');
				return;
			}
			// do it!
			$.post(SolarReg.solarUserURL('/sec/my-nodes/create-cert'), { 
					timeZone : tzInput.val(), 
					country : countryInput.val(),
					keystorePassword : pwdInput.val(),
					_csrf : SolarReg.csrf() 
				}, function(json) {
				document.location.reload(true);
			}).fail(function(data, statusText, xhr) {
				SolarReg.showAlertBefore('#node-cert-create .modal-body > *:first-child', 'alert-warning', statusText);
			});
		} else {
			// show warning
			makeInviteCertCreateVisible(form, true);
		}
	});
	
	(function() {
		var nodeRows = $('.node-row');
		if ( nodeRows.length > 0 ) {
			// show active alert situations
			$.getJSON(SolarReg.solarUserURL('/sec/alerts/user/situations'), function(json) {
				var i, alert;
				if ( json && json.data && Array.isArray(json.data) ) {
					for ( i = 0; i < json.data.length; i++ ) {
						alert = json.data[i];
						if ( alert.nodeId ) {
							nodeRows.filter('[data-node-id='+alert.nodeId+']').find('button.view-situation').each(function(idx, el) {
								$(el).data('alert-id', alert.id);
							}).removeClass('hidden');
						}
					}
				}
			});
		}
	}());
});
