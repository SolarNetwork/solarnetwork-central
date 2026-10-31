/* ==================================================================
 * SelectObjectDatumStreamAliasGrantStreamTests.java - 31/10/2026 8:41:03 pm
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
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantStream.DEFAULT_FETCH_SIZE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantStream;

/**
 * Test cases for the {@link SelectObjectDatumStreamAliasGrantStream} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectObjectDatumStreamAliasGrantStreamTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array grantIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private BDDMyOngoingStubbing<PreparedStatement> givenPrepareStatement() throws SQLException {
		return given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
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
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), resource);
	}

	private void thenStatementResultIs(PreparedStatement result) throws SQLException {
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-stream-select.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setObject(2, aliasNodeId);
		thenStatementResultIs(result);
	}

	@Test
	public void find_filterGranteeAndEnabledIgnored() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setGranteeId(randomLong());
		filter.setEnabled(false);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-stream-select.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setObject(2, aliasNodeId);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_grant() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGrantId(randomLong());

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-stream-select-user-grant.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setObject(2, filter.getUserId());
		then(stmt).should().setObject(3, filter.getGrantId());
		then(stmt).should().setObject(4, aliasNodeId);
		thenStatementResultIs(result);
	}

	@Test
	public void find_users_grants() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserIds(new Long[] { randomLong(), randomLong() });
		filter.setGrantIds(new Long[] { randomLong(), randomLong() });

		givenPrepareStatement();
		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);
		given(con.createArrayOf(eq("bigint"), aryEq(filter.getGrantIds()))).willReturn(grantIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-stream-select-users-grants.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setArray(eq(2), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(grantIdsArray));
		then(grantIdsArray).should().free();
		then(stmt).should().setObject(4, aliasNodeId);
		thenStatementResultIs(result);
	}

	@Test
	public void find_maxAndOffset() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setMax(10);
		filter.setOffset(20L);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-stream-select-max-offset.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setObject(2, aliasNodeId);
		then(stmt).should().setInt(3, 10);
		then(stmt).should().setLong(4, 20L);
		thenStatementResultIs(result);
	}

	@Test
	public void count() throws SQLException {
		// GIVEN
		final Long granteeId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setMax(10);
		filter.setOffset(20L);

		given(con.prepareStatement(any())).willReturn(stmt);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantStream(granteeId,
				aliasNodeId, filter).countPreparedStatementCreator().createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(), "object-datum-stream-alias-grant-stream-count.sql");

		then(stmt).should().setObject(1, granteeId);
		then(stmt).should().setObject(2, aliasNodeId);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
