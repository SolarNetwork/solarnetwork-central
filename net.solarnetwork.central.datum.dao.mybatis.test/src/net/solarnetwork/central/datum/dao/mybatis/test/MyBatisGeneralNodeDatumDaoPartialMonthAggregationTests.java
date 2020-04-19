/* ==================================================================
 * MyBatisGeneralNodeDatumDaoPartialMonthAggregationTests.java - 19/04/2020 7:39:24 am
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Iterator;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for partial month aggregation queries.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoPartialMonthAggregationTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	@Test
	public void findFilteredAggregateMonthPartialDay() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2019, 1, 15, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final DateTime endDate = new DateTime(2019, 3, 15, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime currDate = startDate;
		final String datumJson = "{\"a\":{\"watt_hours\":%d},\"i\":{\"watts\":%d}}";
		do {
			long wh = 0;
			for ( int i = 0; i < 9; i++ ) {
				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setCreated(currDate.plusMinutes(i * 20));
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setSampleJson(String.format(datumJson, wh, (i + 1) * 100));
				dao.store(d);
				d.setNodeId(TEST_2ND_NODE);
				d.setSourceId(TEST_2ND_SOURCE);
				d.setSampleJson(String.format(datumJson, wh * 10, (i + 1) * 1000));
				dao.store(d);
				wh += 1000;
			}
			currDate = currDate.plusDays(1);
		} while ( currDate.isBefore(endDate) );

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusYears(1));
		criteria.setAggregate(Aggregation.Month);
		criteria.setPartialAggregation(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(6L));
		assertThat("3 month results for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		for ( currDate = startDate.monthOfYear().roundFloorCopy(); currDate
				.isBefore(endDate); currDate = currDate.plusMonths(1) ) {
			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_2ND_NODE));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();

			switch (i) {
				case 0:
					assertThat("Aggregate Wh (17d)", data, hasEntry("watt_hours", (Object) 1360000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 5000));
					break;
				case 1:
					assertThat("Aggregate Wh (28d)", data, hasEntry("watt_hours", (Object) 2240000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 5000));
					break;
				case 2:
					assertThat("Aggregate Wh (14d)", data, hasEntry("watt_hours", (Object) 1120000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 5000));
					break;
			}

			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();
			switch (i) {
				case 0:
					assertThat("Aggregate Wh (17d)", data, hasEntry("watt_hours", (Object) 136000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 500));
					break;
				case 1:
					assertThat("Aggregate Wh (28d)", data, hasEntry("watt_hours", (Object) 224000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 500));
					break;
				case 2:
					assertThat("Aggregate Wh (14d)", data, hasEntry("watt_hours", (Object) 112000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 500));
					break;
			}

			i++;
		}
	}

}
