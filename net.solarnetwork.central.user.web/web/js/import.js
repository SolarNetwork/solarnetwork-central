$(document).ready(function() {
	'use strict';
	
	var inputServices = [];
	
	var settingTemplates = $('#import-setting-templates');
	
	var refreshToken;

	function loadDatumImportJobs(preserve) {
		return $.getJSON(SolarReg.solarUserURL('/sec/import/jobs?states=Queued,Staged,Claimed,Executing,Completed'), function(json) {
			console.log('Got import jobs: %o', json);
			if ( json && json.success === true && Array.isArray(json.data) ) {
				populateDatumImportJobs(json.data, preserve);
				
				// turn on auto-refresh if we have any pending jobs
				var pending = json.data.find(function(job) {
					return /^(Queued|Claimed|Executing)$/.test(job.jobState);
				});
				if ( !pending && refreshToken ) {
					clearInterval(refreshToken);
					refreshToken = null;
				} else if ( pending && !refreshToken ) {
					refreshToken = setInterval(function() {
						loadDatumImportJobs(true);
					}, 30000);
				}
			}
		});
	}
	
	function populateDatumImportJobs(jobs, preserve) {
		jobs = Array.isArray(jobs) ? jobs : [];
		var container = $('#datum-import-job-list-container');
		var items = jobs.map(function(job) {
			var id = job.jobId;
			var shortId = id.replace(/-.*/, '');
			var ctx = job.configuration.inputConfiguration;
			ctx.id = id;
			ctx.shortId = shortId;
			ctx.batchSize = job.configuration.batchSize;
			ctx.state = job.jobState;
			var item = SolarReg.Settings.serviceConfigurationItem(ctx, inputServices);
			item.id = id;
			item.shortId = shortId;
			item.timeZoneId = ctx.timeZoneId;
			item.batchSize = job.configuration.batchSize;
			item.state = job.jobState;
			item.progressAmount = (job.percentComplete * 100).toFixed(0);
			item.success = job.success;
			item.message = job.message;
			item.submitDateDisplay = moment(job.submitDate).format('D MMM YYYY HH:mm');
			if ( job.completionDate ) {
				item.completionDateDisplay = moment(job.completionDate).format('D MMM YYYY HH:mm');
			}
			return item;
		});
		SolarReg.Templates.populateTemplateItems(container, items, preserve, function(item, el) {
			el.find('.progress').toggleClass('hidden', item.state === 'Staged' || item.state === 'Completed')
				.find('.progress-bar').attr('aria-valuenow', item.progressAmount).css('width', item.progressAmount +'%');
			el.find('.preview').toggleClass('hidden', item.state !== 'Staged');
			el.find('.success-ok').toggleClass('hidden', item.state !== 'Completed' || !item.success);
			el.find('.success-error').toggleClass('hidden', item.state !== 'Completed' || item.success);
			el.find('.complete').toggleClass('hidden', item.state !== 'Completed');
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

	function renderTemplateProperties(container, template, item) {
		var prop, data;
		if ( !item ) {
			return;
		}
		data = {};
		for ( prop in item ) {
			data.name = prop;
			data.value = item[prop];
			SolarReg.Templates.appendTemplateItem(container, template, data);
		}
	}
	
	// ***** Upload/edit form
	$('#edit-datum-import-job-modal').on('show.bs.modal', function(event) {
		var modal = $(event.target);
		var item = SolarReg.Templates.findContextItem(modal);

		SolarReg.Settings.prepareEditServiceForm(modal, inputServices, settingTemplates);

		// disable create-only reqiured items
		modal.find('.create').toggleClass('hidden', !!item).find('input[required]').prop('required', !item);
		if ( item && item.state !== 'Staged' ) {
			modal.find('input:not([type=hidden])').prop('readonly', true);
			modal.find('select,button[type=submit]').prop('disabled', true);
		}
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('change', function(event) {
		handleServiceIdentifierChange(event, inputServices);
	})
	.on('submit', function(event) {
		var uploadProgressBar = $('.upload .progress-bar', event.target);
		var uploadProgressBarAmount = $('.amount', uploadProgressBar);
		var editing = !!event.target.elements['id'].value;
		var modal = $(event.target);
		
		if ( !editing ) {
			modal.find('.before').addClass('hidden');
			modal.find('.upload').removeClass('hidden');
		}
		
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			loadDatumImportJobs(false);
		}, function serializeDatumImportUploadForm(form) {
			var inputConfig = SolarReg.Settings.encodeServiceItemForm(form);
			
			var config = {
					name: inputConfig.name,
					stage: true,
					batchSize: (inputConfig.batchSize ? inputConfig.batchSize : null),
					inputConfiguration: inputConfig,
			};

			delete inputConfig.id;
			delete inputConfig.data; // delete file field
			delete inputConfig.batchSize; // this is overall config, not input
			
			if ( editing ) {
				return config;
			}
			
			// create
			var formData = new FormData(form);
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
			urlId : editing,
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
		SolarReg.Settings.resetEditServiceForm(this, $('#datum-import-job-list-container'));
	});
	
	// ***** Preview job form
	$('#preview-datum-import-job-modal').on('show.bs.modal', function(event) {
		var modal = $(event.target);
		var container = $('#datum-import-preview-list-container');
		var item = SolarReg.Templates.findContextItem(modal);
		
		if ( !item.id ) {
			return;
		}
		
		// clear out current preview, if any
		SolarReg.Templates.populateTemplateItems(container, []);

		$.getJSON(SolarReg.solarUserURL('/sec/import/jobs/'+encodeURIComponent(item.id)+'/preview'), function(json) {
			if ( !(json && json.success === true && json.data && Array.isArray(json.data.results)) ) {
				return;
			}
			var sampleTemplate = modal.find('.sample-template .template');
			// TODO: display estimated row count via json.data.totalResults
			SolarReg.Templates.populateTemplateItems(container, json.data.results, false, function(item, el) {
				var itemContainer;
				if ( item.i ) {
					renderTemplateProperties(el.find('.instantaneous-sample-container'), sampleTemplate, item.i);
				}
				if ( item.a ) {
					renderTemplateProperties(el.find('.accumulating-sample-container'), sampleTemplate, item.a);
				}
				if ( item.s ) {
					renderTemplateProperties(el.find('.status-sample-container'), sampleTemplate, item.s);
				}
				if ( Array.isArray(item.t) ) {
					el.find('.tag-list').text(item.t.join(', '));
				}
			});
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
		SolarReg.Settings.resetEditServiceForm(this);
	});
	
	// ***** Confirm import form
	$('#confirm-datum-import-job-modal').on('show.bs.modal', function(event) {
		var modal = $(event.target),
			ctx = SolarReg.Templates.findContextItem(modal);
		SolarReg.Settings.prepareEditServiceForm(modal, inputServices, settingTemplates);
		SolarReg.Templates.replaceTemplateProperties(modal, ctx);
	})
	.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
	.on('submit', function(event) {
		SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
			loadDatumImportJobs(true);
		}, function() {
			return null;
		}, {
			urlSerializer: function(url, item) {
				var id = event.target.elements['id'].value;
				return SolarReg.replaceTemplateParameters(url, {id:id});
			}
		});
		return false;
	})
	.on('hidden.bs.modal', function() {
		SolarReg.Settings.resetEditServiceForm(this);
		$(this).find('[data-tprop]').text('');
	});
	
	// ***** Init page
	$('#datum-import-job-list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, inputServices, settingTemplates);
	});

	$('.import.edit-config button.delete-config').on('click', SolarReg.Settings.handleEditServiceItemDeleteAction);

	$('#datum-import-jobs').first().each(function() {
		// get available input services
		$.getJSON(SolarReg.solarUserURL('/sec/import/services/input'), function(json) {
			console.log('Got import input services: %o', json);
			if ( json && json.success === true ) {
				inputServices = json.data;
			}
			// list all jobs for user
			loadDatumImportJobs();
		});
	});
});