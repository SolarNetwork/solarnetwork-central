/* ==================================================================
 * UpdateObjectStreamMetadataJsonTests.java - 27/11/2020 5:01:29 pm
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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectStreamMetadataJson;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;

/**
 * Test casees for the {@link UpdateObjectStreamMetadataJson} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateObjectStreamMetadataJsonTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_node() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setObjectKind(ObjectDatumKind.Node);

		String json = "{\"foo\":\"bar\"}";

		// WHEN
		String sql = new UpdateObjectStreamMetadataJson(filter, json).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("update-node-meta-json.sql", TestSqlResources.class));
	}

	@Test
	public void prep_node() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setObjectKind(ObjectDatumKind.Node);

		String json = "{\"foo\":\"bar\"}";

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, filter.getNodeId());
		stmt.setString(2, filter.getSourceId());
		stmt.setString(3, json);

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new UpdateObjectStreamMetadataJson(filter, json)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-node-meta-json.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

	@Test
	public void sql_location() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setObjectKind(ObjectDatumKind.Location);

		String json = "{\"foo\":\"bar\"}";

		// WHEN
		String sql = new UpdateObjectStreamMetadataJson(filter, json).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("update-loc-meta-json.sql", TestSqlResources.class));
	}

	@Test
	public void prep_location() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setObjectKind(ObjectDatumKind.Location);

		String json = "{\"foo\":\"bar\"}";

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, filter.getLocationId());
		stmt.setString(2, filter.getSourceId());
		stmt.setString(3, json);

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new UpdateObjectStreamMetadataJson(filter, json)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-loc-meta-json.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

}
