/* ==================================================================
 * SelectObjectDatumStreamAliasEntityTests.java - 28/03/2026 7:49:16 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasEntity.DEFAULT_FETCH_SIZE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasEntity;

/**
 * Test cases for the {@link SelectObjectDatumStreamAliasEntity} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectObjectDatumStreamAliasEntityTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array streamIdsArray;

	@Mock
	private Array aliasStreamIdsArray;

	@Mock
	private Array nodeIdsArray;

	@Mock
	private Array aliasNodeIdsArray;

	@Mock
	private Array sourceIdsArray;

	@Mock
	private Array aliasSourceIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private BDDMyOngoingStubbing<PreparedStatement> givenPrepareStatement() throws SQLException {
		return given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private BDDMyOngoingStubbing<Array> givenCreateSqlArray(Long[] expected, Array mock)
			throws SQLException {
		return given(con.createArrayOf(eq("bigint"), aryEq(expected))).willReturn(mock);
	}

	private BDDMyOngoingStubbing<Array> givenCreateSqlArray(String[] expected, Array mock)
			throws SQLException {
		return given(con.createArrayOf(eq("text"), aryEq(expected))).willReturn(mock);
	}

	private BDDMyOngoingStubbing<Array> givenCreateSqlArray(UUID[] expected, Array mock)
			throws SQLException {
		return given(con.createArrayOf(eq("uuid"), aryEq(expected))).willReturn(mock);
	}

	private void thenShouldPrepareStatement() throws SQLException {
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
	}

	private void thenSqlEqualsResource(String sql, String resource) {
		// @formatter:off
		and.then(sql)
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace(getResourceAsString(
					resource,
					TestSqlResources.class,
					SQL_COMMENT))
			;
		// @formatter:on
	}

	private void thenShouldPrepareSql(String resource) throws SQLException {
		thenShouldPrepareStatement();
		thenSqlEqualsResource(sqlCaptor.getValue(), resource);
	}

	private void thenStatementShouldSetAndFreeArray(int idx, Array array) throws SQLException {
		then(stmt).should().setArray(eq(idx), same(array));
		then(array).should().free();
	}

	@Test
	public void find() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();

		// WHEN

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias.sql");

		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_nodes() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getNodeIds(), nodeIdsArray).willReturn(aliasNodeIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-nodes.sql");

		thenStatementShouldSetAndFreeArray(1, nodeIdsArray);
		thenStatementShouldSetAndFreeArray(2, aliasNodeIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_nodesAndSources() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });
		filter.setSourceIds(new String[] { randomString(), randomString() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getNodeIds(), nodeIdsArray).willReturn(aliasNodeIdsArray);
		givenCreateSqlArray(filter.getSourceIds(), sourceIdsArray).willReturn(aliasSourceIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-nodesAndSources.sql");

		thenStatementShouldSetAndFreeArray(1, nodeIdsArray);
		thenStatementShouldSetAndFreeArray(2, aliasNodeIdsArray);
		thenStatementShouldSetAndFreeArray(3, sourceIdsArray);
		thenStatementShouldSetAndFreeArray(4, aliasSourceIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_user_nodesAndSources() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setUserId(randomLong());
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });
		filter.setSourceIds(new String[] { randomString(), randomString() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getNodeIds(), nodeIdsArray).willReturn(aliasNodeIdsArray);
		givenCreateSqlArray(filter.getSourceIds(), sourceIdsArray).willReturn(aliasSourceIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-user-nodesAndSources.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementShouldSetAndFreeArray(2, nodeIdsArray);
		thenStatementShouldSetAndFreeArray(3, aliasNodeIdsArray);
		thenStatementShouldSetAndFreeArray(4, sourceIdsArray);
		thenStatementShouldSetAndFreeArray(5, aliasSourceIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_streams() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamIds(new UUID[] { randomUUID(), randomUUID() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getStreamIds(), aliasStreamIdsArray).willReturn(streamIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-streams.sql");

		thenStatementShouldSetAndFreeArray(1, aliasStreamIdsArray);
		thenStatementShouldSetAndFreeArray(2, streamIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_streams_aliasOnly() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamIds(new UUID[] { randomUUID(), randomUUID() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getStreamIds(), aliasStreamIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter, true)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-streams-aliasOnly.sql");

		thenStatementShouldSetAndFreeArray(1, aliasStreamIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_user_streams_nodesAndSources() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setUserId(randomLong());
		filter.setStreamIds(new UUID[] { randomUUID(), randomUUID() });
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });
		filter.setSourceIds(new String[] { randomString(), randomString() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getStreamIds(), aliasStreamIdsArray).willReturn(streamIdsArray);
		givenCreateSqlArray(filter.getNodeIds(), nodeIdsArray).willReturn(aliasNodeIdsArray);
		givenCreateSqlArray(filter.getSourceIds(), sourceIdsArray).willReturn(aliasSourceIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("select-object-datum-stream-alias-user-streams-nodesAndSources.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementShouldSetAndFreeArray(2, aliasStreamIdsArray);
		thenStatementShouldSetAndFreeArray(3, streamIdsArray);
		thenStatementShouldSetAndFreeArray(4, nodeIdsArray);
		thenStatementShouldSetAndFreeArray(5, aliasNodeIdsArray);
		thenStatementShouldSetAndFreeArray(6, sourceIdsArray);
		thenStatementShouldSetAndFreeArray(7, aliasSourceIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find_user_streams_nodesAndSources_aliasOnly() throws SQLException {
		// GIVEN
		final var filter = new BasicDatumCriteria();
		filter.setUserId(randomLong());
		filter.setStreamIds(new UUID[] { randomUUID(), randomUUID() });
		filter.setNodeIds(new Long[] { randomLong(), randomLong() });
		filter.setSourceIds(new String[] { randomString(), randomString() });

		// WHEN

		givenPrepareStatement();
		givenCreateSqlArray(filter.getStreamIds(), aliasStreamIdsArray);
		givenCreateSqlArray(filter.getNodeIds(), aliasNodeIdsArray);
		givenCreateSqlArray(filter.getSourceIds(), aliasSourceIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasEntity(filter, true)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql(
				"select-object-datum-stream-alias-user-streams-nodesAndSources-aliasOnly.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementShouldSetAndFreeArray(2, aliasStreamIdsArray);
		thenStatementShouldSetAndFreeArray(3, aliasNodeIdsArray);
		thenStatementShouldSetAndFreeArray(4, aliasSourceIdsArray);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
