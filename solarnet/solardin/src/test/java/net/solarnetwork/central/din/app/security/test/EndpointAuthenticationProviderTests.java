/* ==================================================================
 * EndpointAuthenticationProviderTests.java - 23/02/2024 3:59:21 pm
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

package net.solarnetwork.central.din.app.security.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import net.solarnetwork.central.din.app.security.EndpointAuthenticationDetails;
import net.solarnetwork.central.din.app.security.DatumEndpointAuthenticationProvider;
import net.solarnetwork.central.din.security.AuthenticatedEndpointCredentials;
import net.solarnetwork.central.din.security.CredentialAuthorizationDao;

/**
 * Test cases for the {@link DatumEndpointAuthenticationProvider} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class EndpointAuthenticationProviderTests {

	@Mock
	private CredentialAuthorizationDao authDao;

	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private DatumEndpointAuthenticationProvider provider;

	@BeforeEach
	public void setup() {
		provider = new DatumEndpointAuthenticationProvider(authDao, passwordEncoder);
	}

	@Test
	public void supports_upt() {
		boolean result = provider.supports(UsernamePasswordAuthenticationToken.class);
		then(result).as("UserPassword token supported").isTrue();
	}

	@Test
	public void auth_noDetails() {
		// GIVEN
		var auth = UsernamePasswordAuthenticationToken.unauthenticated("user", "pass");

		// WHEN
		Authentication result = provider.authenticate(auth);

		// THEN
		then(result).as("Not authenticated because no UserDetails provided").isNull();
	}

	@Test
	public void auth_notEndpointDetails() {
		// GIVEN
		var auth = UsernamePasswordAuthenticationToken.unauthenticated("user", "pass");
		var authDetails = new WebAuthenticationDetails("127.0.0.1", null);
		auth.setDetails(authDetails);

		// WHEN
		Authentication result = provider.authenticate(auth);

		// THEN
		then(result).as("Not authenticated because UserDetails not EndpointAuthenticationDetails")
				.isNull();
	}

	@Test
	public void auth_ok() {
		// GIVEN
		final Long userId = randomLong();
		final UUID endpointId = UUID.randomUUID();
		final String username = randomString();
		final String password = randomString();
		final String passwordEncoded = passwordEncoder.encode(password);

		var auth = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
		var authDetails = new EndpointAuthenticationDetails("127.0.0.1", null, userId, endpointId);
		auth.setDetails(authDetails);

		AuthenticatedEndpointCredentials details = new AuthenticatedEndpointCredentials(userId,
				endpointId, username, passwordEncoded, true, false);
		given(authDao.credentialsForEndpoint(endpointId, username)).willReturn(details);

		// WHEN
		Authentication result = provider.authenticate(auth);

		// THEN
		// @formatter:off
		then(result)
			.as("Authenticated ")
			.isNotNull()
			.as("Username from input")
			.returns(username, Authentication::getPrincipal)
			.as("Password cleared")
			.returns(null, Authentication::getCredentials)
			.extracting(Authentication::getDetails)
			.as("UserDetails is AuthenticatedEndpointCredentials from DAO")
			.isSameAs(details)
			.asInstanceOf(InstanceOfAssertFactories.type(AuthenticatedEndpointCredentials.class))
			.as("UserDetails password cleared")
			.returns(null, AuthenticatedEndpointCredentials::getPassword)
			;
		// @formatter:on
	}

	@Test
	public void auth_badPassword() {
		// GIVEN
		final Long userId = randomLong();
		final UUID endpointId = UUID.randomUUID();
		final String username = randomString();
		final String password = randomString();
		final String passwordEncoded = passwordEncoder.encode(password);

		var auth = UsernamePasswordAuthenticationToken.unauthenticated(username, "bad.password");
		var authDetails = new EndpointAuthenticationDetails("127.0.0.1", null, userId, endpointId);
		auth.setDetails(authDetails);

		AuthenticatedEndpointCredentials details = new AuthenticatedEndpointCredentials(userId,
				endpointId, username, passwordEncoded, true, false);
		given(authDao.credentialsForEndpoint(endpointId, username)).willReturn(details);

		// WHEN
		BadCredentialsException result = catchThrowableOfType(() -> provider.authenticate(auth),
				BadCredentialsException.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Bad password results in exception")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void auth_userNotFound() {
		// GIVEN
		final Long userId = randomLong();
		final UUID endpointId = UUID.randomUUID();
		final String username = randomString();

		var auth = UsernamePasswordAuthenticationToken.unauthenticated(username, "bad.password");
		var authDetails = new EndpointAuthenticationDetails("127.0.0.1", null, userId, endpointId);
		auth.setDetails(authDetails);

		given(authDao.credentialsForEndpoint(endpointId, username)).willReturn(null);

		// WHEN
		BadCredentialsException result = catchThrowableOfType(() -> provider.authenticate(auth),
				BadCredentialsException.class);

		// THEN
		// @formatter:off
		then(result)
			.as("User not found results in exception")
			.isNotNull()
			;
		// @formatter:on
	}

}
