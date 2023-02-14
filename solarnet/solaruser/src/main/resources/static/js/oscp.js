$(document).ready(function() {
	'use strict';

	$('#oscp-management').first().each(function oscpManagement() {
		/**
		 * A system configuration UI model.
		 *
		 * @typedef {Object} OscpSystemModel
		 * @property {object} _contextItem the configuration entity
		 * @property {string} systemType the system type (e.g. 'co', 'cp')
		 * @property {string} [id] the entity ID
		 * @property {string} [createdDisplay] the entity creation date as a display string
		 * @property {string} [baseUrl] the system base URL
		 * @property {boolean} [enabled] the enabled state
		 * @property {string} [registrationStatus] the system registration status
		 * @property {Array<String>} [measurementStyles] the system measurement styles
		 * @property {string} [measurementStylesDisplay] a concatenated list of system measurement styles
		 * @property {string} [oauthTokenUrl] the OAuth token URL
		 * @property {string} [oauthClientId] the OAuth client ID
		 * @property {Object} [httpHeaders] optional HTTP headers
		 * @property {Object} [urlPaths] optional URL Paths
		 */

		/**
		 * A system configuration.
		 *
		 * @typedef {Object} OscpSystem
		 * @property {string} type one of 'cp' or 'co' (Capacity Provider, Capacity Optimizer)
		 * @property {jQuery} container the element that holds the rendered list of systems
		 * @property {Array<Object>} configs the system configuration entities
		 * @property {Map<Number, OscpSystemModel>} configsMap a mapping of configuration entity IDs to associated entities
		 */

		/**
		 * Create a system configuration.
		 *
		 * @param {jQuery} listContainer the list container
		 * @param {string} type the system type
		 * @returns {OscpSystem} the new system object
		 */
		function createSystem(listContainer, type) {
			return Object.freeze({
				type: type,
				container: listContainer,
				configs: [],
				configsMap: new Map()
			});
		}

		/* ============================
		   Globals
		   ============================ */
		const i18n = SolarReg.i18nData(this);

		const systems = Object.freeze({
			co: createSystem($('#oscp-cos-container'), 'co'),
			cp: createSystem($('#oscp-cps-container'), 'cp'),
			cg: createSystem($('#oscp-cgs-container'), 'cg'),
		});

		/**
		 * Generate a DOM data attribute property name for an OSCP system type.
		 *
		 * @param {string} prefix the desired prefix
		 * @param {string} type the system type
		 * @returns {string} the data attribute name
		 */
		function dataAttributeName(prefix, type) {
			if ( !type ) {
				return prefix;
			}
			return prefix + type.charAt(0).toUpperCase() + type.substring(1);
		}

		/**
		 * Render system-specific names into a DOM tree.
		 *
		 * @param {jQuery} el the selection to render the OSCP type in
		 * @param {string} type the system type
		 */
		function renderOscpSystemType(el, type) {
			el.attr('data-system-type', type)
				.find('.system-type').text(i18n[dataAttributeName('systemType',type)]);
		}

		/**
		 * Render the properties of an object into a <dl> list container.
		 *
		 * @param {jQuery} container the DOM container to render the list in
		 * @param {Object} obj the object whose properties should be rendered into the list
		 */
		function renderObjectPropertiesDl(container, obj) {
			if ( obj ) {
				let listContainer = container.find('dl').empty();
				for ( let prop in obj ) {
					$('<dt>').text(prop).appendTo(listContainer);
					$('<dd>').text(obj[prop]).appendTo(listContainer);
				}
				container.removeClass('hidden');
			} else {
				container.addClass('hidden');
			}
		}

		/**
		 * Generate a display name for a system entity.
		 *
		 * @param {string} type the system type
		 * @param {number} id the system ID
		 * @returns {string} the display name
		 */
		function systemDisplayName(type, id) {
			let sys = systems[type];
			if ( sys ) {
				let config = sys.configsMap.get(id);
				if ( config && config.name ) {
					return id + ' - ' + config.name;
				}
			}
			return ''+id;
		}

		/**
		 * Create a system model out of a configuration entity.
		 *
		 * @param {object} config the OSCP system entity
		 * @param {string} type the system type
		 * @returns {OscpSystemModel} the system list model
		 */
		function createSystemConfigurationModel(config, type) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			config.id = config.configId; // assumed by setttings.js methods
			config.systemType = type;
			model.systemType = type;
			model.id = config.configId;
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			model.baseUrl = config.baseUrl;
			model.enabled = config.enabled;
			model.registrationStatus = config.registrationStatus;
			if ( config.settings && Array.isArray(config.settings.measurementStyles) ) {
				model.measurementStyles = config.settings.measurementStyles;
				model.measurementStylesDisplay = SolarReg.arrayAsDelimitedString(config.settings.measurementStyles);
			}
			if ( config.serviceProps ) {
				if ( config.serviceProps['oauth-token-url'] ) {
					model.oauthTokenUrl = config.serviceProps['oauth-token-url'];
				}
				if ( config.serviceProps['oauth-client-id'] ) {
					model.oauthClientId = config.serviceProps['oauth-client-id'];
				}
				if ( config.serviceProps['http-headers'] ) {
					model.httpHeaders = config.serviceProps['http-headers'];
				}
				if ( config.serviceProps['url-paths'] ) {
					model.urlPaths = config.serviceProps['url-paths'];
				}
			}
			if ( type === 'cg' ) {
				model.identifier = config.identifier;
				model.capacityProviderMeasurementPeriod = config.capacityProviderMeasurementPeriod;
				model.capacityOptimizerMeasurementPeriod = config.capacityOptimizerMeasurementPeriod;
				model.capacityProviderId = config.capacityProviderId;
				model.capacityProviderDisplay = systemDisplayName('cp', config.capacityProviderId);
				model.capacityOptimizerId = config.capacityOptimizerId;
				model.capacityOptimizerDisplay = systemDisplayName('co', config.capacityOptimizerId);
				let settings = groupSettingConfigsMap.get(config.id);
				if ( settings ) {
					model.settings = settings._contextItem;
					model.sourceIdTemplate = settings.sourceIdTemplate;
					model.publishToSolarIn = settings.publishToSolarIn;
					model.publishToSolarFlux = settings.publishToSolarFlux;
				}
			}
			return model;
		}

		/**
		 * Render a list of system configuration entities.
		 *
		 * @param {Array<Object>} configs list of systems to render
		 * @param {string} type the system type
		 * @param {boolean} preserve `true` to update any existing list; `false` to clear and populate
		 */
		function renderSystemConfigs(configs, type, preserve) {
			/** @type {OscpSystem} */
			const sys = systems[type];
			if ( !sys ) {
				return;
			}
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				sys.configsMap.clear();
			}

			/** @type {Array<OscpSystemModel>} */
			var items = configs.map(function(config) {
				var model = createSystemConfigurationModel(config, type);
				sys.configsMap.set(config.id, model);
				return model;
			});

			SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, function populateSystemItem(item, el) {
				renderObjectPropertiesDl(el.find('.headers-container'), item.httpHeaders);
				renderObjectPropertiesDl(el.find('.url-paths-container'), item.urlPaths);

				if ( type === 'cg' ) {
					let settingsEditContainer = el.find('.settings-container').toggleClass('hidden', item.settings === undefined).parent();
					// even if item does not have settings, provide a context item with the charger ID so editing works
					let settingConfig = (item.settings ? item.settings : {id:item.id,groupId:item.id});
					SolarReg.Templates.setContextItem(settingsEditContainer, settingConfig);
				}
			});
			SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
		}

		/**
		 * Render a set of `<option>` elements for selecting from a list of system configurations.
		 *
		 * @param {HTMLSelectElement} select the select element to update
		 * @param {Array<OscpSystem>} configs the list of system configurations to render
		 * @param {number} [selectedConfigId] the currently selected configuration ID, if available
		 */
		function renderOscpSystemOptions(select, configs, selectedConfigId) {
			let el = $(select).empty();
			if ( !(configs && configs.length) ) {
				return;
			}
			el.append('<option>');
			for ( let i = 0, len = configs.length; i < len; i += 1 ) {
				let id = configs[i].id;
				let opt = $('<option>').attr('value', id).text(id + ' - ' + configs[i].name);
				if ( selectedConfigId !== undefined && id === selectedConfigId ) {
					opt.attr('selected', 'selected');
				}
				opt.appendTo(el);
			}
		}

		/**
		 * Create a URL serializer function for group settings.
		 *
		 * @param {string} groupId the group ID
		 * @returns {Function} the URL serializer function; accepts a single 'action' string parameter
		 */
		function groupSettingsFormUrlSerializer(groupId) {
			return (action) => {
				return action.replace(/\/settings$/, '/' + groupId + '/settings');
			};
		}

		/* ============================
		   Settings
		   ============================ */
		const settingsContainer = $('#oscp-settings-container');
		const settingConfigs = []; // only ever one of these, use array for consistency with Settings/Templates functions
		const groupSettingConfigsMap = new Map();

		function settingModel(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			if ( config.groupId === undefined ) {
				config.id = 1; // assign arbitrary ID for default settings
				if ( config.publishToSolarIn === undefined ) {
					config.publishToSolarIn = true;
				}
				if ( config.publishToSolarFlux === undefined ) {
					config.publishToSolarFlux = true;
				}
				if ( config.sourceIdTemplate === undefined ) {
					config.sourceIdTemplate = '/oscp/{role}/{action}/{cp}/{co}/{cgIdentifier}';
				}
			} else {
				config.id = config.groupId;
				model.groupId = config.groupId;
			}
			model.id = config.id;
			model.nodeId = config.nodeId;
			model.publishToSolarIn = config.publishToSolarIn;
			model.publishToSolarFlux = config.publishToSolarFlux;
			model.sourceIdTemplate = config.sourceIdTemplate;
			return model;
		}

		function renderSettingConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			var items = configs.map(settingModel);
			SolarReg.Templates.populateTemplateItems(settingsContainer, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, settingConfigs, settingsContainer);
		}

		function renderGroupSettingConfigs(configs, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				groupSettingConfigsMap.clear();
			}
			var groupItems = [];
			configs.forEach(config => {
				var model = settingModel(config);
				groupSettingConfigsMap.set(model.groupId, model);
				if ( preserve ) {
					let groupItem = systems.cg.configsMap.get(model.groupId);
					if ( groupItem ) {
						groupItem.settings = model._contextItem;
						groupItem.sourceIdTemplate = model.sourceIdTemplate;
						groupItem.publishToSolarIn = !!model.publishToSolarIn;
						groupItem.publishToSolarFlux = !!model.publishToSolarFlux;
						groupItems.push(groupItem);
					}
				}
				return model;
			});
			if ( groupItems.length > 0 ) {
				SolarReg.Templates.populateTemplateItems(systems.cg.container, groupItems, true, (item, el) => {
					let settingsEditContainer = el.find('.settings-container').removeClass('hidden').parent();
					SolarReg.Templates.setContextItem(settingsEditContainer, item.settings);
				});
			}
		}

		$('#oscp-settings-edit-modal').on('show.bs.modal', function handleModalShow(event) {
			// handle both default and capacity-group-specific settings
			var modal = $(event.target),
				config = SolarReg.Templates.findContextItem(modal);
			if ( !config ) {
				config = (settingConfigs.length > 0 ? settingConfigs[0] : undefined);
				SolarReg.Templates.setContextItem(modal, config);
				modal.attr('action', modal.data('action'));
			} else {
				modal.attr('action', modal.data('action-group'));
			}
			modal.find('.group').toggleClass('hidden', config.groupId === undefined);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarIn]'), !!config.publishToSolarIn);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=publishToSolarFlux]'), !!config.publishToSolarFlux);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function handleModalFormSubmit(event) {
			let form = event.target,
				options = {};
			if ( form.elements['groupId'] && form.elements['groupId'].value ) {
				options.urlSerializer = groupSettingsFormUrlSerializer(form.elements['groupId'].value);
			}
			SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
				if ( res.groupId === undefined ) {
					renderSettingConfigs([res], true);
				} else {
					renderGroupSettingConfigs([res], true);
				}
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true),
					config = SolarReg.Templates.findContextItem(form);

				if ( !config || config.groupId === undefined ) {
					delete data.groupId;
				}

				delete data.id;

				if ( !data.userId ) {
					// use actor user ID, i.e. for new settings
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, options);
			return false;
		})
		.on('hidden.bs.modal', function handleModalHidden() {
			SolarReg.Settings.resetEditServiceForm(this, $('#oscp-settings-container .list-container'), (id, deleted) => {
				if ( deleted ) {
					groupSettingConfigsMap.delete(id);
					let groupItem = systems.cg.configsMap.get(id);
					if ( groupItem ) {
						delete groupItem.settings;
						delete groupItem.nodeId;
						delete groupItem.sourceIdTemplate;
						delete groupItem.publishToSolarIn;
						delete groupItem.publishToSolarFlux;
						SolarReg.Templates.populateTemplateItems(systems.cg.container, [groupItem], true, (item, el) => {
							let settingsEditContainer = el.find('.settings-container').addClass('hidden').parent();
							SolarReg.Templates.setContextItem(settingsEditContainer, {id:id,groupId:id});
						});
					}
				}
			});
		})
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

		/* ============================
		   System Token
		   ============================ */

	   function showSystemToken(config) {
			if ( config && config.token ) {
				$('#system-token-name').text(config.name);
				$('#system-token').val(config.token);
				let modal = $('#oscp-system-token-modal');
				renderOscpSystemType(modal, config.systemType);
				modal.modal('show');
				delete config.token;
			}
		}

		$('#oscp-system-token-modal').on('hidden.bs.modal', function handleModalHidden() {
			// clear the token value
			$('#system-token').val('');
		});

		/* ============================
		   Capacity Providers
		   ============================ */

		$('#oscp-add-cp-button').on('click', function createCp() {
			const modal = $('#oscp-system-edit-modal');
			SolarReg.Templates.setContextItem(modal, {systemType:'cp'});
			modal.modal('show');
		});

		systems.cp.container.find('.list-container').on('click', function(event) {
			// edit cp settings
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		/* ============================
		   Capacity Optimizers
		   ============================ */

		$('#oscp-add-co-button').on('click', function createCp() {
			const modal = $('#oscp-system-edit-modal');
			SolarReg.Templates.setContextItem(modal, {systemType:'co'});
			modal.modal('show');
		});

		systems.co.container.find('.list-container').on('click', function(event) {
			// edit co settings
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		/* ============================
		   Capacity Group Assets
		   ============================ */
		 const cgGroupAssets = new Map();

		/**
		 * Create an asset model out of a configuration entity.
		 *
		 * @param {object} config the OSCP system entity
		 * @param {number} groupId the ID of the group the assets are associated with
		 * @returns {object} the asset list model
		 */
		function createCapacityGroupAssetModel(config) {
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			config.id = 'asset:' + config.configId; // qualify with asset: to distinguish from CG ID
			config.systemType = 'asset';
			model.id = config.configId;
			model.systemType = config.systemType;
			model.capacityGroupId = config.capacityGroupId;
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			model.identifier = config.identifier;
			model.audience = config.audience;
			model.audienceDisplay = i18n['role'+config.audience];
			model.enabled = config.enabled;
			model.nodeId = config.nodeId;
			model.sourceId = config.sourceId;
			model.category = config.category;
			model.phase = config.phase;
			model.instantaneous = Object.assign({}, config.instantaneous);
			if ( Array.isArray(model.instantaneous.propertyNames) ) {
				model.instantaneous.propertyNamesDisplay = SolarReg.arrayAsDelimitedString(model.instantaneous.propertyNames);
			}
			model.energy = Object.assign({}, config.energy);
			if ( Array.isArray(model.energy.propertyNames) ) {
				model.energy.propertyNamesDisplay = SolarReg.arrayAsDelimitedString(model.energy.propertyNames);
			}
			return model;
		}

		function renderCapacityGroupAssets(assetsContainer, configs, groupId, preserve) {
			var sys = cgGroupAssets.get(groupId);
			if ( !sys ) {
				sys = createSystem(assetsContainer, 'asset');
				cgGroupAssets.set(groupId, sys);
			}
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
				sys.configsMap.clear();
			}

			/** @type {Array<OscpSystemModel>} */
			var items = configs.map(function(config) {
				var model = createCapacityGroupAssetModel(config);
				sys.configsMap.set(config.id, model);
				return model;
			});

			SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, function populateSystemItem(item, el) {
				// TODO
			}, 'asset.');
			SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
		}

		$('#oscp-asset-edit-modal').on('show.bs.modal', function handleModalShow() {
			const modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false)
				, idField = $(this.elements['id']);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
			if ( idField.val() ) {
				idField.val(config.configId); // set to actual ID, as config.id has asset: prefix
			}
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function handleAssetModalFormSubmit(event) {
			const config = SolarReg.Templates.findContextItem(this),
				sys = cgGroupAssets.get(config.capacityGroupId);
			SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
				renderCapacityGroupAssets(sys.container, [res], config.capacityGroupId, true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);
				return data;
			}, {
				urlId: true
			});
			return false;
		})
		.on('hidden.bs.modal', function handleModalHidden() {
			const config = SolarReg.Templates.findContextItem(this)
				, sys = cgGroupAssets.get(config.capacityGroupId)
				, idField = $(this.elements['id']);
			if ( !sys ) {
				return;
			}
			const container = sys.container.find('.list-container');
			if ( idField.val() ) {
				idField.val('asset:' +config.configId); // add asset: prefix back to UI can update
			}
			SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, sys.configs, sys.container);
				if ( deleted ) {
					sys.configsMap.delete(id);
				}
			});
		})
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

		/* ============================
		   Capacity Groups
		   ============================ */

		$('#oscp-add-cg-button').on('click', function createCg() {
			const modal = $('#oscp-cg-edit-modal');
			SolarReg.Templates.setContextItem(modal, {systemType:'cg'});
			modal.modal('show');
		});

		systems.cg.container.find('.list-container').on('click', function(event) {
			let target = $(event.target);
			if ( target.hasClass('oscp-cg-toggle-assets') ) {
				event.preventDefault();
				let assets = target.closest('.cg-item').next('.cg-item-assets'),
					hide = !assets.hasClass('hidden'),
					config = SolarReg.Templates.findContextItem(target),
					groupId = config ? config.id : undefined;

				if ( groupId ) {
					assets.toggleClass('hidden', hide);
					target.text(i18n[hide ? 'cgAssetsShow' : 'cgAssetsHide']);
					if ( !cgGroupAssets.get(groupId) ) {
						$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-groups/' +groupId + '/assets'), function(json) {
							console.debug('Got OSCP group assets: %o', json);
							if ( json && json.success === true ) {
								renderCapacityGroupAssets(assets, json.data, groupId);
							}
							assets.find('.assets-loading').addClass('hidden');
							assets.find('.asset-configs').removeClass('hidden');
						});
					}
				}
				return;
			} else if ( target.closest('.oscp-add-cg-asset-button').length ) {
				let config = SolarReg.Templates.findContextItem(target)
					, groupId = config ? config.id : undefined
					, editModal = $('#oscp-asset-edit-modal');

				SolarReg.Templates.setContextItem(editModal, {
					systemType: 'asset',
					capacityGroupId: groupId
				});
				editModal.modal('show');
				return;
			}

			// edit cg settings
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		systems.cg.container.find('.edit-link-rel').on('click', function showGroupRelatedEntityModal(event) {
			event.preventDefault();
			const type = event.target.dataset['systemType'],
				configId = Number(event.target.dataset['systemId']),
				sys = systems[type];
			if ( configId && sys ) {
				let model = sys.configsMap.get(configId);
				if ( model && model._contextItem ) {
					let editModal = $('#oscp-system-edit-modal');
					SolarReg.Templates.setContextItem(editModal, model._contextItem);
					editModal.modal('show');
				}
			}
		});

		$('#oscp-cg-edit-modal').on('show.bs.modal', function handleModalShow() {
			var modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);

			renderOscpSystemType(modal, config.systemType);

			renderOscpSystemOptions(this.elements.capacityProviderId, systems.cp.configs, config ? config.capacityProviderId : undefined);
			renderOscpSystemOptions(this.elements.capacityOptimizerId, systems.co.configs, config ? config.capacityOptimizerId : undefined);
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function handleCpModalFormSubmit(event) {
			const config = SolarReg.Templates.findContextItem(this);
			if ( !config.systemType ) {
				return;
			}

			SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
				res.id = res.configId;
				res.systemType = config.systemType;
				renderSystemConfigs([res], config.systemType, true);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( !data.userId ) {
					// use actor user ID, i.e. for new entity
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				urlId: true
			});
			return false;
		})
		.on('hidden.bs.modal', function handleModalHidden() {
			const config = SolarReg.Templates.findContextItem(this),
				type = config.systemType,

				/** @type {OscpSystem} */
				sys = systems[type];
			if ( !sys ) {
				return;
			}
			const container = sys.container.find('.list-container');
			SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, sys.configs, sys.container);
				if ( deleted ) {
					sys.configsMap.delete(id);
				}
			});
		})
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

		/* ============================
		   System Edit
		   ============================ */

		$('#oscp-system-edit-modal').on('show.bs.modal', function handleModalShow() {
			var modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);

			renderOscpSystemType(modal, config.systemType);

			// set system action URL
			const action = this.dataset[dataAttributeName('action', config.systemType)];
			modal.attr('action', action);

			if ( !(config && config.serviceProps) ) {
				return;
			}

			// populate dynamic HTTP Headers list
			SolarReg.Settings.populateDynamicListObjectKeyValues(config.serviceProps['http-headers'], modal,
				'http-headers', 'httpHeaderName', 'httpHeaderValue');

			// populate dynamic URL Paths list
			SolarReg.Settings.populateDynamicListObjectKeyValues(config.serviceProps['url-paths'], modal,
				'url-paths', 'urlPathAction', 'urlPathPath');

			return;
		})
		.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
		.on('submit', function handleCpModalFormSubmit(event) {
			const modal = $(this),
				config = SolarReg.Templates.findContextItem(this);
			if ( !config.systemType ) {
				return;
			}

			SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
				res.id = res.configId;
				res.systemType = config.systemType;
				renderSystemConfigs([res], config.systemType, true);

				// update display name references in any groups
				systems.cg.container.find('[data-system-type=' + res.systemType + '][data-system-id=' + res.id + ']')
					.text(systemDisplayName(res.systemType, res.id));

				// save result as modal context, to possibly show token modal
				SolarReg.Templates.setContextItem(modal, res);
			}, function serializeDataConfigForm(form) {
				var data = SolarReg.Settings.encodeServiceItemForm(form, true);

				if ( !data.userId ) {
					// use actor user ID, i.e. for new cps
					data.userId = form.elements['userId'].dataset.userId;
				}

				return data;
			}, {
				urlId: true
			});
			return false;
		})
		.on('hidden.bs.modal', function handleModalHidden() {
			const config = SolarReg.Templates.findContextItem(this),
				type = config.systemType,

				/** @type {OscpSystem} */
				sys = systems[type];
			if ( !sys ) {
				return;
			}
			const container = sys.container.find('.list-container');
			SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, sys.configs, sys.container);
				if ( deleted ) {
					sys.configsMap.delete(id);
				}
			});
			if ( config && config.token ) {
				// token provided; show value after short delay to allow animation to finish
				setTimeout(function() {
					showSystemToken(config);
				}, 200);
			}
		})
		.on('click', SolarReg.Settings.handleDynamicListAddOrDelete)
		.find('button.toggle').each(function() {
			SolarReg.Settings.setupSettingToggleButton($(this), false);
		});

		/* ============================
		   OSCP entity delete
		   ============================ */

		$('.oscp.edit-config button.delete-config').on('click', function(event) {
			var options = {};
			var form = $(event.target).closest('form').get(0);
			if ( form && form.elements['groupId'] && form.elements['sourceIdTemplate'] ) {
				// group settings use /capacity-groups/X/settings path
				options.urlSerializer = groupSettingsFormUrlSerializer(form.elements['groupId'].value);
			}
			SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
		});

		/* ============================
		   Init
		   ============================ */
		(function initOcppManagement() {
			var loadCountdown = 6;
			var settingConfs = [];
			var groupSettingConfs = [];
			var cpConfs = [];
			var coConfs = [];
			var cgConfs = [];

			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					renderSettingConfigs(settingConfs);
					renderGroupSettingConfigs(groupSettingConfs);
					renderSystemConfigs(cpConfs, 'cp');
					renderSystemConfigs(coConfs, 'co');
					renderSystemConfigs(cgConfs, 'cg');
					SolarReg.showPageLoaded();
				}
			}

			// get settings
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/settings'), function(json) {
				console.debug('Got OSCP settings: %o', json);
				if ( json && json.success === true && (typeof json.data === 'object') ) {
					settingConfs = [json.data];
				} else {
					settingConfs = [{}];
				}
				liftoff();
			});

			// list all group settings
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-groups/settings'), function(json) {
				console.debug('Got OSCP group settings: %o', json);
				if ( json && json.success === true ) {
					groupSettingConfs = json.data;
				}
				liftoff();
			});

			// list all capacity providers
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-providers'), function(json) {
				console.debug('Got OSCP capacity providers: %o', json);
				if ( json && json.success === true ) {
					cpConfs = json.data;
				}
				liftoff();
			});

			// list all capacity optimizers
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-optimizers'), function(json) {
				console.debug('Got OSCP capacity optimizers: %o', json);
				if ( json && json.success === true ) {
					coConfs = json.data;
				}
				liftoff();
			});

			// list all capacity groups
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-groups'), function(json) {
				console.debug('Got OSCP capacity groups: %o', json);
				if ( json && json.success === true ) {
					cgConfs = json.data;
				}
				liftoff();
			});

			// list all capacity group settings
			$.getJSON(SolarReg.solarUserURL('/sec/oscp/capacity-groups/settings'), function(json) {
				console.debug('Got OSCP capacity group settings: %o', json);
				if ( json && json.success === true ) {
					groupSettingConfs = json.data;
				}
				liftoff();
			});
		})();

	});
});
