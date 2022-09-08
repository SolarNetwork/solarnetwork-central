/* ==================================================================
 * UpdateCapacityGroupMeasurementDateTests.java - 21/08/2022 7:58:36 am
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
import net.solarnetwork.central.oscp.dao.jdbc.sql.UpdateCapacityGroupMeasurementDate;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Test cases for the {@link UpdateCapacityGroupMeasurementDate} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateCapacityGroupMeasurementDateTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement stmt, UserLongCompositePK id, Instant expected,
			Instant ts) throws SQLException {
		then(stmt).should().setTimestamp(1, Timestamp.from(ts));
		then(stmt).should().setObject(2, id.getUserId(), Types.BIGINT);
		then(stmt).should().setObject(3, id.getEntityId(), Types.BIGINT);
		if ( expected != null ) {
			then(stmt).should().setTimestamp(4, Timestamp.from(expected));
		}
	}

	@Test
	public void unassignedId() {
		// GIVEN
		UserLongCompositePK id = UserLongCompositePK
				.unassignedEntityIdKey(randomUUID().getMostSignificantBits());
		Instant expected = null;
		Instant ts = Instant.now();

		// WHEN
		assertThrows(IllegalArgumentException.class, () -> {
			new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityProvider, id, expected, ts).getSql();
		});
	}

	@Test
	public void capacityProvider_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = Instant.now().minusSeconds(1);
		Instant ts = Instant.now();

		// WHEN
		String sql = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityProvider, id, expected, ts)
				.getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("""
				UPDATE solaroscp.oscp_cg_cp_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at = ?
				""")));
	}

	@Test
	public void capacityProvider_nullExpected_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = null;
		Instant ts = Instant.now();

		// WHEN
		String sql = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityProvider, id, expected, ts)
				.getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("""
				UPDATE solaroscp.oscp_cg_cp_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at IS NULL
				""")));
	}

	@Test
	public void capacityProvider_nullExpected_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = null;
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityProvider, id,
				expected, ts).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_cg_cp_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at IS NULL
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, expected, ts);
	}

	@Test
	public void capacityProvider_nonNullExpected_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = Instant.now().minusSeconds(1);
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityProvider, id,
				expected, ts).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_cg_cp_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at = ?
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, expected, ts);
	}

	@Test
	public void capacityOptimizer_sql() {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = Instant.now().minusSeconds(1);
		Instant ts = Instant.now();

		// WHEN
		String sql = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityOptimizer, id, expected, ts)
				.getSql();

		// THEN
		assertThat("SQL generated", sql, is(equalTo("""
				UPDATE solaroscp.oscp_cg_co_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at = ?
				""")));
	}

	@Test
	public void capacityOptimizer_nullExpected_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = null;
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityOptimizer, id,
				expected, ts).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_cg_co_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at IS NULL
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, expected, ts);
	}

	@Test
	public void capacityOptimizer_nonNullExpected_prep() throws SQLException {
		// GIVEN
		UserLongCompositePK id = new UserLongCompositePK(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits());
		Instant expected = null;
		Instant ts = Instant.now();

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateCapacityGroupMeasurementDate(OscpRole.CapacityOptimizer, id,
				expected, ts).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		assertThat("Generated SQL", sqlCaptor.getValue(), is(equalTo("""
				UPDATE solaroscp.oscp_cg_co_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at IS NULL
				""")));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, id, expected, ts);
	}

}
