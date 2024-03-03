/* ==================================================================
 * UpdateEnabledIdFilterTests.java - 21/02/2024 10:34:47 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.sql.UpdateEnabledIdFilter;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.test.CommonTestUtils;

/**
 * Test cases for the {@link UpdateEnabledIdFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateEnabledIdFilterTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(anyString())).willReturn(stmt);
	}

	@Test
	public void updateRow() throws SQLException {
		// GIVEN
		String tableName = "example.foo";
		String[] pkColNames = new String[] { "pk1", "pk2" };
		givenPrepStatement();

		// WHEN
		UserStringCompositePK filter = new UserStringCompositePK(CommonTestUtils.randomLong(),
				randomString());
		UpdateEnabledIdFilter sql = new UpdateEnabledIdFilter(tableName, pkColNames, filter, true);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"update-enabled-id-filter-row.sql", TestSqlResources.class, SQL_COMMENT));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verify(stmt).setBoolean(1, true);
		verify(stmt).setObject(2, filter.getUserId());
		verify(stmt).setObject(3, filter.getEntityId());
	}

	@Test
	public void updateGroup() throws SQLException {
		// GIVEN
		String tableName = "example.foo";
		String[] pkColNames = new String[] { "pk1", "pk2" };
		givenPrepStatement();

		// WHEN
		UserStringCompositePK filter = UserStringCompositePK
				.unassignedEntityIdKey(CommonTestUtils.randomLong());
		UpdateEnabledIdFilter sql = new UpdateEnabledIdFilter(tableName, pkColNames, filter, true);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"update-enabled-id-filter-group.sql", TestSqlResources.class, SQL_COMMENT));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verify(stmt).setBoolean(1, true);
		verify(stmt).setObject(2, filter.getUserId());
	}

}
