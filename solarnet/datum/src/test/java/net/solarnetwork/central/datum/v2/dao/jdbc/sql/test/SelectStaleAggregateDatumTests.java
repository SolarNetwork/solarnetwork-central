/* ==================================================================
 * SelectStaleAggregateDatumTests.java - 23/11/2020 9:17:40 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link SelectStaleAggregateDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectStaleAggregateDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_find_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-stale-agg-datum-all.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_hour_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-stale-agg-datum-hour-all.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_hour_all() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		stmt.setString(1, Aggregation.Hour.getKey());

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new SelectStaleAggregateDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-stale-agg-datum-hour-all.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

	@Test
	public void sql_find_hour_nodesAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setSourceId("a");

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-stale-agg-datum-hour-nodesAndSources.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_hour_nodesAndSources_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusDays(1).toInstant());

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-stale-agg-datum-hour-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_hour_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusDays(1).toInstant());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		stmt.setString(3, Aggregation.Hour.getKey());
		Capture<Timestamp> startCaptor = new Capture<>();

		stmt.setTimestamp(eq(4), capture(startCaptor));

		Capture<Timestamp> endCaptor = new Capture<>();
		stmt.setTimestamp(eq(5), capture(endCaptor));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectStaleAggregateDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-stale-agg-datum-hour-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Start timestamp", startCaptor.getValue(),
				equalTo(Timestamp.from(filter.getStartDate())));
		assertThat("End timestamp", endCaptor.getValue(), equalTo(Timestamp.from(filter.getEndDate())));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_hour_nodesAndSources_localDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setLocalStartDate(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS));
		filter.setLocalEndDate(filter.getLocalStartDate().plusDays(1));

		// WHEN
		String sql = new SelectStaleAggregateDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-stale-agg-datum-hour-nodesAndSources-localDates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

}
