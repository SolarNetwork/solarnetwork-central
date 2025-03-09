/* ==================================================================
 * BaseCloudIntegrationService.java - 7/10/2024 7:19:29 am
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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.BasicCloudIntegrationLocalizedServiceInfo;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Abstract base implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.5
 */
public abstract class BaseCloudIntegrationService extends BaseCloudIntegrationsIdentifiableService
		implements CloudIntegrationService {

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#BASE_URL_SETTING}.
	 *
	 * @since 1.2
	 */
	public static final TextFieldSettingSpecifier BASE_URL_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			BASE_URL_SETTING, null);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#USERNAME_SETTING}.
	 *
	 * @since 1.2
	 */
	public static final TextFieldSettingSpecifier USERNAME_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			USERNAME_SETTING, null);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#PASSWORD_SETTING}.
	 *
	 * @since 1.2
	 */
	public static final TextFieldSettingSpecifier PASSWORD_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			PASSWORD_SETTING, null, true);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#OAUTH_CLIENT_ID_SETTING}.
	 *
	 * @since 1.2
	 */
	public static final TextFieldSettingSpecifier OAUTH_CLIENT_ID_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			OAUTH_CLIENT_ID_SETTING, null);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#OAUTH_CLIENT_SECRET_SETTING}.
	 *
	 * @since 1.2
	 */
	public static final TextFieldSettingSpecifier OAUTH_CLIENT_SECRET_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			OAUTH_CLIENT_SECRET_SETTING, null, true);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#OAUTH_ACCESS_TOKEN_SETTING}.
	 *
	 * @since 1.4
	 */
	public static final TextFieldSettingSpecifier OAUTH_ACCESS_TOKEN_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			OAUTH_ACCESS_TOKEN_SETTING, null, true);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#OAUTH_REFRESH_TOKEN_SETTING}.
	 *
	 * @since 1.4
	 */
	public static final TextFieldSettingSpecifier OAUTH_REFRESH_TOKEN_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			OAUTH_REFRESH_TOKEN_SETTING, null, true);

	/**
	 * A setting specifier for the
	 * {@link CloudIntegrationService#API_KEY_SETTING}.
	 *
	 * @since 1.4
	 */
	public static final TextFieldSettingSpecifier API_KEY_SETTING_SPECIFIER = new BasicTextFieldSettingSpecifier(
			API_KEY_SETTING, null, true);

	/** The supported datum stream services. */
	protected final Collection<CloudDatumStreamService> datumStreamServices;

	/** The well known URLs. */
	protected final Map<String, URI> wellKnownUrls;

	/**
	 * Constructor.
	 *
	 * @param serviceIdentifier
	 *        the service identifier
	 * @param displayName
	 *        the display name
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param settings
	 *        the service settings
	 * @param wellKnownUrls
	 *        the well known URLs
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseCloudIntegrationService(String serviceIdentifier, String displayName,
			Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			List<SettingSpecifier> settings, Map<String, URI> wellKnownUrls) {
		super(serviceIdentifier, displayName, userEventAppenderBiz, encryptor, settings);
		this.datumStreamServices = requireNonNullArgument(datumStreamServices, "datumStreamServices");
		this.wellKnownUrls = requireNonNullArgument(wellKnownUrls, "wellKnownUrls");
	}

	/**
	 * Resolve a base URL.
	 *
	 * <p>
	 * This method will look for a
	 * {@link CloudIntegrationService#BASE_URL_SETTING} service property and
	 * attempt to parse that as a URI and return it, falling back to returning
	 * {@code defaultBaseUrl} if that fails.
	 * </p>
	 *
	 * @param integration
	 *        the integration to look for the base URL service property on
	 * @param defaultBaseUrl
	 *        the fallback URL to use
	 * @return the URL, or {@code null} if the
	 *         {@link CloudIntegrationService#BASE_URL_SETTING} service property
	 *         cannot be resolved as a URI and the given {@code defaultBaseUrl}
	 *         is {@code null}
	 * @since 1.1
	 */
	public static URI resolveBaseUrl(CloudIntegrationConfiguration integration, URI defaultBaseUrl) {
		URI result = defaultBaseUrl;
		if ( integration != null && integration.hasServiceProperty(BASE_URL_SETTING) ) {
			try {
				result = new URI(integration.serviceProperty(BASE_URL_SETTING, String.class));
			} catch ( URISyntaxException e ) {
				// ignore, use default
			}
		}
		return result;
	}

	/**
	 * Resolve a base URL string value.
	 *
	 * <p>
	 * This method will look for a
	 * {@link CloudIntegrationService#BASE_URL_SETTING} service property and
	 * return it, falling back to returning {@code defaultBaseUrl} if that
	 * fails.
	 * </p>
	 *
	 * @param integration
	 *        the integration to look for the base URL service property on
	 * @param defaultBaseUrl
	 *        the fallback URL to use
	 * @return the URL, or {@code null} if the
	 *         {@link CloudIntegrationService#BASE_URL_SETTING} service property
	 *         cannot be resolved
	 * @since 1.3
	 */
	public static String resolveBaseUrl(CloudIntegrationConfiguration integration,
			String defaultBaseUrl) {
		String result = defaultBaseUrl;
		if ( integration != null && integration.hasServiceProperty(BASE_URL_SETTING) ) {
			result = integration.serviceProperty(BASE_URL_SETTING, String.class);
		}
		return result;
	}

	@Override
	public LocalizedServiceInfo getLocalizedServiceInfo(Locale locale) {
		return new BasicCloudIntegrationLocalizedServiceInfo(
				super.getLocalizedServiceInfo(locale != null ? locale : Locale.getDefault()),
				getSettingSpecifiers(), wellKnownUrls);
	}

	@Override
	public final Map<String, URI> wellKnownUrls() {
		return wellKnownUrls;
	}

	@Override
	public final Iterable<CloudDatumStreamService> datumStreamServices() {
		return datumStreamServices;
	}

}
