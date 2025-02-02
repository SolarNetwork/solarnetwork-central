/* ==================================================================
 * EgaugeRestOperationsHelper.java - 25/10/2024 1:06:18 pm
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.PASSWORD_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.USERNAME_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeCloudDatumStreamService.DEVICE_ID_FILTER;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeCloudIntegrationService.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.net.URI;
import java.time.Duration;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.service.RemoteServiceException;

/**
 * eGauge REST operations helper.
 *
 * @author matt
 * @version 1.1
 */
public class EgaugeRestOperationsHelper extends RestOperationsHelper {

	/**
	 * A system identifier component to use for access token registration IDs.
	 */
	public static final String CLOUD_INTEGRATION_SYSTEM_IDENTIFIER = "c2c-ds-egauge";

	/**
	 * The API URL path to get an authorization nonce.
	 *
	 * @since 1.1
	 */
	public static final String AUTH_UNAUTHORIZED_PATH = "/api/auth/unauthorized";

	/**
	 * The API URL path to login.
	 *
	 * @since 1.1
	 */
	public static final String AUTH_LOGIN_PATH = "/api/auth/login";

	// use hard-coded access token TTL that eGauge appears to use
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(10);

	private static final String REALM_PROPERTY = "rlm";
	private static final String NONCE_PROPERTY = "nnc";
	private static final String CLIENT_NONCE_PROPERTY = "cnnc";
	private static final String USERNAME_PROPERTY = "usr";
	private static final String HASH_PROPERTY = "hash";
	private static final String JWT_PROPERTY = "jwt";

