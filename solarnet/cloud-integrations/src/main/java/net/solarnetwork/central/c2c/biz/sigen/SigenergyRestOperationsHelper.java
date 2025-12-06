/* ==================================================================
 * SigenergyRestOperationsHelper.java - 5/12/2025 3:43:12 pm
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

package net.solarnetwork.central.c2c.biz.sigen;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.REGION_SETTING;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.codec.JsonUtils.getTreeFromObject;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Extension of {@link RestOperationsHelper} with support for Sigenergy style
 * authentication.
 *
 * @author matt
 * @version 1.0
 */
public class SigenergyRestOperationsHelper extends RestOperationsHelper {

	/**
	 * A system identifier component to use for access token registration IDs.
	 */
	public static final String CLOUD_INTEGRATION_SYSTEM_IDENTIFIER = "c2c-i9n-sigen";

	/** The base URL to the AlsoEnergy API. */
	public static final String BASE_URI_TEMPLATE = "https://api-{region}.sigencloud.com";

	/** The URL path to request a token for a given key. */
	public static final String KEY_TOKEN_REQUEST_PATH = "/openapi/auth/login/key";

	/** The URL path to list the available systems. */
	public static final String SYSTEM_LIST_PATH = "/openapi/system";

	/** The message returned on successful API responses. */
	public static final String RESPONSE_SUCCESS_MESSAGE = "success";

	/** The name of the response data field. */
	public static final String RESPONSE_DATA_FIELD = "data";

	private final InstantSource clock;
	private final ObjectMapper mapper;
	private final ClientAccessTokenDao clientAccessTokenDao;

	/**
	 * An optional cache of locks to synchronize access token requests per
	 * configuration.
	 *
	 * <p>
	 * This cache is assumed to have read-through semantics, in that any request
	 * for a key not already present in the map will return a new lock instance.
	 * </p>
	 */
	private final OptionalService<Cache<UserLongCompositePK, Lock>> locksCache;

