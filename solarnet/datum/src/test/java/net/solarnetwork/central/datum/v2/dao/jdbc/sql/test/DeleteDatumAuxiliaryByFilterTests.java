/* ==================================================================
 * DeleteDatumAuxiliaryByFilterTests.java - 27/05/2026 2:06:55 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.support.SearchFilterUtils.toSqlJsonPath;
import static net.solarnetwork.util.ClassUtils.getResourceAsString;
import static net.solarnetwork.util.SearchFilter.forLDAPSearchFilterString;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatumAuxiliaryByFilter;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;

/**
 * Test cases for the {@link DeleteDatumAuxiliaryByFilter} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DeleteDatumAuxiliaryByFilterTests {

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
					TestSqlResources.class,
					SQL_COMMENT))
			;
		// @formatter:on
	}

	@Test
	public void nodesAndSourcesAndType_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);

		given(con.prepareStatement(any())).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		stmt.setString(3, filter.getDatumAuxiliaryType().name());
		stmt.setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		stmt.setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));

		// WHEN
		PreparedStatement result = new DeleteDatumAuxiliaryByFilter(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(), "delete-datum-aux-nodesAndSources-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

	@Test
	public void nodesAndSourcesAndType_searchFilter_absoluteDates() throws SQLException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusMonths(1).toInstant());
		filter.setDatumAuxiliaryType(DatumAuxiliaryType.Reset);
		filter.setSearchFilter("(m.foo=bar)");

		given(con.prepareStatement(any())).willReturn(stmt);

		given(con.createArrayOf(eq("bigint"), aryEq(filter.getNodeIds()))).willReturn(nodeIdsArray);

		given(con.createArrayOf(eq("text"), aryEq(filter.getSourceIds()))).willReturn(sourceIdsArray);

		stmt.setString(3, filter.getDatumAuxiliaryType().name());
		stmt.setTimestamp(eq(4), eq(Timestamp.from(filter.getStartDate())));
		stmt.setTimestamp(eq(5), eq(Timestamp.from(filter.getEndDate())));
		stmt.setString(6, toSqlJsonPath(forLDAPSearchFilterString(filter.getSearchFilter())));

		// WHEN
		PreparedStatement result = new DeleteDatumAuxiliaryByFilter(filter).createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture());
		thenSqlEqualsResource(sqlCaptor.getValue(),
				"delete-datum-aux-nodesAndSources-searchFilter-dates.sql");

		then(stmt).should().setArray(eq(1), same(nodeIdsArray));
		then(nodeIdsArray).should().free();

		then(stmt).should().setArray(eq(2), same(sourceIdsArray));
		then(sourceIdsArray).should().free();

		and.then(result).as("Connection statement returned").isSameAs(stmt);
	}

}
