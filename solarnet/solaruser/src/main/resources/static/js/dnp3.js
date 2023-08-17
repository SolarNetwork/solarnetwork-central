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
			config.name = config.subjectDn;
			config.systemType = 'ca';
			var model = SolarReg.Settings.serviceConfigurationItem(config, []);
			model.id = config.id;
			model.systemType = config.systemType;
			model.expiresDisplay = moment(config.expires).format('D MMM YYYY');
			model.enabled = config.enabled;
			return model;
		}

		systems.ca.container.find('.list-container').on('click', function(event) {
			// edit ca
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
		});

		// ***** Import form
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

		$('#dnp3-ca-edit-modal').on('show.bs.modal', function handleModalShow() {
			var modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
		})
			.on('submit', function handleCaModalFormSubmit(event) {
				const config = SolarReg.Templates.findContextItem(this);
				if (!config.systemType) {
					return;
				}

				SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
					res.id = res.configId;
					res.systemType = config.systemType;
					renderTrustedIssuerConfigs([res], true);
				}, function serializeDataConfigForm(form) {
					var data = SolarReg.Settings.encodeServiceItemForm(form, true);

					if (!data.userId) {
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

					/** @type {Dnp3System} */
					sys = systems[type];
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
			})
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

			var idx = 0;
			SolarReg.Templates.populateTemplateItems(sys.container, items, preserve, function serverTemplateCallback(item, el) {
				idx += 1;
				el.find('div.panel-heading').attr('id', 'server-heading-' + idx);
				el.find('.panel-title a').attr('href', '#server-body-' + idx).attr('aria-controls', 'server-body-' + idx);
				el.find('div.panel-collapse ').attr('id', 'server-body-' + idx).attr('aria-labelledby', 'server-heading-' + idx);
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

		function handleServerDataPointsImport(item) {
			if (!(item && item.id)) {
				return;
			}
			let editModal = $('#dnp3-server-data-points-import-modal');
			SolarReg.Templates.setContextItem(editModal, item);
			editModal.modal('show');
		}

		systems.s.container.on('click', function handleServersClick(/** @type {MouseEvent} */ event) {
			console.debug('Click on servers %o', event);
			const target = $(event.target);
			if ( target.hasClass('panel-title') || target.hasClass('panel-heading') ) {
				// toggle this panel
				let dest = target.parent().find('a[data-parent="#dnp3-servers-container"]').attr('href');
				if ( dest ) {
					$(dest).collapse('toggle');
				}
				return;
			}
			const action = target.closest('.action');
			if (!action.length) {
				return;
			}
			const item = SolarReg.Templates.findContextItem(action);
			if (action.hasClass('csv-import')) {
				handleServerDataPointsImport(item);
			} else if (action.hasClass('csv-export')) {
				handleServerDataPointsExport(item);
			} else if (action.hasClass('edit')) {
				// TODO
			}
		});

		systems.s.container.find('.list-container').on('click', function(event) {
			// edit server
			SolarReg.Settings.handleEditServiceItemAction(event, [], []);
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
				sys.container.find('thead.hidden').addClass('hidden');
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

		// ***** Add server form
		$('#dnp3-server-add-modal')
			.on('shown.bs.modal', SolarReg.Settings.focusEditServiceForm)
			.on('submit', function(event) {
				SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
					if (Array.isArray(res)) {
						renderServerConfigs(res, true);
					}
				});
				return false;
			})
			.on('hidden.bs.modal', function() {
				SolarReg.Settings.resetEditServiceForm(this);
			});

		// ***** Edit server
		$('#dnp3-server-edit-modal').on('show.bs.modal', function handleModalShow() {
			var modal = $(this)
				, config = SolarReg.Templates.findContextItem(this)
				, enabled = (config && config.enabled === true ? true : false);
			SolarReg.Settings.handleSettingToggleButtonChange(modal.find('button[name=enabled]'), enabled);
			SolarReg.Settings.prepareEditServiceForm(modal, [], []);
		})
			.on('submit', function handleServerModalFormSubmit(event) {
				const config = SolarReg.Templates.findContextItem(this);
				if (!config.systemType) {
					return;
				}

				SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
					res.id = res.configId;
					res.systemType = config.systemType;
					renderServerConfigs([res], true);
				}, undefined, {
					urlId: true
				});
				return false;
			})
			.on('hidden.bs.modal', function handleModalHidden() {
				const config = SolarReg.Templates.findContextItem(this),
					type = config.systemType,

					/** @type {Dnp3System} */
					sys = systems[type];
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
			})
			.find('button.toggle').each(function() {
				SolarReg.Settings.setupSettingToggleButton($(this), false);
			});
			
		// ***** Import data points CSV
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
				var uploadProgressBar = $('.upload .progress-bar', event.target);
				var uploadProgressBarAmount = $('.amount', uploadProgressBar);
				var modal = $(event.target);

				modal.find('.before').addClass('hidden');
				modal.find('.upload').removeClass('hidden');

				SolarReg.Settings.handlePostEditServiceForm(event, function(req, res) {
					// TODO
					console.log('TODO: render imported data points: %o', res);
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
