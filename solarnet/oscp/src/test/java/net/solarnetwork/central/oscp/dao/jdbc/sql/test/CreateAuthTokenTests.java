/* ==================================================================
 * CreateAuthTokenTests.java - 16/08/2022 4:04:05 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.AuthTokenType;
import net.solarnetwork.central.oscp.dao.jdbc.sql.CreateAuthToken;

/**
 * Test cases for the {@link CreateAuthToken} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CreateAuthTokenTests {

	@Test
	public void createFlexibilityProvider() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());

		// WHEN
		String sql = new CreateAuthToken(AuthTokenType.FlexibilityProvider, id).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("{? = call solaroscp.create_fp_token(?, ?)}")));
	}

	@Test
	public void createCapacityProvider() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());

		// WHEN
		String sql = new CreateAuthToken(AuthTokenType.CapacityProvider, id).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("{? = call solaroscp.create_cp_token(?, ?)}")));
	}

	@Test
	public void createCapacityOptimizer() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());

		// WHEN
		String sql = new CreateAuthToken(AuthTokenType.CapacityOptimizer, id).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("{? = call solaroscp.create_co_token(?, ?)}")));
	}

}
