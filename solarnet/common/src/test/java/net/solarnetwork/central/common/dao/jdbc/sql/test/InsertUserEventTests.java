/* ==================================================================
 * InsertUserEventTests.java - 3/08/2022 6:52:33 am
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

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.sql.InsertUserEvent;
import net.solarnetwork.central.domain.UserEvent;

/**
 * Test cases for the {@link InsertUserEvent} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class InsertUserEventTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array tagsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(sqlCaptor.capture())).willReturn(stmt);
	}

	private UserEvent createUserEvent(Long userId) {
		UserEvent event = new UserEvent(userId, UUID.randomUUID(),
				new String[] { "foo", UUID.randomUUID().toString() }, UUID.randomUUID().toString(),
				"{\"foo\":123}");
		return event;
	}

	private void givenSetTagsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(tagsArray);
	}

	private void verifyPrepStatement(PreparedStatement result, UserEvent event) throws SQLException {
		verify(result).setObject(1, event.getUserId());
		verify(result).setObject(2, event.getEventId());
		verify(result).setArray(3, tagsArray);
		if ( event.getMessage() != null ) {
			verify(result).setString(4, event.getMessage());
		} else {
			verify(result).setNull(4, Types.VARCHAR);
		}
		if ( event.getData() != null ) {
			verify(result).setString(5, event.getData());
		} else {
			verify(result).setNull(5, Types.VARCHAR);
		}
	}

	@Test
	public void sql() {
		// GIVEN
		UserEvent event = createUserEvent(randomUUID().getMostSignificantBits());

		// WHEN
		String sql = new InsertUserEvent(event).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("insert-user-event.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		UserEvent event = createUserEvent(randomUUID().getMostSignificantBits());

		// GIVEN
		givenPrepStatement();
		givenSetTagsArrayParameter(event.getTags());

		// WHEN
		PreparedStatement result = new InsertUserEvent(event).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("insert-user-event.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, event);
	}

	@Test
	public void prep_nullMessageAndData() throws SQLException {
		// GIVEN
		UserEvent event = new UserEvent(randomUUID().getMostSignificantBits(), UUID.randomUUID(),
				new String[] { "foo", UUID.randomUUID().toString() }, null, null);

		// GIVEN
		givenPrepStatement();
		givenSetTagsArrayParameter(event.getTags());

		// WHEN
		PreparedStatement result = new InsertUserEvent(event).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("insert-user-event.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verifyPrepStatement(result, event);
	}

}
