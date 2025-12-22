/* ==================================================================
 * SmaCloudIntegrationService.java - 29/03/2025 6:45:04 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.random.RandomGenerator;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.AuthorizationState;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.OAuth2RestOperationsHelper;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.domain.HttpRequestInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * Sma implementation of {@link CloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
public class SmaCloudIntegrationService extends BaseRestOperationsCloudIntegrationService {

	/** The service identifier. */
	public static final String SERVICE_IDENTIFIER = "s10k.c2c.i9n.sma";

	/**
	 * The URL path for listing all available systems.
	 */
	public static final String LIST_SYSTEMS_PATH = "/v1/plants";

	/** The base URL to the SMA API. */
	public static final URI BASE_URI = URI.create("https://monitoring.smaapis.de");

	/** The base authorization URL for the SMA API. */
	public static final URI AUTHORIZATION_BASE_URI = URI.create("https://auth.smaapis.de");

	/** The OAuth authorization path. */
	public static final String AUTH_PATH = "/oauth2/auth";

	/** The OAuth authorization URL. */
	public static final URI AUTH_URI = AUTHORIZATION_BASE_URI.resolve(AUTH_PATH);

	/** The OAuth token URL. */
	public static final String TOKEN_PATH = "/oauth2/token";

	/** The OAuth token URL. */
	public static final URI TOKEN_URI = AUTHORIZATION_BASE_URI.resolve(TOKEN_PATH);

	/**
	 * The well-known URLs.
	 */
	// @formatter:off
	public static final Map<String, URI> WELL_KNOWN_URLS = Map.of(
			API_BASE_WELL_KNOWN_URL, BASE_URI,
			AUTHORIZATION_WELL_KNOWN_URL, AUTH_URI,
			TOKEN_WELL_KNOWN_URL, TOKEN_URI
			);
	// @formatter:on

	/** The service settings. */
	public static final List<SettingSpecifier> SETTINGS;
	static {
		// @formatter:off
		SETTINGS = List.of(
				OAUTH_CLIENT_ID_SETTING_SPECIFIER,
				OAUTH_CLIENT_SECRET_SETTING_SPECIFIER,
				OAUTH_ACCESS_TOKEN_SETTING_SPECIFIER,
				OAUTH_REFRESH_TOKEN_SETTING_SPECIFIER,
				BASE_URL_SETTING_SPECIFIER,
				AUTHORIZATION_BASE_URL_SETTING_SPECIFIER
				);
		// @formatter:on
	}

	/** The service secure setting keys. */
	public static final Set<String> SECURE_SETTINGS = Collections
			.unmodifiableSet(SettingUtils.secureKeys(SETTINGS));

	private final CloudIntegrationConfigurationDao integrationDao;
	private final RandomGenerator rng;
	private final RestOperationsHelper tokenFetchHelper;

	/**
	 * Constructor.
	 *
	 * @param datumStreamServices
	 *        the datum stream services
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param integrationDao
	 *        the integration DAO
	 * @param rng
	 *        the random generator to use
	 * @param restOps
	 *        the REST operations
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param clock
	 *        the clock
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
	public SmaCloudIntegrationService(Collection<CloudDatumStreamService> datumStreamServices,
			UserEventAppenderBiz userEventAppenderBiz, TextEncryptor encryptor,
			CloudIntegrationConfigurationDao integrationDao, RandomGenerator rng, RestOperations restOps,
			OAuth2AuthorizedClientManager oauthClientManager, Clock clock,
			Cache<UserLongCompositePK, Lock> integrationLocksCache) {
		super(SERVICE_IDENTIFIER, "AlsoEnergy", datumStreamServices, List.of(), userEventAppenderBiz,
				encryptor, SETTINGS, WELL_KNOWN_URLS,
				new OAuth2RestOperationsHelper(LoggerFactory.getLogger(SmaCloudIntegrationService.class),
						userEventAppenderBiz, restOps, INTEGRATION_HTTP_ERROR_TAGS, encryptor,
						_ -> SECURE_SETTINGS, oauthClientManager, clock, integrationLocksCache));
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.rng = requireNonNullArgument(rng, "rng");
		this.tokenFetchHelper = new RestOperationsHelper(
				LoggerFactory.getLogger(SmaCloudIntegrationService.class), userEventAppenderBiz, restOps,
				INTEGRATION_HTTP_ERROR_TAGS, encryptor, _ -> SECURE_SETTINGS);
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

		final String oauthAccessToken = integration
				.serviceProperty(CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING, String.class);
		if ( oauthAccessToken == null || oauthAccessToken.isEmpty() ) {
			String errMsg = ms.getMessage("error.oauthAccessToken.missing", null, locale);
			errorDetails.add(
					new ErrorDetail(CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING, null, errMsg));
		}

		final String oauthRefreshToken = integration
				.serviceProperty(CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING, String.class);
		if ( oauthRefreshToken == null || oauthRefreshToken.isEmpty() ) {
			String errMsg = ms.getMessage("error.oauthRefreshToken.missing", null, locale);
			errorDetails.add(
					new ErrorDetail(CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING, null, errMsg));
		}

		if ( !errorDetails.isEmpty() ) {
			String errMsg = ms.getMessage("error.settings.missing", null, locale);
			return Result.error("SACI.0001", errMsg, errorDetails);
		}

		// validate by requesting the available systems
		try {
			final String response = restOpsHelper.httpGet("List systems", integration, String.class,
					_ -> UriComponentsBuilder.fromUri(resolveBaseUrl(integration, BASE_URI))
							.path(SmaCloudIntegrationService.LIST_SYSTEMS_PATH).buildAndExpand().toUri(),
					HttpEntity::getBody);
			log.debug("Validation of config {} succeeded: {}", integration.getConfigId(), response);
			return Result.success();
		} catch ( RemoteServiceException e ) {
			return validationResult(e, null);
		} catch ( Exception e ) {
			return Result.error("SACI.0002", "Validation failed: " + e.getMessage());
		}
	}

	@Override
	public HttpRequestInfo authorizationRequestInfo(CloudIntegrationConfiguration integration,
			URI redirectUri, Locale locale) {
		requireNonNullArgument(integration, "integration");

		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final String oauthClientId = integration
				.serviceProperty(CloudIntegrationService.OAUTH_CLIENT_ID_SETTING, String.class);
		if ( oauthClientId == null || oauthClientId.isEmpty() ) {
			String errMsg = ms.getMessage("error.oauthClientId.missing", null, locale);
			throw new IllegalArgumentException(errMsg);
		}

		final byte[] rand = new byte[32];
		rng.nextBytes(rand);
		final String stateToken = Base64.getUrlEncoder().encodeToString(DigestUtils.sha3_224(rand))
				.replace("=", "");

		integrationDao.saveOAuthAuthorizationState(integration.getId(), stateToken, null);

		String stateValue = new AuthorizationState(integration.getConfigId(), stateToken).stateValue();

		// @formatter:off
		URI uri = UriComponentsBuilder.fromUri(resolveUrl(
					integration, AUTHORIZATION_BASE_URL_SETTING, AUTHORIZATION_BASE_URI))
				.path(AUTH_PATH)
				.queryParam("response_type", "code")
				.queryParam("client_id", oauthClientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("state", stateValue)
				.queryParam("scope", "offline_access")
				.buildAndExpand().toUri();
		// @formatter:on

		return new HttpRequestInfo("GET", uri, null);
	}

	@Override
	public Map<String, ?> fetchAccessToken(CloudIntegrationConfiguration integration,
			Map<String, ?> parameters, Locale locale) {
		requireNonNullArgument(integration, "integration");
		requireNonNullArgument(parameters, "parameters");

		final String code = requireNonNullArgument(parameters.get(AUTHORIZATION_CODE_PARAM),
				AUTHORIZATION_CODE_PARAM).toString();
		final String stateValue = requireNonNullArgument(parameters.get(AUTHORIZATION_STATE_PARAM),
				AUTHORIZATION_STATE_PARAM).toString();
		final String redirectUri = requireNonNullArgument(parameters.get(REDIRECT_URI_PARAM),
				REDIRECT_URI_PARAM).toString();

		final MessageSource ms = requireNonNullArgument(getMessageSource(), "messageSource");

		final AuthorizationState state = requireNonNullArgument(
				AuthorizationState.forStateValue(stateValue), "state");
		if ( !state.integrationId().equals(integration.getConfigId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, state.integrationId());
		}

		if ( !integrationDao.saveOAuthAuthorizationState(integration.getId(), null, state.token()) ) {
			// state mis-match; abort
			String errMsg = ms.getMessage("error.oauth.state.mismtach", null, locale);
			throw new IllegalArgumentException(errMsg);
		}

		final var decrypted = integration.copyWithId(integration.getId());
		decrypted.unmaskSensitiveInformation(_ -> SECURE_SETTINGS, encryptor);

		final JsonNode json = tokenFetchHelper.http("Get OAuth token", HttpMethod.POST, null,
				integration, JsonNode.class, (req) -> {
				// @formatter:off
					URI uri = UriComponentsBuilder.fromUri(resolveUrl(
							integration, AUTHORIZATION_BASE_URL_SETTING, AUTHORIZATION_BASE_URI))
						.path(TOKEN_PATH)
						.queryParam("grant_type", "authorization_code")
						.queryParam(AUTHORIZATION_CODE_PARAM, code)
						.queryParam(REDIRECT_URI_PARAM, redirectUri)
						.queryParam("scope", "offline_access")
						.buildAndExpand().toUri();
					// @formatter:on
					req.setBasicAuth(decrypted.serviceProperty(OAUTH_CLIENT_ID_SETTING, String.class),
							decrypted.serviceProperty(OAUTH_CLIENT_SECRET_SETTING, String.class));
					return uri;
				}, HttpEntity::getBody);

		/*- JSON example:
			{
			  "access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiw...",
			  "expires_in":300,
			  "refresh_expires_in":0,
			  "refresh_token":"eyJhbGciOiJIUzUxMiIsInR5cCIgOiAiSldUIi...",
			  "token_type":"Bearer",
			  "not-before-policy":1626710983,
			  "session_state":"a57421a7-a319-476b-9bad-14de52ec3109",
			  "scope":"profile monitoringApi:read email offline_access"
			}
		 */

		final String accessToken = json.path("access_token").textValue();
		if ( accessToken == null ) {
			String errMsg = ms.getMessage("error.oauth.accessToken.missing", null, locale);
			throw new IllegalStateException(errMsg);
		}

		final String refreshToken = json.path("refresh_token").textValue();
		if ( refreshToken == null ) {
			String errMsg = ms.getMessage("error.oauth.refreshToken.missing", null, locale);
			throw new IllegalStateException(errMsg);
		}

		return Map.of(OAUTH_ACCESS_TOKEN_SETTING, accessToken, OAUTH_REFRESH_TOKEN_SETTING,
				refreshToken);
	}

}
