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

import static java.lang.String.format;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BaseSettingsSpecifierLocalizedServiceInfoProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Locus Energy implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class LocusEnergyCloudIntegrationService
		extends BaseSettingsSpecifierLocalizedServiceInfoProvider<String>
		implements CloudIntegrationService, CloudIntegrationsUserEvents {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.locus";

	/** The partner identifier setting name. */
	public static final String PARTNER_ID_SETTING = "partnerId";

	private static final String[] AUTH_ERROR_TAGS = new String[] { CLOUD_INTEGRATION_TAG,
			AUTHORIZATION_TAG, ERROR_TAG };

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

	private final Collection<CloudDatumStreamService> datumStreamServices;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final RestOperations restOps;
	private final OAuth2AuthorizedClientManager oauthClientManager;

	private static final List<SettingSpecifier> SETTINGS;
	static {
		var settings = new ArrayList<SettingSpecifier>(1);
		settings.add(new BasicTextFieldSettingSpecifier(CLIENT_ID_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(CLIENT_SECRET_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(USERNAME_SETTING, null));
		settings.add(new BasicTextFieldSettingSpecifier(PASSWORD_SETTING, null, true));
		settings.add(new BasicTextFieldSettingSpecifier(PARTNER_ID_SETTING, null));
		SETTINGS = Collections.unmodifiableList(settings);
	}

	/**
	 * Constructor.
	 *
	 * @param datumStreamService
	 *        the datum stream service
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LocusEnergyCloudIntegrationService(LocusEnergyCloudDatumStreamService datumStreamService,
			UserEventAppenderBiz userEventAppenderBiz, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(SERVICE_IDENTIFIER);
		this.datumStreamServices = Collections
				.singleton(requireNonNullArgument(datumStreamService, "datumStreamService"));
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
	}

	@Override
	public String getDisplayName() {
		return "Locus Energy";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return SETTINGS;
	}

	@Override
	public Map<String, URI> wellKnownUrls() {
		return WELL_KNOWN_URLS;
	}

	@Override
	public Iterable<CloudDatumStreamService> datumStreamServices() {
		return datumStreamServices;
	}

	@Override
	public Result<Void> validate(CloudIntegrationConfiguration config) {
		// validate by requesting the available sites for the partner ID
		try {
			HttpHeaders headers = new HttpHeaders();
			addAuthorization(config, headers);

			final URI uri = UriComponentsBuilder.fromUri(BASE_URI).path("/v3/partners/{partnerId}/sites")
					.buildAndExpand(config.getServiceProperties()).toUri();

			HttpEntity<Void> req = new HttpEntity<>(null, headers);
			ResponseEntity<String> res = restOps.exchange(uri, HttpMethod.GET, req, String.class);
			log.debug("Validation of config {} succeeded: {}", config.getConfigId(), res.getBody());
			return Result.success();
		} catch ( Exception e ) {
			return Result.error("LECI.0001", "Validation failed: " + e.getMessage());
		}
	}

	/**
	 * Add appropriate authorization header values to a given
	 * {@link HttpHeaders}.
	 *
	 * @param config
	 *        the configuration to authorize
	 * @param headers
	 *        the headers to add authorization to
	 */
	private void addAuthorization(CloudIntegrationConfiguration config, HttpHeaders headers) {
		final String username = config.serviceProperty(USERNAME_SETTING, String.class);
		final String password = config.serviceProperty(PASSWORD_SETTING, String.class);
		final OAuth2AuthorizeRequest.Builder authReq = OAuth2AuthorizeRequest
				.withClientRegistrationId(config.systemIdentifier());
		if ( username != null && !username.isEmpty() && password != null && !password.isEmpty() ) {
			authReq.principal(new UsernamePasswordAuthenticationToken(username, password));
		} else {
			authReq.principal("%s %s".formatted(config.getId().ident(), config.getName()));
		}
		try {
			OAuth2AuthorizedClient oauthClient = requireNonNullObject(
					oauthClientManager.authorize(authReq.build()), "oauthClient");
			OAuth2AccessToken accessToken = oauthClient.getAccessToken();
			headers.add("Authorization", "Bearer " + accessToken.getTokenValue());
		} catch ( OAuth2AuthorizationException e ) {
			log.warn("Cloud integration {} authorization failed because of an OAuth error: {}",
					config.getConfigId(), e.getMessage());
			userEventAppenderBiz.addEvent(config.getUserId(), eventForConfiguration(config.getId(),
					AUTH_ERROR_TAGS, format("OAuth error: %s", e.getMessage())));
			throw e;
		}
	}

}
