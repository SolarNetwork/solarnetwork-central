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

package net.solarnetwork.central.c2c.http;

import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.PASSWORD_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.USERNAME_SETTING;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.systemIdentifierLongComponents;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.GenericDao;

/**
 * Repository of OAuth client registrations based on external system
 * configuration, using the OAuth Client Credentials or Resource Owner Password
 * flows.
 *
 * <p>
 * The {@code registrationId} passed to {@link #findByRegistrationId(String)}
 * must be in the form as returned by
 * {@link CloudIntegrationConfiguration#systemIdentifier()}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class ClientCredentialsClientRegistrationRepository implements ClientRegistrationRepository {

	private final GenericDao<CloudIntegrationConfiguration, UserLongCompositePK> configurationDao;
	private final URI tokenUri;
	private final ClientAuthenticationMethod clientAuthMethod;

	/**
	 * Constructor.
	 *
	 * @param configurationDao
	 *        the configuration DAO
	 * @param tokenUri
	 *        the OAuth token URL
	 * @param clientAuthMethod
	 *        the OAuth client authentication method
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ClientCredentialsClientRegistrationRepository(
			CloudIntegrationConfigurationDao configurationDao, URI tokenUri,
			ClientAuthenticationMethod clientAuthMethod) {
		super();
		this.configurationDao = requireNonNullArgument(configurationDao, "configurationDao");
		this.tokenUri = requireNonNullArgument(tokenUri, "tokenUri");
		this.clientAuthMethod = requireNonNullArgument(clientAuthMethod, "clientAuthMethod");
	}

	@SuppressWarnings("deprecation")
	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		final List<Long> ids = systemIdentifierLongComponents(registrationId, true);
		if ( ids == null || ids.size() < 2 ) {
			throw new EmptyResultDataAccessException(
					"Configuration for registration ID %s not supported.".formatted(registrationId), 1);
		}
		CloudIntegrationConfiguration conf = configurationDao
				.get(new UserLongCompositePK(ids.get(0), ids.get(1)));
		if ( conf == null ) {
			throw new EmptyResultDataAccessException(
					"Configuration for registration ID %s not found.".formatted(registrationId), 1);
		}

		final String clientId = conf.serviceProperty(CLIENT_ID_SETTING, String.class);
		final String clientSecret = conf.serviceProperty(CLIENT_SECRET_SETTING, String.class);

		final String username = conf.serviceProperty(USERNAME_SETTING, String.class);
		final String password = conf.serviceProperty(PASSWORD_SETTING, String.class);

		// @formatter:off
		ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri(tokenUri.toString())
				.clientAuthenticationMethod(clientAuthMethod)
				;
		// @formatter:on

		if ( clientId != null && !clientId.isEmpty() && clientSecret != null
				&& !clientSecret.isEmpty() ) {
			builder.clientId(clientId).clientSecret(clientSecret);
		}

		if ( username != null && !username.isEmpty() && password != null
				&& !password.toString().isEmpty() ) {
			builder.authorizationGrantType(AuthorizationGrantType.PASSWORD);
		} else {
			builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
		}

		return builder.build();
	}

}
