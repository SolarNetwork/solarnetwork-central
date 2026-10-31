/* ==================================================================
 * SelectObjectDatumStreamAliasGrantEntityTests.java - 1/11/2026 7:12:48 am
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantEntity.DEFAULT_FETCH_SIZE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito.BDDMyOngoingStubbing;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectDatumStreamAliasGrantEntity;

/**
 * Test cases for the {@link SelectObjectDatumStreamAliasGrantEntity} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectObjectDatumStreamAliasGrantEntityTests {

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array userIdsArray;

	@Mock
	private Array grantIdsArray;

	@Mock
	private Array granteeIdsArray;

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

	private void thenStatementResultIs(PreparedStatement result) throws SQLException {
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void find() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select.sql");

		thenStatementResultIs(result);
	}

	@Test
	public void find_emptyArrays() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserIds(new Long[0]);
		filter.setGrantIds(new Long[0]);
		filter.setGranteeIds(new Long[0]);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select.sql");

		thenStatementResultIs(result);
	}

	@Test
	public void find_user_emptyGrants() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGrantIds(new Long[0]);
		filter.setEnabled(true);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-enabled.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setObject(2, true, Types.BOOLEAN);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementResultIs(result);
	}

	@Test
	public void find_users() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserIds(new Long[] { randomLong(), randomLong() });

		givenPrepareStatement();
		givenCreateSqlArray(filter.getUserIds(), userIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-users.sql");

		thenStatementShouldSetAndFreeArray(1, userIdsArray);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_grant() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGrantId(randomLong());

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-grant.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setObject(2, filter.getGrantId());
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_grants() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGrantIds(new Long[] { randomLong(), randomLong() });

		givenPrepareStatement();
		givenCreateSqlArray(filter.getGrantIds(), grantIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-grants.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementShouldSetAndFreeArray(2, grantIdsArray);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_grantee() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGranteeId(randomLong());

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-grantee.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setObject(2, filter.getGranteeId());
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_grantees() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGranteeIds(new Long[] { randomLong(), randomLong() });

		givenPrepareStatement();
		givenCreateSqlArray(filter.getGranteeIds(), granteeIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-grantees.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		thenStatementShouldSetAndFreeArray(2, granteeIdsArray);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_enabled() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setEnabled(true);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-enabled.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setObject(2, true, Types.BOOLEAN);
		thenStatementResultIs(result);
	}

	@Test
	public void find_disabled() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setEnabled(false);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-enabled.sql");

		then(stmt).should().setObject(1, false, Types.BOOLEAN);
		thenStatementResultIs(result);
	}

	@Test
	public void find_users_grants_grantees_enabled() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserIds(new Long[] { randomLong(), randomLong() });
		filter.setGrantIds(new Long[] { randomLong(), randomLong() });
		filter.setGranteeIds(new Long[] { randomLong(), randomLong() });
		filter.setEnabled(true);

		givenPrepareStatement();
		givenCreateSqlArray(filter.getUserIds(), userIdsArray);
		givenCreateSqlArray(filter.getGrantIds(), grantIdsArray);
		givenCreateSqlArray(filter.getGranteeIds(), granteeIdsArray);

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-users-grants-grantees-enabled.sql");

		thenStatementShouldSetAndFreeArray(1, userIdsArray);
		thenStatementShouldSetAndFreeArray(2, grantIdsArray);
		thenStatementShouldSetAndFreeArray(3, granteeIdsArray);
		then(stmt).should().setObject(4, true, Types.BOOLEAN);
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_maxAndOffset() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setMax(10);
		filter.setOffset(20L);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-max-offset.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setInt(2, filter.getMax());
		then(stmt).should().setLong(3, filter.getOffset());
		thenStatementResultIs(result);
	}

	@Test
	public void find_user_offsetOnly() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setOffset(20L);

		givenPrepareStatement();

		// WHEN
		final PreparedStatement result = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.createPreparedStatement(con);

		// THEN
		thenShouldPrepareSql("object-datum-stream-alias-grant-select-user-offset.sql");

		then(stmt).should().setObject(1, filter.getUserId());
		then(stmt).should().setLong(2, filter.getOffset());
		thenStatementResultIs(result);
	}

	@Test
	public void find_customFetchSize() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		final int fetchSize = 123;

		givenPrepareStatement();

		// WHEN
		new SelectObjectDatumStreamAliasGrantEntity(filter, fetchSize).createPreparedStatement(con);

		// THEN
		then(stmt).should().setFetchSize(fetchSize);
	}

	@Test
	public void find_noFetchSize() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		givenPrepareStatement();

		// WHEN
		new SelectObjectDatumStreamAliasGrantEntity(filter, 0).createPreparedStatement(con);

		// THEN
		then(stmt).should(never()).setFetchSize(anyInt());
	}

	@Test
	public void count() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setMax(10);
		filter.setOffset(20L);

		given(con.prepareStatement(any())).willReturn(stmt);

		// WHEN
		final PreparedStatementCreator creator = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.countPreparedStatementCreator();
		final PreparedStatement result = creator.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(), "object-datum-stream-alias-grant-count.sql");

		then(stmt).shouldHaveNoInteractions();
		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void count_users_grants_grantees_enabled() throws SQLException {
		// GIVEN
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserIds(new Long[] { randomLong(), randomLong() });
		filter.setGrantIds(new Long[] { randomLong(), randomLong() });
		filter.setGranteeIds(new Long[] { randomLong(), randomLong() });
		filter.setEnabled(true);
		filter.setMax(10);
		filter.setOffset(20L);

		given(con.prepareStatement(any())).willReturn(stmt);
		givenCreateSqlArray(filter.getUserIds(), userIdsArray);
		givenCreateSqlArray(filter.getGrantIds(), grantIdsArray);
		givenCreateSqlArray(filter.getGranteeIds(), granteeIdsArray);

		// WHEN
		final PreparedStatementCreator creator = new SelectObjectDatumStreamAliasGrantEntity(filter)
				.countPreparedStatementCreator();
		final PreparedStatement result = creator.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"object-datum-stream-alias-grant-count-users-grants-grantees-enabled.sql");

		thenStatementShouldSetAndFreeArray(1, userIdsArray);
		thenStatementShouldSetAndFreeArray(2, grantIdsArray);
		thenStatementShouldSetAndFreeArray(3, granteeIdsArray);
		then(stmt).should().setObject(4, true, Types.BOOLEAN);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
