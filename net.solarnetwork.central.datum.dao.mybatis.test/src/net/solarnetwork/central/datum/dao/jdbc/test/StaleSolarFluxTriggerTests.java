/* ==================================================================
 * StaleSolarFluxTriggerTests.java - 5/11/2019 11:15:32 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.jdbc.test;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.Test;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the DBMS-side population of "stale flux" records when updating
 * aggregate datum rows.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleSolarFluxTriggerTests extends BaseDatumJdbcTestSupport {

	private static final ZoneId UTC = ZoneId.of("UTC");

	@Test
	public void insertHourlyAgg_now() {
		// GIVEN
		ZonedDateTime thisDay = ZonedDateTime.now(UTC).truncatedTo(HOURS);
		insertAggDatumHourlyRow(thisDay.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Hour);

		// THEN
		assertThat("Stale row found", rows, hasSize(1));

		Map<String, Object> row = rows.get(0);
		assertThat("Stale flux node ID", row, Matchers.hasEntry("node_id", TEST_NODE_ID));
		assertThat("Stale flux source ID", row, Matchers.hasEntry("source_id", TEST_SOURCE_ID));
	}

	@Test
	public void insertHourlyAgg_past() {
		// GIVEN
		ZonedDateTime past = ZonedDateTime.now(UTC).truncatedTo(HOURS).minusHours(1);
		insertAggDatumHourlyRow(past.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Hour);

		// THEN
		assertThat("Stale row not found", rows, hasSize(0));
	}

	@Test
	public void insertHourlyAgg_future() {
		// GIVEN
		ZonedDateTime future = ZonedDateTime.now(UTC).truncatedTo(HOURS).plusHours(1);
		insertAggDatumHourlyRow(future.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Hour);

		// THEN
		assertThat("Stale row not found", rows, hasSize(0));
	}

	@Test
	public void insertDailyAgg_now() {
		// GIVEN
		ZonedDateTime thisDay = ZonedDateTime.now(UTC).truncatedTo(DAYS);
		insertAggDatumDailyRow(thisDay.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Day);

		// THEN
		assertThat("Stale row found", rows, hasSize(1));

		Map<String, Object> row = rows.get(0);
		assertThat("Stale flux node ID", row, Matchers.hasEntry("node_id", TEST_NODE_ID));
		assertThat("Stale flux source ID", row, Matchers.hasEntry("source_id", TEST_SOURCE_ID));
	}

	@Test
	public void insertDailyAgg_past() {
		// GIVEN
		ZonedDateTime past = ZonedDateTime.now(UTC).truncatedTo(DAYS).minusDays(1);
		insertAggDatumDailyRow(past.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Day);

		// THEN
		assertThat("Stale row found", rows, hasSize(0));
	}

	@Test
	public void insertDailyAgg_future() {
		// GIVEN
		ZonedDateTime future = ZonedDateTime.now(UTC).truncatedTo(DAYS).plusDays(1);
		insertAggDatumDailyRow(future.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Day);

		// THEN
		assertThat("Stale row found", rows, hasSize(0));
	}

	@Test
	public void insertMonthlyAgg_now() {
		// GIVEN
		ZonedDateTime thisMonth = ZonedDateTime.now(UTC).with(firstDayOfMonth()).truncatedTo(DAYS);
		insertAggDatumMonthlyRow(thisMonth.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Month);

		// THEN
		assertThat("Stale row found", rows, hasSize(1));

		Map<String, Object> row = rows.get(0);
		assertThat("Stale flux node ID", row, Matchers.hasEntry("node_id", TEST_NODE_ID));
		assertThat("Stale flux source ID", row, Matchers.hasEntry("source_id", TEST_SOURCE_ID));
	}

	@Test
	public void insertMonthlyAgg_past() {
		// GIVEN
		ZonedDateTime past = ZonedDateTime.now(UTC).with(firstDayOfMonth()).truncatedTo(DAYS)
				.minusMonths(1);
		insertAggDatumMonthlyRow(past.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Month);

		// THEN
		assertThat("Stale row found", rows, hasSize(0));
	}

	@Test
	public void insertMonthlyAgg_future() {
		// GIVEN
		ZonedDateTime future = ZonedDateTime.now(UTC).with(firstDayOfMonth()).truncatedTo(DAYS)
				.plusMonths(1);
		insertAggDatumMonthlyRow(future.toInstant().toEpochMilli(), TEST_NODE_ID, TEST_SOURCE_ID,
				singletonMap("foo", 1), singletonMap("bar", 234), null, null, null, null);

		// WHEN
		List<Map<String, Object>> rows = getStaleFlux(Aggregation.Month);

		// THEN
		assertThat("Stale row found", rows, hasSize(0));
	}

}
