/* ==================================================================
 * OAuth2UtilsTests.java - 3/10/2024 11:28:16 am
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

package net.solarnetwork.central.c2c.http.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import net.solarnetwork.central.c2c.http.OAuth2Utils;

/**
 * Test cases for the {@link OAuth2Utils} class.
 *
 * @author matt
 * @version 1.0
 */
public class OAuth2UtilsTests {

	@Test
	public void principalCredentialsContextAttributes_typical() {
		// GIVEN
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("foo", "bar");

		OAuth2AuthorizeRequest req = OAuth2AuthorizeRequest.withClientRegistrationId("test")
				.principal(auth).build();

		// WHEN
		Map<String, Object> result = OAuth2Utils.principalCredentialsContextAttributes(req);

		// THEN
		// @formatter:off
		then(result)
			.as("Result provided with expected attribute count")
			.hasSize(2)
			.as("Username attribute populated from Authentication principal")
			.containsEntry(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, auth.getPrincipal())
			.as("Password attribute populated from Authentication credentials")
			.containsEntry(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, auth.getCredentials())
			;
		// @formatter:on
	}

}
