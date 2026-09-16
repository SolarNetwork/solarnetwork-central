/* ==================================================================
 * GeneratedUserAuthTokenTests.java - 16/09/2026 11:05:33 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.domain.GeneratedUserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Test cases for the {@link GeneratedUserAuthToken} class.
 *
 * @author matt
 * @version 1.0
 */
public class GeneratedUserAuthTokenTests {

	private static final String TEST_TOKEN_ID = "12345678901234567890";
	private static final Long TEST_USER_ID = 1L;
	private static final String TEST_SECRET = "s3cr3t";

	private static UserAuthToken testToken() {
		return new UserAuthToken(TEST_TOKEN_ID, TEST_USER_ID, TEST_SECRET, SecurityTokenType.User);
	}

	@Test
	public void token_secretNotSerialized() {
		// WHEN
		Map<String, Object> json = JsonUtils.getStringMap(JsonUtils.getJSONString(testToken()));

		// THEN
		// @formatter:off
		then(json)
			.as("Token serialized")
			.containsKey("id")
			.as("Secret never serialized with the token")
			.doesNotContainKey("authSecret")
			;
		// @formatter:on
	}

	@Test
	public void generatedToken_secretSerialized() {
		// WHEN
		Map<String, Object> json = JsonUtils
				.getStringMap(JsonUtils.getJSONString(new GeneratedUserAuthToken(testToken())));

		// THEN
		// @formatter:off
		then(json)
			.as("Token properties unwrapped alongside the secret")
			.containsEntry("id", TEST_TOKEN_ID)
			.containsKey("userId")
			.as("Secret returned when the token is generated")
			.containsEntry("authSecret", TEST_SECRET)
			;
		// @formatter:on
	}

}
