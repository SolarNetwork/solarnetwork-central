/* ==================================================================
 * MyBatisGeneralLocationDatumDaoTests.java - Nov 13, 2014 8:35:18 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInterval;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralLocationDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumMappingInfo;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.PriceDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralLocationDatumSamples;

/**
 * Test cases for the {@link MyBatisGeneralLocationDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralLocationDatumDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_2ND_SOURCE = "2nd source";

	private MyBatisGeneralLocationDatumDao dao;

	private GeneralLocationDatum lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisGeneralLocationDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralLocationDatum getTestInstance() {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_LOC_ID);
		datum.setPosted(new DateTime());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("watt_hours", 4123);
		samples.setAccumulating(accum);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setStatus(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralLocationDatum datum = getTestInstance();
		GeneralLocationDatumPK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(GeneralLocationDatum src, GeneralLocationDatum entity) {
		assertNotNull("GeneralLocationDatum should exist", entity);
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getPosted(), entity.getPosted());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getSamples(), entity.getSamples());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralLocationDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		GeneralLocationDatum datum = getTestInstance();
		datum.getSamples().getAccumulating().put("watt_hours", 39309570293789380L);
		datum.getSamples().getAccumulating().put("very_big",
				new BigInteger("93475092039478209375027350293523957"));
		datum.getSamples().getInstantaneous().put("watts", 498475890235787897L);
		datum.getSamples().getInstantaneous().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralLocationDatum entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	private int getAuditDatumHourlyPropCount(GeneralLocationDatum datum) {
		DateTime tsStart = datum.getPosted().property(DateTimeFieldType.hourOfDay()).roundFloorCopy();
		Integer propCount = this.jdbcTemplate.queryForObject(
				"SELECT prop_count FROM solaragg.aud_loc_datum_hourly WHERE ts_start = ? AND loc_id = ? AND source_id = ?",
				new Object[] { new java.sql.Timestamp(tsStart.getMillis()), datum.getLocationId(),
						datum.getSourceId() },
				Integer.class);
		return (propCount == null ? 0 : propCount.intValue());
	}

	@Test
	public void storeNewCreatesHourAuditRecord() {
		storeNew();
		int propCount = getAuditDatumHourlyPropCount(lastDatum);
		assertEquals(3, propCount);
	}

	@Test
	public void storeMultiWithinHourUpdatesHourAuditRecord() {
		final DateTime now = new DateTime();

		GeneralLocationDatum datum1 = new GeneralLocationDatum();
		datum1.setCreated(new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC));
		datum1.setPosted(now);
		datum1.setLocationId(TEST_LOC_ID);
		datum1.setSourceId(TEST_SOURCE_ID);
		datum1.setSampleJson("{\"a\":{\"watt_hours\":0}, \"t\":[\"foo\"]}");
		dao.store(datum1);
		lastDatum = datum1;

		GeneralLocationDatum datum2 = new GeneralLocationDatum();
		datum2.setCreated(datum1.getCreated().plusMinutes(20));
		datum2.setPosted(now);
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"a\":{\"watt_hours\":5}}");
		dao.store(datum2);

		GeneralLocationDatum datum3 = new GeneralLocationDatum();
		datum3.setCreated(datum2.getCreated().plusMinutes(20));
		datum3.setPosted(now);
		datum3.setLocationId(TEST_LOC_ID);
		datum3.setSourceId(TEST_SOURCE_ID);
		datum3.setSampleJson("{\"a\":{\"watt_hours\":10}}");
		dao.store(datum3);

		int propCount = getAuditDatumHourlyPropCount(datum1);
		assertEquals(4, propCount);
	}

	@Test
	public void findFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);

		FilterResults<GeneralLocationDatumFilterMatch> results = dao.findFiltered(criteria, null, null,
				null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralLocationDatum datum2 = new GeneralLocationDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"i\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		GeneralLocationDatum datum3 = new GeneralLocationDatum();
		datum3.setCreated(lastDatum.getCreated());
		datum3.setLocationId(TEST_LOC_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setSampleJson("{\"a\":{\"watt_hours\":789}}");
		dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<GeneralLocationDatumPK> ids = new ArrayList<GeneralLocationDatumPK>();
		for ( GeneralLocationDatumFilterMatch d : results ) {
			ids.add(d.getId());
		}
		// expect d3, d1, d2 because sorted by locationId,created,sourceId
		assertEquals("Result order", Arrays.asList(datum3.getId(), lastDatum.getId(), datum2.getId()),
				ids);
	}

	@Test
	public void findFilteredWithMax() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);

		FilterResults<GeneralLocationDatumFilterMatch> results = dao.findFiltered(criteria, null, 0, 1);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralLocationDatum datum2 = new GeneralLocationDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"i\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, 0, 1);
		assertNotNull(results);
		assertEquals("Returned results", 2L, (long) results.getTotalResults());
		assertEquals("Returned result count", 1, (int) results.getReturnedResultCount());
		assertEquals("Datum ID", lastDatum.getId(), results.iterator().next().getId());
	}

	@Test
	public void getAllAvailableSourcesForLocation() {
		storeNew();
		Set<String> sources = dao.getAvailableSources(lastDatum.getLocationId(), null, null);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getLocationId(), null, null);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));

		// add a 2nd source (two more datum to get into reporting table).
		// we also make this on another day, to support getAllAvailableSourcesForLocationAndDateRange() test
		GeneralLocationDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(1));
		dao.store(d3);

		GeneralLocationDatum d4 = getTestInstance();
		d4.setSourceId(d3.getSourceId());
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getLocationId(), null, null);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));
		assertTrue("Source ID returned", sources.contains(d3.getSourceId()));
	}

	@Test
	public void getReportableIntervalNoDatum() {
		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, null);
		Assert.assertNull(result);
	}

	@Test
	public void getReportableIntervalOneDatum() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalTwoDatum() {
		storeNew();

		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalTwoDatumDifferentSources() {
		storeNew();

		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalForSourceNoMatch() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, TEST_2ND_SOURCE);
		Assert.assertNull(result);
	}

	@Test
	public void getReportableIntervalForSourceOneMatch() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, TEST_SOURCE_ID);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalForSourceTwoDatumDifferentSources() {
		storeNew();

		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_LOC_ID, TEST_SOURCE_ID);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());

		result = dao.getReportableInterval(TEST_LOC_ID, TEST_2ND_SOURCE);
		assertNotNull(result);
		assertEquals(d2.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void findMostRecentAllSources() {
		storeNew();

		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		// most recent sorted ascending by source ID
		List<GeneralLocationDatumPK> pks = Arrays.asList(d2.getId(), lastDatum.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(TEST_LOC_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralLocationDatumFilterMatch> results = dao.findFiltered(filter, null, null,
				null);
		assertNotNull(results);
		assertEquals(Long.valueOf(2), results.getTotalResults());
		Iterator<GeneralLocationDatumPK> pkIterator = pks.iterator();
		for ( GeneralLocationDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findMostRecentOneSource() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(TEST_LOC_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralLocationDatumFilterMatch> results = dao.findFiltered(filter, null, null,
				null);
		assertNotNull(results);
		assertEquals(Long.valueOf(1), results.getTotalResults());
		GeneralLocationDatumFilterMatch match = results.getResults().iterator().next();
		assertEquals(lastDatum.getId(), match.getId());
	}

	@Test
	public void findMostRecentTwoSources() {
		storeNew();

		GeneralLocationDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		GeneralLocationDatum d3 = getTestInstance();
		d3.setCreated(d2.getCreated().plus(1000));
		d3.setSourceId("3rd source");
		dao.store(d3);

		// most recent sorted ascending by source ID
		List<GeneralLocationDatumPK> pks = Arrays.asList(d3.getId(), lastDatum.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(TEST_LOC_ID);
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, "3rd source" });
		filter.setMostRecent(true);
		FilterResults<GeneralLocationDatumFilterMatch> results = dao.findFiltered(filter, null, null,
				null);
		assertNotNull(results);
		assertEquals(Long.valueOf(2), results.getTotalResults());
		Iterator<GeneralLocationDatumPK> pkIterator = pks.iterator();
		for ( GeneralLocationDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findFilteredAggregateHourly() {
		GeneralLocationDatum datum1 = new GeneralLocationDatum();
		datum1.setCreated(new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC));
		datum1.setLocationId(TEST_LOC_ID);
		datum1.setSourceId(TEST_SOURCE_ID);
		datum1.setSampleJson("{\"a\":{\"watt_hours\":0}}");
		dao.store(datum1);
		lastDatum = datum1;

		GeneralLocationDatum datum2 = new GeneralLocationDatum();
		datum2.setCreated(datum1.getCreated().plusMinutes(20));
		datum2.setLocationId(TEST_LOC_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"a\":{\"watt_hours\":5}}");
		dao.store(datum2);

		GeneralLocationDatum datum3 = new GeneralLocationDatum();
		datum3.setCreated(datum2.getCreated().plusMinutes(20));
		datum3.setLocationId(TEST_LOC_ID);
		datum3.setSourceId(TEST_SOURCE_ID);
		datum3.setSampleJson("{\"a\":{\"watt_hours\":10}}");
		dao.store(datum3);

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(datum1.getCreated());
		criteria.setEndDate(datum3.getCreated());
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		Map<String, ?> data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(10), data.get("watt_hours"));
	}

	@Test
	public void findFilteredAggregateDaily() {
		// populate 1 hour of data
		findFilteredAggregateHourly();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(lastDatum.getCreated().withTime(0, 0, 0, 0));
		criteria.setEndDate(lastDatum.getCreated().plusDays(1));
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 1L, (long) results.getTotalResults());
		assertEquals("Daily query results", 1, (int) results.getReturnedResultCount());

		Map<String, ?> data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(10), data.get("watt_hours"));

		// ok, add another sample and now check for whole day, we should have 15 Wh
		GeneralLocationDatum datum4 = new GeneralLocationDatum();
		datum4.setCreated(lastDatum.getCreated().plusMinutes(60)); // move to a different hour
		datum4.setLocationId(TEST_LOC_ID);
		datum4.setSourceId(TEST_SOURCE_ID);
		datum4.setSampleJson("{\"a\":{\"watt_hours\":15}}");
		dao.store(datum4);

		processAggregateStaleData();

		criteria.setAggregate(Aggregation.Day);
		results = dao.findAggregationFiltered(criteria, null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 1L, (long) results.getTotalResults());
		assertEquals("Daily query results", 1, (int) results.getReturnedResultCount());
		data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(15), data.get("watt_hours"));
	}

	@Test
	public void findFilteredAggregateRunningTotal() {
		findFilteredAggregateDaily();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);

		FilterResults<ReportingGeneralLocationDatumMatch> results;
		Map<String, ?> data;

		results = dao.findAggregationFiltered(criteria, null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 1L, (long) results.getTotalResults());
		assertEquals("Daily query results", 1, (int) results.getReturnedResultCount());
		data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(15), data.get("watt_hours"));
	}

	@Test
	public void findFilteredAggregateFiveMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"wattHours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FiveMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 11L, (long) results.getTotalResults());
		assertEquals("Minute query results", 11, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			assertEquals("Wh for minute slot " + i, Integer.valueOf(10),
					match.getSampleData().get("wattHours"));
			i++;
		}
		assertEquals("Processed result count", 11, i);
	}

	@Test
	public void findFilteredAggregateFiveMinutePower() {
		// populate 12 5 minute, 120 W segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":120}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FiveMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 12L, (long) results.getTotalResults());
		assertEquals("Minute query results", 12, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			if ( i == 11 ) {
				Assert.assertNull("Last Wh not known", match.getSampleData().get("wattHours"));
			} else {
				assertEquals("Wh for minute slot " + i, Integer.valueOf(10),
						match.getSampleData().get("wattHours"));
			}
			assertEquals("W for minute slot " + i, Integer.valueOf(120),
					match.getSampleData().get("watts"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateTenMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"wattHours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.TenMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 5 ? 20 : 10),
					match.getSampleData().get("wattHours"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateTenMinutePower() {
		// populate 12 5 minute, 120 W segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":120}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.TenMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 5 ? 20 : 10),
					match.getSampleData().get("wattHours"));
			assertEquals("W for minute slot " + i, Integer.valueOf(120),
					match.getSampleData().get("watts"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateWithMinMax() {
		// populate 12 5 minute, 10x W segments
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":" + ((1 + i) * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FifteenMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 4L, (long) results.getTotalResults());
		assertEquals("Minute query results", 4, (int) results.getReturnedResultCount());

		/*-
		 * 2014-02-02 01:00:00+13, 2014-02-02 01:00:00, test.source, {"i":{"watts":20,"watts_min":10,"watts_max":30}}
		 * 2014-02-02 01:15:00+13, 2014-02-02 01:15:00, test.source, {"i":{"watts":50,"watts_min":40,"watts_max":60}}
		 * 2014-02-02 01:30:00+13, 2014-02-02 01:30:00, test.source, {"i":{"watts":80,"watts_min":70,"watts_max":90}}
		 * 2014-02-02 01:45:00+13, 2014-02-02 01:45:00, test.source, {"i":{"watts":110,"watts_min":100,"watts_max":120}}
		 */
		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			int expectedMin = (10 + (i * 30));
			int expectedMax = expectedMin + 20;
			int expected = (expectedMin + 10);
			assertEquals("W for minute slot " + i, Integer.valueOf(expected),
					match.getSampleData().get("watts"));
			assertEquals("Wmin for minute slot " + i, Integer.valueOf(expectedMin),
					match.getSampleData().get("watts_min"));
			assertEquals("Wmax for minute slot " + i, Integer.valueOf(expectedMax),
					match.getSampleData().get("watts_max"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateFifteenMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralLocationDatum datum1 = new GeneralLocationDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setLocationId(TEST_LOC_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(TEST_LOC_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FifteenMinute);

		FilterResults<ReportingGeneralLocationDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		// this query fills in empty slots, so we have :00, :15, :30, :45
		assertEquals("Minute query results", 4L, (long) results.getTotalResults());
		assertEquals("Minute query results", 4, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralLocationDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 3 ? 30 : 20),
					match.getSampleData().get("watt_hours"));
			i++;
		}
	}

	@Test
	public void mapDayDatum() {
		DayDatum day = new DayDatum();
		day.setLocationId(TEST_WEATHER_LOC_ID);

		DatumMappingInfo info = dao.getMappingInfo(day);
		assertNotNull(info);
		assertEquals("Location ID", TEST_LOC_ID, info.getId());
		assertEquals("Time zone", TEST_TZ, info.getTimeZoneId());
		assertEquals("Source ID", TEST_WEATHER_SOURCE_NAME + " Day", info.getSourceId());
	}

	@Test
	public void mapWeatherDatum() {
		WeatherDatum weather = new WeatherDatum();
		weather.setCreated(new DateTime());
		weather.setLocationId(TEST_WEATHER_LOC_ID);

		DatumMappingInfo info = dao.getMappingInfo(weather);
		assertNotNull(info);
		assertEquals("Location ID", TEST_LOC_ID, info.getId());
		assertEquals("Time zone", TEST_TZ, info.getTimeZoneId());
		assertEquals("Source ID", TEST_WEATHER_SOURCE_NAME, info.getSourceId());
	}

	@Test
	public void mapPriceDatum() {
		PriceDatum price = new PriceDatum();
		price.setCreated(new DateTime());
		price.setLocationId(TEST_PRICE_LOC_ID);

		DatumMappingInfo info = dao.getMappingInfo(price);
		assertNotNull(info);
		assertEquals("Location ID", TEST_LOC_ID, info.getId());
		assertEquals("Time zone", TEST_TZ, info.getTimeZoneId());
		assertEquals("Source ID", TEST_PRICE_SOURCE_NAME, info.getSourceId());
	}

}
