/* ==================================================================
 * MyBatisGeneralLocationDatumYearAggregationTests.java - 4/05/2020 11:22:06 am
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Iterator;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for the year aggregation queries.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralLocationDatumYearAggregationTests
		extends MyBatisGeneralLocationDatumDaoTestSupport {

	private static final String TEST_2ND_TZ = "America/New_York";

	@Test
	public void findFilteredAggregateYearly() {
		setupTestLocation(TEST_2ND_LOC);
		final DateTime startDate = new DateTime(2019, 1, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int j = 0; j < 12; j += 4 ) {
			final int count = 145;
			for ( int i = 0; i < count; i++ ) {
				GeneralLocationDatum d = new GeneralLocationDatum();
				d.setCreated(startDate.plusMonths(j).plusMinutes(i * 20));
				d.setLocationId(TEST_LOC_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "},\"i\":{\"watts\":"
						+ ((j + 1) * 1000) + "}}");
				dao.store(d);
				d.setLocationId(TEST_2ND_LOC);
				d.setSourceId(TEST_2ND_SOURCE);
				d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "},\"i\":{\"watts\":"
						+ ((j + 1) * 10000) + "}}");
				dao.store(d);
			}
		}

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationIds(new Long[] { TEST_LOC_ID, TEST_2ND_LOC });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusYears(1));
		criteria.setAggregate(Aggregation.Year);
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count not provided", results.getTotalResults(), nullValue());
		assertThat("One year result for each source", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralLocationDatumMatch> itr = results.iterator();
		ReportingGeneralLocationDatumMatch m;
		Map<String, ?> data;

		m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID", m.getId().getLocationId(), equalTo(TEST_2ND_LOC));
		assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
		data = m.getSampleData();
		assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 21600));
		assertThat("Aggregate W (1 + 5 + 9)/3", data, hasEntry("watts", (Object) 50000));

		m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID", m.getId().getLocationId(), equalTo(TEST_LOC_ID));
		assertThat("Result source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		data = m.getSampleData();
		assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 2160));
		assertThat("Aggregate W (1 + 5 + 9)/3", data, hasEntry("watts", (Object) 5000));
	}

	@Test
	public void findFilteredAggregateYearly_multipleYears() {
		setupTestLocation(TEST_2ND_LOC);
		final DateTime startDate = new DateTime(2017, 1, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int y = 0; y < 3; y++ ) {
			for ( int j = 0; j < 12; j += 4 ) {
				final int count = 145;
				for ( int i = 0; i < count; i++ ) {
					GeneralLocationDatum d = new GeneralLocationDatum();
					d.setCreated(startDate.plusYears(y).plusMonths(j).plusMinutes(i * 20));
					d.setLocationId(TEST_LOC_ID);
					d.setSourceId(TEST_SOURCE_ID);
					d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "},\"i\":{\"watts\":"
							+ ((j + 1) * 1000) + "}}");
					dao.store(d);
					d.setLocationId(TEST_2ND_LOC);
					d.setSourceId(TEST_2ND_SOURCE);
					d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "},\"i\":{\"watts\":"
							+ ((j + 1) * 10000) + "}}");
					dao.store(d);
				}
			}
		}

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationIds(new Long[] { TEST_LOC_ID, TEST_2ND_LOC });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusYears(3));
		criteria.setAggregate(Aggregation.Year);
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count not provided", results.getTotalResults(), nullValue());
		assertThat("One year result for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralLocationDatumMatch> itr = results.iterator();
		ReportingGeneralLocationDatumMatch m;
		Map<String, ?> data;

		for ( int y = 0; y < 3; y++ ) {
			m = itr.next();
			assertThat("Result date is grouped " + y,
					m.getId().getCreated().isEqual(startDate.plusYears(y)), equalTo(true));
			assertThat("Result node ID " + y, m.getId().getLocationId(), equalTo(TEST_2ND_LOC));
			assertThat("Result source ID " + y, m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();
			assertThat("Aggregate Wh " + y, data, hasEntry("watt_hours", (Object) 21600));
			assertThat("Aggregate W (1 + 5 + 9)/3 " + y, data, hasEntry("watts", (Object) 50000));

			m = itr.next();
			assertThat("Result date is grouped " + y,
					m.getId().getCreated().isEqual(startDate.plusYears(y)), equalTo(true));
			assertThat("Result node ID " + y, m.getId().getLocationId(), equalTo(TEST_LOC_ID));
			assertThat("Result source ID " + y, m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();
			assertThat("Aggregate Wh " + y, data, hasEntry("watt_hours", (Object) 2160));
			assertThat("Aggregate W (1 + 5 + 9)/3 " + y, data, hasEntry("watts", (Object) 5000));
		}
	}

	@Test
	public void findFilteredAggregateYearly_multipleYears_localTimeZones() {
		setupTestLocation(TEST_2ND_LOC, TEST_2ND_TZ);
		setupTestNode(TEST_2ND_LOC, TEST_2ND_LOC);
		final LocalDateTime startDate = new LocalDateTime(2017, 1, 1, 0, 0, 0);
		for ( int y = 0; y < 3; y++ ) {
			for ( int j = 0; j < 12; j += 4 ) {
				final int count = 145;
				for ( int i = 0; i < count; i++ ) {
					GeneralLocationDatum d = new GeneralLocationDatum();
					d.setCreated(startDate.toDateTime(DateTimeZone.forID(TEST_TZ)).plusYears(y)
							.plusMonths(j).plusMinutes(i * 20));
					d.setLocationId(TEST_LOC_ID);
					d.setSourceId(TEST_SOURCE_ID);
					d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "},\"i\":{\"watts\":"
							+ ((j + 1) * 1000) + "}}");
					dao.store(d);

					d.setCreated(startDate.toDateTime(DateTimeZone.forID(TEST_2ND_TZ)).plusYears(y)
							.plusMonths(j).plusMinutes(i * 20));
					d.setLocationId(TEST_2ND_LOC);
					d.setSourceId(TEST_2ND_SOURCE);
					d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "},\"i\":{\"watts\":"
							+ ((j + 1) * 10000) + "}}");
					dao.store(d);
				}
			}
		}

		processAggregateStaleData();

		for ( Map<String, Object> row : datumRowsMonthly() ) {
			log.debug("Month row: {}", row);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationIds(new Long[] { TEST_LOC_ID, TEST_2ND_LOC });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setLocalStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setLocalEndDate(criteria.getLocalStartDate().plusYears(3));
		criteria.setAggregate(Aggregation.Year);
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count not provided", results.getTotalResults(), nullValue());
		assertThat("One year result for each source", results.getReturnedResultCount(), equalTo(6));

		Iterator<ReportingGeneralLocationDatumMatch> itr = results.iterator();
		ReportingGeneralLocationDatumMatch m;
		Map<String, ?> data;

		for ( int y = 0; y < 3; y++ ) {
			m = itr.next();
			assertThat("Result date is grouped " + y,
					m.getId().getCreated()
							.isEqual(startDate.toDateTime(DateTimeZone.forID(TEST_TZ)).plusYears(y)),
					equalTo(true));
			assertThat("Result node ID " + y, m.getId().getLocationId(), equalTo(TEST_LOC_ID));
			assertThat("Result source ID " + y, m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
			data = m.getSampleData();
			assertThat("Aggregate Wh " + y, data, hasEntry("watt_hours", (Object) 2160));
			assertThat("Aggregate W (1 + 5 + 9)/3 " + y, data, hasEntry("watts", (Object) 5000));

			m = itr.next();
			assertThat("Result date is grouped " + y,
					m.getId().getCreated()
							.isEqual(startDate.toDateTime(DateTimeZone.forID(TEST_2ND_TZ)).plusYears(y)),
					equalTo(true));
			assertThat("Result node ID " + y, m.getId().getLocationId(), equalTo(TEST_2ND_LOC));
			assertThat("Result source ID " + y, m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
			data = m.getSampleData();
			assertThat("Aggregate Wh " + y, data, hasEntry("watt_hours", (Object) 21600));
			assertThat("Aggregate W (1 + 5 + 9)/3 " + y, data, hasEntry("watts", (Object) 50000));
		}
	}

}