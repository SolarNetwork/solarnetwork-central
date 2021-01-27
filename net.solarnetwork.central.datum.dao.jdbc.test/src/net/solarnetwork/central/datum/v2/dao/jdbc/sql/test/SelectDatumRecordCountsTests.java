/* ==================================================================
 * SelectDatumRecordCountsTests.java - 5/12/2020 4:05:03 pm
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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRecordCounts;

/**
 * Test cases for the {@link SelectDatumRecordCounts} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectDatumRecordCountsTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_drc_nodesAndSources_localDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2021, 1, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumRecordCounts(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("drc-nodesAndSources-localDates.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_drc_nodesAndSources_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2021, 1, 1, 0, 0));

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		for ( int i = 0; i < 4; i++ ) {
			stmt.setObject(3 + (i * 2), filter.getLocalStartDate(), Types.TIMESTAMP);
			stmt.setObject(3 + (i * 2) + 1, filter.getLocalEndDate(), Types.TIMESTAMP);
		}

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectDatumRecordCounts(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"drc-nodesAndSources-localDates.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

}
