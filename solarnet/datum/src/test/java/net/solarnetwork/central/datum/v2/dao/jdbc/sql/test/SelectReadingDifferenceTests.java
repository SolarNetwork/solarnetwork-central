/* ==================================================================
 * ReadingDatumCriteriaPreparedStatementCreatorTests.java - 17/11/2020 2:32:26 pm
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
import java.sql.Types;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectReadingDifference;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.aliased.test.TestAliasedSqlResources;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link SelectReadingDifference} class.
 *
 * @author matt
 * @version 1.2
 */
@ParameterizedClass
@ValueSource(booleans = { false, true }) // for aliased or not
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SelectReadingDifferenceTests {

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
	private Array userIdsArray;

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
	public void sql_diff_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diff-nodes-dates.sql");
	}

	@Test
	public void sql_diff_nodesAndSources_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diff-nodesAndSources-dates.sql");
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diff-nodesAndSourcesAndUsers-dates.sql");
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_absoluteDates_sortNodeSourceTime() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setSorts(SimpleSortDescriptor.sorts("node", "source", "time"));

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diff-nodesAndSourcesAndUsers-dates-sortNodeSourceTime.sql");
	}

	@Test
	public void sql_diff_nodesAndSourcesAndUsers_localDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diff-nodesAndSourcesAndUsers-localDates.sql");
	}

	@Test
	public void sql_diffWithin_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.DifferenceWithin);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diffwithin-nodes-dates.sql");
	}

	@Test
	public void prep_diffNear_nodesAndSourcesAndUsers() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.NearestDifference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 3L, 4L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setTimeTolerance(Period.ofDays(7));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"reading-diffnear-nodesAndSourcesAndUsers-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setObject(eq(6), eq(filter.getTimeTolerance()), eq(Types.OTHER));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void sql_diffAt_nodes_absoluteDates() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.CalculatedAtDifference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		// WHEN
		String sql = new SelectReadingDifference(filter).getSql();

		// THEN
		thenSqlEqualsResource(sql, "reading-diffat-nodes-dates.sql");
	}

	@Test
	public void prep_diffAt_nodesAndSourcesAndUsers() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.CalculatedAtDifference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 3L, 4L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setTimeTolerance(Period.ofDays(7));

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "reading-diffat-nodesAndSourcesAndUsers-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));
		then(stmt).should().setObject(eq(6), eq(filter.getTimeTolerance()), eq(Types.OTHER));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 3L, 4L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "reading-diff-nodesAndSourcesAndUsers-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_absoluteDates_nonWildcardSources() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setUserIds(new Long[] { 3L, 4L });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"reading-diff-nodesAndSourcesAndUsers-dates-literalSources.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_nodesAndSourcesAndUsers_localDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b/*" });
		filter.setUserIds(new Long[] { 3L, 4L });
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getUserIds()))).willReturn(userIdsArray);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"reading-diff-nodesAndSourcesAndUsers-localDates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();
		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();
		then(stmt).should().setArray(eq(3), same(userIdsArray));
		then(userIdsArray).should().free();
		then(stmt).should().setObject(eq(4), eq(filter.getLocalStartDate()), eq(Types.TIMESTAMP));
		then(stmt).should().setObject(eq(5), eq(filter.getLocalEndDate()), eq(Types.TIMESTAMP));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_nodeAndSourceAndUser_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a/*");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(), "reading-diff-nodeAndSourceAndUser-dates.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setObject(3, filter.getUserId());
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void prep_nodeAndSourceAndUser_absoluteDates_nonWildcardSource() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setIncludeStreamAliases(aliased);
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setUserId(2L);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());

		given(con.prepareStatement(any(), eq(TYPE_FORWARD_ONLY), eq(CONCUR_READ_ONLY),
				eq(CLOSE_CURSORS_AT_COMMIT))).willReturn(stmt);

		// WHEN
		PreparedStatement result = new SelectReadingDifference(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(TYPE_FORWARD_ONLY),
				eq(CONCUR_READ_ONLY), eq(CLOSE_CURSORS_AT_COMMIT));
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"reading-diff-nodeAndSourceAndUser-dates-literalSource.sql");

		then(stmt).should().setObject(1, filter.getNodeId());
		then(stmt).should().setString(2, filter.getSourceId());
		then(stmt).should().setObject(3, filter.getUserId());
		then(stmt).should().setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		then(stmt).should().setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
