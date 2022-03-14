/* ==================================================================
 * SelectDatumCalculatedAtTests.java - 19/11/2020 2:55:36 pm
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
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumCalculatedAt;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link SelectDatumCalculatedAt} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumCalculatedAtTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_calcAt_nodes_absoluteDate() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setStartDate(start.toInstant());

		// WHEN
		String sql = new SelectDatumCalculatedAt(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("calc-at-nodes-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_calcAt_nodesAndSources_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());

		// WHEN
		String sql = new SelectDatumCalculatedAt(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("calc-at-nodesAndSources-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_calcAt_nodesAndSourcesAndUsers_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());

		// WHEN
		String sql = new SelectDatumCalculatedAt(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("calc-at-nodesAndSourcesAndUsers-dates.sql",
				TestSqlResources.class));
	}

	@Test
	public void sql_calcAt_nodesAndSourcesAndUsers_absoluteDates_orderByNodeSourceTime() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setSorts(SimpleSortDescriptor.sorts("node", "source", "time"));

		// WHEN
		String sql = new SelectDatumCalculatedAt(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("calc-at-nodesAndSourcesAndUsers-dates-orderByNodeSourceTime.sql",
						TestSqlResources.class));
	}

	@Test
	public void sql_calcAt_nodesAndSourcesAndUsers_localDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setLocalStartDate(start.toLocalDateTime());

		// WHEN
		String sql = new SelectDatumCalculatedAt(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"calc-at-nodesAndSourcesAndUsers-localDates.sql", TestSqlResources.class));
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setTimeTolerance(Period.ofDays(7));

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

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(3, userIdsArray);
		userIdsArray.free();

		stmt.setTimestamp(4, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(5, Timestamp.from(filter.getStartDate()));
		stmt.setObject(6, filter.getTimeTolerance(), Types.OTHER);

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectDatumCalculatedAt(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_localDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setTimeTolerance(Period.ofDays(7));

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

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(3, userIdsArray);
		userIdsArray.free();

		stmt.setObject(4, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(5, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(6, filter.getTimeTolerance(), Types.OTHER);

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectDatumCalculatedAt(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

}
