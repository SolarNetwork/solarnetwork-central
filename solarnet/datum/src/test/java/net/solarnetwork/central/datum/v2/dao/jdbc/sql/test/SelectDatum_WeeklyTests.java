/* ==================================================================
 * SelectDatum_WeeklyTests.java - 5/08/2022 7:03:00 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql.test;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for {@link SelectDatum} with the week-based aggregation types.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SelectDatum_WeeklyTests {

	private static final Logger log = LoggerFactory.getLogger(SelectDatum_WeeklyTests.class);

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array nodeIdsArray;

	@Mock
	private Array sourceIdsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);
	}

	private void givenSetNodeIdsArrayParameter(Long[] value) throws SQLException {
		given(con.createArrayOf(eq("bigint"), aryEq(value))).willReturn(nodeIdsArray);
	}

	private void givenSetSourceIdsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(sourceIdsArray);
	}

	@Test
	public void find_weekly_nodesAndSources_absoluteDates_sql() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Week);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(365, ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plus(366, ChronoUnit.DAYS));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource(
				"select-datum-weekly-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_weekly_nodesAndSources_absoluteDates_sql_prep() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Week);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		givenPrepStatement();
		givenSetNodeIdsArrayParameter(filter.getNodeIds());
		givenSetSourceIdsArrayParameter(filter.getSourceIds());

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-weekly-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
		verify(stmt).setArray(1, nodeIdsArray);
		verify(stmt).setArray(2, sourceIdsArray);
		verify(stmt).setTimestamp(3, Timestamp.from(filter.getStartDate()));
		verify(stmt).setTimestamp(4, Timestamp.from(filter.getEndDate()));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

	@Test
	public void find_woy_nodesAndSources_absoluteDates_sql() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.WeekOfYear);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("select-datum-woy-nodesAndSources-dates.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void find_woy_nodesAndSources_absoluteDates_prep() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.WeekOfYear);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		givenPrepStatement();
		givenSetNodeIdsArrayParameter(filter.getNodeIds());
		givenSetSourceIdsArrayParameter(filter.getSourceIds());

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture(), eq(ResultSet.TYPE_FORWARD_ONLY),
				eq(ResultSet.CONCUR_READ_ONLY), eq(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"select-datum-woy-nodesAndSources-dates.sql", TestSqlResources.class, SQL_COMMENT));
		verify(stmt).setArray(1, nodeIdsArray);
		verify(stmt).setArray(2, sourceIdsArray);
		verify(stmt).setTimestamp(3, Timestamp.from(filter.getStartDate()));
		verify(stmt).setTimestamp(4, Timestamp.from(filter.getEndDate()));
		assertThat("Connection statement returned", result, sameInstance(stmt));
	}

}
