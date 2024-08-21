/* ==================================================================
 * UpdateObjectStreamMetadataAttributesTests.java - 21/11/2021 3:48:04 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.UpdateObjectStreamMetadataAttributes;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link UpdateObjectStreamMetadataAttributes} class.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateObjectStreamMetadataAttributesTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void node_objectId() throws SQLException {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		Long nodeId = 123L;

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, streamId, Types.OTHER);
		stmt.setObject(2, nodeId);

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new UpdateObjectStreamMetadataAttributes(ObjectDatumKind.Node,
				streamId, nodeId, null).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-node-meta-ids-object.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

	@Test
	public void node_objectId_sourceId() throws SQLException {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		Long nodeId = 123L;
		String sourceId = UUID.randomUUID().toString();

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, streamId, Types.OTHER);
		stmt.setObject(2, nodeId);
		stmt.setString(3, sourceId);

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new UpdateObjectStreamMetadataAttributes(ObjectDatumKind.Node,
				streamId, nodeId, sourceId).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-node-meta-ids-object-source.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

	@Test
	public void location_objectId() throws SQLException {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		Long locId = 321L;

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, streamId, Types.OTHER);
		stmt.setObject(2, locId);

		// WHEN
		replay(con, stmt);
		PreparedStatement result = new UpdateObjectStreamMetadataAttributes(ObjectDatumKind.Location,
				streamId, locId, null).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-loc-meta-ids-object.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt);
	}

	@Test
	public void node_propertyNames() throws SQLException {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		String[] iNames = new String[] { "a", "b", "c" };
		String[] aNames = new String[] { "d", "e" };
		String[] sNames = new String[] { "f" };

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Array iArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("TEXT"), aryEq(iNames))).andReturn(iArray);
		iArray.free();

		Array aArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("TEXT"), aryEq(aNames))).andReturn(aArray);
		aArray.free();

		Array sArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("TEXT"), aryEq(sNames))).andReturn(sArray);
		sArray.free();

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor))).andReturn(stmt);

		stmt.setObject(1, streamId, Types.OTHER);
		stmt.setArray(2, iArray);
		stmt.setArray(3, aArray);
		stmt.setArray(4, sArray);
		stmt.setInt(5, iNames.length);
		stmt.setInt(6, aNames.length);
		stmt.setInt(7, sNames.length);

		// WHEN
		replay(con, stmt, iArray, aArray, sArray);
		PreparedStatement result = new UpdateObjectStreamMetadataAttributes(ObjectDatumKind.Node,
				streamId, null, null, iNames, aNames, sNames).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("update-node-meta-attr-propnames.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, iArray, aArray, sArray);
	}
}
