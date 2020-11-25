/* ==================================================================
 * InsertStaleAggregateDatumSelectTests.java - 25/11/2020 3:12:42 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertStaleAggregateDatumSelect;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link InsertStaleAggregateDatumSelect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertStaleAggregateDatumSelectTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_hours_nodesAndSources() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());

		// WHEN
		String sql = new InsertStaleAggregateDatumSelect(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("insert-stale-agg-datum-select-hours-nodesAndSources-dates.sql",
						TestSqlResources.class));
	}

	@Test
	public void prep_hours_nodesAndSources() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());

		InsertStaleAggregateDatumSelect sql = new InsertStaleAggregateDatumSelect(filter);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		expect(con.prepareStatement(sql.getSql())).andReturn(stmt);

		Array nodeArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeArray);
		stmt.setArray(1, nodeArray);
		nodeArray.free();

		Array sourceArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceArray);
		stmt.setArray(2, sourceArray);
		sourceArray.free();

		Capture<Timestamp> startCaptor = new Capture<>();
		stmt.setTimestamp(eq(3), capture(startCaptor));

		Capture<Timestamp> endCaptor = new Capture<>();
		stmt.setTimestamp(eq(4), capture(endCaptor));

		// WHEN
		replay(con, stmt, nodeArray, sourceArray);
		PreparedStatement result = sql.createPreparedStatement(con);

		// THEN
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Start prarameter", startCaptor.getValue(),
				equalTo(Timestamp.from(filter.getStartDate())));
		assertThat("End prarameter", endCaptor.getValue(), equalTo(Timestamp.from(filter.getEndDate())));
		verify(con, stmt, nodeArray, sourceArray);
	}

	@Test
	public void sql_days_nodesAndSources() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusDays(1).toInstant());

		// WHEN
		String sql = new InsertStaleAggregateDatumSelect(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"insert-stale-agg-datum-select-days-nodesAndSources-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_months_nodesAndSources() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new InsertStaleAggregateDatumSelect(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("insert-stale-agg-datum-select-months-nodesAndSources-dates.sql",
						TestSqlResources.class));
	}

}
