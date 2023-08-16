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
			console.log('Progress now ' +percentComplete);
			if ( bar ) {
				bar.attr('aria-valuenow', value).css('width', value+'%');
			}
			if ( barAmount ) {
				barAmount.text(value);
			}
		}

		/* ============================
		   Globals
		   ============================ */
		const i18n = SolarReg.i18nData(this);
		
		const systems = Object.freeze({
			ca: createSystem($('#dnp3-cas-container'), 'ca'),
		});



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
			if ( !sys ) {
				return;
			}
			configs = Array.isArray(configs) ? configs : [];
			if ( !preserve ) {
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
				if ( Array.isArray(res) ) {
					renderTrustedIssuerConfigs(res, true);
				}
			}, function serializeCaImportForm(form) {
				var formData = new FormData(form);
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
			if ( !config.systemType ) {
				return;
			}

			SolarReg.Settings.handlePostEditServiceForm(event, function onSuccess(req, res) {
				res.id = res.configId;
				res.systemType = config.systemType;
				renderTrustedIssuerConfigs([res], true);
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

				/** @type {Dnp3System} */
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
		   Init
		   ============================ */
		(function initDnp3Management() {
			var loadCountdown = 1;
			var caConfs = [];

			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					renderTrustedIssuerConfigs(caConfs);
					SolarReg.showPageLoaded();
				}
			}

			// list all trusted issuers
			$.getJSON(SolarReg.solarUserURL('/sec/dnp3/trusted-issuer-certs'), function(json) {
				console.debug('Got DNP3 trusted issuer certificates: %o', json);
				if ( json && json.success === true ) {
					caConfs = json.data ? json.data.results : undefined;
				}
				liftoff();
			});

		})();

	});
});
