/* ==================================================================
 * SelectDatumPartialAggregateTests.java - 3/12/2020 8:59:15 pm
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumPartialAggregate;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link SelectDatumPartialAggregate} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumPartialAggregateTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_find_nodes_Month_Day_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-pagg-month-day-full-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_nodes_Month_Day_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// partial leading date parameters
		stmt.setObject(2, filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(3, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(4, filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1).plusMonths(1),
				Types.TIMESTAMP);

		// main date parameters
		stmt.setObject(5, LocalDateTime.of(2020, 7, 1, 0, 0), Types.TIMESTAMP);
		stmt.setObject(6, LocalDateTime.of(2020, 8, 1, 0, 0), Types.TIMESTAMP);

		// partial trailing date parameters
		stmt.setObject(7, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(8, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(9, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-pagg-month-day-full-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_nodes_Month_Day_noLeading() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-pagg-month-day-noLeading-nodes.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_nodes_Month_Day_noLeading() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// main date parameters
		stmt.setObject(2, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(3, LocalDateTime.of(2020, 8, 1, 0, 0), Types.TIMESTAMP);

		// partial trailing date parameters
		stmt.setObject(4, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(5, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(6, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-pagg-month-day-noLeading-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_nodes_Month_Day_noTrailing() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("select-datum-pagg-month-day-noTrailing-nodes.sql",
						TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_nodes_Month_Day_noTrailing() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// partial leading date parameters
		stmt.setObject(2, filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1), Types.TIMESTAMP);
		stmt.setObject(3, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(4, filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1).plusMonths(1),
				Types.TIMESTAMP);

		// main date parameters
		stmt.setObject(5, LocalDateTime.of(2020, 7, 1, 0, 0), Types.TIMESTAMP);
		stmt.setObject(6, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(),
				equalToTextResource("select-datum-pagg-month-day-noTrailing-nodes.sql",
						TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_nodes_Month_Day_noPartial() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-pagg-month-day-noPartial-nodes.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_nodes_Month_Day_noPartial() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// main date parameters
		stmt.setObject(2, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(3, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-pagg-month-day-noPartial-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_find_nodes_Day_Hour_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setPartialAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 10, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 7, 15, 10, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-pagg-day-hour-full-nodes.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_find_nodes_Day_Hour_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setPartialAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 10, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 10, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		// set metadata parameters
		Array nodeIdsArray = createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// partial leading date parameters
		stmt.setObject(2, filter.getLocalStartDate().with(ChronoField.HOUR_OF_DAY, 0), Types.TIMESTAMP);
		stmt.setObject(3, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(4, filter.getLocalStartDate().with(ChronoField.HOUR_OF_DAY, 0).plusDays(1),
				Types.TIMESTAMP);

		// main date parameters
		stmt.setObject(5, LocalDateTime.of(2020, 6, 16, 0, 0), Types.TIMESTAMP);
		stmt.setObject(6, LocalDateTime.of(2020, 8, 15, 0, 0), Types.TIMESTAMP);

		// partial trailing date parameters
		stmt.setObject(7, filter.getLocalEndDate().with(ChronoField.HOUR_OF_DAY, 0), Types.TIMESTAMP);
		stmt.setObject(8, filter.getLocalEndDate().with(ChronoField.HOUR_OF_DAY, 0), Types.TIMESTAMP);
		stmt.setObject(9, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-pagg-day-hour-full-nodes.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

}
