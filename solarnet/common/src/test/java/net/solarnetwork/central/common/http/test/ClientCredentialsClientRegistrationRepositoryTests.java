/* ==================================================================
 * ClientCredentialsClientRegistrationRepositoryTests.java - 3/12/2025 7:33:28 am
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

package net.solarnetwork.central.common.http.test;

import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.Map;
import java.util.function.BiFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import net.solarnetwork.central.common.dao.UserServiceConfigurationDao;
import net.solarnetwork.central.common.http.ClientCredentialsClientRegistrationRepository;
import net.solarnetwork.central.common.http.HttpConstants;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Test cases for the {@link ClientCredentialsClientRegistrationRepository}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class ClientCredentialsClientRegistrationRepositoryTests {

	@Mock
	private UserServiceConfigurationDao<UserLongCompositePK> configurationDao;

	@Mock
	private BiFunction<UserLongCompositePK, String, String> secretResolver;

	private ClientCredentialsClientRegistrationRepository repo;

	@BeforeEach
	public void setup() {
		repo = new ClientCredentialsClientRegistrationRepository(configurationDao, secretResolver);
	}

	@Test
	public void unsupportedRegistrationId() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();

		final String registrationId = userIdSystemIdentifier(userId, srvc);

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			repo.findByRegistrationId(registrationId);
		}, "Exception thrown when registration ID has only one Long component")
			.as("Exception is Not Found")
			.isInstanceOf(EmptyResultDataAccessException.class)
			;

		// THEN
		then(configurationDao).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void missingConfiguration() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();
		final Long configId = randomLong();

		final UserLongCompositePK id = new UserLongCompositePK(userId, configId);
		final String registrationId = userIdSystemIdentifier(userId, srvc, configId);

		given(configurationDao.serviceConfiguration(eq(id), same(secretResolver))).willReturn(null);

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			repo.findByRegistrationId(registrationId);
		}, "Exception thrown when configuration not found in DAO")
			.as("Exception is Not Found")
			.isInstanceOf(EmptyResultDataAccessException.class)
			;
		// @formatter:on
	}

	@Test
	public void missingTokenUri() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();
		final Long configId = randomLong();

		final UserLongCompositePK id = new UserLongCompositePK(userId, configId);
		final String registrationId = userIdSystemIdentifier(userId, srvc, configId);

		final String clientId = randomString();
		final String clientSecret = randomString();

		// @formatter:off
		final Map<String, Object> config = Map.of(
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId,
				HttpConstants.OAUTH_CLIENT_SECRET_SETTING, clientSecret
				);
		// @formatter:on
		given(configurationDao.serviceConfiguration(eq(id), same(secretResolver))).willReturn(config);

		// WHEN
		// @formatter:off
		thenThrownBy(() -> {
			repo.findByRegistrationId(registrationId);
		}, "Exception thrown when token URL not found in configuration")
			.as("Exception is Not Found")
			.isInstanceOf(EmptyResultDataAccessException.class)
			;
		// @formatter:on
	}

	@Test
	public void find_clientCreds_basic() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();
		final Long configId = randomLong();

		final UserLongCompositePK id = new UserLongCompositePK(userId, configId);
		final String registrationId = userIdSystemIdentifier(userId, srvc, configId);

		final String tokenUrl = "http://localhost/" + randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();

		// @formatter:off
		final Map<String, Object> config = Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, tokenUrl,
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId,
				HttpConstants.OAUTH_CLIENT_SECRET_SETTING, clientSecret
				);
		// @formatter:on
		given(configurationDao.serviceConfiguration(eq(id), same(secretResolver))).willReturn(config);

		// WHEN
		ClientRegistration result = repo.findByRegistrationId(registrationId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Client credentials grant type resolved")
			.returns(AuthorizationGrantType.CLIENT_CREDENTIALS, from(ClientRegistration::getAuthorizationGrantType))
			.as("HTTP basic authentication resolved")
			.returns(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, from(ClientRegistration::getClientAuthenticationMethod))
			.as("Client ID from configuration")
			.returns(clientId, from(ClientRegistration::getClientId))
			.as("Client secret from configuration")
			.returns(clientSecret, from(ClientRegistration::getClientSecret))
			.as("Registration ID as provided")
			.returns(registrationId, from(ClientRegistration::getRegistrationId))
			.as("Provider details populated")
			.satisfies(reg -> {
				and.then(reg.getProviderDetails())
					.as("Token URI from configuration")
					.returns(tokenUrl, from(ProviderDetails::getTokenUri))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void find_clientCreds_form() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();
		final Long configId = randomLong();

		final UserLongCompositePK id = new UserLongCompositePK(userId, configId);
		final String registrationId = userIdSystemIdentifier(userId, srvc, configId);

		final String tokenUrl = "http://localhost/" + randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();

		// @formatter:off
		final Map<String, Object> config = Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, tokenUrl,
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId,
				HttpConstants.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				HttpConstants.OAUTH_AUTHENTICATION_METHOD_SETTING, "ClientSecretForm"
				);
		// @formatter:on
		given(configurationDao.serviceConfiguration(eq(id), same(secretResolver))).willReturn(config);

		// WHEN
		ClientRegistration result = repo.findByRegistrationId(registrationId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Client credentials grant type resolved")
			.returns(AuthorizationGrantType.CLIENT_CREDENTIALS, from(ClientRegistration::getAuthorizationGrantType))
			.as("POST authentication resolved")
			.returns(ClientAuthenticationMethod.CLIENT_SECRET_POST, from(ClientRegistration::getClientAuthenticationMethod))
			.as("Client ID from configuration")
			.returns(clientId, from(ClientRegistration::getClientId))
			.as("Client secret from configuration")
			.returns(clientSecret, from(ClientRegistration::getClientSecret))
			.as("Registration ID as provided")
			.returns(registrationId, from(ClientRegistration::getRegistrationId))
			.as("Provider details populated")
			.satisfies(reg -> {
				and.then(reg.getProviderDetails())
					.as("Token URI from configuration")
					.returns(tokenUrl, from(ProviderDetails::getTokenUri))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("removal")
	@Test
	public void find_username_basic() {
		// GIVEN
		final Long userId = randomLong();
		final String srvc = randomString();
		final Long configId = randomLong();

		final UserLongCompositePK id = new UserLongCompositePK(userId, configId);
		final String registrationId = userIdSystemIdentifier(userId, srvc, configId);

		final String tokenUrl = "http://localhost/" + randomString();
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();

		// @formatter:off
		final Map<String, Object> config = Map.of(
				HttpConstants.OAUTH_TOKEN_URL_SETTING, tokenUrl,
				HttpConstants.OAUTH_CLIENT_ID_SETTING, clientId,
				HttpConstants.USERNAME_SETTING, username,
				HttpConstants.PASSWORD_SETTING, password
				);
		// @formatter:on
		given(configurationDao.serviceConfiguration(eq(id), same(secretResolver))).willReturn(config);

		// WHEN
		ClientRegistration result = repo.findByRegistrationId(registrationId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Client credentials grant type resolved")
			.returns(AuthorizationGrantType.PASSWORD, from(ClientRegistration::getAuthorizationGrantType))
			.as("HTTP basic authentication resolved")
			.returns(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, from(ClientRegistration::getClientAuthenticationMethod))
			.as("Client ID from configuration")
			.returns(clientId, from(ClientRegistration::getClientId))
			.as("Client secret not available")
			.returns("", from(ClientRegistration::getClientSecret))
			.as("Registration ID as provided")
			.returns(registrationId, from(ClientRegistration::getRegistrationId))
			.as("Provider details populated")
			.satisfies(reg -> {
				and.then(reg.getProviderDetails())
					.as("Token URI from configuration")
					.returns(tokenUrl, from(ProviderDetails::getTokenUri))
					;
			})
			;
		// @formatter:on
	}

}
