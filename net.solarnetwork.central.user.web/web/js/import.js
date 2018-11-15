$(document).ready(function() {
	'use strict';
	
	var inputServices = [];
	
	var settingTemplates = $('#import-setting-templates');

	function loadDatumImportJobs(preserve) {
		return $.getJSON(SolarReg.solarUserURL('/sec/import/jobs?states=Queued,Staged,Claimed,Executing'), function(json) {
			console.log('Got import jobs: %o', json);
			if ( json && json.success === true && Array.isArray(json.data) ) {
				populateDatumImportJobs(json.data, preserve);
			}
		});
	}
	
	function populateDatumImportJobs(jobs, preserve) {
		jobs = Array.isArray(jobs) ? jobs : [];
		var container = $('#datum-import-job-list-container');
		var items = jobs.map(function(job) {
			var id = job.jobId.replace(/-.*/, '');
			var ctx = job.configuration.inputConfiguration;
			ctx.id = id; // so _contextItem.id is set
			ctx.jobId = job.jobId;
			var item = SolarReg.Settings.serviceConfigurationItem(ctx, inputServices);
			item.id = id;
			item.jobId = job.jobId;
			item.state = job.jobState;
			item.progressAmount = (job.percentComplete * 100).toFixed(0);
			return item;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve, function(item, el) {
			el.find('.progress').toggleClass('hidden', item.state === 'Staged');
		});
		container.closest('section').find('.listCount').text(jobs.length);
		return jobs;
	}
	
	function handleServiceIdentifierChange(event, services) {
		var target = event.target;
		if ( target.name === 'serviceIdentifier' ) {
			var service = SolarReg.findByIdentifier(services, $(event.target).val());
			var modal = $(target.form);
			var container = modal.find('.service-props-container').first();
			SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, modal.data('context-item'));
		}
	}
	
	function updateProgressAmount(bar, barAmount, percentComplete) {
		var value = (percentComplete * 100).toFixed(0);
		console.log('Progress now ' +percentComplete);
		if ( bar ) {
			bar.attr('aria-valuenow', value).css('width', value+'%');
		}
		if ( barAmount ) {
			barAmount.text(value);
		}
	}

	// ***** Upload form
	$('#edit-datum-import-job-modal').on('show.bs.modal', function(event) {
		SolarReg.Settings.prepareEditServiceForm($(event.target), inputServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, inputServices);
	})
	.on('submit', function(event) {
		var uploadProgressBar = $('.upload .progress-bar', event.target);
		var uploadProgressBarAmount = $('.amount', uploadProgressBar);
		
		var modal = $(event.target);
		modal.find('.before').addClass('hidden');
		modal.find('.upload').removeClass('hidden');
		
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			loadDatumImportJobs(true);
		}, function serializeDatumImportUploadForm(form) {
			var formData = new FormData(form);
			
			var inputConfig = SolarReg.Settings.encodeServiceItemForm(form);
			delete inputConfig.data; // delete file field
			var config = {
					name: inputConfig.name,
					stage: true,
					inputConfiguration: inputConfig,
			};
			
			// remove formData elements except for 'data'
			for ( var key of formData.keys() ) {
				if ( key !== 'data' ) {
					formData.delete(key);
				}
			}
			
			// add 'config' formData element as JSON
			var configData = new Blob([JSON.stringify(config)], {type : 'application/json'});
			formData.append('config', configData, 'config.json');
			
			return formData;
		}, {
			upload: function(event) {
				if ( event.lengthComputable ) {  
					updateProgressAmount(uploadProgressBar, uploadProgressBarAmount, event.loaded / event.total);
				}
			}
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		var modal = $(this);
		modal.find('.before').removeClass('hidden');
		modal.find('.upload').addClass('hidden');
		updateProgressAmount(modal.find('.upload .progress-bar'), modal.find('.upload .progress-bar .amount'), 0);
	});
	
	// ***** Manage job form
	$('#update-datum-import-job-modal').on('show.bs.modal', function(event) {
		// TODO SolarReg.Settings.prepareEditServiceForm($(event.target), inputServices, settingTemplates);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('submit', function(event) {
		// TODO
		return false;
	})
	.on('hidden.bs.modal', function() {
		// TODO
	});
	
	// ***** Preview job form
	$('#preview-datum-import-job-modal').on('show.bs.modal', function(event) {
		var modal = $(event.target);
		var container = $('#datum-import-preview-list-container');
		var item = SolarReg.Templates.findContextItem(modal);
		
		if ( !item.jobId ) {
			return;
		}
		
		// clear out current preview, if any
		SolarReg.Templates.populateTemplateItems(container, []);

		$.getJSON(SolarReg.solarUserURL('/sec/import/jobs/'+encodeURIComponent(item.jobId)+'/preview'), function(json) {
			if ( json && json.success === true && json.data && Array.isArray(json.data.results) ) {
				// TODO: display estimated row count via json.data.totalResults
				SolarReg.Templates.populateTemplateItems(container, json.data.results, false, function(item, el) {
					if ( item.i ) {
						// TODO: render i props
					}
					if ( item.a ) {
						// TODO: render a props
					}
					if ( item.s ) {
						// TODO: render s props
					}
					if ( item.t ) {
						// TODO: render t array
					}
				});
			}
		}).fail(function(xhr, statusText, error) {
			var json = {};
			if ( xhr.responseJSON ) {
				json = xhr.responseJSON;
			} else if ( xhr.responseText ) {
				json = JSON.parse(xhr.responseText);
			}
			var msg = 'Error: ' +(json && json.message ? json.message : statusText);
			var el = modal.find(SolarReg.Settings.modalAlertBeforeSelector);
			SolarReg.showAlertBefore(el, 'alert-warning', msg);
		});
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('hidden.bs.modal', function() {
		// TODO
	});
	
	// ***** Init page
	$('#datum-import-job-list-container .list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, inputServices, settingTemplates);
	});

	$('#datum-import-jobs').first().each(function() {
		var loadCountdown = 2;

		function liftoff() {
			loadCountdown -= 1;
			if ( loadCountdown === 0 ) {
				// TODO start the party
			}
		}

		// get available input services
		$.getJSON(SolarReg.solarUserURL('/sec/import/services/input'), function(json) {
			console.log('Got import input services: %o', json);
			if ( json && json.success === true ) {
				inputServices = json.data;
			}
			liftoff();
		});

		// list all jobs for user
		loadDatumImportJobs().then(function() {
			liftoff();
		});
	});
});