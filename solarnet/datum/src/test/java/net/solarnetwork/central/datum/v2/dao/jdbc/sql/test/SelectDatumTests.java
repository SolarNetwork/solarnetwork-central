/* ==================================================================
 * SelectDatumTests.java - 22/11/2020 8:12:12 am
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

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SelectDatum} class.
 *
 * @author matt
 * @version 1.4
 */
public class SelectDatumTests {

	private static final int TEST_FETCH_SIZE = 567;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_find_minute() {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Minute);
		thenIllegalArgumentException().isThrownBy(() -> new SelectDatum(filter).getSql());
	}

	@Test
	public void sql_find_mostRecent_agg() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		for ( Aggregation agg : EnumSet.complementOf(
				EnumSet.of(Aggregation.None, Aggregation.Hour, Aggregation.Day, Aggregation.Month)) ) {
			// WHEN
			filter.setAggregation(agg);
			thenIllegalArgumentException()
					.as("MostRecent should not be allowed with aggregation %s", agg)
					.isThrownBy(() -> new SelectDatum(filter).getSql());
		}
	}

	@Test
	public void sql_find_mostRecent_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-mostRecent-users.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_mostRecent_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-mostRecent-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_mostRecent_nodes_localStartDate() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-mostRecent-nodes-localStartDate.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_mostRecent_nodes_localEndDate() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalEndDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-mostRecent-nodes-localEndDate.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_mostRecent_nodes_localStartDate() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(TEST_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		stmt.setObject(eq(2), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-mostRecent-nodes-localStartDate.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_daily_mostRecent_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-daily-mostRecent-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_mostRecent_nodes_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		filter.setEndDate(Instant.now());

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-daily-mostRecent-nodes-dates.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_daily_mostRecent_nodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-daily-mostRecent-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		filter.setEndDate(Instant.now());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(TEST_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		stmt.setTimestamp(2, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(3, Timestamp.from(filter.getEndDate()));

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-daily-mostRecent-nodes-dates.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_absoluteMinDate() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		stmt.setTimestamp(2, Timestamp.from(filter.getStartDate()));

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-daily-mostRecent-nodes-date-min.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 2, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2022, 2, 7, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(TEST_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		stmt.setObject(eq(2), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));
		stmt.setObject(eq(3), eq(filter.getLocalEndDate()), eq(Types.TIMESTAMP));

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-daily-mostRecent-nodes-localDates.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-15min-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-nodesAndSources-dates-count.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-nodesAndSources-dates-sortTimeNodeSource.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_sortTimeNodeSource_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-nodesAndSources-dates-count.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_15min_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(TEST_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		stmt.setTimestamp(3, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(4, Timestamp.from(filter.getEndDate()));
		stmt.setObject(5, filter.getAggregation().getLevel());

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-15min-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-daily-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-nodesAndSources-dates-count.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-nodesAndSources-dates-sortTimeNodeSource.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_sortTimeNodeSource_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-nodesAndSources-dates-count.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_vids() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-virtual-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_vids_combineNodeOnly() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-virtual-mapNodes-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_15min_vids_combineSourceOnly() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-15min-virtual-mapSources-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-virtual-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_daily_vids_nodeAndSources_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(TEST_FETCH_SIZE);

		// set virtual ID parameters
		Long[] virtRealNodeIds = new Long[] { 1L, 2L, 3L };
		Array virtRealNodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(virtRealNodeIds))).andReturn(virtRealNodeIdsArray);
		stmt.setArray(1, virtRealNodeIdsArray);
		virtRealNodeIdsArray.free();
		stmt.setObject(2, 100L);

		// set read ID virtual ordering
		Array virtRealNodeIdsOrderArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(virtRealNodeIds)))
				.andReturn(virtRealNodeIdsOrderArray);
		stmt.setArray(3, virtRealNodeIdsOrderArray);
		virtRealNodeIdsOrderArray.free();

		String[] virtRealSourceIds = new String[] { "a", "b", "c" };
		Array virtRealSourceIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(virtRealSourceIds)))
				.andReturn(virtRealSourceIdsArray);
		stmt.setArray(4, virtRealSourceIdsArray);
		virtRealSourceIdsArray.free();
		stmt.setString(5, "V");

		Array virtRealSourceIdsOrderArray = createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(virtRealSourceIds)))
				.andReturn(virtRealSourceIdsOrderArray);
		stmt.setArray(6, virtRealSourceIdsOrderArray);
		virtRealSourceIdsOrderArray.free();

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(7, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(8, sourceIdsArray);
		sourceIdsArray.free();

		// main date parameters
		stmt.setTimestamp(9, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(10, Timestamp.from(filter.getEndDate()));

		// WHEN
		replay(con, stmt, virtRealNodeIdsArray, virtRealNodeIdsOrderArray, virtRealSourceIdsArray,
				virtRealSourceIdsOrderArray, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-daily-virtual-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, virtRealNodeIdsArray, virtRealNodeIdsOrderArray, virtRealSourceIdsArray,
				virtRealSourceIdsOrderArray, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_counts() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-virtual-nodesAndSources-dates-counts.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource(
						"select-datum-daily-virtual-nodesAndSources-dates-sortTimeNodeSource.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_sortTimeNodeSource_counts() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-daily-virtual-nodesAndSources-dates-counts.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_weekly_vids_nodeAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Week);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(7)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-weekly-virtual-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_seasonal_hod_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-seasonal_hod-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_seasonal_hod_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		stmt.setTimestamp(3, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(4, Timestamp.from(filter.getEndDate()));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-seasonal_hod-nodesAndSources-dates.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_seasonal_hod_nodesAndSources_defaultDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-seasonal_hod-nodesAndSources-localDates.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_seasonal_hod_nodesAndSources_defaultDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);
		stmt.setFetchSize(SelectDatum.DEFAULT_FETCH_SIZE);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		stmt.setObject(3, LocalDate.now(UTC).minusYears(2).atStartOfDay(), Types.TIMESTAMP);
		stmt.setObject(4, LocalDate.now(UTC).plusDays(1).atStartOfDay(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-seasonal_hod-nodesAndSources-localDates.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_doy_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.DayOfYear);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-doy-nodesAndSources-dates.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_hoy_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.HourOfYear);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-hoy-nodesAndSources-dates.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_reverse_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		filter.setUserId(randomLong());
		filter.setStartDate(Instant.now().minusSeconds(1));
		filter.setEndDate(filter.getStartDate().plusSeconds(1));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.info("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-node-source-user-time-reverse-limit.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_reading_month_rollup_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setAggregation(Aggregation.Month);
		filter.setDatumRollupType(DatumRollupType.All);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		filter.setUserId(randomLong());
		filter.setStartDate(Instant.now().minusSeconds(1));
		filter.setEndDate(filter.getStartDate().plusSeconds(1));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.info("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-reading-node-source-user-time-month-rollup.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

}
