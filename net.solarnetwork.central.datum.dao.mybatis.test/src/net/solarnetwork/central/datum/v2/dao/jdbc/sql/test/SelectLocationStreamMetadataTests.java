/* ==================================================================
 * SelectLocationStreamMetadataTests.java - 19/11/2020 3:51:12 pm
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
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectLocationStreamMetadata;

/**
 * Test cases for the {@link SelectLocationStreamMetadata}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectLocationStreamMetadataTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_streamMeta_locsAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");

		// WHEN
		String sql = new SelectLocationStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("loc-stream-meta-locsAndSources.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_locsAndSourcesAndUsers() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectLocationStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("loc-stream-meta-locsAndSourcesAndUsers.sql",
				TestSqlResources.class));
	}

	@Test
	public void prep_streamMeta_locsAndSourcesAndUsers() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L);

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array locIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getLocationIds()))).andReturn(locIdsArray);
		stmt.setArray(1, locIdsArray);
		locIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(3, userIdsArray);
		userIdsArray.free();

		// WHEN
		replay(con, stmt, locIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectLocationStreamMetadata(filter)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, locIdsArray, sourceIdsArray, userIdsArray);
	}

}
