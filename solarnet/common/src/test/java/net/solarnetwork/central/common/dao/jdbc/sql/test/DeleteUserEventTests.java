/* ==================================================================
 * DeleteUserEventTests.java - 3/08/2022 2:44:45 pm
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

package net.solarnetwork.central.common.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.common.dao.UserEventMaintenanceDao.UserEventPurgeFilter;
import net.solarnetwork.central.common.dao.jdbc.sql.DeleteUserEvent;

/**
 * Test cases for the {@link DeleteUserEvent} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DeleteUserEventTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void verifyPrepStatement(PreparedStatement stmt, UserEventPurgeFilter f)
			throws SQLException {
		int p = 0;
		verify(stmt).setObject(++p, f.getUserId());
		if ( f.getEndDate() != null ) {
			verify(stmt).setTimestamp(++p, Timestamp.from(f.getEndDate()));
		}
	}

	@Test
	public void userOlderThan_sql() {
		// WHEN
		String sql = DeleteUserEvent.deleteForUserOlderThanDate(1L, Instant.now()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("delete-user-event-user-older.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void userOlderThan_prep() throws SQLException {
		// GIVEN
		givenPrepStatement();

		// WHEN
		DeleteUserEvent sql = DeleteUserEvent.deleteForUserOlderThanDate(1L, Instant.now());
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"delete-user-event-user-older.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, sql.getFilter());
	}

	@Test
	public void userIdRequired() {
		assertThrows(IllegalArgumentException.class, () -> {
			new DeleteUserEvent(new BasicUserEventFilter());
		}, "A user ID is required in the filter");
	}

}
