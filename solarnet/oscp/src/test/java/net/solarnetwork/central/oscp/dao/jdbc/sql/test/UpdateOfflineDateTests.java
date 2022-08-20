/* ==================================================================
 * UpdateOfflineDateTests.java - 17/08/2022 9:37:18 am
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateOfflineDate;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Test cases for the {@link UpdateOfflineDate} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateOfflineDateTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement stmt, UserLongCompositePK id, Instant ts)
			throws SQLException {
		then(stmt).should().setTimestamp(1, Timestamp.from(ts));
		then(stmt).should().setObject(2, id.getUserId(), Types.BIGINT);
		then(stmt).should().setObject(3, id.getEntityId(), Types.BIGINT);
	}

	@Test
	public void unassignedId() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK
				.unassignedEntityIdKey(randomUUID().getMostSignificantBits());
		Instant ts = Instant.now();

		// WHEN
		assertThrows(IllegalArgumentException.class, () -> {
			new UpdateOfflineDate(OscpRole.CapacityProvider, id, ts).getSql();
		});
	}

	@Test
	public void capacityProvider_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant ts = Instant.now();

		// WHEN
		String sql = new UpdateOfflineDate(OscpRole.CapacityProvider, id, ts).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("""
				UPDATE solaroscp.oscp_cp_conf
				SET offline_at = ?
				WHERE user_id = ? AND id = ?
				""")));
	}

	@Test
	public void capacityProvider_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateOfflineDate(OscpRole.CapacityProvider, id, ts)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_cp_conf
				SET offline_at = ?
				WHERE user_id = ? AND id = ?
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, ts);
	}

	@Test
	public void capacityOptimizer_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant ts = Instant.now();

		// WHEN
		String sql = new UpdateOfflineDate(OscpRole.CapacityOptimizer, id, ts).getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("""
				UPDATE solaroscp.oscp_co_conf
				SET offline_at = ?
				WHERE user_id = ? AND id = ?
				""")));
	}

	@Test
	public void capacityOptimizer_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateOfflineDate(OscpRole.CapacityOptimizer, id, ts)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_co_conf
				SET offline_at = ?
				WHERE user_id = ? AND id = ?
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, ts);
	}

}
