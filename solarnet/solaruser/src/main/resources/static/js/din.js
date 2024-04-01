function dinManagement() {
	'use strict';
	/**
	 * A DIN UI entity model.
	 *
	 * @typedef {Object} DinEntityModel
	 * @property {object} _contextItem the configuration entity
	 * @property {string} entityType the system type (e.g. 'c', 't', 'e', 'ea')
	 * @property {string} [id] the entity ID
	 * @property {string} [createdDisplay] the entity creation date as a display string
	 * @property {boolean} [enabled] the enabled state
	 */
	
	/**
	 * A DIN system configuration.
	 *
	 * @typedef {Object} DinSystem
	 * @property {string} type one of 'c', 't', 'e', 'ea'
	 * @property {jQuery} container the element that holds the rendered list of entities
	 * @property {Array<Object>} configs the entities
	 * @property {Map<Number, DinEntityModel>} configsMap a mapping of entity IDs to associated entities
	 */
	
	/**
	 * Create a DIN system.
	 *
	 * @param {jQuery} listContainer the list container
	 * @param {string} type the entity type
	 * @returns {DinSystem} the new system object
	 */
	function createSystem(listContainer, type) {
		return Object.freeze({
			type: type,
			container: listContainer,
			configs: [],
			configsMap: new Map()
		});
	}
	
	/**
	 * Default edit form setup handler.
	 *
	 * @this {HTMLFormElement} the modal form
	 */
	function modalEditFormShowSetup() {
		const form = this
			, modal = $(this)
			, config = SolarReg.Templates.findContextItem(this)
			, enabled = (config && config.enabled === true ? true : false)
			, pubSolarFlux = (!config || config.publishToSolarFlux === true ? true : false)
			, prevTrack = (config && config.previousInputTracking === true ? true : false)
			, type = (this.dataset ? this.dataset.systemType : undefined);
		
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarFlux]'), pubSolarFlux);		
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=previousInputTracking]'), prevTrack);
		
		SolarReg.Settings.prepareEditServiceForm(modal
			, type == TRANSFORM_SYS ? transformServices : []
			, settingTemplates);
			
		// populate transforms, if available
		if (form.elements.transformId) {
			let menu = form.elements.transformId;
			let selectedIdx = -1;
			let idx = -1;
			$(menu).empty();
			for (let entity of systems[TRANSFORM_SYS].configs) {
				++idx;
				if (config && entity.transformId == config.transformId) {
					selectedIdx = idx;
				}
				form.elements.transformId.options.add(new Option(entity.name, entity.transformId));
			}
			if (selectedIdx > -1) {
				form.elements.transformId.selectedIndex = selectedIdx;
			}
		}
	}

	/**
	 * Default modal edit form submit handler.
	 *
	 * @param {Event} event the form submit event
	 * @param {Function} renderFn the render function to invoke
	 * @returns {Boolean} `false` to return from event callback
	 */
	function modalEditFormSubmit(event, renderFn) {
		SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
			renderFn([res], true);
		});
		return false;
	}

	/**
	 * Default modal edit form cleanup handler.
	 *
	 * @this {HTMLFormElement} the modal form element
	 */
	function modalEditFormHiddenCleanup() {
		const systemType = this.dataset.systemType,
				config = SolarReg.Templates.findContextItem(this);

		/** @type {DinSystem} */
		var sys;
		
		if ( systems[systemType] ) {
			sys = systems[systemType];
		} else if ( config && config.endpointId ) {
			sys = endpointSystems.get(config.endpointId);
			sys = sys ? sys[systemType] : undefined;
		}
		if (!sys) {
			return;
		}
		const modal = $(this);
		const container = sys.container.find('.list-container');
		SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
			if (deleted) {
				SolarReg.deleteServiceConfiguration(id, sys.configs, sys.container);
				sys.configsMap.delete(id);
			}
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarFlux]'), true);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=previousInputTracking]'), false);
		});
	}

	/**
	 * Handle a service identifier menu change.
	 * 
	 * @param {Event} event the event
	 * @param {Map<String,ServiceInfo>} services the service mapping
	 */
	function handleServiceIdentifierChange(event, services) {
		let target = event.target;
		if ( target.name === 'serviceIdentifier' ) {
			let service = SolarReg.findByIdentifier(services, $(event.target).val());
			if (service) {
				let modal = $(target.form);
				let config = SolarReg.Templates.findContextItem(modal);
				let container = modal.find('.service-props-container').first();
				SolarReg.Settings.renderServiceInfoSettings(service, container, settingTemplates, config);
			}
		}
	}

	function updateProgressAmount(bar, barAmount, percentComplete) {
		var value = (percentComplete * 100).toFixed(0);
		if (bar) {
			bar.attr('aria-valuenow', value).css('width', value + '%');
		}
		if (barAmount) {
			barAmount.text(value);
		}
	}

	/* ============================
	   Globals
	   ============================ */
	   
	const CREDENTIAL_SYS = 'c';
	const TRANSFORM_SYS = 't';
	const ENDPOINT_SYS = 'e';
	const ENDPOINT_AUTH_SYS = 'auth';

	/**
	 * Mapping of system keys to associated systems.
	 * 
	 * @type {Object<String,DinSystem>}
	 */
	const systems = Object.freeze({
		c: createSystem($('#din-credentials-container'), CREDENTIAL_SYS),
		t: createSystem($('#din-transforms-container'), TRANSFORM_SYS),
		e: createSystem($('#din-endpoints-container'), ENDPOINT_SYS),
	});
	
	/**
	 * A map of service ID to Object of ServiceInfo properties.
	 * @type {Array<ServiceInfo>}
	 */
	const transformServices = [];

	const endpointSystems = new Map(); // map of endpoint ID to Object of DinSystem properties
	
	const settingTemplates = $('#setting-templates');

	/* ============================
	   DIN entity delete
	   ============================ */

	$('.din.edit-config button.delete-config').on('click', function(event) {
		var options = {};
		SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
	});

	/* ============================
	   Credentials
	   ============================ */

	function renderCredentialConfigs(configs, preserve) {
		/** @type {DinSystem} */
		const sys = systems[CREDENTIAL_SYS];
		if (!sys) {
			return;
		}
		configs = Array.isArray(configs) ? configs : [];
		if (!preserve) {
			sys.configsMap.clear();
		}

		var items = configs.map(function(config) {
			var model = createCredentialModel(config);
			sys.configsMap.set(config.id, model);
			return model;
		});

		SolarReg.Templates.populateTemplateItems(sys.container, items, preserve);
		SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
	}

	function createCredentialModel(config) {
		config.id = config.credentialId;
		config.systemType = CREDENTIAL_SYS;
		var model = SolarReg.Settings.serviceConfigurationItem(config, []);
		SolarReg.fill(model, config);
		model.createdDisplay = moment(config.created).format('D MMM YYYY');
		if (config.expires) {
			model.expiresDisplay = moment(config.expires).format('D MMM YYYY');
		}
		model.valid = !config.expired;
		return model;
	}

	systems[CREDENTIAL_SYS].container.find('.list-container').on('click', function(/** @type {MouseEvent} */ event) {
		SolarReg.Settings.handleEditServiceItemAction(event, [], []);
	});

	// ***** Credential add
	$('#din-credential-add-button').on('click', function() {
		$('#din-credential-edit-modal').modal('show');
	});

	// ***** Credential edit
	$('#din-credential-edit-modal').on('show.bs.modal', modalEditFormShowSetup)
		.on('submit', function credentialEditModalFormSubmit(event) {
			return modalEditFormSubmit(event, renderCredentialConfigs);
		})
		.on('hidden.bs.modal', modalEditFormHiddenCleanup)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

	/* ============================
	   Transforms
	   ============================ */

	function renderTransformConfigs(configs, preserve) {
		/** @type {DinSystem} */
		const sys = systems[TRANSFORM_SYS];
		if (!sys) {
			return;
		}
		configs = Array.isArray(configs) ? configs : [];
		if (!preserve) {
			sys.configsMap.clear();
		}

		var items = configs.map(function(config) {
			var model = createTransformModel(config);
			sys.configsMap.set(config.id, model);
			return model;
		});

		SolarReg.Templates.populateTemplateItems(sys.container, items, preserve);
		SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
	}

	function createTransformModel(config) {
		config.id = config.transformId;
		config.systemType = TRANSFORM_SYS;
		var model = SolarReg.Settings.serviceConfigurationItem(config, transformServices);
		SolarReg.fill(model, config);
		model.createdDisplay = moment(config.created).format('D MMM YYYY');
		return model;
	}

	systems[TRANSFORM_SYS].container.find('.list-container').on('click', function(/** @type {MouseEvent} */ event) {
		SolarReg.Settings.handleEditServiceItemAction(event, [], []);
	});

	// ***** Transform add
	$('#din-transform-add-button').on('click', function() {
		$('#din-transform-edit-modal').modal('show');
	});

	// ***** Transform edit
	$('#din-transform-edit-modal')
		.on('show.bs.modal', modalEditFormShowSetup)
		.on('change', function(event) {
			handleServiceIdentifierChange(event, transformServices);
		})
		.on('submit', function transformEditModalFormSubmit(event) {
			return modalEditFormSubmit(event, renderTransformConfigs);
		})
		.on('hidden.bs.modal', modalEditFormHiddenCleanup)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

	/* ============================
	   Endpoints
	   ============================ */

	function renderEndpointConfigs(configs, preserve) {
		/** @type {DinSystem} */
		const sys = systems[ENDPOINT_SYS];
		if (!sys) {
			return;
		}
		configs = Array.isArray(configs) ? configs : [];
		if (!preserve) {
			sys.configsMap.clear();
		}

		var items = configs.map(function(config) {
			var model = createEndpointModel(config);
			sys.configsMap.set(config.id, model);
			return model;
		});

		SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, function endpointTemplateCallback(item, el) {
			// update all accordian item IDs to be unique, with the associated endpoint ID
			let id = item.id;
			el.find('div.panel-heading').attr('id', 'endpoint-heading-' + id);
			el.find('.panel-title a').attr('href', '#endpoint-body-' + id).attr('aria-controls', 'endpoint-body-' + id);
			el.find('div.panel-collapse ').attr('id', 'endpoint-body-' + id).attr('aria-labelledby', 'endpoint-heading-' + id);
			
			// populate inner details
			SolarReg.Templates.replaceTemplateProperties(el.find('.endpoint-details'), item);
		});
		SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
	}

	function createEndpointModel(config) {
		config.id = config.endpointId;
		config.systemType = ENDPOINT_SYS;
		var model = SolarReg.Settings.serviceConfigurationItem(config, []);
		SolarReg.fill(model, config);
		model.createdDisplay = moment(config.created).format('D MMM YYYY');
		
		if (config.transformId) {
			let xform = systems[TRANSFORM_SYS].configsMap.get(config.transformId);
			if (xform) {
				model.transformDisplay = xform.name + ' (' + config.transformId +')';
			}
		}
		return model;
	}

	systems[ENDPOINT_SYS].container.find('.list-container').on('click', function(/** @type {MouseEvent} */ event) {
		SolarReg.Settings.handleEditServiceItemAction(event, [], []);
	});

	// ***** Endpoint add
	$('#din-endpoint-add-button').on('click', function() {
		$('#din-endpoint-edit-modal').modal('show');
	});

	// ***** Endpoint edit
	$('#din-endpoint-edit-modal')
		.on('show.bs.modal', modalEditFormShowSetup)
		.on('submit', function endpointEditModalFormSubmit(event) {
			return modalEditFormSubmit(event, renderEndpointConfigs);
		})
		.on('hidden.bs.modal', modalEditFormHiddenCleanup)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

	// ***** Endpoints accordian
	$('#din-endpoints-container').on('show.bs.collapse', function handleEndpointCollapseShow(event) {
		const target = event.target;
		const config = SolarReg.Templates.findContextItem(target);
		if ( config ) {
			console.debug('Show endpoint details: %o', config);
			renderEndpointDetails(config, $(target));
		}
	});

	systems[ENDPOINT_SYS].container.on('click', function handleEndpointsClick(/** @type {MouseEvent} */ event) {
		console.debug('Click on endpoints %o', event);

		// check if clicked on accordian title bar, to toggle visibility
		const target = $(event.target);
		if ( target.hasClass('panel-title') || target.hasClass('panel-heading') ) {
			// toggle this panel
			let dest = target.parent().find('a[data-parent="#din-endpoints-accordian"]').attr('href');
			if ( dest ) {
				$(dest).collapse('toggle');
			}
			return;
		}
		
		// check if clicked on add entity button for a related entity
		const btn = target.closest('button');
		if ( btn.hasClass('add-entity') ) {
			let modal = $(btn.data('editModal'));
			let config = SolarReg.Templates.findContextItem(btn);
			// open the edit form, passing the endpoint ID in the context item
			if ( modal && config ) {
				SolarReg.Templates.setContextItem(modal, {endpointId:config.id});
				modal.modal('show');
				event.preventDefault();
			}
			return;
		}
	});

	function renderEndpointDetails(item, el) {
		if ( endpointSystems.has(item.id) ) {
			// already loaded
			return;
		}
		const eSystems = Object.freeze({
			auth: createSystem(el.find('.endpoint-auths'), ENDPOINT_AUTH_SYS),
		});
		endpointSystems.set(item.id, eSystems);

		// load data
		const progressBar = $('.loading .progress-bar', el);
		const progressBarAmount = $('.amount', progressBar);
		const loadTotal = 1;
		var loadCountdown = loadTotal;
		var authConfs = [];

		function liftoff() {
			loadCountdown -= 1;
			updateProgressAmount(progressBar, progressBarAmount, (loadTotal-loadCountdown) / loadTotal);
			if (loadCountdown === 0) {
				renderEndpointDetailConfigs(authConfs, eSystems.auth);
				el.find('.loading').addClass('hidden');
				el.find('.section.hidden').removeClass('hidden');
				console.debug('Endpoint %o data loaded.', item);
			}
		}

		// list all auths
		$.getJSON(SolarReg.solarUserURL('/sec/din/endpoints/auths?endpointId='+item.id), function(json) {
			console.debug('Got DIN endpoint %d auths: %o', item.id, json);
			if (json && json.success === true) {
				authConfs = json.data ? json.data.results : undefined;
				if ( authConfs ) {
					// assign identifier property
					for (let auth of authConfs) {
						auth.identifier = auth.credentialId;
					}
				}
			}
			liftoff();
		});

	}

	/**
	 * Render endpoint detail configurations.
	 *
	 * @argument {Array<Object>} configs the entities
	 * @argument {DinSystem} sys the endpoint system
	 * @argument {Boolean} preserve true to preserve existing template instances
	 */
	function renderEndpointDetailConfigs(configs, sys, preserve) {
		configs = Array.isArray(configs) ? configs : [];
		if (!preserve) {
			sys.configsMap.clear();
		}

		var items = configs.map(function(config) {
			var model = createEndpointDetailModel(config, sys.type);
			sys.configsMap.set(config.id, model);
			return model;
		});

		// show detail table headers only if at least one item
		if ( items.length > 0 ) {
			sys.container.find('thead.hidden').removeClass('hidden');
		} else {
			sys.container.find('thead').addClass('hidden');
		}

		SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, undefined, sys.type+'-');
		SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
	}

	function createEndpointDetailModel(config, type) {
		config.id = config.credentialId; // only credential details
		config.systemType = type;
		var model = SolarReg.Settings.serviceConfigurationItem(config, []);
		SolarReg.fill(model, config);
		model.createdDisplay = moment(config.created).format('D MMM YYYY');
		if (config.credentialId) {
			let cred = systems[CREDENTIAL_SYS].configsMap.get(config.credentialId);
			if (cred) {
				model.credential = cred;
				model.username = cred.username;
			}
		}
		return model;
	}

	function endpointEditModalFormShowSetup(event) {
		const form = this;
		const config = SolarReg.Templates.findContextItem(form);
		
		if (form.elements.credentialId) {
			// populate select with current credentials list, minus other credentials also configured
			let endpointAuths = new Map();
			let endpointAuthsSys = endpointSystems.get(config.endpointId)[ENDPOINT_AUTH_SYS];
			if (endpointAuthsSys) {
				endpointAuths = endpointAuthsSys.configsMap;
			}
			
			let selectedIdx = -1;
			let idx = -1;
			$(form.elements.credentialId).empty();
			for (let entity of systems[CREDENTIAL_SYS].configs) {
				++idx;
				if (entity.credentialId == config.credentialId) {
					selectedIdx = idx;
				} else if(endpointAuths.has(entity.credentialId)) {
					// skip this as already in use and can't select duplicates
					continue;
				}
				form.elements.credentialId.options.add(new Option(entity.username, entity.credentialId));
			}
			if (selectedIdx > -1) {
				form.elements.credentialId.selectedIndex = selectedIdx;
			}

			// make credentialId disabled to avoid changing primary key
			form.elements.credentialId.disabled = !!config.id;
		}
		
		return modalEditFormShowSetup.call(form, event);
	}
		
	function endpointEditModalFormSubmit(event) {
		const config = SolarReg.Templates.findContextItem(this);
		const sys = endpointSystems.get(config.endpointId ? config.endpointId : config.id);
		const systemType = this.dataset.systemType;
		return modalEditFormSubmit.call(this, event, function(configs, preserve) {
			renderEndpointDetailConfigs(configs, sys[systemType], preserve);
		});
	}

	// ***** Endpoint auth edit
	$('#din-auth-edit-modal')
		.on('show.bs.modal', endpointEditModalFormShowSetup)
		.on('submit', endpointEditModalFormSubmit)
		.on('hidden.bs.modal', modalEditFormHiddenCleanup)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});
	
	/* ============================
	   Endpoint Transform Preview
	   ============================ */

	$('#din-endpoint-transform-preview-modal')
		.on('show.bs.modal', function endpointTransformPreviewModalFormShowSetup() {
			const form = this
				, modal = $(form)
				, config = SolarReg.Templates.findContextItem(form);
			console.debug('Endpoint preview for %o', config);
			
			const previewConfig = Object.assign({}, config);
			
			const xform = systems[TRANSFORM_SYS].configsMap.get(config.transformId);
			if (xform) {
				previewConfig.transformDisplay = xform.name;
			}
			SolarReg.Templates.setContextItem(modal, previewConfig);
			SolarReg.Templates.replaceTemplateProperties(modal.find('.endpoint-details-container'), previewConfig);
			
			const container = $('#din-endpoint-transform-output-container');
			container.children().addClass('hidden');
			container.find('.before').removeClass('hidden');
		})
		.on('submit', function endpointTransformPreviewModalFormSubmit(event) {
			event.preventDefault();
			const form = this
				, config = SolarReg.Templates.findContextItem(form)
				, container = $('#din-endpoint-transform-output-container')
				, submitBtn = container.find('button[type=submit]')
				, inputData = form.elements.inputData.value
				, previousInputData = form.elements.previousInputData.value
				, inputType = form.elements.inputType.value
				, queryParams = form.elements.queryParams.value
				, errorContainer = container.find('.error-container')
				;
				
			submitBtn.prop('disabled', true);
			
			const url = encodeURI(SolarReg.replaceTemplateParameters(decodeURI(form.action), config));
			
			const reqBody = {
				contentType: inputType,	
				data: inputData
			};
			if (queryParams) {
				reqBody.query = queryParams;
			}
			if (previousInputData) {
				reqBody.parameters = {
					'previous-input': previousInputData
				};
			}
			
			$.ajax({
				type: 'POST',
				url: url,
				contentType: 'application/json',
				data: JSON.stringify(reqBody),
				dataType: 'json',
				beforeSend: function(xhr) {
					SolarReg.csrf(xhr);
				}
			}).done(function(json) {
				container.children().addClass('hidden');
				if ( json && json.success === true ) {
					
					if (json.data && json.data.message) {
						errorContainer.text(json.data.message).removeClass('hidden');
					} else if (json.data && Array.isArray(json.data.datum) && json.data.datum.length > 0) {
						// provide empty node/source for display if none provided, to replace any
						// previously shown values
						for (let d of json.data.datum) {
							if (d.nodeId === undefined) {
								d.nodeId = '';
							}
							if (d.sourceId === undefined) {
								d.sourceId = '';
							}
						}
						// render datum
						const datumContainer = container.find('.datum-container');
						SolarReg.Templates.populateTemplateItems(datumContainer, json.data.datum, false, function(datum, el) {
							// render datum properties
							for ( let classifier of ['i', 'a', 's', 't']) {
								const propContainer = el.find('.'+classifier+'-props');
								if (datum[classifier]) {
									const propItem = (classifier === 't'
										? {serviceProperties:{'tags':datum[classifier]}}
										: {serviceProperties:datum[classifier]});
									SolarReg.Templates.replaceTemplateProperties(propContainer, propItem);
									propContainer.removeClass('hidden');
								} else {
									propContainer.addClass('hidden');
								}
							}
						});
					} else {
						// TODO: i18n
						let msg = 'No datum generated.';
						if (json.data.transformOutput) {
							msg += ' The transform produced the following output data: ' +json.data.transformOutput;
						}
						errorContainer.text(msg).removeClass('hidden');
					}
				} else {
					let msg = SolarReg.formatResponseMessage(json);
					if ( !msg ) {
						msg = 'Unknown error.';
					}
					errorContainer.text(msg).removeClass('hidden');
				}
			}).fail(function(xhr, statusText, error) {
				container.children().addClass('hidden');
				errorContainer.text(SolarReg.extractResponseMessage(xhr, statusText, error)).removeClass('hidden');
			}).always(function() {
				submitBtn.prop('disabled', false);
			});
			return false;
		})
		.on('hidden.bs.modal', modalEditFormHiddenCleanup);

	$('#din-endpoint-transform-input-type-shortcuts')
		.on('change', function endpointTransformPreviewDataTypeShortcutChange(event) {
			const shortcutValue = $(event.target).val();
			if (shortcutValue) {
				$('#din-endpoint-transform-input-type').val(shortcutValue);
			}
		});

	/* ============================
	   Init
	   ============================ */
	(function init() {
		var loadCountdown = 4;
		var credentialConfs = [];
		var transformConfs = [];
		var endpointConfs = [];

		function liftoff() {
			loadCountdown -= 1;
			if (loadCountdown === 0) {
				renderCredentialConfigs(credentialConfs);
				renderTransformConfigs(transformConfs);
				renderEndpointConfigs(endpointConfs);
				SolarReg.showPageLoaded();
			}
		}

		// list all transform services
		$.getJSON(SolarReg.solarUserURL('/sec/din/services/transform'), function(json) {
			console.debug('Got DIN Transform Services: %o', json);
			if (json && json.success === true) {
				if (Array.isArray(json.data)) {
					transformServices.push(...json.data);
				}
			}
			liftoff();
		});

		// list all credentials
		$.getJSON(SolarReg.solarUserURL('/sec/din/credentials'), function(json) {
			console.debug('Got DIN credentials: %o', json);
			if (json && json.success === true) {
				credentialConfs = json.data ? json.data.results : undefined;
			}
			liftoff();
		});

		// list all transforms
		$.getJSON(SolarReg.solarUserURL('/sec/din/transforms'), function(json) {
			console.debug('Got DIN transforms: %o', json);
			if (json && json.success === true) {
				transformConfs = json.data ? json.data.results : undefined;
			}
			liftoff();
		});

		// list all endpoints
		$.getJSON(SolarReg.solarUserURL('/sec/din/endpoints'), function(json) {
			console.debug('Got DIN endpoints: %o', json);
			if (json && json.success === true) {
				endpointConfs = json.data ? json.data.results : undefined;
			}
			liftoff();
		});

	})();	
}
$(document).ready(() => {
	$('#din-management').first().each(dinManagement);
});