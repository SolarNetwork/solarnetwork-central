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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInterval;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 1.7
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
		return getTestInstance(new DateTime(), TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private GeneralNodeDatum getTestInstance(DateTime created, Long nodeId, String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(created);
		datum.setNodeId(nodeId);
		datum.setPosted(created);
		datum.setSourceId(sourceId);

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
		datum.getSamples().getAccumulating().put("very_big",
				new BigInteger("93475092039478209375027350293523957"));
		datum.getSamples().getInstantaneous().put("watts", 498475890235787897L);
		datum.getSamples().getInstantaneous().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralNodeDatum entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	private Map<String, Object> getAuditDatumHourlyPropCounts(GeneralNodeDatum datum) {
		DateTime tsStart = datum.getPosted().property(DateTimeFieldType.hourOfDay()).roundFloorCopy();
		Map<String, Object> propCounts = this.jdbcTemplate.queryForMap(
				"SELECT datum_count, prop_count FROM solaragg.aud_datum_hourly WHERE ts_start = ? AND node_id = ? AND source_id = ?",
				new Object[] { new java.sql.Timestamp(tsStart.getMillis()), datum.getNodeId(),
						datum.getSourceId() });
		if ( propCounts == null ) {
			return Collections.emptyMap();
		}
		return propCounts;
	}

	@Test
	public void storeNewCreatesHourAuditRecord() {
		storeNew();
		Map<String, Object> propCounts = getAuditDatumHourlyPropCounts(lastDatum);
		assertThat("Audit counts", propCounts,
				allOf(hasEntry("datum_count", (Object) 1), hasEntry("prop_count", (Object) 3)));
	}

	@Test
	public void storeUpdateModifiesHourAuditRecord() {
		DateTime now = new DateTime();
		GeneralNodeDatum datum = dao.get(dao.store(getTestInstance(now, TEST_NODE_ID, TEST_SOURCE_ID)));

		Map<String, Object> propCounts = getAuditDatumHourlyPropCounts(datum);
		assertThat("Audit counts", propCounts,
				allOf(hasEntry("datum_count", (Object) 1), hasEntry("prop_count", (Object) 3)));

		// re-store the same datum, but with one new property added
		GeneralNodeDatum updated = getTestInstance(now, TEST_NODE_ID, TEST_SOURCE_ID);
		updated.getSamples().putAccumulatingSampleValue("just.one.more.after.dinner.mint", 1);
		dao.store(updated);

		// now datum_count should STILL be 1 because this tracks number CREATED only
		// while prop_count INCREMENTS for every insert AND update
		propCounts = getAuditDatumHourlyPropCounts(datum);
		assertThat("Audit counts", propCounts,
				allOf(hasEntry("datum_count", (Object) 1), hasEntry("prop_count", (Object) 7)));
	}

	@Test
	public void storeMultiWithinHourUpdatesHourAuditRecord() {
		final DateTime now = new DateTime();

		GeneralNodeDatum datum1 = new GeneralNodeDatum();
		datum1.setCreated(new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC));
		datum1.setPosted(now);
		datum1.setNodeId(TEST_NODE_ID);
		datum1.setSourceId(TEST_SOURCE_ID);
		datum1.setSampleJson("{\"a\":{\"watt_hours\":0}, \"t\":[\"foo\"]}");
		dao.store(datum1);
		lastDatum = datum1;

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(datum1.getCreated().plusMinutes(20));
		datum2.setPosted(now);
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"a\":{\"watt_hours\":5}}");
		dao.store(datum2);

		GeneralNodeDatum datum3 = new GeneralNodeDatum();
		datum3.setCreated(datum2.getCreated().plusMinutes(20));
		datum3.setPosted(now);
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId(TEST_SOURCE_ID);
		datum3.setSampleJson("{\"a\":{\"watt_hours\":10}}");
		dao.store(datum3);

		Map<String, Object> propCounts = getAuditDatumHourlyPropCounts(lastDatum);
		assertThat("Audit counts", propCounts,
				allOf(hasEntry("datum_count", (Object) 3), hasEntry("prop_count", (Object) 4)));
	}

	@Test
	public void findFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(criteria, null, null,
				null);
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
			assertThat("Local date", d.getLocalDate(), notNullValue());
			assertThat("Local time", d.getLocalTime(), notNullValue());
			assertThat("Sample data", d.getSampleData(), notNullValue());
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
	public void findFilteredWithMaxNoCount() {
		storeNew();

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"i\":{\"watts\":123}}");
		dao.store(datum2);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(criteria, null, 0, 1);
		assertThat("Results", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Total result count disabled", results.getTotalResults(), nullValue());
		assertThat("Result ID", results.getResults().iterator().next().getId(),
				equalTo(lastDatum.getId()));

		results = dao.findFiltered(criteria, null, 1, 1);
		assertNotNull(results);
		assertThat("Results", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Total result count disabled", results.getTotalResults(), nullValue());
		assertThat("Result ID", results.getResults().iterator().next().getId(), equalTo(datum2.getId()));
	}

	@Test
	public void findFilteredWithMaxBeyondTotal() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(criteria, null, 1, 1000);
		assertThat("Results", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(1L));
	}

	@Test
	public void findFilteredWithMaxBeyondTotalNoCount() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(criteria, null, 1, 1000);
		assertThat("Results", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), nullValue());
	}

	@Test
	public void getAvailableSourcesForNode() {
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
	public void getAvailableSourcesForNodesSingleNode() {
		storeNew();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeId(lastDatum.getNodeId());
		Set<String> sources = dao.getAvailableSources(cmd);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(cmd);
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

		sources = dao.getAvailableSources(cmd);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));
		assertTrue("Source ID returned", sources.contains(d3.getSourceId()));
	}

	@Test
	public void findAvailableSourcesForNodesSingleNode() {
		storeNew();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeId(lastDatum.getNodeId());
		Set<NodeSourcePK> sources = dao.findAvailableSources(cmd);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.findAvailableSources(cmd);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned",
				sources.contains(new NodeSourcePK(lastDatum.getNodeId(), d2.getSourceId())));

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

		sources = dao.findAvailableSources(cmd);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned",
				sources.contains(new NodeSourcePK(lastDatum.getNodeId(), d2.getSourceId())));
		assertTrue("Source ID returned",
				sources.contains(new NodeSourcePK(lastDatum.getNodeId(), d3.getSourceId())));
	}

	@Test
	public void getAvailableSourcesForNodesMultipleNodes() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		setupTestNode(TEST_2ND_NODE);

		// add another source but days later, to verify date filters
		GeneralNodeDatum d3 = getTestInstance();
		d3.setNodeId(TEST_2ND_NODE);
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(10));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setNodeId(TEST_2ND_NODE);
		d4.setSourceId(TEST_2ND_SOURCE);
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		Set<String> sources = dao.getAvailableSources(cmd);
		assertThat("Source IDs", sources, contains(TEST_2ND_SOURCE, TEST_SOURCE_ID));

		// now with start and end dates
		cmd.setStartDate(lastDatum.getCreated());
		cmd.setEndDate(d2.getCreated());
		sources = dao.getAvailableSources(cmd);
		assertThat("Source IDs within", sources, contains(TEST_SOURCE_ID));

		// now with just start date
		cmd.setStartDate(d3.getCreated());
		cmd.setEndDate(null);
		sources = dao.getAvailableSources(cmd);
		assertThat("Source IDs since", sources, contains(TEST_2ND_SOURCE));

		// now with just end date
		cmd.setStartDate(null);
		cmd.setEndDate(d2.getCreated());
		sources = dao.getAvailableSources(cmd);
		assertThat("Source IDs before", sources, contains(TEST_SOURCE_ID));
	}

	@Test
	public void findAvailableSourcesForNodesMultipleNodes() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		setupTestNode(TEST_2ND_NODE);

		// add another source but days later, to verify date filters
		GeneralNodeDatum d3 = getTestInstance();
		d3.setNodeId(TEST_2ND_NODE);
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(10));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setNodeId(TEST_2ND_NODE);
		d4.setSourceId(TEST_2ND_SOURCE);
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		Set<NodeSourcePK> sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs", sources, contains(new NodeSourcePK(TEST_2ND_NODE, TEST_2ND_SOURCE),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));

		// now with start and end dates
		cmd.setStartDate(lastDatum.getCreated());
		cmd.setEndDate(d2.getCreated());
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs within", sources,
				contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));

		// now with just start date
		cmd.setStartDate(d3.getCreated());
		cmd.setEndDate(null);
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs since", sources,
				contains(new NodeSourcePK(TEST_2ND_NODE, TEST_2ND_SOURCE)));

		// now with just end date
		cmd.setStartDate(null);
		cmd.setEndDate(d2.getCreated());
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs before", sources,
				contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));
	}

	@Test
	public void findAvailableSourcesForNodesMultipleNodesDuplicateSourceIds() {
		storeNew();

		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		setupTestNode(TEST_2ND_NODE);

		// add another source but days later, to verify date filters
		GeneralNodeDatum d3 = getTestInstance();
		d3.setNodeId(TEST_2ND_NODE);
		d3.setSourceId(TEST_SOURCE_ID);
		d3.setCreated(d2.getCreated().plusDays(10));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setNodeId(TEST_2ND_NODE);
		d4.setSourceId(TEST_SOURCE_ID);
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		Set<NodeSourcePK> sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs", sources, contains(new NodeSourcePK(TEST_2ND_NODE, TEST_SOURCE_ID),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));

		// now with start and end dates
		cmd.setStartDate(lastDatum.getCreated());
		cmd.setEndDate(d2.getCreated());
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs within", sources,
				contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));

		// now with just start date
		cmd.setStartDate(d3.getCreated());
		cmd.setEndDate(null);
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs since", sources,
				contains(new NodeSourcePK(TEST_2ND_NODE, TEST_SOURCE_ID)));

		// now with just end date
		cmd.setStartDate(null);
		cmd.setEndDate(d2.getCreated());
		sources = dao.findAvailableSources(cmd);
		assertThat("Source IDs before", sources,
				contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));
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
		List<GeneralNodeDatum> datum = Arrays.asList(d2, lastDatum);

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(2), results.getTotalResults());
		Iterator<GeneralNodeDatum> datumIterator = datum.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			GeneralNodeDatum expected = datumIterator.next();
			assertEquals(expected.getId(), match.getId());
			assertThat("Local date", match.getLocalDate(), notNullValue());
			assertThat("Local time", match.getLocalTime(), notNullValue());
			assertThat("Sample data", match.getSampleData(), notNullValue());

			Assert.assertTrue("Match class", match instanceof GeneralNodeDatumMatch);
			GeneralNodeDatumMatch m = (GeneralNodeDatumMatch) match;
			assertEquals(expected.getSamples(), m.getSamples());
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
		assertThat("Local date", match.getLocalDate(), notNullValue());
		assertThat("Local time", match.getLocalTime(), notNullValue());
		assertThat("Sample data", match.getSampleData(), notNullValue());
		Assert.assertTrue("Match class", match instanceof GeneralNodeDatumMatch);
		GeneralNodeDatumMatch m = (GeneralNodeDatumMatch) match;
		assertEquals(lastDatum.getSamples(), m.getSamples());
	}

	@Test
	public void findMostRecentOneSourceHour() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Hour);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(filter, null,
				null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(1), results.getTotalResults());
		GeneralNodeDatumFilterMatch match = results.getResults().iterator().next();
		assertThat("Local date", match.getLocalDate(), notNullValue());
		assertThat("Local time", match.getLocalTime(), notNullValue());
		assertThat("Sample data", match.getSampleData(), notNullValue());

		GeneralNodeDatumPK expectedPK = new GeneralNodeDatumPK();
		expectedPK.setCreated(lastDatum.getCreated().hourOfDay().roundFloorCopy());
		expectedPK.setNodeId(lastDatum.getNodeId());
		expectedPK.setSourceId(lastDatum.getSourceId());
		assertEquals(expectedPK, match.getId());
	}

	@Test
	public void findMostRecentOneSourceDay() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Day);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(filter, null,
				null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(1), results.getTotalResults());
		GeneralNodeDatumFilterMatch match = results.getResults().iterator().next();
		assertThat("Local date", match.getLocalDate(), notNullValue());
		assertThat("Local time", match.getLocalTime(), notNullValue());
		assertThat("Sample data", match.getSampleData(), notNullValue());

		GeneralNodeDatumPK expectedPK = new GeneralNodeDatumPK();
		expectedPK.setCreated(lastDatum.getCreated().dayOfYear().roundFloorCopy());
		expectedPK.setNodeId(lastDatum.getNodeId());
		expectedPK.setSourceId(lastDatum.getSourceId());
		assertEquals(expectedPK, match.getId());
	}

	@Test
	public void findMostRecentOneSourceMonth() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		log.debug("Daily rows: {}", jdbcTemplate.queryForList("select * from solaragg.agg_datum_daily"));
		log.debug("Monthly rows: {}",
				jdbcTemplate.queryForList("select * from solaragg.agg_datum_monthly"));

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Month);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(filter, null,
				null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(1), results.getTotalResults());
		GeneralNodeDatumFilterMatch match = results.getResults().iterator().next();
		assertThat("Local date", match.getLocalDate(), notNullValue());
		assertThat("Local time", match.getLocalTime(), notNullValue());
		assertThat("Sample data", match.getSampleData(), notNullValue());

		GeneralNodeDatumPK expectedPK = new GeneralNodeDatumPK();
		expectedPK.setCreated(lastDatum.getCreated().monthOfYear().roundFloorCopy());
		expectedPK.setNodeId(lastDatum.getNodeId());
		expectedPK.setSourceId(lastDatum.getSourceId());
		assertEquals(expectedPK, match.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void findMostRecentFilteredWithAggregation() {
		storeNew();

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Month);
		dao.findFiltered(filter, null, null, null);
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
			assertThat("Local date", match.getLocalDate(), notNullValue());
			assertThat("Local time", match.getLocalTime(), notNullValue());
			assertThat("Sample data", match.getSampleData(), notNullValue());
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
			assertThat("Local date", match.getLocalDate(), notNullValue());
			assertThat("Local time", match.getLocalTime(), notNullValue());
			assertThat("Sample data", match.getSampleData(), notNullValue());
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
		List<GeneralNodeDatum> pks = Arrays.asList(d5, d3, d2);

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setMostRecent(true);
		FilterResults<GeneralNodeDatumFilterMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Long.valueOf(3), results.getTotalResults());
		Iterator<GeneralNodeDatum> datumIterator = pks.iterator();
		for ( GeneralNodeDatumFilterMatch match : results.getResults() ) {
			GeneralNodeDatum expected = datumIterator.next();
			assertEquals(expected.getId(), match.getId());
			assertThat("Local date", match.getLocalDate(), notNullValue());
			assertThat("Local time", match.getLocalTime(), notNullValue());

			Assert.assertTrue("Match class", match instanceof GeneralNodeDatumMatch);
			GeneralNodeDatumMatch m = (GeneralNodeDatumMatch) match;
			assertEquals(expected.getSamples(), m.getSamples());
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
	public void findFilteredAggregateHourlyCombined() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		final int count = 3;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 20));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.Hour);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		Map<String, ?> data = m.getSampleData();
		assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 20));
	}

	@Test
	public void findFilteredAggregateHourlyCombinedNodeOnly() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		final int count = 3;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 20));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.Hour);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo(TEST_2ND_SOURCE));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 10));

		m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 10));
	}

	@Test
	public void findFilteredAggregateHourlyCombinedSourceOnly() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		final int count = 3;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 20));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.Hour);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.getResults().iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(TEST_2ND_NODE));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 10));

		m = itr.next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 10));
	}

	@Test
	public void findFilteredAggregateDaily() {
		// populate 1 hour of data
		findFilteredAggregateHourly();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(lastDatum.getCreated().dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusDays(1));
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
	public void findFilteredAggregateDailyCombined() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2014, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final int count = 13;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 20));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusDays(1));
		criteria.setAggregate(Aggregation.Day);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 120));
	}

	@Test
	public void findFilteredAggregateMonthlyCombined() {
		setupTestNode(TEST_2ND_NODE);
		final DateTime startDate = new DateTime(2014, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final int count = 145;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 20));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 5) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setStartDate(startDate.dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusDays(1));
		criteria.setAggregate(Aggregation.Month);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		Map<String, ?> data = m.getSampleData();
		assertThat("Aggregate Wh", data, hasEntry("watt_hours", (Object) 1440));
	}

	@Test
	public void findFilteredAggregateRunningTotal() {
		// populate 1 hour of data
		findFilteredAggregateDaily();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);

		FilterResults<ReportingGeneralNodeDatumMatch> results;
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
	public void findFilteredAggregateRunningTotalTiered() {
		// this query only loads raw data for latest hour of query end date; the rest is loaded
		// from hourly/daily/monthly aggregate tables
		executeSqlScript(
				"/net/solarnetwork/central/datum/dao/mybatis/test/insert-running-total-data-01.sql",
				false);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(-100L);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);
		criteria.setEndDate(new DateTime(2017, 4, 4, 3, 3, DateTimeZone.UTC));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results", results, notNullValue());
		assertThat("Running total query results", results.getTotalResults(), equalTo(1L));
		assertThat("Running total returned query results", results.getReturnedResultCount(), equalTo(1));
		Map<String, ?> data = results.getResults().iterator().next().getSampleData();
		assertThat("Aggregate foo", data, hasEntry("foo", (Object) 947));
	}

	@Test
	public void findFilteredAggregateRunningTotalTieredMultipleNodes() {
		// this query only loads raw data for latest hour of query end date; the rest is loaded
		// from hourly/daily/monthly aggregate tables
		executeSqlScript(
				"/net/solarnetwork/central/datum/dao/mybatis/test/insert-running-total-data-01.sql",
				false);
		executeSqlScript(
				"/net/solarnetwork/central/datum/dao/mybatis/test/insert-running-total-data-02.sql",
				false);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeIds(new Long[] { -100L, -101L });
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);
		criteria.setEndDate(new DateTime(2017, 4, 4, 3, 3, DateTimeZone.UTC));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results", results, notNullValue());
		assertThat("Running total query results", results.getTotalResults(), equalTo(2L));
		assertThat("Running total returned query results", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		Map<String, ?> data;

		data = itr.next().getSampleData();
		assertThat("Aggregate foo node -101", data, hasEntry("foo", (Object) 9470));

		data = itr.next().getSampleData();
		assertThat("Aggregate foo node -100", data, hasEntry("foo", (Object) 947));
	}

	@Test(expected = IllegalArgumentException.class)
	public void findFilteredAggregateRunningTotalNoSourceId() {
		// populate 1 hour of data
		findFilteredAggregateDaily();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);

		dao.findAggregationFiltered(criteria, null, null, null);
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
			datum1.setSampleJson("{\"a\":{\"wattHours\":" + (i * 10) + "}}");
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
		assertEquals("Minute query results", 11L, (long) results.getTotalResults());
		assertEquals("Minute query results", 11, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			if ( i == 0 ) {
			} else {
				assertEquals("Wh for minute slot " + i, Integer.valueOf(10),
						match.getSampleData().get("wattHours"));
			}
			i++;
		}
		assertEquals("Processed result count", 11, i);
	}

	@Test
	public void findFilteredAggregateFiveMinuteNoEndDate() {
		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime();
		startDate = startDate.minusHours(1).withMinuteOfHour((startDate.getMinuteOfHour() / 5) * 5)
				.minuteOfDay().roundFloorCopy();
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"a\":{\"wattHours\":" + (i * 10) + "}}");
			dao.store(datum1);
			lastDatum = datum1;
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setAggregate(Aggregation.FiveMinute);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals("Minute query results", 11L, (long) results.getTotalResults());
		assertEquals("Minute query results", 11, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
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
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":120}}");
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
		assertEquals("Minute query results", 12L, (long) results.getTotalResults());
		assertEquals("Minute query results", 12, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
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
		assertEquals("Processed result count", 12, i);
	}

	@Test
	public void findFilteredAggregateFiveMinutePowerWithMinMax() {
		// populate 12 5 minute, segments
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":" + ((1 + i) * 10) + "}}");
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
		assertEquals("Minute query results", 4L, (long) results.getTotalResults());
		assertEquals("Minute query results", 4, (int) results.getReturnedResultCount());

		/*-
		 * 2014-02-02 01:00:00+13, 2014-02-02 01:00:00, test.source, {"i":{"watts":20,"watts_min":10,"watts_max":30}}
		 * 2014-02-02 01:15:00+13, 2014-02-02 01:15:00, test.source, {"i":{"watts":50,"watts_min":40,"watts_max":60}}
		 * 2014-02-02 01:30:00+13, 2014-02-02 01:30:00, test.source, {"i":{"watts":80,"watts_min":70,"watts_max":90}}
		 * 2014-02-02 01:45:00+13, 2014-02-02 01:45:00, test.source, {"i":{"watts":110,"watts_min":100,"watts_max":120}}
		 */
		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			int expectedMin = (10 + (i * 30));
			int expectedMax = expectedMin + 20;
			int expected = (expectedMin + 10);
			BigDecimal expectedWh = BigDecimal.valueOf(6.25 + (7.5 * i));
			if ( i == 3 ) {
				expectedWh = new BigDecimal("18.333");
			}
			assertEquals("W for minute slot " + i, Integer.valueOf(expected),
					match.getSampleData().get("watts"));
			assertEquals("Wmin for minute slot " + i, Integer.valueOf(expectedMin),
					match.getSampleData().get("watts_min"));
			assertEquals("Wmax for minute slot " + i, Integer.valueOf(expectedMax),
					match.getSampleData().get("watts_max"));

			assertEquals("Wh for minute slot " + i, expectedWh, match.getSampleData().get("wattHours"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateFiveMinutePowerTenMinuteIntervals() {
		// populate 6 10 minute, 120 W segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 6; i++ ) {
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 10));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":120}}");
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
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			if ( i == 5 ) {
				Assert.assertNull("Last Wh not known", match.getSampleData().get("wattHours"));
			} else {
				assertEquals("Wh for minute slot " + i, Integer.valueOf(20),
						match.getSampleData().get("wattHours"));
			}
			assertEquals("W for minute slot " + i, Integer.valueOf(120),
					match.getSampleData().get("watts"));
			i++;
		}
		assertEquals("Processed result count", 6, i);
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
			datum1.setSampleJson("{\"a\":{\"wattHours\":" + (i * 10) + "}}");
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
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
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
			GeneralNodeDatum datum1 = new GeneralNodeDatum();
			datum1.setCreated(startDate.plusMinutes(i * 5));
			datum1.setNodeId(TEST_NODE_ID);
			datum1.setSourceId(TEST_SOURCE_ID);
			datum1.setSampleJson("{\"i\":{\"watts\":120}}");
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
		assertEquals("Minute query results", 6L, (long) results.getTotalResults());
		assertEquals("Minute query results", 6, (int) results.getReturnedResultCount());

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertEquals("Wh for minute slot", Integer.valueOf(i < 5 ? 20 : 10),
					match.getSampleData().get("wattHours"));
			assertEquals("W for minute slot " + i, Integer.valueOf(120),
					match.getSampleData().get("watts"));
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
			assertEquals("Wh for minute slot", Integer.valueOf(i < 3 ? 30 : 20),
					match.getSampleData().get("watt_hours"));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateFifteenMinuteCombined() {
		setupTestNode(TEST_2ND_NODE);

		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int i = 0; i < 12; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(startDate.plusMinutes(i * 5));
			d.setNodeId(TEST_NODE_ID);
			d.setSourceId(TEST_SOURCE_ID);
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 10) + "}}");
			dao.store(d);
			d.setNodeId(TEST_2ND_NODE);
			d.setSourceId(TEST_2ND_SOURCE);
			dao.store(d);
		}

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(1));
		criteria.setAggregate(Aggregation.FifteenMinute);
		criteria.setWithoutTotalResultsCount(true);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(4));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch m : results ) {
			DateTime slotDate = startDate.plusMinutes(i * 15);
			assertThat("Result date is grouped", m.getId().getCreated().isEqual(slotDate),
					equalTo(true));
			assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
			assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
			assertThat("Aggregate Wh for minute slot " + i + "(" + slotDate + ")", m.getSampleData(),
					hasEntry("watt_hours", (Object) Integer.valueOf(i < 3 ? 30 : 20)));
			i++;
		}
	}

	@Test
	public void findFilteredAggregateNone() {
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
		criteria.setAggregate(Aggregation.None);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();

		assertThat("Match 1", itr.next().getId(), equalTo(datum1.getId()));
		assertThat("Match 2", itr.next().getId(), equalTo(datum2.getId()));
		assertThat("No more matches", itr.hasNext(), equalTo(false));
	}

	@Test
	public void findAuditCountNoData() {
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(6));

		long count = dao.getAuditPropertyCountTotal(criteria);
		assertEquals(0, count);
	}

	@Test
	public void findAuditCountNodeIdMultiSourceIds() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-01.sql",
				false);

		DateTime startDate = new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(2)); // exclusive

		long count = dao.getAuditPropertyCountTotal(criteria);
		assertEquals("Two hours across both sources", 12, count);
	}

	@Test
	public void findAuditDatumQueryCountNodeIdMultiSourceIds() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-02.sql",
				false);

		DateTime startDate = new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setDataPath("DatumQuery");
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(2)); // exclusive

		long count = dao.getAuditPropertyCountTotal(criteria);
		assertEquals("Two hours across both sources", 52, count);
	}

	@Test
	public void getAuditIntervalNoDatum() {
		ReadableInterval result = dao.getAuditInterval(TEST_NODE_ID, null);
		Assert.assertNull(result);
	}

	@Test
	public void getAuditIntervalForNode() {
		final DateTime expectedStartDate = new DateTime(2017, 1, 1, 12, 0, 0, DateTimeZone.UTC);
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-01.sql",
				false);
		ReadableInterval result = dao.getAuditInterval(TEST_NODE_ID, null);
		assertNotNull(result);
		assertEquals(expectedStartDate.getMillis(), result.getStart().getMillis());
		assertEquals(expectedStartDate.plusHours(2).getMillis(), result.getEnd().getMillis());
	}

	@Test
	public void findAuditDatumStoredCountNoData() {
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setDataPath("DatumStored");
		long count = dao.getAuditCountTotal(criteria);
		assertEquals("Total datum count", 0, count);
	}

	@Test
	public void findAuditDatumStoredCountNodeAndSourceAllTime() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-03.sql",
				false);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setDataPath("DatumStored");
		long count = dao.getAuditCountTotal(criteria);
		assertEquals("Total datum count", 740, count);
	}

	@Test
	public void findAuditDatumStoredCountNodeAndSourceTimeRange() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-03.sql",
				false);
		final DateTime expectedStartDate = new DateTime(2017, 1, 1, 0, 0, 0, DateTimeZone.UTC);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setDataPath("DatumStored");
		criteria.setStartDate(expectedStartDate);
		criteria.setEndDate(expectedStartDate.dayOfMonth().addToCopy(1));
		long count = dao.getAuditCountTotal(criteria);
		assertEquals("Total datum count", 394, count);
	}

	@Test
	public void findAuditDatumStoredCountNodeAndSourcesAllTime() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-03.sql",
				false);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		criteria.setDataPath("DatumStored");
		long count = dao.getAuditCountTotal(criteria);
		assertEquals("No data", 1448, count);
	}

	@Test
	public void findAuditDatumStoredCountAllNodeAllSourceAllTime() {
		executeSqlScript("/net/solarnetwork/central/datum/dao/mybatis/test/insert-audit-data-03.sql",
				false);
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setDataPath("DatumStored");
		long count = dao.getAuditCountTotal(criteria);
		assertEquals("No data", 2124, count);
	}

	private void insertAuditDatumHourlyRow(long ts, Long nodeId, String sourceId, Integer rawCount,
			Integer propInCount, Integer datumOutCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_hourly (ts_start,node_id,source_id,datum_count,prop_count,datum_q_count) VALUES (?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, propInCount, datumOutCount);
	}

	private void insertAuditDatumDailyRow(long ts, Long nodeId, String sourceId, Integer rawCount,
			Integer hourCount, Boolean dailyPresent, Integer propInCount, Integer datumOutCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_daily (ts_start,node_id,source_id,datum_count,datum_hourly_count,datum_daily_pres,prop_count,datum_q_count) VALUES (?,?,?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, hourCount, dailyPresent, propInCount,
				datumOutCount);
	}

	private void insertAuditDatumMonthlyRow(long ts, Long nodeId, String sourceId, Integer rawCount,
			Integer hourCount, Integer dailyCount, Boolean monthPresent, Integer propInCount,
			Integer datumOutCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_monthly (ts_start,node_id,source_id,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres,prop_count,datum_q_count) VALUES (?,?,?,?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, hourCount, dailyCount, monthPresent,
				propInCount, datumOutCount);
	}

	private void assertAuditDatumRecordCounts(String desc, AuditDatumRecordCounts row, DateTime ts,
			Long nodeId, String sourceId, Long rawCount, Long hourCount, Integer dayCount,
			Integer monthCount, Long propInCount, Long datumOutCount) {
		assertThat(desc + " not null", row, notNullValue());
		if ( ts != null ) {
			assertThat(desc + " created", row.getCreated().withZone(DateTimeZone.forID(TEST_TZ)),
					equalTo(ts));
		} else {
			assertThat(desc + " created", row.getCreated(), nullValue());
		}
		assertThat(desc + " node ID", row.getNodeId(), equalTo(nodeId));
		assertThat(desc + " source ID", row.getSourceId(), equalTo(sourceId));
		assertThat(desc + " datum count", row.getDatumCount(), equalTo(rawCount));
		assertThat(desc + " datum hourly count", row.getDatumHourlyCount(), equalTo(hourCount));
		assertThat(desc + " datum daily count", row.getDatumDailyCount(), equalTo(dayCount));
		assertThat(desc + " datum monthly count", row.getDatumMonthlyCount(), equalTo(monthCount));
		assertThat(desc + " prop posted count", row.getDatumPropertyPostedCount(), equalTo(propInCount));
		assertThat(desc + " datum query count", row.getDatumQueryCount(), equalTo(datumOutCount));
	}

	private void setupTestAuditDatumRecords(DateTime start, Long nodeId, String sourceId, int count,
			int hourStep, List<DateTime> hours, List<DateTime> days, List<DateTime> months) {
		for ( int i = 0; i < count; i++ ) {
			DateTime h = start.plusHours(i * hourStep);
			insertAuditDatumHourlyRow(h.getMillis(), nodeId, sourceId, 60, 100, 5);
			hours.add(h);

			DateTime d = h.dayOfMonth().roundFloorCopy();
			if ( days.isEmpty() || !days.get(days.size() - 1).isEqual(d) ) {
				insertAuditDatumDailyRow(d.getMillis(), nodeId, sourceId, 100, 24, true, 1000, 10);
				days.add(d);
			}

			DateTime m = h.monthOfYear().roundFloorCopy();
			if ( months.isEmpty() || !months.get(months.size() - 1).isEqual(m) ) {
				insertAuditDatumMonthlyRow(m.getMillis(), nodeId, sourceId, 3000, 720, 30, true, 30000,
						300);
				months.add(m);
			}
		}
	}

	@Test
	public void findAuditDatumRecordCountsHourAggregationNoRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setAggregate(Aggregation.Hour);
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Hour rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			assertAuditDatumRecordCounts("Hour " + i, row, hours.get(i), TEST_NODE_ID, TEST_SOURCE_ID,
					60L, null, null, null, 100L, 5L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsHourAggregationTimeAndNodeRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);
		// add another source
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8,
				(int) TimeUnit.DAYS.toHours(7), new ArrayList<DateTime>(), new ArrayList<DateTime>(),
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setAggregate(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time, DatumRollupType.Node });
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Rolled up hour rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(4));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			// audit counts doubled from rollup
			assertAuditDatumRecordCounts("Hour " + i, row, hours.get(i), TEST_NODE_ID, null, 120L, null,
					null, null, 200L, 10L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsHourAggregationTimeRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);
		// add another source for same node
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8,
				(int) TimeUnit.DAYS.toHours(7), new ArrayList<DateTime>(), new ArrayList<DateTime>(),
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), new ArrayList<DateTime>(), new ArrayList<DateTime>(),
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setAggregate(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time });
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Rolled up hour rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(4));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			// audit counts tripled from rollup
			assertAuditDatumRecordCounts("Hour " + i, row, hours.get(i), null, null, 180L, null, null,
					null, 300L, 15L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsHourAggregationAllRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);
		// add another source for same node
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8,
				(int) TimeUnit.DAYS.toHours(7), new ArrayList<DateTime>(), new ArrayList<DateTime>(),
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), new ArrayList<DateTime>(), new ArrayList<DateTime>(),
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setAggregate(Aggregation.Hour);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.All });
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Rolled up hour rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(1));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			// audit counts tripled * 4 from rollup
			assertAuditDatumRecordCounts("Hour " + i, row, null, null, null, 720L, null, null, null,
					1200L, 60L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsDefaultAggregationNoRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			assertAuditDatumRecordCounts("Daily " + i, row, days.get(i), TEST_NODE_ID, TEST_SOURCE_ID,
					100L, 24L, 1, null, 1000L, 10L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsDayAggregationNoRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			assertAuditDatumRecordCounts("Daily " + i, row, days.get(i), TEST_NODE_ID, TEST_SOURCE_ID,
					100L, 24L, 1, null, 1000L, 10L);
			i++;
		}
	}

	@Test
	public void findAuditDatumRecordCountsMonthAggregationNoRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> hours = new ArrayList<DateTime>();
		List<DateTime> days = new ArrayList<DateTime>();
		List<DateTime> months = new ArrayList<DateTime>();
		setupTestAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8,
				(int) TimeUnit.DAYS.toHours(7), hours, days, months);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setAggregate(Aggregation.Month);
		FilterResults<AuditDatumRecordCounts> results = dao.findAuditRecordCountsFiltered(filter, null,
				null, null);

		// then
		assertThat("Month rows for first 4 weeks returned", results.getReturnedResultCount(),
				equalTo(1));
		int i = 0;
		for ( AuditDatumRecordCounts row : results ) {
			assertAuditDatumRecordCounts("Month " + i, row, months.get(i), TEST_NODE_ID, TEST_SOURCE_ID,
					3000L, 720L, 30, 1, 30000L, 300L);
			i++;
		}
	}

	private void insertAccumulativeAuditDatumDailyRow(long ts, Long nodeId, String sourceId,
			Integer rawCount, Integer hourCount, Integer dayCount, Integer monthCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_acc_datum_daily (ts_start,node_id,source_id,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_count) VALUES (?,?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, hourCount, dayCount, monthCount);
	}

	private void setupTestAccumulativeAuditDatumRecords(DateTime start, Long nodeId, String sourceId,
			int count, int dayStep, List<DateTime> days) {
		DateTime currMonth = null;
		int iMonth = 1;
		for ( int i = 1; i <= count; i++ ) {
			DateTime d = start.plusDays((i - 1) * dayStep);
			DateTime m = d.monthOfYear().roundFloorCopy();
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			insertAccumulativeAuditDatumDailyRow(d.getMillis(), nodeId, sourceId, 100 * i, 24 * i, i,
					iMonth);
			days.add(d);
		}
	}

	private void assertAuditDatumRecordCounts(String desc, AuditDatumRecordCounts row, DateTime ts,
			Long nodeId, String sourceId, Long rawCount, Long hourCount, Integer dayCount,
			Integer monthCount) {
		assertThat(desc + " not null", row, notNullValue());
		if ( ts != null ) {
			assertThat(desc + " created", row.getCreated().withZone(DateTimeZone.forID(TEST_TZ)),
					equalTo(ts));
		} else {
			assertThat(desc + " created", row.getCreated(), nullValue());
		}
		assertThat(desc + " node ID", row.getNodeId(), equalTo(nodeId));
		assertThat(desc + " source ID", row.getSourceId(), equalTo(sourceId));
		assertThat(desc + " datum count", row.getDatumCount(), equalTo(rawCount));
		assertThat(desc + " datum hourly count", row.getDatumHourlyCount(), equalTo(hourCount));
		assertThat(desc + " datum daily count", row.getDatumDailyCount(), equalTo(dayCount));
		assertThat(desc + " datum monthly count", row.getDatumMonthlyCount(), equalTo(monthCount));
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationNoRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		DateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRecordCounts row : results ) {
			DateTime d = days.get(i - 1);
			DateTime m = d.monthOfYear().roundFloorCopy();
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			assertAuditDatumRecordCounts("Daily acc " + i, row, d, TEST_NODE_ID, TEST_SOURCE_ID,
					100L * i, 24L * i, i, iMonth);
			i++;
		}
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationTimeAndNodeRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);
		// add another source
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8, 7,
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time, DatumRollupType.Node });
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		DateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRecordCounts row : results ) {
			DateTime d = days.get(i - 1);
			DateTime m = d.monthOfYear().roundFloorCopy();
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			// results doubled from rollup; no source ID
			assertAuditDatumRecordCounts("Daily acc " + i, row, d, TEST_NODE_ID, null, 2 * 100L * i,
					2 * 24L * i, 2 * i, 2 * iMonth);
			i++;
		}
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationTimeRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);
		// add another source
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8, 7,
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAccumulativeAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8, 7,
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time });
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(4));
		DateTime currMonth = null;
		int i = 1, iMonth = 1;
		for ( AuditDatumRecordCounts row : results ) {
			DateTime d = days.get(i - 1);
			DateTime m = d.monthOfYear().roundFloorCopy();
			if ( currMonth == null ) {
				currMonth = m;
			} else if ( !m.isEqual(currMonth) ) {
				iMonth++;
				currMonth = m;
			}
			// results tripled from rollup; no node ID; no source ID
			assertAuditDatumRecordCounts("Daily acc " + i, row, d, null, null, 3 * 100L * i, 3 * 24L * i,
					3 * i, 3 * iMonth);
			i++;
		}
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationAllRollup() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);
		// add another source
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8, 7,
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAccumulativeAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8, 7,
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setStartDate(start);
		filter.setEndDate(start.plusWeeks(4));
		filter.setUserId(TEST_USER_ID);
		filter.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.All });
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Day rows for first 4 weeks returned", results.getReturnedResultCount(), equalTo(1));
		// results combined from rollup; no ts; no node ID; no source ID
		// e.g. raw = (3 * 100) + (3 * 100 * 2) + (3 * 100 * 3) + (3 * 100 * 4) = 3000
		assertAuditDatumRecordCounts("Daily acc 1", results.iterator().next(), null, null, null, 3000L,
				720L, 30, 12);
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationMostRecentOneNodeOneSource() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);
		// add another source
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8, 7,
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAccumulativeAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8, 7,
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setMostRecent(true);
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Most recent row returned", results.getReturnedResultCount(), equalTo(1));
		// results are accumulation of 8 days over 2 months
		// e.g. raw = (3 * 100) + (3 * 100 * 2) + (3 * 100 * 3) + (3 * 100 * 4) = 3000
		assertAuditDatumRecordCounts("Daily acc most recent", results.iterator().next(),
				days.get(days.size() - 1), TEST_NODE_ID, TEST_SOURCE_ID, 800L, 192L, 8, 2);
	}

	@Test
	public void findAccumulativeAuditDatumRecordCountsDayAggregationMostRecent() {
		// given
		DateTime start = new DateTime(DateTimeZone.forID(TEST_TZ)).monthOfYear().roundFloorCopy()
				.minusMonths(2);
		List<DateTime> days = new ArrayList<DateTime>();
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_SOURCE_ID, 8, 7, days);
		// add another source
		setupTestAccumulativeAuditDatumRecords(start, TEST_NODE_ID, TEST_2ND_SOURCE, 8, 7,
				new ArrayList<DateTime>());

		// add another node
		setupTestNode(TEST_2ND_NODE);
		setupTestAccumulativeAuditDatumRecords(start, TEST_2ND_NODE, TEST_SOURCE_ID, 8, 7,
				new ArrayList<DateTime>());

		setupUserNodeEntity(TEST_NODE_ID, TEST_USER_ID);
		setupUserNodeEntity(TEST_2ND_NODE, TEST_USER_ID);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setMostRecent(true);
		FilterResults<AuditDatumRecordCounts> results = dao
				.findAccumulativeAuditRecordCountsFiltered(filter, null, null, null);

		// then
		assertThat("Most recent rows returned", results.getReturnedResultCount(), equalTo(3));
		// results are accumulation of 8 days over 2 months for each node+source combo
		// and order is node,source
		Iterator<AuditDatumRecordCounts> itr = results.iterator();
		assertAuditDatumRecordCounts("Daily acc most recent 1", itr.next(), days.get(days.size() - 1),
				TEST_2ND_NODE, TEST_SOURCE_ID, 800L, 192L, 8, 2);
		assertAuditDatumRecordCounts("Daily acc most recent 2", itr.next(), days.get(days.size() - 1),
				TEST_NODE_ID, TEST_2ND_SOURCE, 800L, 192L, 8, 2);
		assertAuditDatumRecordCounts("Daily acc most recent 3", itr.next(), days.get(days.size() - 1),
				TEST_NODE_ID, TEST_SOURCE_ID, 800L, 192L, 8, 2);
	}

	@Test
	public void findDatumAtLocalNoDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 231));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4123));
	}

	@Test
	public void findDatumAtLocalExactDateStart() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 231));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4123));
	}

	@Test
	public void findDatumAtLocalExactDateEnd() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts", m.getSampleData().get("watts"), equalTo((Object) 345));
		assertThat("Watt hours", m.getSampleData().get("watt_hours"), equalTo((Object) 4445));
	}

	@Test
	public void findDatumAtLocalNoDataInRange() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusDays(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusDays(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalOnlyDataBefore() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(90), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.minusSeconds(60), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalOnlyDataAfter() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.plusSeconds(60), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(90), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAtLocalEvenDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 4284));
	}

	@Test
	public void findDatumAtLocalSixtyFortyDifference() {
		// given
		DateTime ts = new DateTime(DateTimeZone.forID(TEST_TZ)).hourOfDay().roundFloorCopy();
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(40), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(20), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				ts.toLocalDateTime(), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", (BigDecimal) m.getSampleData().get("watt_hours"),
				closeTo(new BigDecimal("4337.666"), new BigDecimal("0.001")));
	}

	@Test
	public void findDatumAtLocalMultipleNodesDifferentTimeZones() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusSeconds(40), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusSeconds(20), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");
		final Long nodeId2 = -19889L;
		setupTestLocation(-9889L, tz2.getID());
		setupTestNode(nodeId2, -9889L);

		DateTime ts2 = new DateTime(2018, 8, 1, 0, 0, 0, tz2);
		GeneralNodeDatum d3 = getTestInstance(ts2.minusSeconds(20), nodeId2, TEST_SOURCE_ID);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusSeconds(40), nodeId2, TEST_SOURCE_ID);
		d4.getSamples().putInstantaneousSampleValue("watts", 482);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, nodeId2 });
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts2.getZone()), equalTo(ts2));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(nodeId2));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"),
				equalTo((Object) new BigDecimal("356.5")));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 5530));

		m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", (BigDecimal) m.getSampleData().get("watt_hours"),
				closeTo(new BigDecimal("4337.666"), new BigDecimal("0.001")));
	}

	@Test
	public void findDatumBetweenLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d3.getSamples().putInstantaneousSampleValue("watts", 462);
		d3.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d4.getSamples().putInstantaneousSampleValue("watts", 482);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 380));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 3910));
	}

	@Test
	public void findDatumBetweenLocalNoData() {
		// given

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumBetweenOnlyStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 0));
	}

	@Test
	public void findDatumBetweenOnlyEnd() {
		// given
		DateTime ts = new DateTime(2018, 9, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putInstantaneousSampleValue("watts", 345);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.hours(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date", m.getId().getCreated().withZone(ts.getZone()), equalTo(ts));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watts avg", m.getSampleData().get("watts"), equalTo((Object) 288));
		assertThat("Watt hours projection", m.getSampleData().get("watt_hours"), equalTo((Object) 0));
	}

	@Test
	public void findDatumAccumulationNoData() {
		// given

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void findDatumAccumulation() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 4445);
		dao.store(d1);
		dao.store(d2);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d3.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d4.getSamples().putAccumulatingSampleValue("watt_hours", 8344);
		dao.store(d3);
		dao.store(d4);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date d1", m.getId().getCreated().withZone(ts.getZone()), equalTo(d1.getCreated()));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation between d1 - d3", m.getSampleData().get("watt_hours"),
				equalTo((Object) 4042));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 4002));
		assertThat("Watt hours end d3", m.getSampleData().get("watt_hours_end"), equalTo((Object) 8044));
		assertThat("End date", m.getSampleData().get("endDate"), equalTo((Object) ISODateTimeFormat
				.dateTime().print(d3.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	@Test
	public void findDatumAccumulationOnlyStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		dao.store(d1);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date d1", m.getId().getCreated().withZone(ts.getZone()), equalTo(d1.getCreated()));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation zero", m.getSampleData().get("watt_hours"),
				equalTo((Object) 0));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 4002));
		assertThat("Watt hours end d1", m.getSampleData().get("watt_hours_end"), equalTo((Object) 4002));
		assertThat("End date", m.getSampleData().get("endDate"), equalTo((Object) ISODateTimeFormat
				.dateTime().print(d1.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	@Test
	public void findDatumAccumulationOnlyEnd() {
		// given
		DateTime ts = new DateTime(2018, 9, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		dao.store(d1);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0),
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("Date d1", m.getId().getCreated().withZone(ts.getZone()), equalTo(d1.getCreated()));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation zero", m.getSampleData().get("watt_hours"),
				equalTo((Object) 0));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 4002));
		assertThat("Watt hours end d1", m.getSampleData().get("watt_hours_end"), equalTo((Object) 4002));
		assertThat("End date", m.getSampleData().get("endDate"), equalTo((Object) ISODateTimeFormat
				.dateTime().print(d1.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}
}
