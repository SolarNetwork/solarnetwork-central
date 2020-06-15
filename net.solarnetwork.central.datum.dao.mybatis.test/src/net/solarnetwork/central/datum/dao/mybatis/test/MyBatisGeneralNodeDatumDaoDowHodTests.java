/* ==================================================================
 * MyBatisGeneralNodeDatumDaoDowHodTests.java - 26/05/2020 4:03:59 pm
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
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for finding {@code DayOfWeek} and {@code HourOfDay} aggregate
 * values.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoDowHodTests extends MyBatisGeneralNodeDatumDaoTestSupport {

	@Test
	public void findFilteredAggregateDayOfWeek() {
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
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_2ND_SOURCE);
				d.setSampleJson(String.format(datumJson, wh * 10, (i + 1) * 1000));
				dao.store(d);
				wh += 1000;
			}
			currDate = currDate.plusDays(1);
		} while ( currDate.isBefore(endDate) );

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsHourly() ) {
			log.debug("Hour row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusYears(1));
		criteria.setAggregate(Aggregation.DayOfWeek);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(14L));
		assertThat("7 day results for each source", results.getReturnedResultCount(), equalTo(14));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		LocalDate currLocalDate;
		for ( currLocalDate = new LocalDate(2001, 1, 1); currLocalDate
				.isBefore(new LocalDate(2001, 1, 8)); currLocalDate = currLocalDate.plusDays(1) ) {
			m = itr.next();
			assertThat("Result date is grouped", m.getLocalDate().isEqual(currLocalDate), equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();

			switch (i) {
				case 0:
				case 4:
				case 5:
				case 6:
					assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 640000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 5000));
					break;
				case 1:
				case 2:
				case 3:
					assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 720000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 5000));
					break;
			}

			m = itr.next();
			assertThat("Result date is grouped", m.getLocalDate().isEqual(currLocalDate), equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();
			switch (i) {
				case 0:
				case 4:
				case 5:
				case 6:
					assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 64000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 500));
					break;
				case 1:
				case 2:
				case 3:
					assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 72000));
					assertThat("Aggregate W", data, hasEntry("watts", (Object) 500));
					break;
			}

			i++;
		}
	}

	@Test
	public void findFilteredAggregateHourOfDay() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2019, 1, 15, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final DateTime endDate = new DateTime(2019, 1, 18, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime currDate = startDate;
		final String datumJson = "{\"a\":{\"watt_hours\":%d},\"i\":{\"watts\":%d}}";
		long wh = 0;
		do {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(currDate);
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson(String.format(datumJson, wh, 100));
			dao.store(d);
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_2ND_SOURCE);
			d.setSampleJson(String.format(datumJson, wh * 10, 1000));
			dao.store(d);
			wh += 1000;
			currDate = currDate.plusMinutes(20);
		} while ( currDate.isBefore(endDate) );

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsHourly() ) {
			log.debug("Hour row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusYears(1));
		criteria.setAggregate(Aggregation.HourOfDay);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(48L));
		assertThat("24 day results for each source", results.getReturnedResultCount(), equalTo(48));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		LocalDateTime currLocalDate;
		for ( currLocalDate = new LocalDateTime(2001, 1, 1, 0, 0, 0); currLocalDate.isBefore(
				new LocalDateTime(2001, 1, 2, 0, 0, 0)); currLocalDate = currLocalDate.plusHours(1) ) {
			m = itr.next();
			assertThat("Result date is grouped",
					m.getLocalDate().toLocalDateTime(m.getLocalTime()).isEqual(currLocalDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();
			if ( i == 23 ) {
				assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 80000));
			} else {
				assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 90000));
			}
			assertThat("Aggregate W", data, hasEntry("watts", (Object) 1000));

			m = itr.next();
			assertThat("Result date is grouped",
					m.getLocalDate().toLocalDateTime(m.getLocalTime()).isEqual(currLocalDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();
			if ( i == 23 ) {
				assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 8000));
			} else {
				assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 9000));
			}
			assertThat("Aggregate W", data, hasEntry("watts", (Object) 100));

			i++;
		}
	}
}
