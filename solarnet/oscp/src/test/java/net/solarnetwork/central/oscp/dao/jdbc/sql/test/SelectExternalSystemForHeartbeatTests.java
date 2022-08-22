/* ==================================================================
 * SelectExternalSystemForHeartbeatTests.java - 21/08/2022 5:34:59 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.oscp.dao.BasicLockingFilter.ONE_FOR_UPDATE_SKIP;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.oscp.dao.LockingFilter;
import net.solarnetwork.central.oscp.dao.jdbc.sql.SelectExternalSystemForHeartbeat;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Test cases for the {@link SelectExternalSystemForHeartbeat} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectExternalSystemForHeartbeatTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void thenPrepStatement(PreparedStatement result, LockingFilter filter) throws SQLException {
		int p = 0;
		if ( filter.getMax() != null ) {
			then(result).should().setInt(++p, filter.getMax());
		}
		if ( filter.getOffset() != null ) {
			then(result).should().setInt(++p, filter.getOffset());
		}
	}

	@Test
	public void capacityProvider_forUpdateSkip_sql() {
		// GIVEN

		// WHEN
		String sql = new SelectExternalSystemForHeartbeat(OscpRole.CapacityProvider, ONE_FOR_UPDATE_SKIP)
				.getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-capacity-provider-heartbeat-locked.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void capacityProvider_forUpdateSkip_prep() throws SQLException {
		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectExternalSystemForHeartbeat(OscpRole.CapacityProvider,
				ONE_FOR_UPDATE_SKIP).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"select-capacity-provider-heartbeat-locked.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, ONE_FOR_UPDATE_SKIP);
	}

	@Test
	public void capacityOptimizer_forUpdateSkip_sql() {
		// GIVEN

		// WHEN
		String sql = new SelectExternalSystemForHeartbeat(OscpRole.CapacityOptimizer,
				ONE_FOR_UPDATE_SKIP).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-capacity-optimizer-heartbeat-locked.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void capacityOptimizer_forUpdateSkip_prep() throws SQLException {
		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectExternalSystemForHeartbeat(OscpRole.CapacityOptimizer,
				ONE_FOR_UPDATE_SKIP).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"select-capacity-optimizer-heartbeat-locked.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, ONE_FOR_UPDATE_SKIP);
	}

}
