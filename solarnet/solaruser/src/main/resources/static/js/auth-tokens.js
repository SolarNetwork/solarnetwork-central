$(document).ready(function() {
	'use strict';
	
	if ( !document.getElementById('user-auth-tokens') ) {
		return;
	}

	const activePolicy = {nodeIds:[], sourceIds:[]};
	let tokenCreated = false;
	
	(() => {
		// clone node list menu into token create forms
		const nodeList = document.getElementById('node-list');
		if ( nodeList ) {
			for ( const container of document.querySelectorAll('.node-list') ) {
				const menu = nodeList.content.cloneNode(true);
				menu.querySelector('select').id = container.dataset.id;
				container.appendChild(menu);
			}
		}
	})();
	
	function handleAuthTokenCreated(json, status, xhr, form) {
		if ( json.success === true && json.data ) {
			tokenCreated = true;
			const f = form[0];
			f.elements.tokenId.value = json.data.id;
			f.elements.tokenSecret.value = json.data.authSecret;
		}
		form.find('.before').hide();
		form.find('.after').show();
	}
	
	function reloadIfTokenCreated() {
		if ( tokenCreated ) {
			document.location.reload(true);
		}
	}
	
	function resetAuthTokenForm() {
		const form = this;
		form.reset();
		$('#create-user-auth-token .nav-pills > *:first').tab('show');
		reloadIfTokenCreated();
	}

	/**
	 * Generate an array of values from a select element.
	 * 
	 * @param {HTMLSelectElement} menu - the menu
	 * @returns {Array<string>} the values as an array 
	 */
	function arrayFromMenu(menu) {
		let result = [];
		for ( const opt of menu.selectedOptions ) {
			result.push(opt.value);
		}
		return result;
	}
		
	function setupActivePolicyFromForm(form) {
		const containerText = form.find('textarea[name=sourceIds]').val(),
			containerSourceIds = (containerText ? containerText.split(/\s*,\s*/) : []),
			containerNodeMetadataText = form.find('textarea[name=nodeMetadataPaths]').val(),
			containerNodeMetadataPaths = (containerNodeMetadataText ? containerNodeMetadataText.split(/\s*,\s*/) : []),
			containerUserMetadataText = form.find('textarea[name=userMetadataPaths]').val(),
			containerUserMetadataPaths = (containerUserMetadataText ? containerUserMetadataText.split(/\s*,\s*/) : []),
			containerApiPathText = form.find('textarea[name=apiPaths]').val(),
			containerApiPaths = (containerApiPathText ? containerApiPathText.split(/\s*,\s*/) : []),
			containerNotAfter = form.find('input[name=notAfter]').val(),
			containerRefreshAllowed = form.find('input[name=refreshAllowed]:checked').val(),
			containerNodeIds = arrayFromMenu(form[0].elements.nodeIds);
		
		activePolicy.nodeIds = containerNodeIds;
		activePolicy.sourceIds = containerSourceIds;
		activePolicy.nodeMetadataPaths = containerNodeMetadataPaths;
		activePolicy.userMetadataPaths = containerUserMetadataPaths;
		activePolicy.apiPaths = containerApiPaths;
		if ( containerRefreshAllowed === 'true' ) {
			activePolicy.refreshAllowed = true;
		} else {
			delete activePolicy.refreshAllowed;
		}
		if ( containerNotAfter ) {
			activePolicy.notAfter = containerNotAfter;
		} else {
			delete activePolicy.notAfter; 
		}
	}
	
	function beforeSubmitTokenForm(array) {
		var sourceIdsIdx = array.findIndex(function(obj) {
				return obj.name === 'sourceIds';
			}),
			nodeMetadataPathsIdx,
			userMetadataPathsIdx,
			apiPathsIdx;

		if ( sourceIdsIdx >= 0 ) {
			array.splice(sourceIdsIdx, 1);
		}
		nodeMetadataPathsIdx = array.findIndex(function(obj) {
			return obj.name === 'nodeMetadataPaths';
		});
		if ( nodeMetadataPathsIdx >= 0 ) {
			array.splice(nodeMetadataPathsIdx, 1);
		}
		userMetadataPathsIdx = array.findIndex(function(obj) {
			return obj.name === 'userMetadataPaths';
		});
		if ( userMetadataPathsIdx >= 0 ) {
			array.splice(userMetadataPathsIdx, 1);
		}
		apiPathsIdx = array.findIndex(function(obj) {
			return obj.name === 'apiPaths';
		});
		if ( apiPathsIdx >= 0 ) {
			array.splice(apiPathsIdx, 1);
		}

		activePolicy.sourceIds.forEach(function(sourceId) {
			array.push({
				name : 'sourceId',
				value : sourceId,
				type : 'text'
			});
		});
		activePolicy.nodeIds.forEach(function(nodeId) {
			array.push({
				name : 'nodeId',
				value : nodeId,
				type : 'text'
			});
		});
		activePolicy.nodeMetadataPaths.forEach(function(path) {
			array.push({
				name : 'nodeMetadataPath',
				value : path,
				type : 'text'
			});
		});
		activePolicy.userMetadataPaths.forEach(function(path) {
			array.push({
				name : 'userMetadataPath',
				value : path,
				type : 'text'
			});
		});
		activePolicy.apiPaths.forEach(function(path) {
			array.push({
				name : 'apiPath',
				value : path,
				type : 'text'
			});
		});
	}

	$('#create-user-auth-token').on('hidden.bs.modal', resetAuthTokenForm).each(function() {
		$(this).ajaxForm({
			dataType: 'json',
			beforeSerialize: setupActivePolicyFromForm,
			beforeSubmit: beforeSubmitTokenForm,
			success: handleAuthTokenCreated,
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#create-user-auth-token .modal-body > *:first-child', 'alert-warning', statusText);
			}
		});
	});
	
	$('#create-data-auth-token').on('hidden.bs.modal', resetAuthTokenForm).each(function() {
		$(this).ajaxForm({
			dataType: 'json',
			beforeSerialize: setupActivePolicyFromForm,
			beforeSubmit: beforeSubmitTokenForm,
			success: handleAuthTokenCreated,
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#create-data-auth-token .modal-body > *:first-child', 'alert-error', statusText);
			}
		});
	});
	
	$('.create-auth-token-download-csv').on('click', function downloadTokenCsv() {
		const link = this;
		const form = $(this).closest('form')[0];
		const csv = 'Token Type,Token ID,Token Secret,Name,Description\r\n'
			+ (form.elements.tokenType.value === 'User' ? 'User' : 'Data')
			+ ','
			+ form.elements.tokenId.value
			+ ','
			+ form.elements.tokenSecret.value
			+ ',"'
			+ form.elements.name.value.replaceAll('"','""')
			+ '","'
			+ form.elements.description.value.replaceAll('"','""')
			+ '"\r\n';
		const uri = encodeURI('data:text/csv;charset=utf-8,'+csv);
		link.setAttribute("href", uri);
	});

	
	$('.edit-token-info').on('click', function(event) {
		var target = $(event.target),
			tokenId = target.data('token-id'),
			name = target.data('token-name') || '',
			desc = target.data('token-description') || '',
			form = $('#edit-auth-token-info');
		event.preventDefault();
		form[0].elements['id'].value = tokenId;
		form[0].elements['name'].value = name;
		form[0].elements['description'].value = desc;
		form.modal('show');
	});
	
	$('#edit-auth-token-info').each(function() {
		$(this).ajaxForm({
			dataType: 'json',
			success: function(json, status, xhr, form) {
				form.modal('hide');
				document.location.reload(true);
			},
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#edit-auth-token-info .modal-body > *:first-child', 'alert-error', statusText);
			}
		});
	});
	
	$('.token-delete').on('click', function(event) {
		var target = $(event.target),
			tokenId = target.data('token-id'),
			form = $('#delete-auth-token');
		event.preventDefault();
		form[0].elements['id'].value = tokenId;
		form.modal('show');
	});
	
	$('#delete-auth-token').each(function() {
		$(this).ajaxForm({
			dataType: 'json',
			success: function(json, status, xhr, form) {
				form.modal('hide');
				document.location.reload(true);
			},
			error: function(xhr, status, statusText) {
				SolarReg.showAlertBefore('#delete-auth-token .modal-body > *:first-child', 'alert-error', statusText);
			}
		});
	});
	
	$('.token-change-status').on('click', function(event) {
		var target = $(event.target),
			tokenId = target.data('token-id'),
			newStatus = (target.data('status') === 'Active' ? 'Disabled' : 'Active'),
			data = {id:tokenId, status:newStatus, '_csrf':SolarReg.csrfData.token};
		event.preventDefault();
		$.post(target.data('action'), data, function() {
				document.location.reload(true);
			}, 'json');
	});
	
	/*
	$('.action-data-token').find('button').on('click', function(event) {
		//.user-token-change-status or .user-token-delete
		event.preventDefault();
		var button = $(this);
		var tokenId = button[0].form.elements['id'].value;
		var csrf = button[0].form.elements['_csrf'].value;
		if ( button.hasClass('data-token-delete') ) {
			var form = $('#delete-data-auth-token');
			form[0].elements['id'].value = tokenId;
			form.find('.container-token').text(tokenId);
			form.modal('show');
		} else if ( button.hasClass('data-token-change-status') ) {
			var newStatus = (button.data('status') === 'Active' ? 'Disabled' : 'Active');
			$.post(button.data('action'), {id:tokenId, status:newStatus, '_csrf':csrf}, function(data) {
				document.location.reload(true);
			}, 'json');
		}
	});
	*/
});
