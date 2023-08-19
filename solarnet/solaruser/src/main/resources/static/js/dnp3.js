$(document).ready(function() {
	'use strict';

	$('#dnp3-management').first().each(function dnp3Management() {
		/**
		 * A DNP3 UI entity model.
		 *
		 * @typedef {Object} Dnp3EntityModel
		 * @property {object} _contextItem the configuration entity
		 * @property {string} entityType the system type (e.g. 'ca', 's', 'au', 'm', 'c')
		 * @property {string} [id] the entity ID
		 * @property {string} [createdDisplay] the entity creation date as a display string
		 * @property {boolean} [enabled] the enabled state
		 */

		/**
		 * A system configuration.
		 *
		 * @typedef {Object} Dnp3System
		 * @property {string} type one of 'ca', 's', 'au', 'm', 'c'
		 * @property {jQuery} container the element that holds the rendered list of entities
		 * @property {Array<Object>} configs the entities
		 * @property {Map<Number, Dnp3EntityModel>} configsMap a mapping of entity IDs to associated entities
		 */

		/**
		 * Create an system.
		 *
		 * @param {jQuery} listContainer the list container
		 * @param {string} type the entity type
		 * @returns {Dnp3System} the new system object
		 */
		function createSystem(listContainer, type) {
			return Object.freeze({
				type: type,
				container: listContainer,
				configs: [],
				configsMap: new Map()
			});
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

		/**
		 * Default edit form setup handler.
		 *
		 * @this {HTMLFormElement} the modal form
		 */
		function modalEditFormShowSetup() {
			var modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
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

			/** @type {Dnp3System} */
			var sys;
			
			if ( systems[systemType] ) {
				sys = systems[systemType];
			} else if ( config && config.serverId ) {
				sys = serverSystems.get(config.serverId);
				sys = sys ? sys[systemType] : undefined;
			}
			if (!sys) {
				return;
			}
			const container = sys.container.find('.list-container');
			SolarReg.Settings.resetEditServiceForm(this, container, (id, deleted) => {
				SolarReg.deleteServiceConfiguration(deleted ? id : null, sys.configs, sys.container);
				if (deleted) {
					sys.configsMap.delete(id);
				}
			});
		}

		/* ============================
		   Globals
		   ============================ */
		const i18n = SolarReg.i18nData(this);

		const systems = Object.freeze({
			ca: createSystem($('#dnp3-cas-container'), 'ca'),
			s: createSystem($('#dnp3-servers-container'), 's'),
		});

		const serverSystems = new Map(); // map of server ID to Object of Dnp3System properties

		/* ============================
		   DNP3 entity delete
		   ============================ */

		$('.dnp3.edit-config button.delete-config').on('click', function(event) {
			var options = {};
			//var form = $(event.target).closest('form').get(0);
			SolarReg.Settings.handleEditServiceItemDeleteAction(event, options);
		});

		/* ============================
		   Trusted Issuers
		   ============================ */

		function renderTrustedIssuerConfigs(configs, preserve) {
			/** @type {Dnp3System} */
			const sys = systems['ca'];
			if (!sys) {
				return;
			}
			configs = Array.isArray(configs) ? configs : [];
			if (!preserve) {
				sys.configsMap.clear();
			}

			var items = configs.map(function(config) {
				var model = createTrustedIssuerModel(config);
				sys.configsMap.set(config.id, model);
				return model;
			});

			SolarReg.Templates.populateTemplateItems(sys.container, items, preserve);
			SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
		}

		function createTrustedIssuerModel(config) {
			config.id = config.subjectDn;
			config.systemType = 'ca';
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			Object.assign(model, config);
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			model.expiresDisplay = moment(config.expires).format('D MMM YYYY');
			return model;
		}

		systems.ca.container.find('.list-container').on('click', function(event) {
			// edit ca
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		// ***** CA import button
		$('#dnp3-ca-add-button').on('click', function() {
			$('#dnp3-ca-import-modal').modal('show');
		})

		// ***** CA import form
		$('#dnp3-ca-import-modal')
			.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
			.on('submit', function(event) {
				var uploadProgressBar = $('.upload .progress-bar', event.target);
				var uploadProgressBarAmount = $('.amount', uploadProgressBar);
				var modal = $(event.target);

				modal.find('.before').addClass('hidden');
				modal.find('.upload').removeClass('hidden');

				SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
					if (Array.isArray(res)) {
						renderTrustedIssuerConfigs(res, true);
					}
				}, function serializeCaImportForm(form) {
					var formData = new FormData(form);
					return formData;
				}, {
					upload: function(event) {
						if (event.lengthComputable) {
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
				SolarReg.Settings.resetEditServiceForm(this);
			});

		$('#dnp3-ca-edit-modal').on('show.bs.modal', modalEditFormShowSetup)
			.on('submit', function handleCaModalFormSubmit(event) {
				return modalEditFormSubmit(event, renderTrustedIssuerConfigs);
			})
			.on('hidden.bs.modal', modalEditFormHiddenCleanup)
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});

		/* ============================
		   Servers
		   ============================ */

		function renderServerConfigs(configs, preserve) {
			/** @type {Dnp3System} */
			const sys = systems['s'];
			if (!sys) {
				return;
			}
			configs = Array.isArray(configs) ? configs : [];
			if (!preserve) {
				sys.configsMap.clear();
			}

			var items = configs.map(function(config) {
				var model = createServerModel(config);
				sys.configsMap.set(config.id, model);
				return model;
			});

			SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, function serverTemplateCallback(item, el) {
				// update all accordian item IDs to be unique, with the associated server ID
				let id = item.id;
				el.find('div.panel-heading').attr('id', 'server-heading-' + id);
				el.find('.panel-title a').attr('href', '#server-body-' + id).attr('aria-controls', 'server-body-' + id);
				el.find('div.panel-collapse ').attr('id', 'server-body-' + id).attr('aria-labelledby', 'server-heading-' + id);
			});
			SolarReg.saveServiceConfigurations(configs, preserve, sys.configs, sys.container);
		}

		function createServerModel(config) {
			config.id = config.serverId;
			config.systemType = 's';
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			model.id = config.id;
			model.systemType = config.systemType;
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			model.modifiedDisplay = moment(config.modified).format('D MMM YYYY');
			model.enabled = config.enabled;
			return model;
		}

		function handleServerDataPointsExport(item) {
			if (!(item && item.id)) {
				return;
			}
			let url = document.location.href + '/servers/' + item.id + '/csv';
			document.location = url;
		}

		systems.s.container.on('click', function handleServersClick(/** @type {MouseEvent} */ event) {
			console.debug('Click on servers %o', event);

			// check if clicked on server title bar, to toggle visibility
			const target = $(event.target);
			if ( target.hasClass('panel-title') || target.hasClass('panel-heading') ) {
				// toggle this panel
				let dest = target.parent().find('a[data-parent="#dnp3-servers-accordian"]').attr('href');
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
				// open the edit form, passing the server ID in the context item
				if ( modal && config ) {
					SolarReg.Templates.setContextItem(modal, {serverId:config.id});
					modal.modal('show');
					event.preventDefault();
				}
				return;
			}

			// check if clicked on download CSV button
			const action = target.closest('.action');
			if ( action.length && action.hasClass('csv-export') ) {
				handleServerDataPointsExport(SolarReg.Templates.findContextItem(action));
			} else {
				SolarReg.Settings.handleEditServiceItemAction(event, [], []);
			}
		});

		// ***** Servers accordian
		$('#dnp3-servers-container').on('show.bs.collapse', function handleServerCollapseShow(event) {
			const target = event.target;
			const config = SolarReg.Templates.findContextItem(target);
			if ( config ) {
				console.log('Show server details: %o', config);
				renderServerDetails(config, $(target));
			}
		});

		function renderServerDetails(item, el) {
			if ( serverSystems.has(item.id) ) {
				// already loaded
				return;
			}
			const sSystems = Object.freeze({
				auth: createSystem(el.find('.server-auths'), 'auth'),
				meas: createSystem(el.find('.server-measurements'), 'meas'),
				ctrl: createSystem(el.find('.server-controls'), 'ctrl'),
			});
			serverSystems.set(item.id, sSystems);

			// load data
			const progressBar = $('.loading .progress-bar', el);
			const progressBarAmount = $('.amount', progressBar);
			const loadTotal = 3;
			var loadCountdown = loadTotal;
			var authConfs = [];
			var measConfs = [];
			var ctrlConfs = [];

			function liftoff() {
				loadCountdown -= 1;
				updateProgressAmount(progressBar, progressBarAmount, (loadTotal-loadCountdown) / loadTotal);
				if (loadCountdown === 0) {
					renderServerDetailConfigs(authConfs, sSystems.auth);
					renderServerDetailConfigs(measConfs, sSystems.meas);
					renderServerDetailConfigs(ctrlConfs, sSystems.ctrl);
					el.find('.loading').addClass('hidden');
					el.find('.section.hidden').removeClass('hidden');
					console.log('Server %o data loaded.', item);
				}
			}

			// list all auths
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/servers/auths?serverId='+item.id), function(json) {
				console.debug('Got DNP3 server %d auths: %o', item.id, json);
				if (json && json.success === true) {
					authConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});

			// list all measurements
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/servers/measurements?serverId=' +item.id), function(json) {
				console.debug('Got DNP3 server %d measurements: %o', item.id, json);
				if (json && json.success === true) {
					measConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});

			// list all controls
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/servers/controls?serverId=' +item.id), function(json) {
				console.debug('Got DNP3 server %d controls: %o', item.id, json);
				if (json && json.success === true) {
					ctrlConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});
		}

		/**
		 * Render server detail configurations.
		 *
		 * @argument {Array<Object>} configs the entities
		 * @argument {Dnp3System} sys the server system
		 * @argument {Boolean} preserve true to preserve existing template instances
		 */
		function renderServerDetailConfigs(configs, sys, preserve) {
			configs = Array.isArray(configs) ? configs : [];
			if (!preserve) {
				sys.configsMap.clear();
			}

			var items = configs.map(function(config) {
				var model = createServerDetailModel(config, sys.type);
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

		function createServerDetailModel(config, type) {
			config.id = config.identifier ? config.identifier : config.index;
			config.systemType = type;
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			Object.assign(model, config);
			model.createdDisplay = moment(config.created).format('D MMM YYYY');
			return model;
		}
		
		// ***** Server add button
		$('#dnp3-server-add-button').on('click', function() {
			$('#dnp3-server-edit-modal').modal('show');
		})

		// ***** Server edit
		$('#dnp3-server-edit-modal').on('show.bs.modal', modalEditFormShowSetup)
			.on('submit', function serverEditModalFormSubmit(event) {
				return modalEditFormSubmit(event, renderServerConfigs);
			})
			.on('hidden.bs.modal', modalEditFormHiddenCleanup)
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});

		// ***** Server data points import CSV
		$('#dnp3-server-data-points-import-modal')
			.on('show.bs.modal', function() {
				const modal = $(this);
				/** @type {HTMLFormElement} */
				const form = this;
				const config = SolarReg.Templates.findContextItem(this);
				form.elements.name.value = config.name;
				form.action = form.dataset.action + '/' + config.id +'/csv';
			})
			.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
			.on('submit', function(event) {
				const uploadProgressBar = $('.upload .progress-bar', event.target);
				const uploadProgressBarAmount = $('.amount', uploadProgressBar);
				const modal = $(event.target);
				const config = SolarReg.Templates.findContextItem(this);

				modal.find('.before').addClass('hidden');
				modal.find('.upload').removeClass('hidden');

				SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
					const sys = serverSystems.get(config.id);
					if ( !sys ) {
						return;
					}
					renderServerDetailConfigs(res.measurementConfigs, sys['meas'], false);
					renderServerDetailConfigs(res.controlConfigs, sys['ctrl'], false);
				}, function serializeServerDataPointsImportForm(form) {
					var formData = new FormData(form);
					return formData;
				}, {
					upload: function(event) {
						if (event.lengthComputable) {
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
				SolarReg.Settings.resetEditServiceForm(this);
			});
			
		function serverModalEditFormShowSetup(event) {
			const form = this;
			const config = SolarReg.Templates.findContextItem(form);
			const serverSys = serverSystems.get(config.serverId ? config.serverId : config.id);
			const systemType = form.dataset.systemType;
			
			/** @type {Dnp3System} */
			const sys = serverSys[systemType];
			
			var idProperty = undefined;
			if ( systemType === 'auth' ) {
				idProperty = 'identifier';
			} else {
				idProperty = 'index';
			}
			// the id property field is read-only when editing existing item, write when adding new item
			if ( config[idProperty] !== undefined ) {
				$(form.elements[idProperty]).prop('readonly', true);
			} else {
				$(form.elements[idProperty]).prop('readonly', false);
				if ( idProperty === 'index' ) {				
					// default new index to highest existing index + 1
					var newIndex = -1;
					if ( sys ) {
						sys.configs.forEach(function (e) {
							if ( e.index > newIndex ) {
								newIndex = e.index;
							}
						});
						newIndex += 1;
						form.elements['index'].value = newIndex;
					}
				}
			}

			return modalEditFormShowSetup.call(form, event);
		}
			
		function serverEditModalFormSubmit(event) {
			const config = SolarReg.Templates.findContextItem(this);
			const sys = serverSystems.get(config.serverId ? config.serverId : config.id);
			const systemType = this.dataset.systemType;
			return modalEditFormSubmit.call(this, event, function(configs, preserve) {
				renderServerDetailConfigs(configs, sys[systemType], preserve);
			});
		}

		/* ============================
		   Server Measurements
		   ============================ */

		// ***** Server auth edit
		$('#dnp3-auth-edit-modal').on('show.bs.modal', serverModalEditFormShowSetup)
			.on('submit', serverEditModalFormSubmit)
			.on('hidden.bs.modal', modalEditFormHiddenCleanup)
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});

		// ***** Server measurement edit
		$('#dnp3-measurement-edit-modal').on('show.bs.modal', serverModalEditFormShowSetup)
			.on('submit', serverEditModalFormSubmit)
			.on('hidden.bs.modal', modalEditFormHiddenCleanup)
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});

		// ***** Server control edit
		$('#dnp3-control-edit-modal').on('show.bs.modal', serverModalEditFormShowSetup)
			.on('submit', serverEditModalFormSubmit)
			.on('hidden.bs.modal', modalEditFormHiddenCleanup)
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});

		/* ============================
		   Init
		   ============================ */
		(function initDnp3Management() {
			var loadCountdown = 2;
			var caConfs = [];
			var serverConfs = [];

			function liftoff() {
				loadCountdown -= 1;
				if (loadCountdown === 0) {
					renderTrustedIssuerConfigs(caConfs);
					renderServerConfigs(serverConfs);
					SolarReg.showPageLoaded();
				}
			}

			// list all trusted issuers
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/trusted-issuer-certs'), function(json) {
				console.debug('Got DNP3 trusted issuer certificates: %o', json);
				if (json && json.success === true) {
					caConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});

			// list all servers
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/servers'), function(json) {
				console.debug('Got DNP3 servers: %o', json);
				if (json && json.success === true) {
					serverConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});

		})();

	});
});