	/**
	 * Constructor.
	 *
	 * @param log
	 *        the logger
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param restOps
	 *        the REST operations
	 * @param errorEventTags
	 *        the error event tags
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param sensitiveKeyProvider
	 *        the sensitive key provider
	 * @param clock
	 *        the clock to use
	 * @param mapper
	 *        the mapper to use
	 * @param clientAccessTokenDao
	 *        the client access token DAO
	 * @param locksCache
	 *        optional integration locks cache
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SigenergyRestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, List<String> errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider, InstantSource clock, ObjectMapper mapper,
			ClientAccessTokenDao clientAccessTokenDao,
			OptionalService<Cache<UserLongCompositePK, Lock>> locksCache) {
		super(log, userEventAppenderBiz, restOps, errorEventTags, encryptor, sensitiveKeyProvider);
		this.clock = requireNonNullArgument(clock, "clock");
		this.mapper = requireNonNullArgument(mapper, "mapper");
		this.clientAccessTokenDao = requireNonNullArgument(clientAccessTokenDao, "clientAccessTokenDao");
		this.locksCache = requireNonNullArgument(locksCache, "locksCache");
	}

	@Override
	public <B, R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T http(
			String description, HttpMethod method, B body, C configuration, Class<R> responseType,
			Function<HttpHeaders, URI> setup, Function<ResponseEntity<R>, T> handler) {
		return super.http(description, method, body, configuration, responseType, (headers) -> {
			if ( configuration instanceof CloudIntegrationConfiguration c
					&& c.hasServiceProperty(APP_KEY_SETTING) ) {
				addSigenergyBearerAuthorization(c, headers);
			}
			return setup.apply(headers);
		}, handler);
	}

	/**
	 * Get an access token primary key for an integration configuration.
	 *
	 * @param config
	 *        the configuration
	 * @return the access token ID, or {@code null} if no
	 *         {@link SigenergyCloudIntegrationService.APP_KEY_SETTING} setting
	 *         is available
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserStringStringCompositePK accessTokenId(final CloudIntegrationConfiguration config) {
		requireNonNullArgument(config, "config");

		final String appKey = nonEmptyString(config.serviceProperty(APP_KEY_SETTING, String.class));
		if ( appKey == null ) {
			return null;
		}

		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());

		return new UserStringStringCompositePK(config.getUserId(), registrationId, appKey);
	}

	/**
	 * Add appropriate Sigenergy bearer authorization header value to a given
	 * {@link HttpHeaders}.
	 *
	 * @param config
	 *        the configuration to authorize
	 * @param headers
	 *        the headers to add authorization to
	 * @throws RemoteServiceException
	 *         if authorization fails
	 */
	public void addSigenergyBearerAuthorization(final CloudIntegrationConfiguration config,
			final HttpHeaders headers) {
		final Cache<UserLongCompositePK, Lock> lockCache = service(locksCache);
		final Lock lock = (lockCache != null ? lockCache.get(config.getId()) : null);
		try {
			if ( lock != null ) {
				lock.lock();
			}
			addSigenergyBearerAuthorizationInternal(config, headers);
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

	/**
	 * Resolve a Sigenergy region key.
	 *
	 * @param config
	 *        the configuration to resolve the region key from
	 * @return the region (never {@code null})
	 */
	public static SigenergyRegion resolveRegion(final CloudIntegrationConfiguration config) {
		SigenergyRegion result = SigenergyRegion.AustraliaNewZealand;
		if ( config.hasServiceProperty(REGION_SETTING) ) {
			try {
				result = SigenergyRegion.valueOf(config.serviceProperty(REGION_SETTING, String.class));
			} catch ( Exception e ) {
				// ignore
			}
		}
		return result;
	}

	private void addSigenergyBearerAuthorizationInternal(final CloudIntegrationConfiguration config,
			final HttpHeaders headers) {
		final UserStringStringCompositePK accessTokenId = accessTokenId(config);
		if ( accessTokenId == null ) {
			return;
		}

		ClientAccessTokenEntity registration = clientAccessTokenDao.get(accessTokenId);
		if ( registration != null && !registration.accessTokenExpired(clock) ) {
			headers.setBearerAuth(registration.getAccessTokenValue());
			return;
		}

		final var decrypted = config.copyWithId(config.getId());
		decrypted.unmaskSensitiveInformation(sensitiveKeyProvider, encryptor);

		final String appKey = nonEmptyString(decrypted.serviceProperty(APP_KEY_SETTING, String.class));
		if ( appKey == null ) {
			return;
		}

		final String appSecret = nonEmptyString(
				decrypted.serviceProperty(APP_SECRET_SETTING, String.class));
		if ( appSecret == null ) {
			return;
		}

		final SigenergyRegion region = resolveRegion(decrypted);

		final URI tokenRequestUri = fromUriString(resolveBaseUrl(config, BASE_URI_TEMPLATE))
				.path(KEY_TOKEN_REQUEST_PATH).buildAndExpand(region.getKey()).toUri();

		final JsonNode res = restOps.postForObject(tokenRequestUri,
				getTreeFromObject(Map.of("key", encodeAuthKey(appKey, appSecret))), JsonNode.class);

		requireSuccessResponse("Authenticate", tokenRequestUri, res);

		registration = parseToken(res, accessTokenId);

		if ( registration == null ) {
			throw new RemoteServiceException("Access token not available in authentication response.");
		}

		clientAccessTokenDao.save(registration);
		headers.setBearerAuth(registration.getAccessTokenValue());
	}

	/**
	 * Encode an application key and secret into an authentication key value.
	 *
	 * @param appKey
	 *        the application key
	 * @param appSecret
	 *        the application secret
	 * @return the authentication key
	 */
	public static String encodeAuthKey(String appKey, String appSecret) {
		return Base64.getEncoder().withoutPadding()
				.encodeToString((appKey + ':' + appSecret).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Parse token details from a login response.
	 *
	 * <p>
	 * Example JSON:
	 * </p>
	 *
	 * <pre>{@code {
	 *   "timestamp":123456789,
	 *   "data": "{\"tokenType\":\"Bearer\", \"accessToken\":\"...\", \"expiresIn\":43199}"
	 * }
	 * }</pre>
	 *
	 * @param res
	 *        the login response
	 * @param id
	 *        the token ID
	 * @return the parsed entity, or {@code null} if one could not be parsed
	 */
	private ClientAccessTokenEntity parseToken(JsonNode res, UserStringStringCompositePK id) {
		final Instant timestamp = Instant.ofEpochSecond(res.path("timestamp").longValue());
		final JsonNode data = jsonObjectOrArray(mapper, res, RESPONSE_DATA_FIELD);
		final String tokenType = nonEmptyString(data.path("tokenType").textValue());
		final String token = nonEmptyString(data.path("accessToken").textValue());
		final int expiresInSeconds = data.path("expiresIn").intValue();
		if ( tokenType != null && token != null && expiresInSeconds > 0 ) {
			ClientAccessTokenEntity result = new ClientAccessTokenEntity(id, clock.instant());
			result.setAccessTokenIssuedAt(timestamp);
			result.setAccessTokenType(tokenType);
			result.setAccessToken(token.getBytes(UTF_8));
			result.setEnabled(true);
			result.setAccessTokenExpiresAt(timestamp.plusSeconds(expiresInSeconds));
			return result;
		}
		return null;
	}

	/**
	 * Get a JSON object or array from a field on a JSON object.
	 *
	 * <p>
	 * The Sigenergy API returns some nested fields as JSON strings, which must
	 * be decoded.
	 * </p>
	 *
	 * @param mapper
	 *        the mapper to decode JSON strings with
	 * @param json
	 *        the object to extract a JSON field from
	 * @param field
	 *        the name of the JSON field to extract as JSON
	 * @return the JSON tree
	 */
	public static JsonNode jsonObjectOrArray(ObjectMapper mapper, JsonNode json, String field) {
		if ( json == null ) {
			return null;
		}
		JsonNode data = json.path(field);
		if ( data.isObject() || data.isArray() || data.isNull() ) {
			return data;
		}
		// decode as a JSON string
		try {
			return mapper.readTree(data.asText());
		} catch ( JsonProcessingException e ) {
			throw new IllegalArgumentException("Unable to parse field [%s] as JSON", e);
		}
	}

	/**
	 * Verify that a JSON response contains the "success" message, otherwise
	 * throw an exception.
	 *
	 * @param action
	 *        an optional action to use in error messages
	 * @param uri
	 *        the URI to use in error messages
	 * @param response
	 *        the response to test ({@code null} is allowed}
	 * @return {@code true} if the response contains the success message
	 * @throws RemoteServiceException
	 *         if the response does not include a success message
	 */
	public static void requireSuccessResponse(String action, URI uri, JsonNode response) {
		if ( response != null && RESPONSE_SUCCESS_MESSAGE.equals(response.path("msg").asText()) ) {
			return;
		}

		final String code = nonEmptyString(response != null ? response.path("code").asText() : null);
		final String msg = nonEmptyString(response != null ? response.path("msg").asText() : null);

		final StringBuilder buf = new StringBuilder();
		if ( action != null ) {
			buf.append(action);
		} else {
			buf.append("Request");
		}
		buf.append(" failed");
		if ( uri != null ) {
			buf.append(" at [").append(uri).append("]");
		}
		if ( code != null ) {
			buf.append("; code ").append(code);
		}
		if ( msg != null ) {
			buf.append(": ").append(msg);
		}

		throw new RemoteServiceException(buf.toString());
	}

}
