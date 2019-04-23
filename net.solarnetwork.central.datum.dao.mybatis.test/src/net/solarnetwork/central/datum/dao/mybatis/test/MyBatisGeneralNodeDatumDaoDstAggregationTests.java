/* ==================================================================
 * MyBatisGeneralNodeDatumDaoDstAggregationTests.java - 15/03/2019 8:14:48 pm
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for aggregation support across DST boundaries.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoDstAggregationTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final Long NY_LOC_ID = -55L;
	private static final String NY_TZ = "America/New_York";
	private static final DateTimeZone NY_DTZ = DateTimeZone.forID(NY_TZ);

	@Override
	@Before
	public void setup() {
		super.setup();
		setupTestLocation(NY_LOC_ID, NY_TZ);
		setupTestNode(TEST_2ND_NODE, NY_LOC_ID);
	}

	@Test
	public void dayAggregationOverDstStart() {
		// populate 100Wh hourly data from D-1 to D+1, where D is a DST boundary day
		final DateTime start = new DateTime(2019, 3, 9, 0, 0, NY_DTZ);
		final DateTime end = new DateTime(2019, 3, 12, 0, 0, NY_DTZ);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusHours(1);
		}

		processAggregateStaleData();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 3L, (long) results.getTotalResults());
		assertEquals("Daily query results", 3, (int) results.getReturnedResultCount());

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows for 3 days + 1 hour - 1 DST hour", hourly, hasSize(72));
		assertThat("26th hour is 1am 10 March, before DST switch", hourly.get(25),
				hasEntry("local_date", new Timestamp(new DateTime(2019, 3, 10, 1, 0).getMillis())));
		assertThat("27th hour is 3am 10 March, after DST switch", hourly.get(26),
				hasEntry("local_date", new Timestamp(new DateTime(2019, 3, 10, 3, 0).getMillis())));

		List<Map<String, Object>> daily = getDatumAggregateDaily();
		assertThat("Daily agg rows for 3 days + extra", daily, hasSize(4));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results.getResults() ) {
			Map<String, ?> data = match.getSampleData();
			if ( i == 1 ) {
				assertThat("DST day Wh", data, hasEntry("watt_hours", 2300));
			} else {
				assertThat("Day Wh", data, hasEntry("watt_hours", 2400));
			}
			i++;
		}
	}

	@Test
	public void dayAggregationOverDstEnd() {
		// populate 100Wh hourly data from D-1 to D+1, where D is a DST boundary day
		final DateTime start = new DateTime(2018, 11, 3, 0, 0, NY_DTZ);
		final DateTime end = new DateTime(2018, 11, 6, 0, 0, NY_DTZ);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			GeneralNodeDatum d = getTestInstance(date, TEST_2ND_NODE, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue("watt_hours", wh);
			dao.store(d);
			wh += 100L;
			date = date.plusHours(1);
		}

		processAggregateStaleData();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_2ND_NODE);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 3L, (long) results.getTotalResults());
		assertEquals("Daily query results", 3, (int) results.getReturnedResultCount());

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg rows for 3 days + DST hour + 1 hour", hourly, hasSize(74));
		assertThat("26th hour is 1am 4 Nov, before DST switch", hourly.get(25),
				hasEntry("local_date", new Timestamp(new DateTime(2018, 11, 4, 1, 0).getMillis())));
		assertThat("27th hour is still 1am 4 March, during DST switch", hourly.get(26),
				hasEntry("local_date", new Timestamp(new DateTime(2018, 11, 4, 1, 0).getMillis())));
		assertThat("28th hour is 2am 4 March, after DST switch", hourly.get(27),
				hasEntry("local_date", new Timestamp(new DateTime(2018, 11, 4, 2, 0).getMillis())));

		List<Map<String, Object>> daily = getDatumAggregateDaily();
		assertThat("Daily agg rows for 3 days + extra", daily, hasSize(4));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results.getResults() ) {
			Map<String, ?> data = match.getSampleData();
			if ( i == 1 ) {
				assertThat("DST day Wh", data, hasEntry("watt_hours", 2500));
			} else {
				assertThat("Day Wh", data, hasEntry("watt_hours", 2400));
			}
			i++;
		}
	}

}
