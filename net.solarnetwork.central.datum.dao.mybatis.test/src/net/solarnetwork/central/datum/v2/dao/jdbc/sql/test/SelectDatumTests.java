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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.SQL_COMMENT;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test(expected = IllegalArgumentException.class)
	public void sql_find_minute() {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Minute);
		new SelectDatum(filter).getSql();
	}

	@Test
	public void sql_find_mostRecent_agg() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);
		filter.setMostRecent(true);
		for ( Aggregation agg : EnumSet.complementOf(
				EnumSet.of(Aggregation.None, Aggregation.Hour, Aggregation.Day, Aggregation.Month)) ) {
			// WHEN
			filter.setAggregation(agg);
			try {
				new SelectDatum(filter).getSql();
				Assert.fail("MostRecent should not be allowed with aggregation " + agg);
			} catch ( IllegalArgumentException e ) {
				// ok
			}
		}
	}

	@Test
	public void sql_find_mostRecent_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);
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
		filter.setNodeId(1L);
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-mostRecent-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void sql_find_daily_mostRecent_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-daily-mostRecent-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_daily_mostRecent_nodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setMostRecent(true);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

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
	public void sql_find_15min_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());

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

		stmt.setTimestamp(3, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(4, Timestamp.from(filter.getEndDate()));
		stmt.setObject(5, filter.getAggregation().getLevel());

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setNodeId(1L);
		filter.setSourceId("a");
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
		filter.setSourceIds(new String[] { "a", "b", "c" });
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
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

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
		filter.setSourceIds(new String[] { "a", "b", "c" });
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
		filter.setSourceIds(new String[] { "a", "b", "c" });
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
		filter.setSourceIds(new String[] { "a", "b", "c" });
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

}
