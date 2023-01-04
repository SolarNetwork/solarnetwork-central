/* ==================================================================
 * UserBizConstantsTests.java - 17/05/2022 2:25:56 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.biz.dao.UserBizConstants;

/**
 * Test cases for the {@link UserBizConstants} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserBizConstantsTests {

	private SecureRandom rng;

	@BeforeEach
	public void setup() {
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate auth token", e);
		}

	}

	@Test
	public void createUnconfirmedEmail() {
		// GIVEN
		final String email = "foo@example.com";

		// WHEN
		String result = UserBizConstants.getUnconfirmedEmail(email);

		// THEN
		assertThat("Unconfirmed email ends with original email", result, endsWith(email));
		assertThat("Unconfirmed email has prefix for 20 characters + @ delimiter", result,
				hasLength(email.length() + UserBizConstants.UNCONFIRMED_EMAIL_PREFIX_LENGTH));
	}

	@Test
	public void decodeUnconfirmedEmail() {
		// GIVEN
		final String email = "foo@example.com";
		final String unconfirmed = UserBizConstants.getUnconfirmedEmail(email);

		// WHEN
		String result = UserBizConstants.getOriginalEmail(unconfirmed);

		// THEN
		assertThat("Email decoded", result, is(equalTo(email)));
	}

	@Test
	public void generateRandomAuthToken() {
		// WHEN
		String tok = UserBizConstants.generateRandomAuthToken(rng);

		// THEN
		assertThat("Token generated with expected length", tok,
				hasLength(UserBizConstants.RANDOM_AUTH_TOKEN_BYTE_COUNT));
	}

	@Test
	public void generateRandomToken() {
		// GIVEN
		final int length = rng.nextInt(64) + 16;

		// WHEN
		String tok = UserBizConstants.generateRandomToken(rng, length);

		// THEN
		assertThat("Token generated with expected length", tok, hasLength(length));
	}

}