	private final InstantSource clock;
	private final RandomGenerator rng;
	private final ClientAccessTokenDao clientAccessTokenDao;
	private final CloudIntegrationConfigurationDao integrationDao;

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
	 * @param rng
	 *        the random generator to use
	 * @param clientAccessTokenDao
	 *        the client access token DAO
	 * @param integrationDao
	 *        the integration DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EgaugeRestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, String[] errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider, InstantSource clock, RandomGenerator rng,
			ClientAccessTokenDao clientAccessTokenDao, CloudIntegrationConfigurationDao integrationDao) {
		super(log, userEventAppenderBiz, restOps, errorEventTags, encryptor, sensitiveKeyProvider);
		this.clock = requireNonNullArgument(clock, "clock");
		this.rng = requireNonNullArgument(rng, "rng");
		this.clientAccessTokenDao = requireNonNullArgument(clientAccessTokenDao, "clientAccessTokenDao");
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
	}

	@Override
	public <R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T httpGet(
			String description, C configuration, Class<R> responseType, Function<HttpHeaders, URI> setup,
			Function<ResponseEntity<R>, T> handler) {
		try {
			return super.httpGet(description, configuration, responseType, (headers) -> {
				if ( configuration instanceof CloudDatumStreamConfiguration c ) {
					addEgaugeBearerAuthorization(c, headers);
				}
				return setup.apply(headers);
			}, handler);
		} catch ( RemoteServiceException e ) {
			// perhaps the access token has expired; let's try again with a new one if we got a 4xx response
			if ( configuration instanceof CloudDatumStreamConfiguration datumStream
					&& e.getCause() instanceof HttpClientErrorException ex
					&& HttpStatus.UNAUTHORIZED.isSameCodeAs(ex.getStatusCode()) ) {
				final UserStringStringCompositePK accessTokenId = accessTokenId(datumStream);
				ClientAccessTokenEntity registration = clientAccessTokenDao.get(accessTokenId);
				if ( registration != null ) {
					clientAccessTokenDao.delete(registration);
					JsonNode realm = ex.getResponseBodyAs(JsonNode.class);
					return super.httpGet(description, configuration, responseType, (headers) -> {
						var integration = integrationDao.integrationForDatumStream(datumStream.getId());
						addEgaugeBearerAuthorization(integration, datumStream, headers, accessTokenId,
								realm);
						return setup.apply(headers);
					}, handler);
				}
			}
			throw e;
		}
	}

	private UserStringStringCompositePK accessTokenId(CloudDatumStreamConfiguration config) {
		final String username = nonEmptyString(config.serviceProperty(USERNAME_SETTING, String.class));
		final String deviceId = nonEmptyString(config.serviceProperty(DEVICE_ID_FILTER, String.class));
		if ( username == null || deviceId == null ) {
			return null;
		}

		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, deviceId);

		return new UserStringStringCompositePK(config.getUserId(), registrationId, username);
	}

	/**
	 * Add appropriate OAuth authorization header values to a given
	 * {@link HttpHeaders}.
	 *
	 * <p>
	 * If the {@link CloudIntegrationService#USERNAME_SETTING} and
	 * {@link CloudIntegrationService#PASSWORD_SETTING} service properties are
	 * available, they will be used to create a
	 * {@link UsernamePasswordAuthenticationToken} principal. Otherwise, a
	 * string will be created like {@code "I N"} where {@code I} is the
	 * configuration's ID's identifier and {@code N} is the configuration name.
	 * </p>
	 *
	 * @param config
	 *        the configuration to authorize
	 * @param headers
	 *        the headers to add authorization to
	 * @throws RemoteServiceException
	 *         if authorization fails
	 */
	public void addEgaugeBearerAuthorization(CloudDatumStreamConfiguration config, HttpHeaders headers) {
		final UserStringStringCompositePK accessTokenId = accessTokenId(config);

		ClientAccessTokenEntity registration = clientAccessTokenDao.get(accessTokenId);
		if ( registration != null && !registration.accessTokenExpired(clock) ) {
			headers.setBearerAuth(registration.getAccessTokenValue());
			return;
		}

		final CloudIntegrationConfiguration integration = integrationDao
				.integrationForDatumStream(config.getId());

		final String deviceId = nonEmptyString(config.serviceProperty(DEVICE_ID_FILTER, String.class));
		JsonNode realm;
		// get nonce data... we expect a 401 response here
		try {
			realm = restOps.getForObject(
					fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
							.path(AUTH_UNAUTHORIZED_PATH).buildAndExpand(deviceId).toUri(),
					JsonNode.class);
		} catch ( HttpClientErrorException e ) {
			if ( e.getStatusCode().is4xxClientError() ) {
				realm = e.getResponseBodyAs(JsonNode.class);
			} else {
				throw e;
			}
		}
		addEgaugeBearerAuthorization(integration, config, headers, accessTokenId, realm);
	}

	private void addEgaugeBearerAuthorization(CloudIntegrationConfiguration integration,
			CloudDatumStreamConfiguration config, HttpHeaders headers,
			UserStringStringCompositePK accessTokenId, JsonNode realm) {
		requireNonNullArgument(realm, "realm");
		final String username = nonEmptyString(config.serviceProperty(USERNAME_SETTING, String.class));
		final String deviceId = nonEmptyString(config.serviceProperty(DEVICE_ID_FILTER, String.class));
		if ( username == null || deviceId == null ) {
			return;
		}

		// get new token
		var decrypted = config.clone();
		decrypted.unmaskSensitiveInformation(sensitiveKeyProvider, encryptor);
		final String password = nonEmptyString(
				decrypted.serviceProperty(PASSWORD_SETTING, String.class));
		if ( password == null ) {
			return;
		}

		final String rlm = realm.path(REALM_PROPERTY).asText();
		final String nnc = realm.path(NONCE_PROPERTY).asText();
		final byte[] cnncBytes = new byte[16];
		rng.nextBytes(cnncBytes);
		final String cnnc = HexFormat.of().formatHex(cnncBytes);
		final String hash = md5Hex(
				"%s:%s:%s".formatted(md5Hex("%s:%s:%s".formatted(username, rlm, password)), nnc, cnnc));

		// get access token
		var authReqTime = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		var authHeaders = new HttpHeaders();
		authHeaders.setContentType(MediaType.APPLICATION_JSON);
		var authBody = new HttpEntity<>(Map.of(REALM_PROPERTY, rlm, NONCE_PROPERTY, nnc,
				CLIENT_NONCE_PROPERTY, cnnc, USERNAME_PROPERTY, username, HASH_PROPERTY, hash),
				authHeaders);
		final JsonNode tokenRes = restOps
				.postForObject(
						fromUriString(resolveBaseUrl(integration, BASE_URI_TEMPLATE))
								.path(AUTH_LOGIN_PATH).buildAndExpand(deviceId).toUri(),
						authBody, JsonNode.class);

		final String accessTokenValue = nonEmptyString(
				tokenRes != null ? tokenRes.path(JWT_PROPERTY).asText() : null);
		if ( accessTokenValue != null ) {
			var registration = new ClientAccessTokenEntity(accessTokenId, clock.instant());
			registration.setAccessTokenIssuedAt(authReqTime);
			registration.setAccessTokenType("Bearer");
			registration.setAccessToken(accessTokenValue.getBytes(UTF_8));
			registration.setAccessTokenExpiresAt(authReqTime.plus(ACCESS_TOKEN_TTL));
			clientAccessTokenDao.save(registration);
			headers.setBearerAuth(accessTokenValue);
		}
	}

}
