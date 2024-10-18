/* ==================================================================
 * LocusEnergyCloudIntegrationService.java - 30/09/2024 12:05:16 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * Locus Energy implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.1
 */
public class LocusEnergyCloudIntegrationService extends BaseOAuth2ClientCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.locus";

	/**
	 * The URL template for sites for a given {@code \{partnerId\}} parameter.
	 */
	public static final String V3_SITES_FOR_PARTNER_ID_URL_TEMPLATE = "/v3/partners/{partnerId}/sites";

	/**
	 * The URL template for components for a given {@code \{siteId\}} parameter.
	 */
	public static final String V3_COMPONENTS_FOR_SITE_ID_URL_TEMPLATE = "/v3/sites/{siteId}/components";

	/**
	 * The URL template for nodes (data available) for a given
	 * {@code \{componentId\}} parameter.
	 */
	public static final String V3_NODES_FOR_COMPOENNT_ID_URL_TEMPLATE = "/v3/components/{componentId}/dataavailable";

	/**
	 * The URL template for data for a given {@code \{componentId\}} parameter.
	 */
	public static final String V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE = "/v3/components/{componentId}/data";

	/** The partner identifier setting name. */
	public static final String PARTNER_ID_SETTING = "partnerId";

	/** The base URL to the Locus Energy API. */
	public static final URI BASE_URI = URI.create("https://api.locusenergy.com");

	/** The OAuth token URL. */
	public static final URI TOKEN_URI = BASE_URI.resolve("/oauth/token");

	/**
	 * The well-known URLs.
	 */
	// @formatter:off
	public static final Map<String, URI> WELL_KNOWN_URLS = Map.of(
			API_BASE_WELL_KNOWN_URL, BASE_URI,
			TOKEN_WELL_KNOWN_URL, TOKEN_URI
			);
	// @formatter:on

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		var settings = new ArrayList<SettingSpecifier>(1);
		settings.add(new BasicTextFieldSettingSpecifier(OAUTH_CLIENT_ID_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(OAUTH_CLIENT_SECRET_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(USERNAME_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(PASSWORD_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(PARTNER_ID_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(BASE_URL_SETTING, null));
		SETTINGS = Collections.unmodifiableList(settings);
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	/**
	 * Constructor.
	 *
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocusEnergyCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(SERVICE_IDENTIFIER, "Locus Energy", datumStreamServices, userEventAppenderBiz, encryptor,
				SETTINGS, WELL_KNOWN_URLS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(LocusEnergyCloudIntegrationService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS, oauthClientManager),
				oauthClientManager);
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale) {
		// check that authentication settings provided
		final List<ErrorDetail> errorDetails = new ArrayList<>(2);
		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String oauthClientId = integration
				.serviceProperty(CloudIntegrationService.OAUTH_CLIENT_ID_SETTING, String.class);
		if ( oauthClientId == null || oauthClientId.isEmpty() ) {
			String errMsg = ms.getMessage("error.oauthClientId.missing", null, locale);
			errorDetails
					.add(new ErrorDetail(CloudIntegrationService.OAUTH_CLIENT_ID_SETTING, null, errMsg));
		}

		final String oauthClientSecret = integration
				.serviceProperty(CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, String.class);
		if ( oauthClientSecret == null || oauthClientSecret.isEmpty() ) {
			String errMsg = ms.getMessage("error.oauthClientSecret.missing", null, locale);
			errorDetails.add(
					new ErrorDetail(CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, null, errMsg));
		}

		final String username = integration.serviceProperty(CloudIntegrationService.USERNAME_SETTING,
				String.class);
		if ( username == null || username.isEmpty() ) {
			String errMsg = ms.getMessage("error.username.missing", null, locale);
			errorDetails.add(new ErrorDetail(CloudIntegrationService.USERNAME_SETTING, null, errMsg));
		}

		final String password = integration.serviceProperty(CloudIntegrationService.PASSWORD_SETTING,
				String.class);
		if ( password == null || password.isEmpty() ) {
			String errMsg = ms.getMessage("error.password.missing", null, locale);
			errorDetails.add(new ErrorDetail(CloudIntegrationService.PASSWORD_SETTING, null, errMsg));
		}

		final String partnerId = integration.serviceProperty(PARTNER_ID_SETTING, String.class);
		if ( partnerId == null || partnerId.isEmpty() ) {
			String errMsg = ms.getMessage("error.partnerId.missing", null, locale);
			errorDetails.add(new ErrorDetail(PARTNER_ID_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("LECI.0001", errMsg, errorDetails);
		}

		// validate by requesting the available sites for the partner ID
		try {
			final String response = restOpsHelper.httpGet("List sites", integration, String.class,
					(req) -> UriComponentsBuilder.fromUri(resolveBaseUrl(integration, BASE_URI)).path(
							LocusEnergyCloudIntegrationService.V3_SITES_FOR_PARTNER_ID_URL_TEMPLATE)
							.buildAndExpand(integration.getServiceProperties()).toUri(),
					res -> res.getBody());
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( Exception e ) {
			return Result.error("LECI.0002", "Validation failed: " + e.getMessage());
		}
	}

}
