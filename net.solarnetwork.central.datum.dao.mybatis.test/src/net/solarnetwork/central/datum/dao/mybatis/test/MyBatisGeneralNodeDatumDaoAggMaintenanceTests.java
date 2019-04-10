/* ==================================================================
 * MyBatisGeneralNodeDatumDaoAggMaintenanceTests.java - 10/04/2019 9:46:48 am
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for aggregation maintenance functions.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoAggMaintenanceTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final Long NY_LOC_ID = -55L;
	private static final String NY_TZ = "America/New_York";

	@Override
	@Before
	public void setup() {
		super.setup();
		setupTestLocation(NY_LOC_ID, NY_TZ);
		setupTestNode(TEST_2ND_NODE, NY_LOC_ID);
	}

	@Test
	public void markStaleNoData() {
		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(new DateTime().minusDays(1));
		criteria.setEndDate(new DateTime());
		dao.markDatumAggregatesStale(criteria);

		// then
		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows", hourly, hasSize(0));
	}

	@Test
	public void markStaleNoMatchingDataByNode() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(5);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusMinutes(30);
		}

		clearAggStaleRecords();

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", hourly, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		dao.markDatumAggregatesStale(criteria);

		// then
		hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows", hourly, hasSize(0));
	}

	@Test
	public void markStaleNoMatchingDataBySource() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(5);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusMinutes(30);
		}

		clearAggStaleRecords();

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", hourly, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId("not.a.source.id");
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		dao.markDatumAggregatesStale(criteria);

		// then
		hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows", hourly, hasSize(0));
	}

	@Test
	public void markStaleNoMatchingDataByDate() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(5);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusMinutes(30);
		}

		clearAggStaleRecords();

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", hourly, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(end.minusDays(1));
		criteria.setEndDate(end);
		dao.markDatumAggregatesStale(criteria);

		// then
		hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows", hourly, hasSize(0));
	}

	@Test
	public void markStale() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(3);

		DateTime date = start;
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().setI(null);
			d.getSamples().setS(null);
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusMinutes(30);
		}

		List<Map<String, Object>> rows = getDatum();
		log.debug("Populated datum: {}", rows);

		clearAggStaleRecords();

		rows = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", rows, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);

		dao.markDatumAggregatesStale(criteria);

		// then
		rows = getStaleDatum(Aggregation.Hour);
		assertThat("Stale hourly agg rows", rows, hasSize(3));
		int i = 0;
		for ( Map<String, Object> row : rows ) {
			assertThat("Stale node ID", row, hasEntry("node_id", TEST_2ND_NODE));
			assertThat("Stale source ID", row, hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Stale date", row,
					hasEntry("ts_start", new Timestamp(start.plusHours(i).getMillis())));
			i++;
		}
	}

	@Test
	public void markStaleSubset() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(3);

		DateTime date = start;
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().setI(null);
			d.getSamples().setS(null);
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusMinutes(30);
		}

		List<Map<String, Object>> rows = getDatum();
		log.debug("Populated datum: {}", rows);

		clearAggStaleRecords();

		rows = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", rows, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusHours(2));

		dao.markDatumAggregatesStale(criteria);

		// then
		rows = getStaleDatum(Aggregation.Hour);
		assertThat("Stale hourly agg rows", rows, hasSize(2));
		int i = 0;
		for ( Map<String, Object> row : rows ) {
			assertThat("Stale node ID", row, hasEntry("node_id", TEST_2ND_NODE));
			assertThat("Stale source ID", row, hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Stale date", row,
					hasEntry("ts_start", new Timestamp(start.plusHours(i).getMillis())));
			i++;
		}
	}

	@Test
	public void markStaleMulti() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(3);

		DateTime date = start;
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().setI(null);
			d.getSamples().setS(null);
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);

			d.setNodeId(TEST_NODE_ID);
			dao.store(d);

			wh += 100L;
			date = date.plusMinutes(30);
		}

		List<Map<String, Object>> rows = getDatum();
		log.debug("Populated datum: {}", rows);

		clearAggStaleRecords();

		rows = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", rows, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);

		dao.markDatumAggregatesStale(criteria);

		// then
		rows = getStaleDatumOrderedByNode(Aggregation.Hour);
		assertThat("Stale hourly agg rows", rows, hasSize(6));
		int i = 0;
		for ( Map<String, Object> row : rows.subList(0, 3) ) {
			assertThat("Stale node ID " + i, row, hasEntry("node_id", TEST_2ND_NODE));
			assertThat("Stale source ID " + i, row, hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Stale date " + i, row,
					hasEntry("ts_start", new Timestamp(start.plusHours(i).getMillis())));
			i++;
		}
		i = 0;
		for ( Map<String, Object> row : rows.subList(3, 6) ) {
			assertThat("Stale node ID " + i, row, hasEntry("node_id", TEST_NODE_ID));
			assertThat("Stale source ID " + i, row, hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Stale date " + i, row,
					hasEntry("ts_start", new Timestamp(start.plusHours(i).getMillis())));
			i++;
		}
	}

	@Test
	public void markStaleMultiMatchOneNode() {
		// given
		final DateTime start = new DateTime().minusDays(1).hourOfDay().roundFloorCopy();
		final DateTime end = start.plusHours(3);

		DateTime date = start;
		long wh = 0;
		while ( date.isBefore(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().setI(null);
			d.getSamples().setS(null);
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);

			d.setNodeId(TEST_NODE_ID);
			dao.store(d);

			wh += 100L;
			date = date.plusMinutes(30);
		}

		List<Map<String, Object>> rows = getDatum();
		log.debug("Populated datum: {}", rows);

		clearAggStaleRecords();

		rows = getDatumAggregateHourly();
		assertThat("Hourly agg rows empty", rows, hasSize(0));

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID });
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);

		dao.markDatumAggregatesStale(criteria);

		// then
		rows = getStaleDatumOrderedByNode(Aggregation.Hour);
		assertThat("Stale hourly agg rows", rows, hasSize(3));
		int i = 0;
		for ( Map<String, Object> row : rows.subList(0, 3) ) {
			assertThat("Stale node ID " + i, row, hasEntry("node_id", TEST_NODE_ID));
			assertThat("Stale source ID " + i, row, hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Stale date " + i, row,
					hasEntry("ts_start", new Timestamp(start.plusHours(i).getMillis())));
			i++;
		}
	}
}
