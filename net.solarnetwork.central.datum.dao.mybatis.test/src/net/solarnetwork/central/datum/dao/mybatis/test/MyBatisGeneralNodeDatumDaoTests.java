/* ==================================================================
 * MyBatisGeneralNodeDatumDaoTests.java - Nov 14, 2014 6:21:15 AM
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
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInterval;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_2ND_SOURCE = "2nd source";
	private static final Long TEST_2ND_NODE = -200L;

	private MyBatisGeneralNodeDatumDao dao;

	private GeneralNodeDatum lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisGeneralNodeDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralNodeDatum getTestInstance() {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(new DateTime());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
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
		GeneralNodeDatum datum = getTestInstance();
		GeneralNodeDatumPK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(GeneralNodeDatum src, GeneralNodeDatum entity) {
		assertNotNull("GeneralNodeDatum should exist", entity);
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getPosted(), entity.getPosted());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getSamples(), entity.getSamples());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralNodeDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		GeneralNodeDatum datum = getTestInstance();
		datum.getSamples().getAccumulating().put("watt_hours", 39309570293789380L);
		datum.getSamples().getAccumulating()
				.put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getSamples().getInstantaneous().put("watts", 498475890235787897L);
		datum.getSamples().getInstantaneous()
				.put("floating", new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralNodeDatum entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao
				.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"i\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		GeneralNodeDatum datum3 = new GeneralNodeDatum();
		datum3.setCreated(lastDatum.getCreated());
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setSampleJson("{\"a\":{\"watt_hours\":789}}");
		dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<GeneralNodeDatumPK> ids = new ArrayList<GeneralNodeDatumPK>();
		for ( GeneralNodeDatumFilterMatch d : results ) {
			ids.add(d.getId());
		}
		// expect d3, d1, d2 because sorted by nodeId,created,sourceId
		assertEquals("Result order", Arrays.asList(datum3.getId(), lastDatum.getId(), datum2.getId()),
				ids);
	}

	@Test
	public void findFilteredWithMax() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(criteria, null, 0, 1);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
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
	public void getAllAvailableSourcesForNode() {
		storeNew();
		Set<String> sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));

		// add a 2nd source (two more datum to get into reporting table).
		// we also make this on another day, to support getAllAvailableSourcesForNodeAndDateRange() test
		GeneralNodeDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(1));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setSourceId(d3.getSourceId());
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));
		assertTrue("Source ID returned", sources.contains(d3.getSourceId()));
	}

	@Test
	public void getReportableIntervalNoDatum() {
		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, null);
		Assert.assertNull(result);
	}

	@Test
	public void getReportableIntervalOneDatum() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalTwoDatum() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalTwoDatumDifferentSources() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, null);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalForSourceNoMatch() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, TEST_2ND_SOURCE);
		Assert.assertNull(result);
	}

	@Test
	public void getReportableIntervalForSourceOneMatch() {
		storeNew();
		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, TEST_SOURCE_ID);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void getReportableIntervalForSourceTwoDatumDifferentSources() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		ReadableInterval result = dao.getReportableInterval(TEST_NODE_ID, TEST_SOURCE_ID);
		assertNotNull(result);
		assertEquals(lastDatum.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(lastDatum.getCreated().getMillis(), result.getEnd().getMillis());

		result = dao.getReportableInterval(TEST_NODE_ID, TEST_2ND_SOURCE);
		assertNotNull(result);
		assertEquals(d2.getCreated().getMillis(), result.getStart().getMillis());
		assertEquals(d2.getCreated().getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void findMostRecentAllSources() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		// most recent sorted ascending by source ID
		List<GeneralNodeDatumPK> pks = Arrays.asList(d2.getId(), lastDatum.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(2), results.getTotalResults());
		Iterator<GeneralNodeDatumPK> pkIterator = pks.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findMostRecentOneSource() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(1), results.getTotalResults());
		GeneralNodeDatumFilterMatch match = results.getResults().iterator().next();
		assertEquals(lastDatum.getId(), match.getId());
	}

	@Test
	public void findMostRecentTwoSources() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setSourceId(TEST_2ND_SOURCE);
		dao.store(d2);

		GeneralNodeDatum d3 = getTestInstance();
		d3.setCreated(d2.getCreated().plus(1000));
		d3.setSourceId("3rd source");
		dao.store(d3);

		// most recent sorted ascending by source ID
		List<GeneralNodeDatumPK> pks = Arrays.asList(d3.getId(), lastDatum.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, "3rd source" });
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(2), results.getTotalResults());
		Iterator<GeneralNodeDatumPK> pkIterator = pks.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findMostRecentTwoNodes() {
		storeNew();

		setupTestNode(TEST_2ND_NODE);

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		GeneralNodeDatum d3 = getTestInstance();
		d3.setNodeId(TEST_2ND_NODE);
		d3.setCreated(d2.getCreated().plus(1000));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setNodeId(TEST_2ND_NODE);
		d4.setSourceId(TEST_2ND_SOURCE);
		d4.setCreated(d3.getCreated());
		dao.store(d4);

		GeneralNodeDatum d5 = getTestInstance();
		d5.setNodeId(TEST_2ND_NODE);
		d5.setSourceId(TEST_2ND_SOURCE);
		d5.setCreated(d4.getCreated().plus(1000));
		dao.store(d5);

		// most recent sorted ascending by node ID, source ID
		List<GeneralNodeDatumPK> pks = Arrays.asList(d5.getId(), d3.getId(), d2.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(3), results.getTotalResults());
		Iterator<GeneralNodeDatumPK> pkIterator = pks.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findMostRecentForUser() {
		storeNew();
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "1st node");

		setupTestNode(TEST_2ND_NODE);
		setupTestUserNode(TEST_USER_ID, TEST_2ND_NODE, "2nd node");

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		GeneralNodeDatum d3 = getTestInstance();
		d3.setNodeId(TEST_2ND_NODE);
		d3.setCreated(d2.getCreated().plus(1000));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setNodeId(TEST_2ND_NODE);
		d4.setSourceId(TEST_2ND_SOURCE);
		d4.setCreated(d3.getCreated());
		dao.store(d4);

		GeneralNodeDatum d5 = getTestInstance();
		d5.setNodeId(TEST_2ND_NODE);
		d5.setSourceId(TEST_2ND_SOURCE);
		d5.setCreated(d4.getCreated().plus(1000));
		dao.store(d5);

		// most recent sorted ascending by node ID, source ID
		List<GeneralNodeDatumPK> pks = Arrays.asList(d5.getId(), d3.getId(), d2.getId());

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(3), results.getTotalResults());
		Iterator<GeneralNodeDatumPK> pkIterator = pks.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			assertEquals(pkIterator.next(), match.getId());
		}
	}

	@Test
	public void findFilteredAggregateHourly() {
		GeneralNodeDatum datum1 = new GeneralNodeDatum();
		datum1.setCreated(new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC));
		datum1.setNodeId(TEST_NODE_ID);
		datum1.setSourceId(TEST_SOURCE_ID);
		datum1.setSampleJson("{\"a\":{\"watt_hours\":0}}");
		dao.store(datum1);
		lastDatum = datum1;

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(datum1.getCreated().plusMinutes(20));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"a\":{\"watt_hours\":5}}");
		dao.store(datum2);

		GeneralNodeDatum datum3 = new GeneralNodeDatum();
		datum3.setCreated(datum2.getCreated().plusMinutes(20));
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId(TEST_SOURCE_ID);
		datum3.setSampleJson("{\"a\":{\"watt_hours\":10}}");
		dao.store(datum3);

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(datum1.getCreated());
		criteria.setEndDate(datum3.getCreated());
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
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
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(lastDatum.getCreated().withTime(0, 0, 0, 0));
		criteria.setEndDate(lastDatum.getCreated().plusDays(1));
		criteria.setAggregate(Aggregation.Day);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Daily query results", 1L, (long) results.getTotalResults());
		assertEquals("Daily query results", 1, (int) results.getReturnedResultCount());

		Map<String, ?> data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(10), data.get("watt_hours"));

		// ok, add another sample and now check for whole day, we should have 15 Wh
		GeneralNodeDatum datum4 = new GeneralNodeDatum();
		datum4.setCreated(lastDatum.getCreated().plusMinutes(60)); // move to a different hour
		datum4.setNodeId(TEST_NODE_ID);
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
	public void findFilteredAggregateFiveMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FiveMinute);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		// this query fills in empty slots, so we have :05, :10, :15, ... :50, :55
		assertEquals("Minute query results", 12L, (long) results.getTotalResults());
		assertEquals("Minute query results", 12, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 11 ? 10 : 0), match.getSampleData()
					.get("watt_hours"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateTenMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.TenMinute);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		// this query fills in empty slots, so we have :00, :10, :20, :30, :40, and :50
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 5 ? 20 : 10), match.getSampleData()
					.get("watt_hours"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateFifteenMinute() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FifteenMinute);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		// this query fills in empty slots, so we have :00, :15, :30, :45
		assertEquals("Minute query results", 4L, (long) results.getTotalResults());
		assertEquals("Minute query results", 4, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 3 ? 30 : 20), match.getSampleData()
					.get("watt_hours"));
			i++;
		}
	}
}
