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
		var modal = $(this)
			, config = SolarReg.Templates.findContextItem(this)
			, enabled = (config && config.enabled === true ? true : false)
			, type = (this.dataset ? this.dataset.systemType : undefined);
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
		SolarReg.Settings.prepareEditServiceForm(modal
			, type == TRANSFORM_SYS ? transformServices : []
			, settingTemplates);
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
			sys = serverSystems.get(config.endpointId);
			sys = sys ? sys[systemType] : undefined;
		}
		if (!sys) {
			return;
		}
		const container = sys.container.find('.list-container');
		SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
			if (deleted) {
				SolarReg.deleteServiceConfiguration(id, sys.configs, sys.container);
				sys.configsMap.delete(id);
			}
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

	/* ============================
	   Globals
	   ============================ */
	   
	const CREDENTIAL_SYS = 'c';
	const TRANSFORM_SYS = 't';
	const ENDPOINT_SYS = 'e';

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

	const endpointSystems = new Map(); // map of server ID to Object of Dnp3System properties
	
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
				// TODO renderEndpointConfigs(endpointConfs);
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