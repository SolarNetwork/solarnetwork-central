/* ==================================================================
 * SelectAuditDatumTests.java - 20/11/2020 12:05:56 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectAuditDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectAuditDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_hour_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-hour-users.sql", TestSqlResources.class));
	}

	@Test
	public void sql_day_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-day-users.sql", TestSqlResources.class));
	}

	@Test
	public void sql_day_users_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setUserId(1L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-day-users-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_day_users_localDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setUserId(1L);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-audit-datum-day-users-localDates.sql",
				TestSqlResources.class));
	}

	@Test
	public void sql_month_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectAuditDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-audit-datum-month-users.sql", TestSqlResources.class));
	}

	@Test
	public void prep_users_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(1, userIdsArray);
		userIdsArray.free();

		Capture<Timestamp> startCaptor = new Capture<>();
		stmt.setTimestamp(eq(2), capture(startCaptor));

		Capture<Timestamp> endCaptor = new Capture<>();
		stmt.setTimestamp(eq(3), capture(endCaptor));

		// WHEN
		replay(con, stmt, userIdsArray);
		PreparedStatement result = new SelectAuditDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Start timestamp", startCaptor.getValue(),
				equalTo(Timestamp.from(filter.getStartDate())));
		assertThat("End timestamp", endCaptor.getValue(), equalTo(Timestamp.from(filter.getEndDate())));
		verify(con, stmt, userIdsArray);
	}

	@Test
	public void prep_users_localDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(2L);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(1, userIdsArray);
		userIdsArray.free();

		stmt.setObject(eq(2), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));
		stmt.setObject(eq(3), eq(filter.getLocalEndDate()), eq(Types.TIMESTAMP));

		// WHEN
		replay(con, stmt, userIdsArray);
		PreparedStatement result = new SelectAuditDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, userIdsArray);
	}

	@Test
	public void prep_users_mostRecent() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(2L);
		filter.setMostRecent(true);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(1, userIdsArray);
		userIdsArray.free();

		// WHEN
		replay(con, stmt, userIdsArray);
		PreparedStatement result = new SelectAuditDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, userIdsArray);
	}

}
