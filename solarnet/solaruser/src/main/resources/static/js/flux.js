function fluxManagement() {
	'use strict';
	/**
	 * A flux UI entity model.
	 *
	 * @typedef {Object} FluxEntityModel
	 * @property {object} _contextItem the configuration entity
	 * @property {string} entityType the system type (e.g. `*_SYS` constant)
	 * @property {string} [id] the entity ID
	 * @property {string} [createdDisplay] the entity creation date as a display string
	 * @property {boolean} [enabled] the enabled state
	 */
	
	/**
	 * A flux system configuration.
	 *
	 * @typedef {Object} FluxSystem
	 * @property {string} type one of `*_SYS` constants
	 * @property {jQuery} container the element that holds the rendered list of entities
	 * @property {Array<Object>} configs the entities
	 * @property {Map<Number, FluxEntityModel>} configsMap a mapping of entity IDs to associated entities
	 */
	
	/* ============================
	   Globals
	   ============================ */
	   
	const AGG_PUB_DEF_SETTINGS_SYS = 'dpd';
	const AGG_PUB_SETTINGS_SYS = 'dp';

	/**
	 * Mapping of system keys to associated systems.
	 * 
	 * @type {Object<String,FluxSystem>}
	 */
	const systems = Object.freeze({
		dpd: createSystem($('#flux-agg-pub-default-settings-container'), AGG_PUB_DEF_SETTINGS_SYS),
		dp: createSystem($('#flux-agg-pub-settings-container'), AGG_PUB_SETTINGS_SYS),
	});

	/**
	 * Create a flux system.
	 *
	 * @param {jQuery} listContainer the list container
	 * @param {string} type the entity type
	 * @returns {FluxSystem} the new system object
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
			, modal = $(form);
		SolarReg.Settings.prepareEditServiceForm(modal, []);
	}

	/**
	 * Default modal edit form submit handler.
	 *
	 * @param {Event} event the form submit event
	 * @param {Function} renderFn the render function to invoke
	 * @returns {Boolean} `false` to return from event callback
	 */
	function modalEditFormSubmit(event, renderFn) {
		SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(_req, res) {
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
		const config = SolarReg.Templates.findContextItem(this),
			systemType = (config ? config.systemType : this.dataset.systemType);

		/** @type {FluxSystem} */
		var sys;
		
		if ( systems[systemType] ) {
			sys = systems[systemType];
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

	/* ============================
	   Flux entity delete
	   ============================ */

	$('.flux-agg-pub.edit-config button.delete-config').on('click', function(event) {
		var options = {};
		SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
	});
	
	/* ============================
	   Agg Publish Settings (Shared)
	   ============================ */

	function createAggPubSettingsModel(config) {
		if (config.id) {
			config.systemType = AGG_PUB_SETTINGS_SYS;
		} else {
			config.id = -1; // assign ID for default settings
			config.systemType = AGG_PUB_DEF_SETTINGS_SYS;
		}
		config.nodeIdsDisplay = SolarReg.arrayAsDelimitedString(config.nodeIds,', ', '*');
		if (Array.isArray(config.nodeIds)) {
			config.nodeIdsValue = config.nodeIds.join(',');
		}
		config.sourceIdsDisplay = SolarReg.arrayAsDelimitedString(config.sourceIds,', ', '*');
		if (Array.isArray(config.sourceIds)) {
			config.sourceIdsValue = config.sourceIds.join(',');
		}
		var model = SolarReg.Settings.serviceConfigurationItem(config, []);
		SolarReg.fill(model, config);
		model.createdDisplay = moment(config.created).format('D MMM YYYY');
		return model;
	}

	function renderAggPubSettingsConfigurations(/** @type {FluxSystem} */ sys, configs, preserve) {
		if (!sys) {
			return;
		}
		configs = Array.isArray(configs) ? configs : [];
		if (!preserve) {
			sys.configsMap.clear();
		}

		var items = configs.map(function(config) {
			var model = createAggPubSettingsModel(config);
			sys.configsMap.set(config.id, model);
			return model;
		});

		SolarReg.Templates.populateTemplateItems(sys.container, items, preserve);
		SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
	}
	
	function showAggPubSettingsModal() {
		const form = this,
			modal = $(form);
		let config = SolarReg.Templates.findContextItem(form);
		// handle both default and charger-specific settings
		if ( !config ) {
			config = (systems[AGG_PUB_DEF_SETTINGS_SYS].configs.length > 0 
				? systems[AGG_PUB_DEF_SETTINGS_SYS].configs[0] 
				: undefined);
			if (config) {			
				// remove fake ID property so don't try to append to URL
				config = Object.assign({}, config);
				delete config.id;
			}
			SolarReg.Templates.setContextItem(modal, config);
			modal.attr('action', modal.data('action-default'));
			form.dataset.ajaxMethod = 'put';
		} else {
			modal.attr('action', modal.data('action'));
			form.dataset.ajaxMethod = 'post';
		}
		
		modal.find('.rule')
			.toggleClass('hidden', config.systemType !== AGG_PUB_SETTINGS_SYS);
			
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publish]'), 
			config && config.publish === true);
		SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=retain]'), 
			config && config.retain === true);
		
		modalEditFormShowSetup.apply(form);
	}

	$('#flux-agg-pub-settings-edit-modal')
		.on('show.bs.modal', showAggPubSettingsModal)
		.on('submit', function aggPubSettingsEditModalFormSubmit(event) {
			const config = SolarReg.Templates.findContextItem(this);
			return modalEditFormSubmit(event, function renderAggPubSettingsSubmitResult(configs, preserve) {
				if (config.systemType === AGG_PUB_SETTINGS_SYS) {
					renderAggPubSettingsConfigs(configs, preserve);
				} else {
					renderAggPubDefaultSettingsConfigs(configs, preserve);
				}
			});
		})
		.on('hidden.bs.modal', modalEditFormHiddenCleanup)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

	/* ============================
	   Agg Publish Default Settings
	   ============================ */

	function renderAggPubDefaultSettingsConfigs(configs, preserve) {
		renderAggPubSettingsConfigurations(systems[AGG_PUB_DEF_SETTINGS_SYS], configs, preserve);
	}

	/* ============================
	   Agg Publish Settings
	   ============================ */

	function renderAggPubSettingsConfigs(configs, preserve) {
		renderAggPubSettingsConfigurations(systems[AGG_PUB_SETTINGS_SYS], configs, preserve);
	}

	systems[AGG_PUB_SETTINGS_SYS].container.find('.list-container').on('click', function(event) {
		SolarReg.Settings.handleEditServiceItemAction(event, [], []);
	});

	// ***** Agg Publish Settings add
	$('#flux-agg-pub-settings-add-button').on('click', function() {
		const modal = $('#flux-agg-pub-settings-edit-modal');
		SolarReg.Templates.setContextItem(modal, {systemType:AGG_PUB_SETTINGS_SYS});
		modal.modal('show');
	});

	/* ============================
	   Init
	   ============================ */
	(function init() {
		var loadCountdown = 2;
		var aggPubDefaultSettingsConfs = [];
		var aggPubSettingsConfs = [];

		function liftoff() {
			loadCountdown -= 1;
			if (loadCountdown === 0) {
				renderAggPubDefaultSettingsConfigs(aggPubDefaultSettingsConfs);
				renderAggPubSettingsConfigs(aggPubSettingsConfs);
				SolarReg.showPageLoaded();
			}
		}

		// request agg publish default settings
		$.getJSON(SolarReg.solarUserURL('/sec/flux/agg/pub/default-settings'), function(json) {
			console.debug('Got flux agg publish default settings: %o', json);
			if (json && json.success === true) {
				aggPubDefaultSettingsConfs = json.data ? [json.data] : undefined;
			}
			liftoff();
		});

		// request agg publish settings
		$.getJSON(SolarReg.solarUserURL('/sec/flux/agg/pub/settings'), function(json) {
			console.debug('Got flux agg publish settings: %o', json);
			if (json && json.success === true) {
				aggPubSettingsConfs = json.data ? json.data.results : undefined;
			}
			liftoff();
		});

	})();	
}
$(document).ready(() => {
	$('#flux-management').first().each(fluxManagement);
});