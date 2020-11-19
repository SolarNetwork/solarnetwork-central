/* ==================================================================
 * StreamMetadataPreparedStatementCreatorTests.java - 19/11/2020 3:51:12 pm
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
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.StreamMetadataPreparedStatementCreator;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.test.TestSqlResources;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link StreamMetadataPreparedStatementCreator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StreamMetadataPreparedStatementCreatorTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_streamMeta() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());

		// WHEN
		String sql = new StreamMetadataPreparedStatementCreator(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("stream-meta.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_orderByObjSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("obj", "source"));

		// WHEN
		String sql = new StreamMetadataPreparedStatementCreator(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-orderByObjSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_orderByNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("node", "source"));

		// WHEN
		String sql = new StreamMetadataPreparedStatementCreator(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-orderByObjSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_orderByLocSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("loc", "source"));

		// WHEN
		String sql = new StreamMetadataPreparedStatementCreator(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-orderByObjSource.sql", TestSqlResources.class));
	}

	@Test
	public void prep_streamMeta() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array streamIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("uuid"), aryEq(filter.getStreamIds()))).andReturn(streamIdsArray);
		stmt.setArray(1, streamIdsArray);
		streamIdsArray.free();

		// WHEN
		replay(con, stmt, streamIdsArray);
		PreparedStatement result = new StreamMetadataPreparedStatementCreator(filter)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, streamIdsArray);
	}

}
