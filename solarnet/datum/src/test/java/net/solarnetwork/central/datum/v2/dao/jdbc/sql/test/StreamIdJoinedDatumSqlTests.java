/* ==================================================================
 * StreamIdJoinedDatumSqlTests.java - 31/10/2026 3:40:12 pm
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

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DeleteDatumAuxiliaryByFilter;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertStaleAggregateDatumSelect;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumAuxiliary;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatumRecordCounts;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;

/**
 * Test cases for the SQL statements that join stream data directly on the
 * stream metadata {@code stream_id} column, and thus must only consider real
 * streams, never aliases.
 *
 * @author matt
 * @version 1.0
 */
public class StreamIdJoinedDatumSqlTests {

	private static BasicDatumCriteria filter(boolean localDates, boolean includeAliases) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a/*", "b" });
		if ( localDates ) {
			filter.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
			filter.setLocalEndDate(LocalDateTime.of(2021, 1, 1, 0, 0));
		} else {
			filter.setStartDate(Instant.ofEpochSecond(1000));
			filter.setEndDate(Instant.ofEpochSecond(2000));
		}
		if ( includeAliases ) {
			filter.setIncludeStreamAliases(true);
		}
		return filter;
	}

	private static void thenAliasesIgnored(boolean localDates,
			Function<BasicDatumCriteria, SqlProvider> statement) {
		// WHEN
		final String result = statement.apply(filter(localDates, true)).getSql();

		// THEN
		// @formatter:off
		then(result)
			.as("Stream alias flag has no effect on the generated SQL")
			.isEqualTo(statement.apply(filter(localDates, false)).getSql())
			.as("Stream alias view not used")
			.doesNotContain("_aliased")
			;
		// @formatter:on
	}

	@Test
	public void deleteDatum_aliasesIgnored() {
		thenAliasesIgnored(true, DeleteDatum::new);
	}

	@Test
	public void deleteDatumAuxiliaryByFilter_aliasesIgnored() {
		thenAliasesIgnored(true, DeleteDatumAuxiliaryByFilter::new);
	}

	@Test
	public void selectDatumAuxiliary_aliasesIgnored() {
		thenAliasesIgnored(false, SelectDatumAuxiliary::new);
	}

	@Test
	public void selectDatumRecordCounts_aliasesIgnored() {
		thenAliasesIgnored(true, SelectDatumRecordCounts::new);
	}

	@Test
	public void insertStaleAggregateDatumSelect_aliasesIgnored() {
		thenAliasesIgnored(false, InsertStaleAggregateDatumSelect::new);
	}

	@Test
	public void selectStaleAggregateDatum_aliasesIgnored() {
		thenAliasesIgnored(false, SelectStaleAggregateDatum::new);
	}

}
