/* ==================================================================
 * InsertAuthTokenTests.java - 17/08/2022 8:04:58 am
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
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertAuthToken;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Test case for the {@link InsertAuthToken} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class InsertAuthTokenTests {

	@Mock
	private Connection con;

	@Mock
	private CallableStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepCall() throws SQLException {
		given(con.prepareCall(any())).willReturn(stmt);
	}

	private void thenPrepCall(CallableStatement call, UserLongCompositePK id, String token)
			throws SQLException {
		then(call).should().setObject(1, id.getUserId(), Types.BIGINT);
		then(call).should().setObject(2, id.getEntityId(), Types.BIGINT);
		then(call).should().setString(3, token);
	}

	@Test
	public void flexibilityProvider() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		// WHEN
		assertThrows(IllegalArgumentException.class, () -> {
			new InsertAuthToken(OscpRole.FlexibilityProvider, id, token).getSql();
		});
	}

	@Test
	public void unassignedId() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK
				.unassignedEntityIdKey(randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		// WHEN
		assertThrows(IllegalArgumentException.class, () -> {
			new InsertAuthToken(OscpRole.CapacityProvider, id, token).getSql();
		});
	}

	@Test
	public void capacityProvider_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		// WHEN
		String sql = new InsertAuthToken(OscpRole.CapacityProvider, id, token).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("{call solaroscp.save_cp_token(?, ?, ?)}")));
	}

	@Test
	public void capacityProvider_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		givenPrepCall();

		// WHEN
		CallableStatement result = new InsertAuthToken(OscpRole.CapacityProvider, id, token)
				.createCallableStatement(con);

		// THEN
		then(con).should().prepareCall(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				is(equalTo("{call solaroscp.save_cp_token(?, ?, ?)}")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepCall(result, id, token);
	}

	@Test
	public void capacityOptimizer_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		// WHEN
		String sql = new InsertAuthToken(OscpRole.CapacityOptimizer, id, token).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("{call solaroscp.save_co_token(?, ?, ?)}")));
	}

	@Test
	public void capacityOptimizer_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		String token = randomUUID().toString();

		givenPrepCall();

		// WHEN
		CallableStatement result = new InsertAuthToken(OscpRole.CapacityOptimizer, id, token)
				.createCallableStatement(con);

		// THEN
		then(con).should().prepareCall(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				is(equalTo("{call solaroscp.save_co_token(?, ?, ?)}")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepCall(result, id, token);
	}

}
