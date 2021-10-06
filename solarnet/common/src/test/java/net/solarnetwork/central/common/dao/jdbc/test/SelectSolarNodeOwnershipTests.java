/* ==================================================================
 * SelectSolarNodeOwnershipTests.java - 6/10/2021 10:11:10 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static net.solarnetwork.central.common.dao.jdbc.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import java.sql.Array;
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
import net.solarnetwork.central.common.dao.jdbc.SelectSolarNodeOwnership;

/**
 * Test cases for the {@link SelectSolarNodeOwnership} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectSolarNodeOwnershipTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array nodeIdsArray;

	@Mock
	private Array userIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetLongArrayParameter(int p, Long[] value, Array array) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(array);
		stmt.setArray(p, array);
		nodeIdsArray.free();

	}

	@Test
	public void all_sql() {
		// WHEN
		String sql = new SelectSolarNodeOwnership(null, null).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-node-ownership-all.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void all_prep() throws SQLException {
		// GIVEN
		givenPrepStatement();

		// WHEN
		PreparedStatement result = new SelectSolarNodeOwnership(null, null).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-node-ownership-all.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void node_sql() {
		// WHEN
		String sql = SelectSolarNodeOwnership.selectForNode(123L).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-node-ownership-node.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void node_prep() throws SQLException {
		// GIVEN
		final Long nodeId = 123L;
		givenPrepStatement();
		stmt.setObject(1, nodeId);

		// WHEN
		PreparedStatement result = SelectSolarNodeOwnership.selectForNode(nodeId)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-node-ownership-node.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void user_sql() {
		// WHEN
		String sql = SelectSolarNodeOwnership.selectForUser(321L).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-node-ownership-user.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void user_prep() throws SQLException {
		// GIVEN
		final Long userId = 321L;
		givenPrepStatement();
		stmt.setObject(1, userId);

		// WHEN
		PreparedStatement result = SelectSolarNodeOwnership.selectForUser(userId)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-node-ownership-user.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void nodeAndUser_sql() {
		// WHEN
		String sql = SelectSolarNodeOwnership.selectForNodeUser(123L, 321L).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-node-ownership-node-and-user.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void nodeAndUser_prep() throws SQLException {
		// GIVEN
		final Long nodeId = 123L;
		final Long userId = 321L;
		givenPrepStatement();
		stmt.setObject(1, nodeId);
		stmt.setObject(2, userId);

		// WHEN
		PreparedStatement result = SelectSolarNodeOwnership.selectForNodeUser(nodeId, userId)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-node-ownership-node-and-user.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void nodesAndUsers_sql() {
		// WHEN
		String sql = new SelectSolarNodeOwnership(new Long[] { 123L, 234L }, new Long[] { 321L, 432L })
				.getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-node-ownership-nodes-and-users.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void nodesAndUsers_prep() throws SQLException {
		// GIVEN
		final Long[] nodeIds = new Long[] { 123L, 234L };
		final Long[] userIds = new Long[] { 321L, 432L };
		givenPrepStatement();
		givenSetLongArrayParameter(1, nodeIds, nodeIdsArray);
		givenSetLongArrayParameter(2, userIds, userIdsArray);

		// WHEN
		PreparedStatement result = new SelectSolarNodeOwnership(nodeIds, userIds)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-node-ownership-nodes-and-users.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

}
