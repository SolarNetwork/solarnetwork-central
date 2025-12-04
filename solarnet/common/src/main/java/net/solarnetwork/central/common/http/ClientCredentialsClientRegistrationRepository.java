/* ==================================================================
 * ClientCredentialsClientRegistrationRepository.java - 2/10/2024 2:13:24 pm
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

package net.solarnetwork.central.common.http;

import static net.solarnetwork.central.common.http.HttpConstants.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.OAUTH_TOKEN_URL_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.PASSWORD_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.USERNAME_SETTING;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.systemIdentifierLongComponents;
import static net.solarnetwork.util.CollectionUtils.getMapString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import net.solarnetwork.central.common.dao.UserServiceConfigurationDao;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Repository of OAuth client registrations based on external system
 * configuration, using the OAuth Client Credentials or Resource Owner Password
 * flows.
 *
 * <p>
 * The {@code registrationId} passed to {@link #findByRegistrationId(String)}
 * must be in a form parsable by
 * {@link net.solarnetwork.central.domain.UserIdentifiableSystem#systemIdentifier()}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ClientCredentialsClientRegistrationRepository implements ClientRegistrationRepository {

	private final UserServiceConfigurationDao<UserLongCompositePK> configurationDao;
	private final BiFunction<UserLongCompositePK, String, String> secretResolver;

	/**
	 * Constructor.
	 *
	 * @param configurationDao
	 *        the configuration DAO
	 * @param secretResolver
	 *        function to decrypt values with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ClientCredentialsClientRegistrationRepository(
			UserServiceConfigurationDao<UserLongCompositePK> configurationDao,
			BiFunction<UserLongCompositePK, String, String> secretResolver) {
		super();
		this.configurationDao = requireNonNullArgument(configurationDao, "configurationDao");
		this.secretResolver = requireNonNullArgument(secretResolver, "secretResolver");
	}

	@SuppressWarnings("removal")
	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		final List<Long> ids = systemIdentifierLongComponents(registrationId, true);
		if ( ids == null || ids.size() < 2 ) {
			throw new EmptyResultDataAccessException(
					"Configuration for registration ID %s not supported.".formatted(registrationId), 1);
		}
		final var id = new UserLongCompositePK(ids.get(0), ids.get(1));
		final Map<String, Object> conf = configurationDao.serviceConfiguration(id, secretResolver);
		if ( conf == null ) {
			throw new EmptyResultDataAccessException(
					"Configuration for registration ID %s not found.".formatted(registrationId), 1);
		}

		final String tokenUri = nonEmptyString(getMapString(OAUTH_TOKEN_URL_SETTING, conf));
		if ( tokenUri == null ) {
			throw new EmptyResultDataAccessException(
					"Configuration for registration ID %s missing %s setting.".formatted(registrationId,
							OAUTH_TOKEN_URL_SETTING),
					1);
		}

		final String clientId = nonEmptyString(getMapString(OAUTH_CLIENT_ID_SETTING, conf));
		final String clientSecret = nonEmptyString(getMapString(OAUTH_CLIENT_SECRET_SETTING, conf));
		final String authMethod = nonEmptyString(
				getMapString(HttpConstants.OAUTH_AUTHENTICATION_METHOD_SETTING, conf));

		final String username = nonEmptyString(getMapString(USERNAME_SETTING, conf));
		final String password = nonEmptyString(getMapString(PASSWORD_SETTING, conf));

		// @formatter:off
		ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri(tokenUri)
				.clientAuthenticationMethod(clientAuthMethod(authMethod))
				;
		// @formatter:on

		if ( clientId != null ) {
			builder.clientId(clientId);
		}
		if ( clientSecret != null ) {
			builder.clientSecret(clientSecret);
		}

		if ( username != null && password != null ) {
			builder.authorizationGrantType(AuthorizationGrantType.PASSWORD);
		} else {
			builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
		}

		return builder.build();
	}

	private ClientAuthenticationMethod clientAuthMethod(String authMethod) {
		OAuth2AuthenticationMethod method = OAuth2AuthenticationMethod.ClientSecretBasic;
		if ( authMethod != null ) {
			try {
				method = OAuth2AuthenticationMethod.valueOf(authMethod);
			} catch ( Exception e ) {
				// ignore and use default
			}
		}
		return switch (method) {
			case ClientSecretBasic -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
			case ClientSecretForm -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
			case ClientSecretJwt -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
			case None -> ClientAuthenticationMethod.NONE;
		};
	}

}
