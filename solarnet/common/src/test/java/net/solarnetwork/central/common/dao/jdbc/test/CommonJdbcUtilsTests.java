/* ==================================================================
 * CommonJdbcUtilsTests.java - 3/08/2022 11:20:45 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link CommonJdbcUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CommonJdbcUtilsTests {

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
		RowMapper<LocationRequest> mapper = EasyMock.createMock(RowMapper.class);
		PreparedStatementCreator countSql = EasyMock.createMock(PreparedStatementCreator.class);

		// execute count query
		expect(sql.countPreparedStatementCreator()).andReturn(countSql);
		expect(jdbcTemplate.query(same(countSql), anyObject(ResultSetExtractor.class))).andReturn(123L);

		// execute actual query
		LocationRequest datum = new LocationRequest();
		List<LocationRequest> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper, countSql);
		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setMax(1);
		filter.setOffset(1);
		FilterResults<LocationRequest, Long> results = CommonJdbcUtils.executeFilterQuery(jdbcTemplate,
				filter, sql, mapper);

		// THEN
		assertThat("Result returned", results, notNullValue());
		assertThat("Result count from data", results.getReturnedResultCount(), equalTo(data.size()));
		assertThat("Result offset from criteria", results.getStartingOffset(), equalTo(1));
		assertThat("Result total available with CountPreparedStatementCreatorProvider implementation",
				results.getTotalResults(), equalTo(123L));
		assertThat("Result results is data", results.getResults(), sameInstance(data));
		verify(jdbcTemplate, sql, mapper, countSql);
	}

	@Test
	public void executeFilterQuery_noCountSupport() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreator sql = EasyMock.createMock(PreparedStatementCreator.class);
		@SuppressWarnings("unchecked")
		RowMapper<LocationRequest> mapper = EasyMock.createMock(RowMapper.class);

		LocationRequest datum = new LocationRequest();
		List<LocationRequest> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper);
		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setMax(1);
		filter.setOffset(1);
		FilterResults<LocationRequest, Long> results = CommonJdbcUtils.executeFilterQuery(jdbcTemplate,
				filter, sql, mapper);

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

	@Test
	public void executeFilterQuery_page_withoutTotalResultsCount() throws SQLException {
		// GIVEN
		JdbcOperations jdbcTemplate = EasyMock.createMock(JdbcOperations.class);
		PreparedStatementCreator sql = EasyMock.createMock(PreparedStatementCreator.class);
		@SuppressWarnings("unchecked")
		RowMapper<LocationRequest> mapper = EasyMock.createMock(RowMapper.class);

		LocationRequest datum = new LocationRequest();
		List<LocationRequest> data = Collections.singletonList(datum);
		expect(jdbcTemplate.query(sql, mapper)).andReturn(data);

		// WHEN
		replay(jdbcTemplate, sql, mapper);
		BasicCoreCriteria filter = new BasicCoreCriteria();
		filter.setMax(1);
		FilterResults<LocationRequest, Long> results = CommonJdbcUtils.executeFilterQuery(jdbcTemplate,
				filter, sql, mapper);

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
