/* ==================================================================
 * SelectDatumAuxiliaryTests.java - 28/11/2020 10:07:49 am
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
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumAuxiliary;

/**
 * Test cases for the {@link SelectDatumAuxiliary} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumAuxiliaryTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_nodesAndSources_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectDatumAuxiliary(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-aux-nodesAndSources-dates.sql",
				TestSqlResources.class));
	}

	@Test
	public void sql_nodesAndSourcesAndType_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

		// WHEN
		String sql = new SelectDatumAuxiliary(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-aux-nodesAndSourcesAndType-dates.sql", TestSqlResources.class));
	}

	@Test
	public void prep_nodesAndSourcesAndType_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

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

		stmt.setString(3, filter.getDatumAuxiliaryType().name());
		stmt.setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		stmt.setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatumAuxiliary(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-aux-nodesAndSourcesAndType-dates.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

}
