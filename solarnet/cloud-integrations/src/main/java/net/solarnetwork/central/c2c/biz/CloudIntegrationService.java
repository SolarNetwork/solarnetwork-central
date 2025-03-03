/* ==================================================================
 * CloudIntegrationService.java - 29/09/2024 2:45:47 pm
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

package net.solarnetwork.central.c2c.biz;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.LocalizedServiceInfoProvider;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * API for a cloud integration service.
 *
 * @author matt
 * @version 1.3
 */
public interface CloudIntegrationService
		extends Identity<String>, SettingSpecifierProvider, LocalizedServiceInfoProvider {

	/** An API "base" URL to the external service API. */
	String API_BASE_WELL_KNOWN_URL = "baseUrl";

	/** An API OAuth token-granting URL used by the external service API. */
	String TOKEN_WELL_KNOWN_URL = "tokenUrl";

	/** A standard OAuth client identifier setting name. */
	String OAUTH_CLIENT_ID_SETTING = "oauthClientId";

	/** A standard OAuth client secret setting name. */
	String OAUTH_CLIENT_SECRET_SETTING = "oauthClientSecret";

	/**
	 * A standard OAuth access token.
	 *
	 * @since 1.3
	 */
	String OAUTH_ACCESS_TOKEN_SETTING = "oauthAccessToken";

	/**
	 * A standard OAuth refresh token.
	 *
	 * @since 1.3
	 */
	String OAUTH_REFRESH_TOKEN_SETTING = "oauthRefreshToken";

	/** A standard username setting name. */
	String USERNAME_SETTING = "username";

	/** A standard password setting name. */
	String PASSWORD_SETTING = "password";

	/**
	 * A standard base URL setting name, to support proxy servers for example.
	 *
	 * @since 1.1
	 */
	String BASE_URL_SETTING = "baseUrl";

	/**
	 * The audit service name for content processed (bytes).
	 *
	 * @since 1.2
	 */
	String CONTENT_PROCESSED_AUDIT_SERVICE = "ccio";

	/**
	 * Get a mapping of "well known" service URIs.
	 *
	 * @return the well-known URLs, never {@literal null}
	 */
	Map<String, URI> wellKnownUrls();

	/**
	 * Get the datum stream services supported by this integration.
	 *
	 * @return the supported datum stream services, never {@literal null}
	 */
	Iterable<CloudDatumStreamService> datumStreamServices();

	/**
	 * Validate a configuration.
	 *
	 * <p>
	 * This method can be used to verify an integration's configuration is
	 * valid, such as credentials.
	 * </p>
	 *
	 * @param integration
	 *        the integration configuration to validate
	 * @param locale
	 *        the locale to use for error messages
	 * @return the validation results, never {@literal null}
	 */
	Result<Void> validate(CloudIntegrationConfiguration integration, Locale locale);

}
