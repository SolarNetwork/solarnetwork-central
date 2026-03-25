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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static org.assertj.core.api.BDDAssertions.and;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.PreparedStatementCreator;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link DatumSqlUtils} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DatumSqlUtilsTests {

	@Mock
	private Connection connection;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array sqlArray;

	@Mock
	private Array sqlArray2;

	@Test
	public void nodeMetadataFilterSql_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("No parameters added")
			.isEqualTo(0)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forNode() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Node ID parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.node_id = ?
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forNodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Node IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.node_id = ANY(?)
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("a/*");

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Source ID parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.source_id ~ solarcommon.ant_pattern_to_regexp(?)
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forSource_nonWildcard() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("a");

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Source ID parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.source_id = ?
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceIds(new String[] { "a/*", "b/*" });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Source IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forSources_nonWildcard() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceIds(new String[] { "a", "b" });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Source IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.source_id = ANY(?)
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forStream() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Stream ID parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.stream_id = ?
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forStreams() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamIds(new UUID[] { UUID.randomUUID(), UUID.randomUUID() });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Stream IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.stream_id = ANY(?)
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forUser() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(1L);

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("User IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE un.user_id = ?
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forUsers() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("User IDs parameter added")
			.isEqualTo(1)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					INNER JOIN solaruser.user_node un ON un.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE un.user_id = ANY(?)
					""")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataFilterSql_forNodesAndSources() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });

		// WHEN
		StringBuilder buf = new StringBuilder();
		int count = DatumSqlUtils.nodeMetadataFilterSql(filter, buf);

		// THEN
		// @formatter:off
		and.then(count)
			.as("Node IDs and source IDs parameters added")
			.isEqualTo(2)
			;
		and.then(buf.toString())
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace("""
					SELECT s.stream_id, s.node_id, s.source_id, s.names_i, s.names_a, s.names_s, s.jdata,
						'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone
					FROM solardatm.da_datm_meta s
					LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id
					LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id
					WHERE s.node_id = ANY(?)
					AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))
					""")
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forNode() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(randomLong());

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setObject(1, filter.getNodeId());

		and.then(count)
			.as("Node ID parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forNodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });

		given(connection.createArrayOf(eq("bigint"), eq(filter.getNodeIds()))).willReturn(sqlArray);

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setArray(eq(1), same(sqlArray));
		then(sqlArray).should().free();

		and.then(count)
			.as("Node IDs parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forSource() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId(randomString());

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setString(1, filter.getSourceId());

		and.then(count)
			.as("Source ID parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forSources() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceIds(new String[] { "a/*", "b/*" });

		given(connection.createArrayOf(eq("text"), eq(filter.getSourceIds()))).willReturn(sqlArray);

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setArray(eq(1), same(sqlArray));
		then(sqlArray).should().free();

		and.then(count)
			.as("Source IDs parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forStream() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(randomUUID());

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setObject(1, filter.getStreamId());

		and.then(count)
			.as("Stream ID parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forStreams() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamIds(new UUID[] { UUID.randomUUID(), UUID.randomUUID() });

		given(connection.createArrayOf(eq("uuid"), eq(filter.getStreamIds()))).willReturn(sqlArray);

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setArray(eq(1), same(sqlArray));
		then(sqlArray).should().free();

		and.then(count)
			.as("Stream IDs parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forUser() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(randomLong());

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setObject(1, filter.getUserId());

		and.then(count)
			.as("User ID parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forUsers() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });

		given(connection.createArrayOf(eq("bigint"), eq(filter.getUserIds()))).willReturn(sqlArray);

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setArray(eq(1), same(sqlArray));
		then(sqlArray).should().free();

		and.then(count)
			.as("User IDs parameter set")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forNodeAndSource() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setObject(1, filter.getNodeId());

		then(stmt).should().setString(2, filter.getSourceId());

		and.then(count)
			.as("Node ID and source ID parameters set")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	@Test
	public void prepareObjectMetadataFilter_forNodesAndSources() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });

		given(connection.createArrayOf(eq("bigint"), eq(filter.getNodeIds()))).willReturn(sqlArray);

		given(connection.createArrayOf(eq("text"), eq(filter.getSourceIds()))).willReturn(sqlArray2);

		// WHEN
		int count = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, connection,
				stmt, 0);

		// THEN
		// @formatter:off
		then(stmt).should().setArray(eq(1), same(sqlArray));
		then(sqlArray).should().free();

		then(stmt).should().setArray(eq(2), same(sqlArray2));
		then(sqlArray2).should().free();

		and.then(count)
			.as("Node IDs and source IDs parameters set")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	public static interface PreparedStatementCreatorWithCount
			extends PreparedStatementCreator, CountPreparedStatementCreatorProvider {
		// marker API for EasyMock
	}

	@Test
	public void hasMetadataSortKey_no() {
		// WHEN
		assertThat("Metadata key not present",
				DatumSqlUtils.hasMetadataSortKey(sorts("time", "foo", "bar")), equalTo(false));
	}

	@Test
	public void hasMetadataSortKey_yes() {
		assertThat("'loc' is metadata key", DatumSqlUtils.hasMetadataSortKey(sorts("time", "loc")),
				equalTo(true));
		assertThat("'node' is metadata key", DatumSqlUtils.hasMetadataSortKey(sorts("time", "node")),
				equalTo(true));
		assertThat("'source' is metadata key", DatumSqlUtils.hasMetadataSortKey(sorts("time", "source")),
				equalTo(true));
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_noDateRange_latest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, true, buf);

		// THEN
		and.then(result).as("No parameters generated").isZero();
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
					ORDER BY datum.ts DESC
					LIMIT 1
				) late ON late.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_noDateRange_earliest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, false, buf);

		// THEN
		and.then(result).as("No parameters generated").isZero();
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
					ORDER BY datum.ts
					LIMIT 1
				) early ON early.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_dateRange_latest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, true, buf);

		// THEN
		and.then(result).as("Start/date parameters generated").isEqualTo(2);
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
						AND datum.ts >= ?
						AND datum.ts < ?
					ORDER BY datum.ts DESC
					LIMIT 1
				) late ON late.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_dateRange_earliest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, false, buf);

		// THEN
		and.then(result).as("Start/date parameters generated").isEqualTo(2);
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
						AND datum.ts >= ?
						AND datum.ts < ?
					ORDER BY datum.ts
					LIMIT 1
				) early ON early.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_localDateRange_latest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocalStartDate(LocalDateTime.now());
		filter.setLocalEndDate(filter.getLocalStartDate().plusHours(1));

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, true, buf);

		// THEN
		and.then(result).as("Local start/date parameters generated").isEqualTo(2);
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
						AND datum.ts >= ? AT TIME ZONE s.time_zone
						AND datum.ts < ? AT TIME ZONE s.time_zone
					ORDER BY datum.ts DESC
					LIMIT 1
				) late ON late.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_localDateRange_earliest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocalStartDate(LocalDateTime.now());
		filter.setLocalEndDate(filter.getLocalStartDate().plusHours(1));

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.da_datm",
				Aggregation.None, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, false, buf);

		// THEN
		and.then(result).as("Local start/date parameters generated").isEqualTo(2);
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.da_datm datum
					WHERE datum.stream_id = s.stream_id
						AND datum.ts >= ? AT TIME ZONE s.time_zone
						AND datum.ts < ? AT TIME ZONE s.time_zone
					ORDER BY datum.ts
					LIMIT 1
				) early ON early.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_agg_hourly_latest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.agg_datm_hourly",
				Aggregation.Hour, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, true, buf);

		// THEN
		and.then(result).as("No parameters generated").isZero();
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.agg_datm_hourly datum
					WHERE datum.stream_id = s.stream_id
					ORDER BY datum.ts_start DESC
					LIMIT 1
				) late ON late.stream_id = s.stream_id
				""");
	}

	@Test
	public void joinStreamMetadataExtremeDatumSql_agg_hourly_dateRange_latest() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.HOURS));

		// WHEN
		StringBuilder buf = new StringBuilder();
		int result = DatumSqlUtils.joinStreamMetadataExtremeDatumSql(filter, "solardatm.agg_datm_hourly",
				Aggregation.Hour, DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, true, buf);

		// THEN
		and.then(result).as("Start/date parameters generated").isEqualTo(2);
		and.then(buf.toString()).as("SQL generated").isEqualToIgnoringWhitespace("""
				INNER JOIN LATERAL (
					SELECT datum.*
					FROM solardatm.agg_datm_hourly datum
					WHERE datum.stream_id = s.stream_id
						AND datum.ts_start >= ?
						AND datum.ts_start < ?
					ORDER BY datum.ts_start DESC
					LIMIT 1
				) late ON late.stream_id = s.stream_id
				""");
	}
}
