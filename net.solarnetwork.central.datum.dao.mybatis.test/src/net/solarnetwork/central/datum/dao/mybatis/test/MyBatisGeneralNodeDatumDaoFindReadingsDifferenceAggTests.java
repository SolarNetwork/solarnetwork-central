/* ==================================================================
 * MyBatisGeneralNodeDatumDaoFindReadingsDifferenceAggTests.java - 13/02/2019 12:24:44 pm
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for querying {@code Difference} style reading aggregates.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoFindReadingsDifferenceAggTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final Long TEST_2ND_LOC = -879L;
	private static final String TEST_2ND_TZ = "America/Los_Angeles";

	@Test
	public void findHourly() {
		// given
		final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
		for ( int i = 0; i < 3; i++ ) {
			Map<String, Number> inst = Collections.singletonMap("watts", i);
			Map<String, Number> as = Collections.singletonMap("watt_hours", i);
			Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
			Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
			DateTime date = start.plusHours(i);
			readings.add(
					new GeneralNodeDatumReadingAggregate(date, TEST_NODE_ID, TEST_SOURCE_ID, as, af, a));
			insertAggDatumHourlyRow(date.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, inst, null, null, as,
					af, a);
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusHours(3));
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 3; i++ ) {
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(),
					equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, start.plusHours(i), TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(i));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(i + 1));
		}
	}

	private static class SortByDateNodeSource implements Comparator<GeneralNodeDatumPK> {

		@Override
		public int compare(GeneralNodeDatumPK o1, GeneralNodeDatumPK o2) {
			if ( o1 == o2 ) {
				return 0;
			}
			int comparison = o1.getCreated().compareTo(o2.getCreated());
			if ( comparison != 0 ) {
				return comparison;
			}
			comparison = o1.getNodeId().compareTo(o2.getNodeId());
			if ( comparison != 0 ) {
				return comparison;
			}
			return o1.getSourceId().compareTo(o2.getSourceId());
		}

	}

	@Test
	public void findHourlyLocalDateRange() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);
		final List<GeneralNodeDatumPK> pks = new ArrayList<>(6);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusHours(i);
				pks.add(new GeneralNodeDatumPK(nodes[z], date, TEST_SOURCE_ID));
				insertAggDatumHourlyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null, as,
						af, a);
			}
		}

		Collections.sort(pks, new SortByDateNodeSource());

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusHours(3));
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			LocalDateTime date = pks.get(i).getCreated().toLocalDateTime();
			int o = new Period(localStart, date).getHours();
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(pks.get(i)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

	@Test
	public void findHourlyLocalDateRangeWithoutSourceId() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);
		final List<GeneralNodeDatumPK> pks = new ArrayList<>(6);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusHours(i);
				pks.add(new GeneralNodeDatumPK(nodes[z], date, TEST_SOURCE_ID));
				insertAggDatumHourlyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null, as,
						af, a);
			}
		}

		Collections.sort(pks, new SortByDateNodeSource());

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusHours(3));
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			LocalDateTime date = pks.get(i).getCreated().toLocalDateTime();
			int o = new Period(localStart, date).getHours();
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(pks.get(i)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

	@Test
	public void findDaily() {
		// given
		final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
		for ( int i = 0; i < 3; i++ ) {
			Map<String, Number> inst = Collections.singletonMap("watts", i);
			Map<String, Number> as = Collections.singletonMap("watt_hours", i);
			Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
			Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
			DateTime date = start.plusDays(i);
			readings.add(
					new GeneralNodeDatumReadingAggregate(date, TEST_NODE_ID, TEST_SOURCE_ID, as, af, a));
			insertAggDatumDailyRow(date.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, inst, null, null, as,
					af, a);
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusDays(3));
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 3; i++ ) {
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(),
					equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, start.plusDays(i), TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(i));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(i + 1));
		}
	}

	@Test
	public void findDailyLocalDateRange() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusDays(i);
				readings.add(
						new GeneralNodeDatumReadingAggregate(date, nodes[z], TEST_SOURCE_ID, as, af, a));
				insertAggDatumDailyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null, as,
						af, a);
			}
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusDays(3));
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			int z = i % 2;
			int o = i / 2;
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(new GeneralNodeDatumPK(nodes[z],
					localStart.plusDays(o).toDateTime(zones[z]).withZone(DateTimeZone.getDefault()),
					TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

	@Test
	public void findDailyLocalDateRangeWithoutSourceId() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusDays(i);
				readings.add(
						new GeneralNodeDatumReadingAggregate(date, nodes[z], TEST_SOURCE_ID, as, af, a));
				insertAggDatumDailyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null, as,
						af, a);
			}
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusDays(3));
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			int z = i % 2;
			int o = i / 2;
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(new GeneralNodeDatumPK(nodes[z],
					localStart.plusDays(o).toDateTime(zones[z]).withZone(DateTimeZone.getDefault()),
					TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

	@Test
	public void findMonthly() {
		// given
		final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
		for ( int i = 0; i < 3; i++ ) {
			Map<String, Number> inst = Collections.singletonMap("watts", i);
			Map<String, Number> as = Collections.singletonMap("watt_hours", i);
			Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
			Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
			DateTime date = start.plusMonths(i);
			readings.add(
					new GeneralNodeDatumReadingAggregate(date, TEST_NODE_ID, TEST_SOURCE_ID, as, af, a));
			insertAggDatumMonthlyRow(date.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, inst, null, null,
					as, af, a);
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusMonths(3));
		criteria.setAggregate(Aggregation.Month);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 3; i++ ) {
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(),
					equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, start.plusMonths(i), TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(i));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(i + 1));
		}
	}

	@Test
	public void findMonthlyLocalDateRange() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusMonths(i);
				readings.add(
						new GeneralNodeDatumReadingAggregate(date, nodes[z], TEST_SOURCE_ID, as, af, a));
				insertAggDatumMonthlyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null,
						as, af, a);
			}
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusMonths(3));
		criteria.setAggregate(Aggregation.Month);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			int z = i % 2;
			int o = i / 2;
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(new GeneralNodeDatumPK(nodes[z],
					localStart.plusMonths(o).toDateTime(zones[z]).withZone(DateTimeZone.getDefault()),
					TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

	@Test
	public void findMonthlyLocalDateRangeWithoutSourceId() {
		// given
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_NODE, TEST_2ND_LOC);

		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final DateTimeZone[] zones = new DateTimeZone[] { DateTimeZone.forID(TEST_TZ),
				DateTimeZone.forID(TEST_2ND_TZ) };
		final LocalDateTime localStart = new LocalDateTime(2019, 2, 1, 0, 0, 0);

		for ( int z = 0; z < zones.length; z++ ) {
			final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, zones[z]);
			List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
			for ( int i = 0; i < 3; i++ ) {
				Map<String, Number> inst = Collections.singletonMap("watts", i);
				Map<String, Number> as = Collections.singletonMap("watt_hours", i);
				Map<String, Number> af = Collections.singletonMap("watt_hours", i + 1);
				Map<String, Number> a = Collections.singletonMap("watt_hours", 1);
				DateTime date = start.plusMonths(i);
				readings.add(
						new GeneralNodeDatumReadingAggregate(date, nodes[z], TEST_SOURCE_ID, as, af, a));
				insertAggDatumMonthlyRow(date.getMillis(), nodes[z], TEST_SOURCE_ID, inst, null, null,
						as, af, a);
			}
		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(nodes);
		criteria.setLocalStartDate(localStart);
		criteria.setLocalEndDate(localStart.plusMonths(3));
		criteria.setAggregate(Aggregation.Month);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(6L, (long) results.getTotalResults());
		assertEquals(6, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		for ( int i = 0; i < 6; i++ ) {
			int z = i % 2;
			int o = i / 2;
			ReportingGeneralNodeDatumMatch m = itr.next();
			assertThat("PK " + i, m.getId(), equalTo(new GeneralNodeDatumPK(nodes[z],
					localStart.plusMonths(o).toDateTime(zones[z]).withZone(DateTimeZone.getDefault()),
					TEST_SOURCE_ID)));
			Map<String, ?> data = m.getSampleData();
			assertThat("Sample data " + i, data, notNullValue());
			assertThat("Wh " + i, data.get("watt_hours"), equalTo(1));
			assertThat("Wh start " + i, data.get("watt_hours_start"), equalTo(o));
			assertThat("Wh end " + i, data.get("watt_hours_end"), equalTo(o + 1));
		}
	}

}
