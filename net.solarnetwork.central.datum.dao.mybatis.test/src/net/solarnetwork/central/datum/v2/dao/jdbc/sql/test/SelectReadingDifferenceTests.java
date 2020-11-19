/* ==================================================================
 * ReadingDatumCriteriaPreparedStatementCreatorTests.java - 17/11/2020 2:32:26 pm
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
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link SelectReadingDifference} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectReadingDifferenceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_diff_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("reading-diff-nodes-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_diff_nodesAndSources_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("reading-diff-nodesAndSources-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"reading-diff-nodesAndSourcesAndUsers-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_absoluteDates_orderByNodeSourceTime() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setSorts(SimpleSortDescriptor.sorts("node", "source", "time"));

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource(
						"reading-diff-nodesAndSourcesAndUsers-dates-orderByNodeSourceTime.sql",
						TestSqlResources.class));
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_localDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"reading-diff-nodesAndSourcesAndUsers-localDates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_diffWithin_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.DifferenceWithin);
		filter.setNodeId(1L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("reading-diffwithin-nodes-dates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_diffNear_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.NearestDifference);
		filter.setNodeId(1L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("reading-diffnear-nodes-dates.sql", TestSqlResources.class));
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

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

		Capture<Timestamp> startCaptor = new Capture<>();
		stmt.setTimestamp(eq(4), capture(startCaptor));

		Capture<Timestamp> endCaptor = new Capture<>();
		stmt.setTimestamp(eq(5), capture(endCaptor));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectReadingDifference(filter)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Start timestamp", startCaptor.getValue(),
				equalTo(Timestamp.from(filter.getStartDate())));
		assertThat("End timestamp", endCaptor.getValue(), equalTo(Timestamp.from(filter.getEndDate())));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_localDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

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

		stmt.setObject(eq(4), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));

		stmt.setObject(eq(5), eq(filter.getLocalEndDate()), eq(Types.TIMESTAMP));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectReadingDifference(filter)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_nearDiff() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.NearestDifference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
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

		Capture<Timestamp> startCaptor = new Capture<>();
		stmt.setTimestamp(eq(4), capture(startCaptor));

		Capture<Timestamp> endCaptor = new Capture<>();
		stmt.setTimestamp(eq(5), capture(endCaptor));

		Capture<Object> toleranceCaptor = new Capture<>();
		stmt.setObject(eq(6), capture(toleranceCaptor), eq(Types.OTHER));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectReadingDifference(filter)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		assertThat("Start timestamp", startCaptor.getValue(),
				equalTo(Timestamp.from(filter.getStartDate())));
		assertThat("End timestamp", endCaptor.getValue(), equalTo(Timestamp.from(filter.getEndDate())));
		assertThat("Tolerance prarameter", toleranceCaptor.getValue(),
				equalTo(filter.getTimeTolerance()));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

}
