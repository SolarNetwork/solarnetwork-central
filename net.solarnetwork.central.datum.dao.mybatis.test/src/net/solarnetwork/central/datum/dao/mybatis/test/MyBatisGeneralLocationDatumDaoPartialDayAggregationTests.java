/* ==================================================================
 * MyBatisGeneralLocationDatumDaoPartialDayAggregationTests.java - 4/05/2020 10:38:38 am
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
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for partial day aggregation queries.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralLocationDatumDaoPartialDayAggregationTests
		extends MyBatisGeneralLocationDatumDaoTestSupport {

	@Test
	public void findFilteredAggregateDayPartialHour() {
		setupTestLocation(TEST_2ND_LOC);
		final DateTime startDate = new DateTime(2019, 1, 15, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final DateTime endDate = new DateTime(2019, 1, 18, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime currDate = startDate;
		final String datumJson = "{\"a\":{\"watt_hours\":%d},\"i\":{\"watts\":%d}}";
		long wh = 0;
		do {
			GeneralLocationDatum d = new GeneralLocationDatum();
			d.setCreated(currDate);
			d.setLocationId(TEST_LOC_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson(String.format(datumJson, wh, currDate.getMinuteOfHour() * 100));
			dao.store(d);
			d.setLocationId(TEST_2ND_LOC);
			d.setSourceId(TEST_2ND_SOURCE);
			d.setSampleJson(String.format(datumJson, wh * 10, currDate.getMinuteOfHour() * 1000));
			dao.store(d);
			wh += 1000;
			currDate = currDate.plusMinutes(20);
		} while ( currDate.isBefore(endDate) );

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsHourly() ) {
			log.debug("Hour row: {}", row);
		}

		for ( Map<String, Object> row : datumRowsDaily() ) {
			log.debug("Day row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationIds(new Long[] { TEST_LOC_ID, TEST_2ND_LOC });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.plusHours(12));
		criteria.setEndDate(endDate.minusHours(8));
		criteria.setAggregate(Aggregation.Day);
		criteria.setPartialAggregation(Aggregation.Hour);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count provided", results.getTotalResults(), equalTo(6L));
		assertThat("3 day results for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralLocationDatumMatch> itr = results.iterator();
		ReportingGeneralLocationDatumMatch m;
		Map<String, ?> data;

		int i = 0;
		for ( currDate = startDate; currDate.isBefore(endDate); currDate = currDate.plusDays(1) ) {
			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getLocationId(), equalTo(TEST_2ND_LOC));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();

			assertThat("Aggregate W", data, hasEntry("watts", (Object) 20000));
			switch (i) {
				case 0:
					assertThat("Aggregate Wh (12h x 30000 Wh)", data,
							hasEntry("watt_hours", (Object) 360000));
					break;
				case 1:
					assertThat("Aggregate Wh (1d x 720000 Wh)", data,
							hasEntry("watt_hours", (Object) 720000));
					break;
				case 2:
					assertThat("Aggregate Wh (16h x 30000 Wh)", data,
							hasEntry("watt_hours", (Object) 480000));
					break;
			}

			m = itr.next();
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(currDate),
					equalTo(true));
			assertThat("Result node ID", m.getId().getLocationId(), equalTo(TEST_LOC_ID));
			assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();

			assertThat("Aggregate W", data, hasEntry("watts", (Object) 2000));
			switch (i) {
				case 0:
					assertThat("Aggregate Wh (12h)", data, hasEntry("watt_hours", (Object) 36000));
					break;
				case 1:
					assertThat("Aggregate Wh (1d)", data, hasEntry("watt_hours", (Object) 72000));
					break;
				case 2:
					assertThat("Aggregate Wh (16h)", data, hasEntry("watt_hours", (Object) 48000));
					break;
			}

			i++;
		}
	}

}