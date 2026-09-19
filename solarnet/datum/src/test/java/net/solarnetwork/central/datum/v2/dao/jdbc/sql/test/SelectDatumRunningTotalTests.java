/* ==================================================================
 * SelectDatumRunningTotalTests.java - 10/12/2020 6:53:37 am
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

import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRunningTotal;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.aliased.test.TestAliasedSqlResources;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link SelectDatumRunningTotal} class.
 *
 * @author matt
 * @version 1.1
 */
@ParameterizedClass
@ValueSource(booleans = { false, true }) // for aliased or not
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectDatumRunningTotalTests {

	@Parameter
	private boolean aliased;

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

	private void thenSqlEqualsResource(String sql, String resource) {
		// @formatter:off
		and.then(sql)
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace(getResourceAsString(
					resource,
					(aliased ? TestAliasedSqlResources.class : TestSqlResources.class),
					SQL_COMMENT))
			;
		// @formatter:on
	}

	@Test
	public void prep_find_runningTotal_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(Instant.now());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumRunningTotal(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-runtot-nodesAndSources-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_runningTotal_nodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeIds(new Long[] { 1L, 2L });

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		final PreparedStatement result = new SelectDatumRunningTotal(filter)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-runtot-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(2), eq(Timestamp.from(Instant.EPOCH)));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_runningTotal_nodes_sortNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSorts(sorts("node", "source"));

		// WHEN
		String sql = new SelectDatumRunningTotal(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-runtot-nodes-sortNodeSource.sql");
	}

	@Test
	public void sql_find_runningTotal_nodes_sortNodeSourceTime() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSorts(sorts("node", "source", "time"));

		// WHEN
		String sql = new SelectDatumRunningTotal(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-runtot-nodes-sortNodeSourceTime.sql");
	}

}
