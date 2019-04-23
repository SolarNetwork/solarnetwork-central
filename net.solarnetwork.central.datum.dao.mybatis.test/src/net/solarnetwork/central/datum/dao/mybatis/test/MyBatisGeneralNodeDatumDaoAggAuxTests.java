/* ==================================================================
 * MyBatisGeneralNodeDatumDaoAggAuxTests.java - 23/04/2019 10:13:32 am
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumAuxiliaryDao;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for aggregate data combined with auxiliary data.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoAggAuxTests extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final String WATT_HOURS = "watt_hours";

	private MyBatisGeneralNodeDatumAuxiliaryDao auxDao;

	@Override
	@Before
	public void setup() {
		super.setup();
		auxDao = new MyBatisGeneralNodeDatumAuxiliaryDao();
		auxDao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralNodeDatumAuxiliary getTestAuxInstance(DateTime created, Long nodeId, String sourceId,
			Long finalReading, Long startReading) {
		GeneralNodeDatumAuxiliary datum = new GeneralNodeDatumAuxiliary();
		datum.setCreated(created);
		datum.setNodeId(nodeId);
		datum.setSourceId(sourceId);
		datum.setType(DatumAuxiliaryType.Reset);

		GeneralNodeDatumSamples samplesFinal = new GeneralNodeDatumSamples();
		samplesFinal.putAccumulatingSampleValue(WATT_HOURS, finalReading);
		datum.setSamplesFinal(samplesFinal);

		GeneralNodeDatumSamples samplesStart = new GeneralNodeDatumSamples();
		samplesStart.putAccumulatingSampleValue(WATT_HOURS, startReading);
		datum.setSamplesStart(samplesStart);

		return datum;
	}

	@Test
	public void listHourlyAggregateWithSingleResetAuxiliary() {
		final DateTime start = new DateTime(2019, 4, 23, 0, 0, DateTimeZone.UTC);
		final DateTime discontUp = new DateTime(2019, 4, 23, 1, 0, DateTimeZone.UTC);
		final long discontUpJump = 1000000000L;
		final DateTime end = new DateTime(2019, 4, 23, 2, 0, DateTimeZone.UTC);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			if ( date.isEqual(discontUp) ) {
				wh = discontUpJump;
			}
			GeneralNodeDatum d = getTestInstance(date, TEST_NODE_ID, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue(WATT_HOURS, wh);
			dao.store(d);
			wh += 50L;
			date = date.plusMinutes(30);
		}

		GeneralNodeDatumAuxiliary aux = getTestAuxInstance(discontUp, TEST_NODE_ID, TEST_SOURCE_ID, 100L,
				discontUpJump);
		auxDao.store(aux);

		List<Map<String, Object>> raw = findDatumForTimeSpan(TEST_NODE_ID, TEST_SOURCE_ID, start,
				"1 hour");
		assertThat("Raw data with auxiliary count", raw, hasSize(4));

		processAggregateStaleData();

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg row count", hourly, hasSize(3));

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 2L, (long) results.getTotalResults());
		assertEquals("Daily query results", 2, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results.getResults() ) {
			assertThat("Agg result ID " + i, match.getId(),
					equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, start.plusHours(i), TEST_SOURCE_ID)));
			Map<String, ?> data = match.getSampleData();
			assertThat("Agg result Wh " + i, data, hasEntry(WATT_HOURS, 100));
			i++;
		}
	}

	@Test
	public void listHourlyAggregateWithDoubleResetAuxiliary() {
		final DateTime start = new DateTime(2019, 4, 23, 0, 0, DateTimeZone.UTC);
		final DateTime discontUp = new DateTime(2019, 4, 23, 1, 0, DateTimeZone.UTC);
		final long discontUpJump = 1000000000L;
		final DateTime discontDown = new DateTime(2019, 4, 23, 1, 30, DateTimeZone.UTC);
		final long discontDownJump = 150L;
		final DateTime end = new DateTime(2019, 4, 23, 2, 0, DateTimeZone.UTC);

		DateTime date = start.withZone(DateTimeZone.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			if ( date.isEqual(discontUp) ) {
				wh = discontUpJump;
			} else if ( date.isEqual(discontDown) ) {
				wh = discontDownJump;
			}
			GeneralNodeDatum d = getTestInstance(date, TEST_NODE_ID, TEST_SOURCE_ID);
			d.getSamples().getI().clear();
			d.getSamples().getS().clear();
			d.getSamples().putAccumulatingSampleValue(WATT_HOURS, wh);
			dao.store(d);
			wh += 50L;
			date = date.plusMinutes(30);
		}

		GeneralNodeDatumAuxiliary aux = getTestAuxInstance(discontUp, TEST_NODE_ID, TEST_SOURCE_ID, 100L,
				discontUpJump);
		auxDao.store(aux);

		GeneralNodeDatumAuxiliary aux2 = getTestAuxInstance(discontDown, TEST_NODE_ID, TEST_SOURCE_ID,
				discontUpJump + 50, discontDownJump);
		auxDao.store(aux2);

		List<Map<String, Object>> raw = findDatumForTimeSpan(TEST_NODE_ID, TEST_SOURCE_ID, start,
				"1 hour");
		assertThat("Raw data with auxiliary count 1", raw, hasSize(4));

		List<Map<String, Object>> raw2 = findDatumForTimeSpan(TEST_NODE_ID, TEST_SOURCE_ID,
				start.plusHours(1), "1 hour");
		assertThat("Raw data with auxiliary count 2", raw2, hasSize(6));

		processAggregateStaleData();

		List<Map<String, Object>> hourly = getDatumAggregateHourly();
		assertThat("Hourly agg row count", hourly, hasSize(3));

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(end);
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 2L, (long) results.getTotalResults());
		assertEquals("Daily query results", 2, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results.getResults() ) {
			assertThat("Agg result ID " + i, match.getId(),
					equalTo(new GeneralNodeDatumPK(TEST_NODE_ID, start.plusHours(i), TEST_SOURCE_ID)));
			Map<String, ?> data = match.getSampleData();
			assertThat("Agg result Wh " + i, data, hasEntry(WATT_HOURS, 100));
			i++;
		}
	}

}
