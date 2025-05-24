/* ==================================================================
 * DeleteDatumTests.java - 6/12/2020 8:21:25 am
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatum;

/**
 * Test cases for the {@link DeleteDatum} class.
 *
 * @author matt
 * @version 1.1
 */
public class DeleteDatumTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_drc_nodesAndSources_localDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2021, 1, 1, 0, 0));

		// WHEN
		String sql = new DeleteDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("delete-datum-nodesAndSources-localDates.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep_drc_nodesAndSources_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
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

		stmt.setObject(3, filter.getLocalStartDate(), Types.TIMESTAMP);
		stmt.setObject(4, filter.getLocalEndDate(), Types.TIMESTAMP);

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		PreparedStatement result = new DeleteDatum(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"delete-datum-nodesAndSources-localDates.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}

}
