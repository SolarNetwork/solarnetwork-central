/* ==================================================================
 * SelectDatumPartialAggregateTests.java - 3/12/2020 8:59:15 pm
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
import static java.sql.Types.TIMESTAMP;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.firstDayOfNextMonth;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumPartialAggregate;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.aliased.test.TestAliasedSqlResources;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link SelectDatumPartialAggregate} class.
 *
 * @author matt
 * @version 1.2
 */
@ParameterizedClass
@ValueSource(booleans = { false, true }) // for aliased or not
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectDatumPartialAggregateTests {

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

	@Mock
	private Array virtRealNodeIdsArray;

	@Mock
	private Array virtRealNodeIdsOrderArray;

	@Mock
	private Array virtRealSourceIdsArray;

	@Mock
	private Array virtRealSourceIdsOrderArray;

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
	public void sql_find_nodes_Year_Month_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-year-month-full-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Year_Month_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-year-month-full-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3,
				filter.getLocalStartDate().with(ChronoField.MONTH_OF_YEAR, 1).plusYears(1), TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(4, LocalDateTime.of(2021, 1, 1, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(5, LocalDateTime.of(2023, 1, 1, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(6, filter.getLocalEndDate().with(ChronoField.MONTH_OF_YEAR, 1),
				TIMESTAMP);
		then(stmt).should().setObject(7, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Year_Month_full_customSort() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-year-month-full-nodes-sortTimeNodeSource.sql");
	}

	@Test
	public void sql_find_nodes_Year_Month_virtual_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-year-month-virtual-full-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Year_Month_virtual_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// @formatter:off
		given(con.createArrayOf(eq("bigint"), eq(filter.getNodeIds())))
			.willReturn(virtRealNodeIdsArray)
			.willReturn(virtRealNodeIdsOrderArray)
			.willReturn(nodeIdsArray)
			;

		given(con.createArrayOf(eq("text"), aryEq(new String[] { "a", "b", "c" })))
			.willReturn(virtRealSourceIdsArray)
			.willReturn(virtRealSourceIdsOrderArray)
			;
		// @formatter:on

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-pagg-year-month-virtual-full-nodes.sql");

		// set virtual node ID parameters
		then(stmt).should().setArray(eq(1), same(virtRealNodeIdsArray));
		then(virtRealNodeIdsArray).should().free();
		then(stmt).should().setObject(2, 100L);

		// set virtual node ID ordering
		then(stmt).should().setArray(eq(3), same(virtRealNodeIdsOrderArray));
		then(virtRealNodeIdsOrderArray).should().free();

		// set virtual source ID parameters
		then(stmt).should().setArray(eq(4), same(virtRealSourceIdsArray));
		then(virtRealSourceIdsArray).should().free();
		then(stmt).should().setString(5, "V");

		// set virtual source ID ordering
		then(stmt).should().setArray(eq(6), same(virtRealSourceIdsOrderArray));
		then(virtRealSourceIdsOrderArray).should().free();

		// general criteria
		then(stmt).should().setArray(eq(7), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(8, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(9, filter.getLocalStartDate().with(MONTH_OF_YEAR, 1).plusYears(1),
				TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(10, LocalDateTime.of(2021, 1, 1, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(11, LocalDateTime.of(2023, 1, 1, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(12, filter.getLocalEndDate().with(MONTH_OF_YEAR, 1), TIMESTAMP);
		then(stmt).should().setObject(13, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Year_Month_virtual_full_customSort() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql,
				"select-datum-pagg-year-month-virtual-full-nodes-sortTimeNodeSource.sql");
	}

	@Test
	public void sql_find_nodes_Month_Day_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-month-day-full-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Month_Day_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-month-day-full-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3,
				filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1).plusMonths(1), TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(4, LocalDateTime.of(2020, 7, 1, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(5, LocalDateTime.of(2020, 8, 1, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(6, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1),
				TIMESTAMP);
		then(stmt).should().setObject(7, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Month_Day_virtual_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-month-day-virtual-full-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Month_Day_virtual_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// @formatter:off
		given(con.createArrayOf(eq("bigint"), eq(filter.getNodeIds())))
			.willReturn(virtRealNodeIdsArray)
			.willReturn(virtRealNodeIdsOrderArray)
			.willReturn(nodeIdsArray)
			;

		given(con.createArrayOf(eq("text"), aryEq(new String[] { "a", "b", "c" })))
			.willReturn(virtRealSourceIdsArray)
			.willReturn(virtRealSourceIdsOrderArray)
			;
		// @formatter:on

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-pagg-month-day-virtual-full-nodes.sql");

		// set virtual node ID parameters
		then(stmt).should().setArray(eq(1), same(virtRealNodeIdsArray));
		then(virtRealNodeIdsArray).should().free();
		then(stmt).should().setObject(2, 100L);

		// set virtual node ID ordering
		then(stmt).should().setArray(eq(3), same(virtRealNodeIdsOrderArray));
		then(virtRealNodeIdsOrderArray).should().free();

		// set virtual node ID parameters
		then(stmt).should().setArray(eq(4), same(virtRealSourceIdsArray));
		then(virtRealSourceIdsArray).should().free();
		then(stmt).should().setString(5, "V");

		// set virtual node ID ordering
		then(stmt).should().setArray(eq(6), same(virtRealSourceIdsOrderArray));
		then(virtRealSourceIdsOrderArray).should().free();

		then(stmt).should().setArray(eq(7), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(8, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(9,
				filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1).plusMonths(1), TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(10, LocalDateTime.of(2020, 7, 1, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(11, LocalDateTime.of(2020, 8, 1, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(12, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1),
				TIMESTAMP);
		then(stmt).should().setObject(13, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Month_Day_noLeading() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-month-day-noLeading-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Month_Day_noLeading() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-month-day-noLeading-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// main date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3, LocalDateTime.of(2020, 8, 1, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(4, filter.getLocalEndDate().with(ChronoField.DAY_OF_MONTH, 1),
				TIMESTAMP);
		then(stmt).should().setObject(5, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Month_Day_noTrailing() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-month-day-noTrailing-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Month_Day_noTrailing() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-month-day-noTrailing-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3,
				filter.getLocalStartDate().with(ChronoField.DAY_OF_MONTH, 1).plusMonths(1), TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(4, LocalDateTime.of(2020, 7, 1, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(5, LocalDateTime.of(2020, 8, 1, 0, 0), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Month_Day_noPartial() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-month-day-noPartial-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Month_Day_noPartial() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 1, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-month-day-noPartial-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// main date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodes_Day_Hour_full() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setPartialAggregation(Aggregation.Hour);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 10, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 7, 15, 10, 0));

		// WHEN
		String sql = new SelectDatumPartialAggregate(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-pagg-day-hour-full-nodes.sql");
	}

	@Test
	public void prep_find_nodes_Day_Hour_full() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setPartialAggregation(Aggregation.Hour);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 6, 15, 10, 0));
		filter.setLocalEndDate(LocalDateTime.of(2020, 8, 15, 10, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-day-hour-full-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3, filter.getLocalStartDate().with(HOUR_OF_DAY, 0).plusDays(1),
				TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(4, LocalDateTime.of(2020, 6, 16, 0, 0), TIMESTAMP);
		then(stmt).should().setObject(5, LocalDateTime.of(2020, 8, 15, 0, 0), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(6, filter.getLocalEndDate().with(HOUR_OF_DAY, 0), TIMESTAMP);
		then(stmt).should().setObject(7, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_nodes_Month_Day_rollup_All() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Month);
		filter.setPartialAggregation(Aggregation.Day);
		filter.setDatumRollupType(DatumRollupType.All);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2021, 1, 15, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2021, 12, 15, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-month-day-nodes-rollup-all.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial leading date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3, filter.getLocalStartDate().with(firstDayOfNextMonth()),
				TIMESTAMP);

		// main date parameters
		then(stmt).should().setObject(4, filter.getLocalStartDate().with(firstDayOfNextMonth()),
				TIMESTAMP);
		then(stmt).should().setObject(5, filter.getLocalEndDate().with(firstDayOfMonth()), TIMESTAMP);

		// partial trailing date parameters
		then(stmt).should().setObject(6, filter.getLocalEndDate().with(firstDayOfMonth()), TIMESTAMP);
		then(stmt).should().setObject(7, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_nodes_Year_Month_rollup_All() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Year);
		filter.setPartialAggregation(Aggregation.Month);
		filter.setDatumRollupType(DatumRollupType.All);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(LocalDateTime.of(2020, 3, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2023, 3, 1, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatumPartialAggregate(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-pagg-year-month-nodes-rollup-all.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		// partial single month-range date parameters
		then(stmt).should().setObject(2, filter.getLocalStartDate(), TIMESTAMP);
		then(stmt).should().setObject(3, filter.getLocalEndDate(), TIMESTAMP);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
