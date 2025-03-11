/* ==================================================================
 * AuthorizationStateTests.java - 11/03/2025 3:27:06 pm
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

package net.solarnetwork.central.c2c.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.domain.AuthorizationState;

/**
 * Test cases for the {@link AuthorizationState} class.
 *
 * @author matt
 * @version 1.0
 */
public class AuthorizationStateTests {

	@Test
	public void decode() {
		// GIVEN
		String state = "123,abc123";

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue(state);

		// THEN
		// @formatter:off
		then(result)
			.as("State decoded")
			.isNotNull()
			.as("Integration ID decoded")
			.returns(123L, from(AuthorizationState::integrationId))
			.as("Token decoded")
			.returns("abc123", from(AuthorizationState::token))
			;
		// @formatter:on
	}

	@Test
	public void decode_negativeId() {
		// GIVEN
		String state = "-123,abc123";

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue(state);

		// THEN
		// @formatter:off
		then(result)
			.as("State decoded")
			.isNotNull()
			.as("Integration ID decoded")
			.returns(-123L, from(AuthorizationState::integrationId))
			.as("Token decoded")
			.returns("abc123", from(AuthorizationState::token))
			;
		// @formatter:on
	}

	@Test
	public void decode_null() {
		// GIVEN

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue(null);

		// THEN
		// @formatter:off
		then(result)
			.as("State not decoded")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void decode_notValid() {
		// GIVEN

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue("foo");

		// THEN
		// @formatter:off
		then(result)
			.as("State not decoded")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void decode_notValid_emptyId() {
		// GIVEN

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue(",foo");

		// THEN
		// @formatter:off
		then(result)
			.as("State not decoded")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void decode_notValid_emptyToken() {
		// GIVEN

		// WHEN
		AuthorizationState result = AuthorizationState.forStateValue("123,");

		// THEN
		// @formatter:off
		then(result)
			.as("State not decoded")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void encode() {
		// GIVEN
		AuthorizationState state = new AuthorizationState(randomLong(), randomString());

		// WHEN
		String result = state.stateValue();

		// THEN
		// @formatter:off
		then(result)
			.as("State encoded")
			.isEqualTo("%d,%s".formatted(state.integrationId(), state.token()))
			;
		// @formatter:on
	}

}
