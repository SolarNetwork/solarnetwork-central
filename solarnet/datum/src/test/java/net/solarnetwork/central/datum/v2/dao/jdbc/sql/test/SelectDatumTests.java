/* ==================================================================
 * SelectDatumTests.java - 22/11/2020 8:12:12 am
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
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum.DEFAULT_FETCH_SIZE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.dao.jdbc.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
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
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.aliased.test.TestAliasedSqlResources;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SelectDatum} class.
 *
 * @author matt
 * @version 1.5
 */
@ParameterizedClass
@ValueSource(booleans = { false, true }) // for aliased or not
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectDatumTests {

	private static final int TEST_FETCH_SIZE = 567;

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
	public void sql_find_minute() {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Minute);
		thenIllegalArgumentException().isThrownBy(() -> new SelectDatum(filter).getSql());
	}

	@Test
	public void sql_find_mostRecent_agg() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		for ( Aggregation agg : EnumSet.complementOf(
				EnumSet.of(Aggregation.None, Aggregation.Hour, Aggregation.Day, Aggregation.Month)) ) {
			// WHEN
			filter.setAggregation(agg);
			thenIllegalArgumentException()
					.as("MostRecent should not be allowed with aggregation %s", agg)
					.isThrownBy(() -> new SelectDatum(filter).getSql());
		}
	}

	@Test
	public void sql_find_mostRecent_users() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-mostRecent-users.sql");
	}

	@Test
	public void sql_find_mostRecent_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-mostRecent-nodes.sql");
	}

	@Test
	public void sql_find_mostRecent_nodes_localStartDate() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-mostRecent-nodes-localStartDate.sql");
	}

	@Test
	public void sql_find_mostRecent_nodes_localEndDate() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalEndDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-mostRecent-nodes-localEndDate.sql");
	}

	@Test
	public void prep_find_mostRecent_nodes_localStartDate() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 03, 28, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-mostRecent-nodes-localStartDate.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setObject(eq(2), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_daily_mostRecent_nodes() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-mostRecent-nodes.sql");
	}

	@Test
	public void sql_find_daily_mostRecent_nodes_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		filter.setEndDate(now());
		filter.setIncludeStreamAliases(aliased);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-mostRecent-nodes-dates.sql");
	}

	@Test
	public void prep_find_daily_mostRecent_nodes() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setIncludeStreamAliases(aliased);

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-daily-mostRecent-nodes.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));
		filter.setEndDate(Instant.now());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-daily-mostRecent-nodes-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(2), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_absoluteMinDate() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-daily-mostRecent-nodes-date-min.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(2), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_daily_mostRecent_nodes_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setLocalStartDate(LocalDateTime.of(2022, 2, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2022, 2, 7, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-daily-mostRecent-nodes-localDates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setObject(eq(2), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));
		then(stmt).should().setObject(eq(3), eq(filter.getLocalEndDate()), eq(Types.TIMESTAMP));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-nodesAndSources-dates-count.sql");
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-nodesAndSources-dates-sortTimeNodeSource.sql");
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_sortTimeNodeSource_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-nodesAndSources-dates-count.sql");
	}

	@Test
	public void prep_find_15min_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-15min-nodesAndSources-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setObject(eq(5), eq(filter.getAggregation().getLevel()));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_15min_nodesAndSources_absoluteDatesWithOneLocalDate() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setLocalStartDate(start.toLocalDateTime());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		and.then(sqlCaptor.getValue()).as("Absolute dates used in SQL").containsPattern(
				"rollup_datm_for_time_span_slots\\(s\\.(orig_)?stream_id, \\?, \\?, \\?\\)");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		// the absolute dates are bound, as the SQL uses them without both local dates
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setObject(eq(5), eq(filter.getAggregation().getLevel()));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_15min_nodesAndSources_absoluteDatesWithOneLocalDate_count()
			throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setLocalEndDate(start.plusHours(1).toLocalDateTime());

		given(con.prepareStatement(any())).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).countPreparedStatementCreator()
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		and.then(sqlCaptor.getValue()).as("Absolute dates used in count SQL")
				.containsPattern("count_datm_time_span_slots\\(s\\.(orig_)?stream_id, \\?, \\?, \\?\\)");

		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setObject(eq(5), eq(filter.getAggregation().getLevel()));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-nodesAndSources-dates-count.sql");
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-nodesAndSources-dates-sortTimeNodeSource.sql");
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_sortTimeNodeSource_count() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-nodesAndSources-dates-count.sql");
	}

	@Test
	public void sql_find_15min_vids() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-virtual-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_15min_vids_combineNodeOnly() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-virtual-mapNodes-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_15min_vids_combineSourceOnly() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-15min-virtual-mapSources-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-virtual-nodesAndSources-dates.sql");
	}

	@Test
	public void prep_find_daily_vids_nodeAndSources_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// @formatter:off
		given(con.createArrayOf(eq("bigint"), eq(filter.getNodeIds())))
			.willReturn(virtRealNodeIdsArray)
			.willReturn(virtRealNodeIdsOrderArray)
			.willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds())))
			.willReturn(virtRealSourceIdsArray)
			.willReturn(virtRealSourceIdsOrderArray)
			.willReturn(sourceIdsArray);
		// @formatter:on

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-daily-virtual-nodesAndSources-dates.sql");

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

		// general criteria
		then(stmt).should().setArray(eq(7), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(8), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(9), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(10), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_counts() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-virtual-nodesAndSources-dates-counts.sql");
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql,
				"select-datum-daily-virtual-nodesAndSources-dates-sortTimeNodeSource.sql");
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_sortTimeNodeSource_counts() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-daily-virtual-nodesAndSources-dates-counts.sql");
	}

	@Test
	public void sql_find_weekly_vids_nodeAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Week);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(7)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V:a,b,c" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-weekly-virtual-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_seasonal_hod_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-seasonal_hod-nodesAndSources-dates.sql");
	}

	@Test
	public void prep_find_seasonal_hod_nodesAndSources_absoluteDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-seasonal_hod-nodesAndSources-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_seasonal_hod_nodesAndSources_absoluteDatesWithOneLocalDate()
			throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setLocalStartDate(LocalDateTime.of(2022, 2, 1, 0, 0));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		and.then(sqlCaptor.getValue()).as("Absolute dates used in SQL")
				.containsPattern("find_agg_datm_hod_seasonal\\(s\\.(orig_)?stream_id, \\?, \\?\\)");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		// the absolute dates are bound, as the SQL uses them without both local dates
		then(stmt).should().setTimestamp(eq(3), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_seasonal_hod_nodesAndSources_defaultDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-seasonal_hod-nodesAndSources-localDates.sql");
	}

	@Test
	public void prep_find_seasonal_hod_nodesAndSources_defaultDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.SeasonalHourOfDay);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-seasonal_hod-nodesAndSources-localDates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setObject(3, LocalDate.now(UTC).minusYears(2).atStartOfDay(), TIMESTAMP);
		then(stmt).should().setObject(4, LocalDate.now(UTC).plusDays(1).atStartOfDay(), TIMESTAMP);
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_doy_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.DayOfYear);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-doy-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_hoy_nodesAndSources_absoluteDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.HourOfYear);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-hoy-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_find_reverse_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		filter.setUserId(randomLong());
		filter.setStartDate(Instant.now().minusSeconds(1));
		filter.setEndDate(filter.getStartDate().plusSeconds(1));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-node-source-user-time-reverse-limit.sql");
	}

	@Test
	public void sql_find_reading_month_rollup_all() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setAggregation(Aggregation.Month);
		filter.setDatumRollupType(DatumRollupType.All);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		filter.setUserId(randomLong());
		filter.setStartDate(Instant.now().minusSeconds(1));
		filter.setEndDate(filter.getStartDate().plusSeconds(1));

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-reading-node-source-user-time-month-rollup.sql");
	}

	private void thenSqlEqualsResource(String sql, String resource, String suffix) {
		// @formatter:off
		and.then(sql)
			.as("Generated SQL")
			.isEqualToNormalizingWhitespace(getResourceAsString(
					resource,
					(aliased ? TestAliasedSqlResources.class : TestSqlResources.class),
					SQL_COMMENT) + "\n" + suffix)
			;
		// @formatter:on
	}

	private BasicDatumCriteria accessorQueryCriteria() {
		// the criteria QueryingDatumStreamsAccessor uses to find the datum before those it has
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setUserId(2L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(90, ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plus(90, ChronoUnit.DAYS));
		filter.setMax(3);
		return filter;
	}

	@Test
	public void sql_find_streamLimit_nodeAndSourceAndUser_sortStreamTimeDesc() {
		// GIVEN
		BasicDatumCriteria filter = accessorQueryCriteria();

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-node-source-user-dates-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void prep_find_streamLimit_nodeAndSourceAndUser_sortStreamTimeDesc() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = accessorQueryCriteria();

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-node-source-user-dates-sortStreamTimeDesc-limit.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setObject(3, filter.getUserId());
		then(stmt).should().setTimestamp(4, Timestamp.from(filter.getStartDate()));
		then(stmt).should().setTimestamp(5, Timestamp.from(filter.getEndDate()));
		// the per-stream limit, then the overall limit
		then(stmt).should().setLong(6, 3L);
		then(stmt).should().setInt(7, 3);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_count_streamLimit_nodeAndSourceAndUser_sortStreamTimeDesc() {
		// GIVEN
		BasicDatumCriteria filter = accessorQueryCriteria();

		// WHEN
		String sql = ((SqlProvider) new SelectDatum(filter).countPreparedStatementCreator()).getSql();

		// THEN
		// the count includes every row, not just those within the per-stream limit
		thenSqlEqualsResource(sql, "select-datum-node-source-user-dates-count.sql");
	}

	@Test
	public void prep_count_streamLimit_nodeAndSourceAndUser_sortStreamTimeDesc() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = accessorQueryCriteria();

		given(con.prepareStatement(any())).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).countPreparedStatementCreator()
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-node-source-user-dates-count.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setObject(3, filter.getUserId());
		then(stmt).should().setTimestamp(4, Timestamp.from(filter.getStartDate()));
		then(stmt).should().setTimestamp(5, Timestamp.from(filter.getEndDate()));
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_streamLimit_nodeAndSource_defaultSort_offset() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(10);
		filter.setOffset(20L);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-node-source-dates-limit-offset.sql");
	}

	@Test
	public void prep_find_streamLimit_nodeAndSource_defaultSort_offset() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(10);
		filter.setOffset(20L);

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-node-source-dates-limit-offset.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setTimestamp(3, Timestamp.from(filter.getStartDate()));
		then(stmt).should().setTimestamp(4, Timestamp.from(filter.getEndDate()));
		// each stream must provide offset + max rows, then the offset and limit apply to them all
		then(stmt).should().setLong(5, 30L);
		then(stmt).should().setInt(6, 10);
		then(stmt).should().setLong(7, 20L);
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_find_streamLimit_nodeAndSource_localDates() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setLocalStartDate(LocalDateTime.of(2022, 2, 1, 0, 0));
		filter.setLocalEndDate(LocalDateTime.of(2022, 2, 7, 0, 0));
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setMax(5);

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectDatum(filter, TEST_FETCH_SIZE).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-node-source-localDates-sortStreamTimeDesc-limit.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setObject(3, filter.getLocalStartDate(), Types.TIMESTAMP);
		then(stmt).should().setObject(4, filter.getLocalEndDate(), Types.TIMESTAMP);
		then(stmt).should().setLong(5, 5L);
		then(stmt).should().setInt(6, 5);
		then(stmt).should().setFetchSize(TEST_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_streamLimit_nodeAndSource_noDates() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-node-source-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void prep_find_streamLimit_stream() throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setStreamId(UUID.randomUUID());
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(100);

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "select-datum-stream-dates-limit.sql");

		then(stmt).should().setObject(1, filter.getStreamId());
		then(stmt).should().setTimestamp(2, Timestamp.from(filter.getStartDate()));
		then(stmt).should().setTimestamp(3, Timestamp.from(filter.getEndDate()));
		then(stmt).should().setLong(4, 100L);
		then(stmt).should().setInt(5, 100);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_streamLimit_locationAndSource_sortStreamTimeDesc() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setLocationId(1L);
		filter.setSourceId("a");
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setUserId(-1L);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-loc-source-dates-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void sql_find_streamLimit_hourly_nodeAndSource_sortStreamTimeDesc() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Hour);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plus(1, ChronoUnit.DAYS));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "select-datum-hourly-node-source-dates-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void sql_find_streamLimit_daily_nodesAndSources_sortTimeNodeSource() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));
		filter.setMax(100);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// sorted by time first, the plain join must read every row of every stream in the range
		thenSqlEqualsResource(sql,
				"select-datum-daily-nodesAndSources-dates-sortTimeNodeSource-limit.sql");
	}

	@Test
	public void prep_find_streamLimit_nodesAndSources_sortTimeDescNodeSource_offset()
			throws SQLException {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true), new SimpleSortDescriptor("node"),
				new SimpleSortDescriptor("source")));
		filter.setMax(50);
		filter.setOffset(100L);

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		// WHEN
		PreparedStatement result = new SelectDatum(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"select-datum-nodesAndSources-dates-sortTimeDescNodeSource-limit-offset.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(stmt).should().setTimestamp(3, Timestamp.from(filter.getStartDate()));
		then(stmt).should().setTimestamp(4, Timestamp.from(filter.getEndDate()));
		then(stmt).should().setLong(5, 150L);
		then(stmt).should().setInt(6, 50);
		then(stmt).should().setLong(7, 100L);
		then(stmt).should().setFetchSize(DEFAULT_FETCH_SIZE);
		then(stmt).shouldHaveNoMoreInteractions();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_find_nodeAndSourcePattern_sortStreamTimeDesc_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a/*");
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// a source pattern can match many streams, sorted by stream first: not limited per stream
		thenSqlEqualsResource(sql, "select-datum-node-sourcePattern-dates-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void sql_find_nodesAndSources_sortStreamTimeDesc_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setSorts(QueryingDatumStreamsAccessor.SORT_BY_DATE_DESCENDING);
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// many streams sorted by stream first: not limited per stream
		thenSqlEqualsResource(sql, "select-datum-nodesAndSources-dates-sortStreamTimeDesc-limit.sql");
	}

	@Test
	public void sql_find_nodeAndSource_sortNodeSource_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setSorts(sorts("node", "source"));
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// no time ordering within the stream: not limited per stream
		thenSqlEqualsResource(sql, "select-datum-node-source-dates-sortNodeSource-limit.sql");
	}

	@Test
	public void sql_find_daily_nodesAndSources_absoluteDates_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setMax(10);
		filter.setOffset(10L);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// many streams sorted by stream first: not limited per stream
		thenSqlEqualsResource(sql, "select-datum-daily-nodesAndSources-dates.sql", "LIMIT ? OFFSET ?");
	}

	@Test
	public void sql_find_15min_nodesAndSources_absoluteDates_sortTimeNodeSource_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		filter.setSorts(sorts("time", "node", "source"));
		filter.setMax(10);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// the minute aggregate function is not limited per stream
		thenSqlEqualsResource(sql, "select-datum-15min-nodesAndSources-dates-sortTimeNodeSource.sql",
				"LIMIT ?");
	}

	@Test
	public void sql_find_daily_vids_nodeAndSources_absoluteDates_sortTimeNodeSource_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setAggregation(Aggregation.Day);
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "a/*", "b", "c" });
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.DAYS));
		filter.setEndDate(filter.getStartDate().plusSeconds(TimeUnit.DAYS.toSeconds(1)));
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "100:1,2,3" });
		filter.setSourceIdMaps(new String[] { "V1:a,b,c" });
		filter.setSorts(sorts("time", "node", "source"));
		filter.setMax(10);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// virtual streams are not limited per stream
		thenSqlEqualsResource(sql,
				"select-datum-daily-virtual-nodesAndSources-dates-sortTimeNodeSource.sql", "LIMIT ?");
	}

	@Test
	public void sql_find_mostRecent_nodes_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		filter.setMax(10);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// most recent already limits each stream to one row
		thenSqlEqualsResource(sql, "select-datum-mostRecent-nodes.sql", "LIMIT ?");
	}

	@Test
	public void sql_find_reading_month_rollup_all_limit() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setAggregation(Aggregation.Month);
		filter.setDatumRollupType(DatumRollupType.All);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomString());
		filter.setSorts(List.of(new SimpleSortDescriptor("time", true)));
		filter.setUserId(randomLong());
		filter.setStartDate(Instant.now().minusSeconds(1));
		filter.setEndDate(filter.getStartDate().plusSeconds(1));
		filter.setMax(1);

		// WHEN
		String sql = new SelectDatum(filter).getSql();

		// THEN
		// a rollup must see every row
		thenSqlEqualsResource(sql, "select-reading-node-source-user-time-month-rollup.sql", "LIMIT ?");
	}

}
