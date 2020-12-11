/* ==================================================================
 * SelectDatumRunningTotalTests.java - 10/12/2020 6:53:37 am
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
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRunningTotal;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectDatumRunningTotal} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumRunningTotalTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_find_runningTotal_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(Instant.now());

		// WHEN
		String sql = new SelectDatumRunningTotal(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-runtot-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_runningTotal_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(Instant.now());

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

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatumRunningTotal(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-runtot-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_find_runningTotal_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeId(1L);

		// WHEN
		String sql = new SelectDatumRunningTotal(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-runtot-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_runningTotal_nodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeId(1L);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		stmt.setTimestamp(2, Timestamp.from(Instant.EPOCH));

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumRunningTotal(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-runtot-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_runningTotal_nodes_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeId(1L);
		filter.setSorts(sorts("node", "source"));

		// WHEN
		String sql = new SelectDatumRunningTotal(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-runtot-nodes-sortNodeSource.sql", TestSqlResources.class, SQL_COMMENT));
	}

}
