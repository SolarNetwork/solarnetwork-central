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
public class MyBatisGeneralNodeDatumDaoPartialYearAggregationTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	@Test
	public void findFilteredAggregateYearPartialDay() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2019, 1, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int j = 0; j < 28; j += 4 ) {
			final int count = 145;
			for ( int i = 0; i < count; i++ ) {
				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setCreated(startDate.plusMonths(j).plusMinutes(i * 20));
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
				dao.store(d);
				d.setNodeId(TEST_2ND_NODE);
				d.setSourceId(TEST_2ND_SOURCE);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "}}");
				dao.store(d);
			}
		}

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsDaily() ) {
			log.debug("Day row: {}", row);
		}
		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ)));
		criteria.setEndDate(criteria.getStartDate().plusYears(2));
		criteria.setAggregate(Aggregation.Year);
		criteria.setPartialAggregation(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(6L));
		assertThat("3 year results for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		for ( DateTime currDate = startDate.year().roundFloorCopy(); currDate.isBefore(
				criteria.getEndDate().year().roundCeilingCopy()); currDate = currDate.plusYears(1) ) {
			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_2ND_NODE));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();
			switch (i) {
				case 0:
					assertThat("Aggregate Wh (4d * 3600)", data, hasEntry("watt_hours", (Object) 14400));
					break;
				case 1:
					assertThat("Aggregate Wh (3m * 7200)", data, hasEntry("watt_hours", (Object) 21600));
					break;
				case 2:
					assertThat("Aggregate Wh (2d * 3600)", data, hasEntry("watt_hours", (Object) 7200));
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
					assertThat("Aggregate Wh (4d * 360)", data, hasEntry("watt_hours", (Object) 1440));
					break;
				case 1:
					assertThat("Aggregate Wh (3m * 720)", data, hasEntry("watt_hours", (Object) 2160));
					break;
				case 2:
					assertThat("Aggregate Wh (2d * 360)", data, hasEntry("watt_hours", (Object) 720));
					break;
			}

			i++;
		}
	}

	@Test
	public void findFilteredAggregateYearPartialMonth() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2019, 1, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int j = 0; j < 28; j += 4 ) {
			final int count = 145;
			for ( int i = 0; i < count; i++ ) {
				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setCreated(startDate.plusMonths(j).plusMinutes(i * 20));
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
				dao.store(d);
				d.setNodeId(TEST_2ND_NODE);
				d.setSourceId(TEST_2ND_SOURCE);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "}}");
				dao.store(d);
			}
		}

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsDaily() ) {
			log.debug("Day row: {}", row);
		}
		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ)));
		criteria.setEndDate(criteria.getStartDate().plusYears(2));
		criteria.setAggregate(Aggregation.Year);
		criteria.setPartialAggregation(Aggregation.Month);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(6L));
		assertThat("3 year results for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		for ( DateTime currDate = startDate.year().roundFloorCopy(); currDate.isBefore(
				criteria.getEndDate().year().roundCeilingCopy()); currDate = currDate.plusYears(1) ) {
			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_2ND_NODE));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();
			switch (i) {
				case 0:
					assertThat("Aggregate Wh (4d * 3600)", data, hasEntry("watt_hours", (Object) 14400));
					break;
				case 1:
					assertThat("Aggregate Wh (3m * 7200)", data, hasEntry("watt_hours", (Object) 21600));
					break;
				case 2:
					assertThat("Aggregate Wh (2d * 3600)", data, hasEntry("watt_hours", (Object) 7200));
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
					assertThat("Aggregate Wh (4d * 360)", data, hasEntry("watt_hours", (Object) 1440));
					break;
				case 1:
					assertThat("Aggregate Wh (3m * 720)", data, hasEntry("watt_hours", (Object) 2160));
					break;
				case 2:
					assertThat("Aggregate Wh (2d * 360)", data, hasEntry("watt_hours", (Object) 720));
					break;
			}

			i++;
		}
	}

}
