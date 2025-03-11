$(document).ready(function() {
	'use strict';

	$('#c2c-oauth-management').first().each(function c2cOauthManagement() {
		/**
		 * An integration configuration model.
		 *
		 * @typedef {Object} CloudIntegration
		 * @property {number} userId the owner user ID
		 * @property {number} configId the integration ID
		 * @property {string} name a display name
		 * @property {string} serviceIdentifier the Cloud Integration Service ID
		 * @property {object} serviceProperties the service properties
		 */

		/**
		 * Authorization request info.
		 *
		 * @typedef {Object} AuthRequestInfo
		 * @property {string} method the HTTP method
		 * @property {string} uri the URI to redirect to
		 * @property {object} headers optional HTTP header values to include
		 */

		/** @type {Map<String, CloudIntegration>} */
		const integrations = new Map(); // configId -> config
		
		const integrationsForm = $('#c2c-oauth-connect-form');

		const integrationsSelect = $('#c2c-oauth-integration-select');
		
		function renderIntegrationConfigs(/** @type {Array<CloudIntegration>} */ configs) {
			configs = Array.isArray(configs) ? configs : [];
			integrations.clear();
			integrationsSelect.empty();
			integrationsSelect.append('<option>');
			for ( const config of configs ) {
				integrations.set(String(config.configId), config);
				integrationsSelect.append(new Option(config.name, config.configId))
			}
		}
		
		integrationsSelect.on('change', function handleIntegrationChange() {
			const selectedConfigId = this.value;
			const form = $(this.form);
			const config = selectedConfigId ? integrations.get(selectedConfigId) : undefined;
			
			SolarReg.Templates.replaceTemplateProperties(form, {
				clientId: config.serviceProperties.oauthClientId,
				clientSecretPresent: !!config.serviceProperties.oauthClientSecret,
			});
			
			form.find('.active').toggleClass('hidden', !config);
		});
		
		integrationsForm.on('submit', function handleIntegrationFormSubmit() {
			const integrationId = integrationsSelect.val();
			const form = this;
			if ( integrationId ) {
				const url = encodeURI(SolarReg.replaceTemplateParameters(decodeURI(form.action), {integrationId:integrationId}));
				$.getJSON(url).done((json) => {
					if (json && json.success === true) {
						/** @type {AuthRequestInfo} */
						const authInfo = json.data;
						if ( authInfo.uri ) {
							// assuming GET: redirect browser to given location
							console.info('Redirecting for OAuth authorization code flow, to: %s', authInfo.uri);
							location.href = authInfo.uri;
						}
					}
				});
			}
			return false;
		});
		
		/* ============================
		   Init
		   ============================ */
		(function initC2COAuthManagement() {
			var loadCountdown = 1;
			var integrationConfs = [];
	
			function liftoff() {
				loadCountdown -= 1;
				if ( loadCountdown === 0 ) {
					renderIntegrationConfigs(integrationConfs);
					const root = $('#c2c-oauth-connect');
					root.find('.no-entities').toggleClass('hidden', integrationConfs.length > 0);
					root.find('.some-entities').toggleClass('hidden', integrationConfs.length < 1);
					SolarReg.populateListCount(root, integrationConfs);
					
					const selectedConfigId = new URLSearchParams(location.search).get('integrationId');
					if ( selectedConfigId && integrations.has(selectedConfigId) ) {
						// jump to given configuration
						integrationsSelect.val(selectedConfigId).trigger('change');
					}
					
					SolarReg.showPageLoaded();
				}
			}
	
			// list all group settings
			$.getJSON(SolarReg.solarUserURL('/sec/c2c/oauth/integrations'), function(json) {
				console.debug('Got OAuth integration settings: %o', json);
				if ( json && json.success === true ) {
					integrationConfs = json.data.results;
				}
				liftoff();
			});
		})();
	
	});
});
