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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.FilterResults;

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
		assertThat(buf.toString().contains("WHERE s.node_id = ANY(?)"), equalTo(true));
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
		assertThat("Source IDs parameter added", count, equalTo(1));
		assertThat(buf.toString().contains(
				"WHERE s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))"),
				equalTo(true));
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
		assertThat(buf.toString().contains("WHERE s.stream_id = ANY(?)"), equalTo(true));
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
		assertThat(buf.toString().contains("WHERE s.node_id = ANY(?)"), equalTo(true));
		assertThat(buf.toString().contains(
				"AND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))"),
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

	@Test
	public void executeFilterQuery_noCountSupport() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreator sql = EasyMock.createMock(PreparedStatementCreator.class);
		@SuppressWarnings("unchecked")
		RowMapper<Datum> mapper = EasyMock.createMock(RowMapper.class);

		Datum datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, null);
		List<Datum> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setMax(1);
		filter.setOffset(1);
		FilterResults<Datum, DatumPK> results = DatumSqlUtils.executeFilterQuery(jdbcTemplate, filter,
				sql, mapper);

		// THEN
		assertThat("Result returned", results, notNullValue());
		assertThat("Result count from data", results.getReturnedResultCount(), equalTo(data.size()));
		assertThat("Result offset from criteria", results.getStartingOffset(), equalTo(1));
		assertThat(
				"Result total not available without CountPreparedStatementCreatorProvider implementation",
				results.getTotalResults(), nullValue());
		assertThat("Result results is data", results.getResults(), sameInstance(data));
		verify(jdbcTemplate, sql, mapper);
	}

	public static interface PreparedStatementCreatorWithCount
			extends PreparedStatementCreator, CountPreparedStatementCreatorProvider {
		// marker API for EasyMock
	}

	@SuppressWarnings("unchecked")
	@Test
	public void executeFilterQuery_countSupport() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreatorWithCount sql = EasyMock
				.createMock(PreparedStatementCreatorWithCount.class);
		RowMapper<Datum> mapper = EasyMock.createMock(RowMapper.class);
		PreparedStatementCreator countSql = EasyMock.createMock(PreparedStatementCreator.class);

		// execute count query
		expect(sql.countPreparedStatementCreator()).andReturn(countSql);
		expect(jdbcTemplate.query(same(countSql), anyObject(ResultSetExtractor.class))).andReturn(123L);

		// execute actual query
		Datum datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, null);
		List<Datum> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper, countSql);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setMax(1);
		filter.setOffset(1);
		FilterResults<Datum, DatumPK> results = DatumSqlUtils.executeFilterQuery(jdbcTemplate, filter,
				sql, mapper);

		// THEN
		assertThat("Result returned", results, notNullValue());
		assertThat("Result count from data", results.getReturnedResultCount(), equalTo(data.size()));
		assertThat("Result offset from criteria", results.getStartingOffset(), equalTo(1));
		assertThat(
				"Result total not available without CountPreparedStatementCreatorProvider implementation",
				results.getTotalResults(), equalTo(123L));
		assertThat("Result results is data", results.getResults(), sameInstance(data));
		verify(jdbcTemplate, sql, mapper, countSql);
	}

	@Test
	public void executeFilterQuery_withoutTotalResultsCount() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreator sql = EasyMock.createMock(PreparedStatementCreator.class);
		@SuppressWarnings("unchecked")
		RowMapper<Datum> mapper = EasyMock.createMock(RowMapper.class);

		Datum datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, null);
		List<Datum> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(true);
		FilterResults<Datum, DatumPK> results = DatumSqlUtils.executeFilterQuery(jdbcTemplate, filter,
				sql, mapper);

		// THEN
		assertThat("Result returned", results, notNullValue());
		assertThat("Result count from data", results.getReturnedResultCount(), equalTo(data.size()));
		assertThat("Result offset", results.getStartingOffset(), equalTo(0));
		assertThat("Result total set given withoutTotalResultsCount but without max",
				results.getTotalResults(), equalTo(1L));
		assertThat("Result results is data", results.getResults(), sameInstance(data));
		verify(jdbcTemplate, sql, mapper);
	}

	@Test
	public void executeFilterQuery_page_withoutTotalResultsCount() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreator sql = EasyMock.createMock(PreparedStatementCreator.class);
		@SuppressWarnings("unchecked")
		RowMapper<Datum> mapper = EasyMock.createMock(RowMapper.class);

		Datum datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, null);
		List<Datum> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(true);
		filter.setMax(1);
		FilterResults<Datum, DatumPK> results = DatumSqlUtils.executeFilterQuery(jdbcTemplate, filter,
				sql, mapper);

		// THEN
		assertThat("Result returned", results, notNullValue());
		assertThat("Result count from data", results.getReturnedResultCount(), equalTo(data.size()));
		assertThat("Result offset", results.getStartingOffset(), equalTo(0));
		assertThat("Result total not set given withoutTotalResultsCount", results.getTotalResults(),
				nullValue());
		assertThat("Result results is data", results.getResults(), sameInstance(data));
		verify(jdbcTemplate, sql, mapper);
	}
}
