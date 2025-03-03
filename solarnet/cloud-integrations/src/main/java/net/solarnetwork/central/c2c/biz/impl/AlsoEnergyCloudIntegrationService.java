/* ==================================================================
 * AlsoEnergyCloudIntegrationService.java - 22/11/2024 9:04:29 am
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
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * AlsoEnergy implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.1
 */
public class AlsoEnergyCloudIntegrationService extends BaseOAuth2ClientCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.also";

	/**
	 * The URL template for listing all available sites.
	 */
	public static final String LIST_SITES_URL = "/sites";

	/** The base URL to the AlsoEnergy API. */
	public static final URI BASE_URI = URI.create("https://api.alsoenergy.com");

	/** The OAuth token URL. */
	public static final URI TOKEN_URI = BASE_URI.resolve("/auth/token");

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
		SETTINGS = List.of(OAUTH_CLIENT_ID_SETTING_SPECIFIER, USERNAME_SETTING_SPECIFIER,
				PASSWORD_SETTING_SPECIFIER, BASE_URL_SETTING_SPECIFIER);
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
	 * @param clock
	 *        the clock to use
	 * @param integrationLocksCache
	 *        an optional cache that, when provided, will be used to obtain a
	 *        lock before acquiring an access token; this can be used in prevent
	 *        concurrent requests using the same {@code config} from making
	 *        multiple token requests; not the cache is assumed to have
	 *        read-through semantics that always returns a new lock for missing
	 *        keys
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AlsoEnergyCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, InstantSource clock,
			Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "AlsoEnergy", datumStreamServices, userEventAppenderBiz, encryptor,
				SETTINGS, WELL_KNOWN_URLS,
				new OAuth2RestOperationsHelper(
						LoggerFactory.getLogger(AlsoEnergyCloudIntegrationService.class),
						userEventAppenderBiz, restOps, HTTP_ERROR_TAGS, encryptor,
						integrationServiceIdentifier -> SECURE_SETTINGS, oauthClientManager, clock,
						integrationLocksCache),
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

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("AECI.0001", errMsg, errorDetails);
		}

		// validate by requesting the available sites
		try {
			final String response = restOpsHelper.httpGet("List sites", integration, String.class,
					(req) -> UriComponentsBuilder.fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(AlsoEnergyCloudIntegrationService.LIST_SITES_URL).buildAndExpand()
							.toUri(),
					HttpEntity::getBody);
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( Exception e ) {
			return Result.error("AECI.0002", "Validation failed: " + e.getMessage());
		}
	}

}
