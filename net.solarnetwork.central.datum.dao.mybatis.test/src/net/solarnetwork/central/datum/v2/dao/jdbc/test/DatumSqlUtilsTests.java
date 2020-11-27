/* ==================================================================
 * DatumSqlUtilsTests.java - 17/11/2020 12:32:22 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;

/**
 * Test cases for the {@link DatumSqlUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumSqlUtilsTests {

	@Test
	public void nodeMetadataFilterSql_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("No parameters added", count, equalTo(0));
	}

	@Test
	public void nodeMetadataFilterSql_forNodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("Node IDs parameter added", count, equalTo(1));
		assertThat(buf.toString().contains("WHERE meta.node_id = ANY(?)"), equalTo(true));
	}

	@Test
	public void nodeMetadataFilterSql_forSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("a");

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("Node IDs parameter added", count, equalTo(1));
		assertThat(buf.toString().contains("WHERE meta.source_id = ANY(?)"), equalTo(true));
	}

	@Test
	public void nodeMetadataFilterSql_forStreams() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("Node IDs parameter added", count, equalTo(1));
		assertThat(buf.toString().contains("WHERE meta.stream_id = ANY(?)"), equalTo(true));
	}

	@Test
	public void nodeMetadataFilterSql_forUsers() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("User IDs parameter added", count, equalTo(1));
		assertThat(buf.toString().contains("WHERE un.user_id = ANY(?)"), equalTo(true));
	}

	@Test
	public void nodeMetadataFilterSql_forNodesAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		assertThat("Node IDs and source IDs parameters added", count, equalTo(2));
		assertThat(buf.toString().contains("WHERE meta.node_id = ANY(?)\n\tAND meta.source_id = ANY(?)"),
				equalTo(true));
	}

	@Test
	public void objectMetadataFilterPrepare_forNodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		// WHEN
		replay(con, stmt, nodeIdsArray);
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, 0);

		// THEN
		assertThat("Node IDs parameter set", count, equalTo(1));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void objectMetadataFilterPrepare_forSources() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("a");
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(1, sourceIdsArray);
		sourceIdsArray.free();

		// WHEN
		replay(con, stmt, sourceIdsArray);
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, 0);

		// THEN
		assertThat("Source IDs parameter set", count, equalTo(1));
		verify(con, stmt, sourceIdsArray);
	}

	@Test
	public void objectMetadataFilterPrepare_forStreams() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array streamIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("uuid"), aryEq(filter.getStreamIds()))).andReturn(streamIdsArray);
		stmt.setArray(1, streamIdsArray);
		streamIdsArray.free();

		// WHEN
		replay(con, stmt, streamIdsArray);
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, 0);

		// THEN
		assertThat("Stream IDs parameter set", count, equalTo(1));
		verify(con, stmt, streamIdsArray);
	}

	@Test
	public void objectMetadataFilterPrepare_forUsers() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array userIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).andReturn(userIdsArray);
		stmt.setArray(1, userIdsArray);
		userIdsArray.free();

		// WHEN
		replay(con, stmt, userIdsArray);
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, 0);

		// THEN
		assertThat("User IDs parameter set", count, equalTo(1));
		verify(con, stmt, userIdsArray);
	}

	@Test
	public void objectMetadataFilterPrepare_forNodesAndSources() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(1, nodeIdsArray);
		nodeIdsArray.free();

		Array sourceIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).andReturn(sourceIdsArray);
		stmt.setArray(2, sourceIdsArray);
		sourceIdsArray.free();

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray);
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, 0);

		// THEN
		assertThat("Node IDs and source IDs parameters set", count, equalTo(2));
		verify(con, stmt, nodeIdsArray, sourceIdsArray);
	}
}
