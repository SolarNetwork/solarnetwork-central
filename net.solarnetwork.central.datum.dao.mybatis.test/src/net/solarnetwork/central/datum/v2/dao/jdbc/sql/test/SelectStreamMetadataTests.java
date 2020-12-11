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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.equalToTextResource;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.security.MessageDigest;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStreamMetadata;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link SelectStreamMetadata} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectStreamMetadataTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_streamMeta() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());

		// WHEN
		String sql = new SelectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("stream-meta.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_sortObjSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("obj", "source"));

		// WHEN
		String sql = new SelectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-sortObjSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("node", "source"));

		// WHEN
		String sql = new SelectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-sortObjSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_sortLocSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		filter.setSorts(SimpleSortDescriptor.sorts("loc", "source"));

		// WHEN
		String sql = new SelectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-sortObjSource.sql", TestSqlResources.class));
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
		PreparedStatement result = new SelectStreamMetadata(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, streamIdsArray);
	}

	@Test
	public void cacheKey_oneStreamId() throws Exception {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		String streamIdString = "043eef9f-08ec-4977-bbfd-09957d68002a";
		UUID streamId = UUID.fromString(streamIdString);
		filter.setStreamId(streamId);

		// WHEN
		String cacheKey = new SelectStreamMetadata(filter, 1L, null).getCacheKey();

		// THEN
		assertThat("Cache key matches", cacheKey, equalTo(streamIdString));
	}

	@Test
	public void cacheKey_multiStreamIds() throws Exception {
		// GIVEN
		String[] streamIdStrings = new String[] {
				// @formatter:off
				"043eef9f-08ec-4977-bbfd-09957d68002a",
				"30be6109-c4e7-49a1-a9c1-a1585905b8ff",
				"a0f2037b-e495-4227-b8dc-d5e064211283",
				// @formatter:on
		};
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamIds(stream(streamIdStrings).map(UUID::fromString).toArray(UUID[]::new));

		// WHEN
		String cacheKey = new SelectStreamMetadata(filter, 1L, null).getCacheKey();

		// THEN
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		UUID[] sortedStreamIds = new UUID[streamIdStrings.length];
		System.arraycopy(filter.getStreamIds(), 0, sortedStreamIds, 0, sortedStreamIds.length);
		Arrays.sort(sortedStreamIds);
		assertThat("Sorted order different from given order", sortedStreamIds,
				allOf(arrayContainingInAnyOrder(filter.getStreamIds()),
						not(arrayContaining(filter.getStreamIds()))));
		String digestInputString = stream(sortedStreamIds).map(UUID::toString).collect(joining())
				.replace("-", "");
		byte[] sha1 = digest.digest(ByteUtils.decodeHexString(digestInputString));
		String sha1Hex = ByteUtils.encodeHexString(sha1, 0, sha1.length, false, true);

		assertThat("Cache key matches", cacheKey, equalTo(sha1Hex));
	}

	@Test
	public void sql_streamMeta_geo_sortLocSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSorts(sorts("loc", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		locFilter.setRegion("Wellington");
		locFilter.setStateOrProvince("Wellywood");
		locFilter.setLocality("Te Aro");
		locFilter.setPostalCode("6011");
		locFilter.setStreet("Eva Street");
		locFilter.setTimeZoneId("Pacific/Auckland");
		filter.setLocation(locFilter);

		// WHEN
		String sql = new SelectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("stream-meta-geo-sortObjSource.sql", TestSqlResources.class));
	}

}
