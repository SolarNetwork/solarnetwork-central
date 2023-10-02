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

import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.MetadataSelectStyle;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectObjectStreamMetadata;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SelectObjectStreamMetadata} class.
 * 
 * @author matt
 * @version 1.1
 */
public class SelectObjectStreamMetadataTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Test
	public void sql_streamMeta_nodesAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("node-stream-meta-nodesAndSources.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_nodesAndSources_minimum() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node,
				MetadataSelectStyle.Minimum).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"node-stream-meta-minimum-nodesAndSources.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(Instant.now());

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("node-stream-meta-nodes-dates.sql", TestSqlResources.class));
	}

	@Test
	public void prep_streamMeta_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(Instant.now());

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		stmt.setTimestamp(1, Timestamp.from(filter.getStartDate()));
		stmt.setTimestamp(2, Timestamp.from(filter.getEndDate()));

		Array nodeIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).andReturn(nodeIdsArray);
		stmt.setArray(3, nodeIdsArray);
		nodeIdsArray.free();

		// WHEN
		replay(con, stmt, nodeIdsArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("node-stream-meta-nodes-dates.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray);
	}

	@Test
	public void sql_streamMeta_nodesAndSources_localDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setLocalStartDate(LocalDateTime.now().truncatedTo(ChronoUnit.HOURS));
		filter.setLocalEndDate(LocalDateTime.now());

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("node-stream-meta-nodes-localDates.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_nodesAndSourcesAndUsers() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L);

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"node-stream-meta-nodesAndSourcesAndUsers.sql", TestSqlResources.class));
	}

	@Test
	public void prep_streamMeta_nodesAndSourcesAndUsers() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L);

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

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"node-stream-meta-nodesAndSourcesAndUsers.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, userIdsArray);
	}

	@Test
	public void sql_streamMeta_nodesAndSourcesAndTokens() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setTokenId("foobar");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource(
				"node-stream-meta-nodesAndSourcesAndTokens.sql", TestSqlResources.class));
	}

	@Test
	public void prep_streamMeta_nodesAndSourcesAndTokens() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setTokenId("foobar");

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

		Array tokenIdsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getTokenIds()))).andReturn(tokenIdsArray);
		stmt.setArray(3, tokenIdsArray);
		tokenIdsArray.free();

		// WHEN
		replay(con, stmt, nodeIdsArray, sourceIdsArray, tokenIdsArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter).createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"node-stream-meta-nodesAndSourcesAndTokens.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, nodeIdsArray, sourceIdsArray, tokenIdsArray);
	}

	@Test
	public void sql_streamMeta_geo_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setSorts(sorts("node", "source"));
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
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("node-stream-meta-geo-sortNodeSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_geo_fts_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setSorts(sorts("node", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setName("Wellington");
		locFilter.setTimeZoneId("Pacific/Auckland");
		filter.setLocation(locFilter);

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("node-stream-meta-geo-fts-sortNodeSource.sql",
				TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_geo_tag_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setSorts(sorts("node", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		filter.setLocation(locFilter);
		filter.setSearchFilter("(/t=weather)");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"node-stream-meta-geo-tagsAnd-sortNodeSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_streamMeta_geo_tags_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setSorts(sorts("node", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		filter.setLocation(locFilter);
		filter.setSearchFilter("(|(/t=weather)(/t=foo))");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"node-stream-meta-geo-tagsOr-sortNodeSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_loc_streamMeta_locsAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Location).getSql();

		// THEN
		assertThat("SQL matches", sql,
				equalToTextResource("loc-stream-meta-locsAndSources.sql", TestSqlResources.class));
	}

	@Test
	public void sql_loc_streamMeta_locsAndSourcesAndUsers() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L); // ignored for location query

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Location).getSql();

		// THEN
		assertThat("SQL matches", sql, equalToTextResource("loc-stream-meta-locsAndSourcesAndUsers.sql",
				TestSqlResources.class));
	}

	@Test
	public void prep_loc_streamMeta_locsAndSourcesAndUsers() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setUserId(1L); // ignored for location query

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

		// WHEN
		replay(con, stmt, locIdsArray, sourceIdsArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Location)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"loc-stream-meta-locsAndSourcesAndUsers.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, locIdsArray, sourceIdsArray);
	}

	@Test
	public void sql_loc_streamMeta_geo_sortLocSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
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
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql,
				equalToTextResource("loc-stream-meta-geo-sortLocSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_loc_streamMeta_geo_fts_sortLocSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setName("Wellington");
		locFilter.setTimeZoneId("Pacific/Auckland");
		filter.setLocation(locFilter);

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("loc-stream-meta-geo-fts-sortLocSource.sql",
				TestSqlResources.class));
	}

	@Test
	public void sql_loc_streamMeta_geo_tag_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		filter.setLocation(locFilter);
		filter.setSearchFilter("(/t=weather)");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"loc-stream-meta-geo-tagsAnd-sortLocSource.sql", TestSqlResources.class));
	}

	@Test
	public void sql_loc_streamMeta_geo_tags_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		filter.setLocation(locFilter);
		filter.setSearchFilter("(|(/t=weather)(/t=forecast))");

		// WHEN
		String sql = new SelectObjectStreamMetadata(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"loc-stream-meta-geo-tagsOr-sortLocSource.sql", TestSqlResources.class));
	}

	@Test
	public void prep_loc_streamMeta_geo_tags_sortNodeSource() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc", "source"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setCountry("NZ");
		filter.setLocation(locFilter);
		filter.setSearchFilter("(|(/t=weather)(/t=forecast))");

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		stmt.setString(1, locFilter.getCountry());

		Array tagsArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(new String[] { "weather", "forecast" })))
				.andReturn(tagsArray);
		stmt.setArray(2, tagsArray);
		tagsArray.free();

		// WHEN
		replay(con, stmt, tagsArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Location)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"loc-stream-meta-geo-tagsOr-sortLocSource.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, tagsArray);
	}

	@Test
	public void prep_streamMeta_propertyNames() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setPropertyNames(new String[] { "a", "b" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array namesArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getPropertyNames()))).andReturn(namesArray);
		stmt.setArray(1, namesArray);
		stmt.setArray(2, namesArray);
		stmt.setArray(3, namesArray);
		namesArray.free();

		// WHEN
		replay(con, stmt, namesArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("node-stream-meta-propertyNames.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, namesArray);
	}

	@Test
	public void prep_streamMeta_instantaneousPropertyNames() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setInstantaneousPropertyNames(new String[] { "a", "b" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array namesArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getInstantaneousPropertyNames())))
				.andReturn(namesArray);
		stmt.setArray(1, namesArray);
		namesArray.free();

		// WHEN
		replay(con, stmt, namesArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"node-stream-meta-instantaneousPropertyNames.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, namesArray);
	}

	@Test
	public void prep_streamMeta_accumulatingPropertyNames() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setAccumulatingPropertyNames(new String[] { "a", "b" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array namesArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getAccumulatingPropertyNames())))
				.andReturn(namesArray);
		stmt.setArray(1, namesArray);
		namesArray.free();

		// WHEN
		replay(con, stmt, namesArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(), equalToTextResource(
				"node-stream-meta-accumulatingPropertyNames.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, namesArray);
	}

	@Test
	public void prep_streamMeta_statusPropertyNames() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setStatusPropertyNames(new String[] { "a", "b" });

		Connection con = EasyMock.createMock(Connection.class);
		PreparedStatement stmt = EasyMock.createMock(PreparedStatement.class);

		Capture<String> sqlCaptor = new Capture<>();
		expect(con.prepareStatement(capture(sqlCaptor), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).andReturn(stmt);

		Array namesArray = EasyMock.createMock(Array.class);
		expect(con.createArrayOf(eq("text"), aryEq(filter.getStatusPropertyNames())))
				.andReturn(namesArray);
		stmt.setArray(1, namesArray);
		namesArray.free();

		// WHEN
		replay(con, stmt, namesArray);
		PreparedStatement result = new SelectObjectStreamMetadata(filter, ObjectDatumKind.Node)
				.createPreparedStatement(con);

		// THEN
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("SQL matches", sqlCaptor.getValue(),
				equalToTextResource("node-stream-meta-statusPropertyNames.sql", TestSqlResources.class));
		assertThat("Connection statement returned", result, sameInstance(stmt));
		verify(con, stmt, namesArray);
	}

}
