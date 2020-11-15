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

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInterval;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.context.transaction.TestTransaction;
import net.solarnetwork.central.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.central.dao.BulkExportingDao.ExportCallbackAction;
import net.solarnetwork.central.dao.BulkExportingDao.ExportResult;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.support.FilterableBulkExportOptions;
import net.solarnetwork.central.support.SimpleBulkLoadingOptions;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisGeneralNodeDatumDaoTests extends MyBatisGeneralNodeDatumDaoTestSupport {

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

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(lastDatum.getNodeId());

		Set<String> sources = dao.getAvailableSources(filter);
		assertThat("Source returned", sources, contains(lastDatum.getSourceId()));

		// add a 2nd source
		GeneralNodeDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(lastDatum.getCreated().plusDays(1));
		dao.store(d3);

		sources = dao.getAvailableSources(filter);
		assertThat("Sources set", sources,
				Matchers.containsInAnyOrder(lastDatum.getSourceId(), d3.getSourceId()));
	}

	@Test
	public void getAvailableSourcesForNodesSingleNode() {
		storeNew();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeId(lastDatum.getNodeId());
		Set<String> sources = dao.getAvailableSources(cmd);
		assertThat("Sources", sources, contains(lastDatum.getSourceId()));

		// add a 2nd source (two more datum to get into reporting table).
		GeneralNodeDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(lastDatum.getCreated().plusDays(1));
		dao.store(d3);

		sources = dao.getAvailableSources(cmd);
		assertThat("Sources set", sources,
				Matchers.containsInAnyOrder(lastDatum.getSourceId(), d3.getSourceId()));
	}

	@Test
	public void findAvailableSourcesForNodesSingleNode() {
		storeNew();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeId(lastDatum.getNodeId());

		// we are querying da_datum_range table, so results available without 2nd row
		Set<NodeSourcePK> sources = dao.findAvailableSources(cmd);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned",
				sources.contains(new NodeSourcePK(lastDatum.getNodeId(), lastDatum.getSourceId())));

		// add a 2nd source (two more datum to get into reporting table).
		// we also make this on another day, to support getAllAvailableSourcesForNodeAndDateRange() test
		GeneralNodeDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(lastDatum.getCreated().plusDays(1));
		dao.store(d3);

		sources = dao.findAvailableSources(cmd);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned",
				sources.contains(new NodeSourcePK(lastDatum.getNodeId(), lastDatum.getSourceId())));
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

	private List<Map<String, Object>> selectAllDatumMostRecent() {
		return jdbcTemplate
				.queryForList("select * from solardatum.da_datum_range ORDER BY node_id, source_id");
	}

	@Test
	public void mostRecentTablePopulatedOnInsert() {
		List<Map<String, Object>> mrRows = selectAllDatumMostRecent();
		assertThat("No most recent rows at start", mrRows, hasSize(0));

		storeNew();
		mrRows = selectAllDatumMostRecent();
		assertThat("Most recent row inserted", mrRows, hasSize(1));
		Map<String, Object> mrRow = mrRows.get(0);
		assertThat(mrRow, hasEntry("node_id", lastDatum.getNodeId()));
		assertThat(mrRow, hasEntry("source_id", lastDatum.getSourceId()));
		assertThat(mrRow, hasEntry("ts_min", new Timestamp(lastDatum.getCreated().getMillis())));
		assertThat(mrRow, hasEntry("ts_max", new Timestamp(lastDatum.getCreated().getMillis())));
	}

	@Test
	public void mostRecentTablePopulatedOnUpdateNewer() {
		List<Map<String, Object>> mrRows = selectAllDatumMostRecent();
		assertThat("No most recent rows at start", mrRows, hasSize(0));

		storeNew();
		mrRows = selectAllDatumMostRecent();
		assertThat("Most recent row inserted", mrRows, hasSize(1));
		Map<String, Object> mrRow = mrRows.get(0);
		assertThat(mrRow, hasEntry("node_id", lastDatum.getNodeId()));
		assertThat(mrRow, hasEntry("source_id", lastDatum.getSourceId()));
		assertThat(mrRow, hasEntry("ts_min", new Timestamp(lastDatum.getCreated().getMillis())));
		assertThat(mrRow, hasEntry("ts_max", new Timestamp(lastDatum.getCreated().getMillis())));

		GeneralNodeDatum datum = getTestInstance();
		datum.setCreated(lastDatum.getCreated().plusMinutes(1));
		GeneralNodeDatumPK id = dao.store(datum);

		mrRows = selectAllDatumMostRecent();
		assertThat("Most recent row updated", mrRows, hasSize(1));
		mrRow = mrRows.get(0);
		assertThat(mrRow, hasEntry("node_id", lastDatum.getNodeId()));
		assertThat(mrRow, hasEntry("source_id", lastDatum.getSourceId()));
		assertThat(mrRow, hasEntry("ts_min", new Timestamp(lastDatum.getCreated().getMillis())));
		assertThat(mrRow, hasEntry("ts_max", new Timestamp(id.getCreated().getMillis())));
	}

	@Test
	public void mostRecentTablePopulatedOnUpdateOlder() {
		List<Map<String, Object>> mrRows = selectAllDatumMostRecent();
		assertThat("No most recent rows at start", mrRows, hasSize(0));

		storeNew();
		mrRows = selectAllDatumMostRecent();
		assertThat("Most recent row inserted", mrRows, hasSize(1));
		Map<String, Object> mrRow = mrRows.get(0);
		assertThat(mrRow, hasEntry("node_id", lastDatum.getNodeId()));
		assertThat(mrRow, hasEntry("source_id", lastDatum.getSourceId()));
		assertThat(mrRow, hasEntry("ts_min", new Timestamp(lastDatum.getCreated().getMillis())));
		assertThat(mrRow, hasEntry("ts_max", new Timestamp(lastDatum.getCreated().getMillis())));

		GeneralNodeDatum datum = getTestInstance();
		datum.setCreated(lastDatum.getCreated().minusMinutes(1));
		GeneralNodeDatumPK id = dao.store(datum);

		mrRows = selectAllDatumMostRecent();
		assertThat("Most recent row updated", mrRows, hasSize(1));
		mrRow = mrRows.get(0);
		assertThat(mrRow, hasEntry("node_id", lastDatum.getNodeId()));
		assertThat(mrRow, hasEntry("source_id", lastDatum.getSourceId()));
		assertThat(mrRow, hasEntry("ts_min", new Timestamp(id.getCreated().getMillis())));
		assertThat(mrRow, hasEntry("ts_max", new Timestamp(lastDatum.getCreated().getMillis())));
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
	public void findFilteredAggregateDailyCombinedAverage() {
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
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "}}");
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
		criteria.setCombiningType(CombiningType.Average);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		List<Map<String, Object>> rows = getDatumAggregateDaily();
		for ( Map<String, Object> row : rows ) {
			log.debug("Day row: {}", row);
		}

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", (Object) 330));
	}

	@Test
	public void findFilteredAggregateDailyCombinedDifference() {
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
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "}}");
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
		criteria.setCombiningType(CombiningType.Difference);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_2ND_NODE))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		List<Map<String, Object>> rows = getDatumAggregateDaily();
		for ( Map<String, Object> row : rows ) {
			log.debug("Day row: {}", row);
		}

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", -540));
	}

	@Test
	public void findFilteredAggregateDailyCombinedDifferenceReverse() {
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
			d.setSampleJson("{\"a\":{\"watt_hours\":" + (i * 50) + "}}");
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
		criteria.setCombiningType(CombiningType.Difference);
		criteria.setNodeIdMappings(Collections.singletonMap(-5000L,
				(Set<Long>) new LinkedHashSet<Long>(Arrays.asList(TEST_2ND_NODE, TEST_NODE_ID))));
		criteria.setSourceIdMappings(
				Collections.singletonMap("Foobar", (Set<String>) new LinkedHashSet<String>(
						Arrays.asList(TEST_SOURCE_ID, TEST_2ND_SOURCE))));

		List<Map<String, Object>> rows = getDatumAggregateDaily();
		for ( Map<String, Object> row : rows ) {
			log.debug("Day row: {}", row);
		}

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, null, null);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReportingGeneralNodeDatumMatch m = results.getResults().iterator().next();
		assertThat("Result date is grouped", m.getId().getCreated().isEqual(startDate), equalTo(true));
		assertThat("Result node ID is virutal", m.getId().getNodeId(), equalTo(-5000L));
		assertThat("Result source ID is virutal", m.getId().getSourceId(), equalTo("Foobar"));
		assertThat("Aggregate Wh", m.getSampleData(), hasEntry("watt_hours", 540));
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
	public void findFilteredAggregateRunningTotalIgnoreStartDateParameter() {
		// populate 1 hour of data
		findFilteredAggregateDaily();

		// first, verify that the the day is also at 10 Wh
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setAggregate(Aggregation.RunningTotal);
		criteria.setStartDate(lastDatum.getCreated().dayOfMonth().roundFloorCopy());
		criteria.setEndDate(criteria.getStartDate().plusDays(1));

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
	public void findFilteredAggregateFiveMinute_withTotalResultsCount() {
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
				null, 0, 3);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(11L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(3));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertThat("Agg date", match.getId().getCreated(),
					equalTo(startDate.plusMinutes(5 * i).withZone(DateTimeZone.forID(TEST_TZ))));
			if ( i > 0 ) {
				assertThat("Wh for minute slot " + i, match.getSampleData().get("wattHours"),
						equalTo(10));
			}
			i++;
		}
		assertThat("Processed result count", i, equalTo(3));
	}

	public void findFilteredAggregateFiveMinute_page2() {
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
				null, 3, 3);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(11L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(3));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertThat("Agg date", match.getId().getCreated(),
					equalTo(startDate.plusMinutes(5 * (i + 3)).withZone(DateTimeZone.forID(TEST_TZ))));
			if ( i > 0 ) {
				assertThat("Wh for minute slot " + i, match.getSampleData().get("wattHours"),
						equalTo(10));
			}
			i++;
		}
		assertThat("Processed result count", i, equalTo(3));
	}

	@Test
	public void findFilteredAggregateFiveMinute_withoutTotalResultsCount() {
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
		criteria.setWithoutTotalResultsCount(true);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFiltered(criteria,
				null, 0, 3);

		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), nullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(3));

		int i = 0;
		for ( ReportingGeneralNodeDatumMatch match : results ) {
			assertThat("Agg date", match.getId().getCreated(),
					equalTo(startDate.plusMinutes(5 * i).withZone(DateTimeZone.forID(TEST_TZ))));
			if ( i > 0 ) {
				assertThat("Wh for minute slot " + i, match.getSampleData().get("wattHours"),
						equalTo(10));
			}
			i++;
		}
		assertThat("Processed result count", i, equalTo(3));
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

	@Test(expected = IllegalArgumentException.class)
	public void findFilteredAggregateFifteenMinute_truncatedTimeRange() {
		dao.setMaxMinuteAggregationHours(1);

		// populate 24 5 minute, 10 Wh segments, for a total of 120 Wh in 60 minutes
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);
		for ( int i = 0; i < 24; i++ ) {
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
		criteria.setEndDate(startDate.plusHours(2));
		criteria.setAggregate(Aggregation.FifteenMinute);

		dao.findAggregationFiltered(criteria, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findFilteredAggregateMinute() {
		DateTime startDate = new DateTime(2014, 2, 1, 12, 0, 0, DateTimeZone.UTC);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(startDate);
		criteria.setEndDate(startDate.plusHours(2));
		criteria.setAggregate(Aggregation.Minute);

		dao.findAggregationFiltered(criteria, null, null, null);
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
		criteria.setDataPath("Property");

		long count = dao.getAuditCountTotal(criteria);
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
		criteria.setDataPath("Property");

		long count = dao.getAuditCountTotal(criteria);
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

		long count = dao.getAuditCountTotal(criteria);
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

	private void assertDatumRecordCounts(String desc, DatumRecordCounts row, Long rawCount,
			Long hourCount, Integer dayCount, Integer monthCount) {
		assertThat(desc + " not null", row, notNullValue());
		assertThat(desc + " datum count", row.getDatumCount(), equalTo(rawCount));
		assertThat(desc + " datum hourly count", row.getDatumHourlyCount(), equalTo(hourCount));
		assertThat(desc + " datum daily count", row.getDatumDailyCount(), equalTo(dayCount));
		assertThat(desc + " datum monthly count", row.getDatumMonthlyCount(), equalTo(monthCount));
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
	public void findDatumAtSixtyFortyDifference() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateAt(filter, ts,
				Period.hours(1));

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
	public void findDatumBetween() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.calculateBetween(filter, ts, ts2,
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
	public void findDatumBetweenLocalOnlyStart() {
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
	public void findDatumBetweenLocalOnlyEnd() {
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
	public void bulkImport() {
		// given
		SimpleBulkLoadingOptions options = new SimpleBulkLoadingOptions("Test load", null,
				LoadingTransactionMode.SingleTransaction, null);

		// when
		List<GeneralNodeDatum> data = createSampleData(59,
				new DateTime().hourOfDay().roundCeilingCopy().minusHours(1));
		try (LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> ctx = dao.createBulkLoadingContext(
				options, new LoadingExceptionHandler<GeneralNodeDatum, GeneralNodeDatumPK>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( GeneralNodeDatum d : data ) {
				ctx.load(d);
			}
			ctx.commit();
		}

		// then
		try {
			assertThat("Datum rows imported", datumRowCount(), equalTo(data.size()));

			List<Map<String, Object>> ranges = getDatumRanges();
			assertThat("Range created", ranges, hasSize(1));
			assertThat("Range node", ranges.get(0), hasEntry("node_id", TEST_NODE_ID));
			assertThat("Range source", ranges.get(0), hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Range min", ranges.get(0),
					hasEntry("ts_min", new Timestamp(data.get(0).getCreated().getMillis())));
			assertThat("Range max", ranges.get(0), hasEntry("ts_max",
					new Timestamp(data.get(data.size() - 1).getCreated().getMillis())));
		} finally {
			// manually clean up transactionally circumvented data import data
			TestTransaction.end();
			jdbcTemplate.execute(new ConnectionCallback<Object>() {

				@Override
				public Object doInConnection(Connection con) throws SQLException, DataAccessException {
					con.setAutoCommit(true);
					con.createStatement().executeUpdate("delete from solardatum.da_datum");
					con.createStatement().executeUpdate("delete from solardatum.da_datum_range");
					con.createStatement().executeUpdate("delete from solaragg.agg_stale_datum");
					con.createStatement().executeUpdate("delete from solaragg.aud_datum_hourly");
					return null;
				}
			});
		}
	}

	@Test
	public void bulkImportMultipleSources() {
		// given
		SimpleBulkLoadingOptions options = new SimpleBulkLoadingOptions("Test load", null,
				LoadingTransactionMode.SingleTransaction, null);

		// when
		List<GeneralNodeDatum> data1 = createSampleData(59,
				new DateTime().hourOfDay().roundCeilingCopy().minusHours(1), TEST_NODE_ID,
				TEST_SOURCE_ID);
		List<GeneralNodeDatum> data2 = createSampleData(59,
				new DateTime().hourOfDay().roundCeilingCopy().minusHours(1).minusDays(1), TEST_2ND_NODE,
				TEST_2ND_SOURCE);
		List<GeneralNodeDatum> data = new ArrayList<>(data1);
		data.addAll(data2);
		try (LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> ctx = dao.createBulkLoadingContext(
				options, new LoadingExceptionHandler<GeneralNodeDatum, GeneralNodeDatumPK>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( GeneralNodeDatum d : data ) {
				ctx.load(d);
			}
			ctx.commit();
		}

		// then
		try {
			assertThat("Datum rows imported", datumRowCount(), equalTo(data.size()));

			List<Map<String, Object>> ranges = getDatumRanges();
			assertThat("Ranges created", ranges, hasSize(2));

			assertThat("Range 0 node", ranges.get(0), hasEntry("node_id", TEST_2ND_NODE));
			assertThat("Range 0 source", ranges.get(0), hasEntry("source_id", TEST_2ND_SOURCE));
			assertThat("Range 0 min", ranges.get(0),
					hasEntry("ts_min", new Timestamp(data2.get(0).getCreated().getMillis())));
			assertThat("Range 0 max", ranges.get(0), hasEntry("ts_max",
					new Timestamp(data2.get(data2.size() - 1).getCreated().getMillis())));

			assertThat("Range 1 node", ranges.get(1), hasEntry("node_id", TEST_NODE_ID));
			assertThat("Range 1 source", ranges.get(1), hasEntry("source_id", TEST_SOURCE_ID));
			assertThat("Range 1 min", ranges.get(1),
					hasEntry("ts_min", new Timestamp(data1.get(0).getCreated().getMillis())));
			assertThat("Range 1 max", ranges.get(1), hasEntry("ts_max",
					new Timestamp(data1.get(data1.size() - 1).getCreated().getMillis())));

		} finally {
			// manually clean up transactionally circumvented data import data
			TestTransaction.end();
			jdbcTemplate.execute(new ConnectionCallback<Object>() {

				@Override
				public Object doInConnection(Connection con) throws SQLException, DataAccessException {
					con.setAutoCommit(true);
					con.createStatement().executeUpdate("delete from solardatum.da_datum");
					con.createStatement().executeUpdate("delete from solardatum.da_datum_range");
					con.createStatement().executeUpdate("delete from solaragg.agg_stale_datum");
					con.createStatement().executeUpdate("delete from solaragg.aud_datum_hourly");
					return null;
				}
			});
		}
	}

	@Test
	public void findDatumRecordCountsLocal() {
		// given
		DateTime ts = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		DateTime ts2 = new DateTime(2018, 11, 2, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d3);
		dao.store(d4);

		DateTime ts3 = new DateTime(2018, 11, 3, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d5 = getTestInstance(ts3.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d6 = getTestInstance(ts3.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d5);
		dao.store(d6);

		processAggregateStaleData();

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 11, 3, 0, 0));
		DatumRecordCounts result = dao.countDatumRecords(filter);

		// then
		assertDatumRecordCounts("Counts ", result, 4L /* d2, d3, d4, d5 */,
				4L /* 2018-11-01 00, 2018-11-01 23, 2018-11-02 00, 2018-02-23 */,
				2 /* 2018-11-01, 2018-11-02 */, 0);
	}

	@Test
	public void deleteForFilterLocal() {
		// given
		findDatumRecordCountsLocal();

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 11, 3, 0, 0));
		long result = dao.deleteFiltered(filter);

		processAggregateStaleData();

		// then
		assertThat("Raw delete count", result, equalTo(4L));

		DateTime ts = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts3 = new DateTime(2018, 11, 3, 0, 0, 0, ts.getZone());

		List<Map<String, Object>> rawData = getDatum();
		assertThat("Remaining raw count", rawData, hasSize(2));
		assertThat("Raw 1 date", rawData.get(0).get("ts"),
				equalTo(new Timestamp(ts.minusMinutes(1).getMillis())));
		assertThat("Raw 2 date", rawData.get(1).get("ts"),
				equalTo(new Timestamp(ts3.plusMinutes(1).getMillis())));

		List<Map<String, Object>> hourData = getDatumAggregateHourly();
		assertThat("Remaining hour count", hourData, hasSize(2));
		assertThat("Hour 1 date", hourData.get(0).get("ts_start"),
				equalTo(new Timestamp(ts.minusMinutes(1).hourOfDay().roundFloorCopy().getMillis())));
		assertThat("Hour 2 date", hourData.get(1).get("ts_start"),
				equalTo(new Timestamp(ts3.plusMinutes(1).hourOfDay().roundFloorCopy().getMillis())));

		List<Map<String, Object>> dayData = getDatumAggregateDaily();
		assertThat("Remaining day count", dayData, hasSize(2));
		assertThat("Day 1 date", dayData.get(0).get("ts_start"),
				equalTo(new Timestamp(ts.minusMinutes(1).dayOfMonth().roundFloorCopy().getMillis())));
		assertThat("Day 2 date", dayData.get(1).get("ts_start"),
				equalTo(new Timestamp(ts3.plusMinutes(1).dayOfMonth().roundFloorCopy().getMillis())));

		List<Map<String, Object>> monthData = getDatumAggregateMonthly();
		assertThat("Remaining month count", monthData, hasSize(2));
		assertThat("Month 1 date", monthData.get(0).get("ts_start"),
				equalTo(new Timestamp(ts.minusMinutes(1).monthOfYear().roundFloorCopy().getMillis())));
		assertThat("Month 2 date", monthData.get(1).get("ts_start"),
				equalTo(new Timestamp(ts3.plusMinutes(1).monthOfYear().roundFloorCopy().getMillis())));
	}

	@Test
	public void deleteForFilterLocal_partialHourRange() {
		// given
		final DateTime start = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final DateTime end = new DateTime(2018, 11, 2, 1, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime curr = start;
		long rawInsertedCount = 0;
		List<DateTime> rowDates = new ArrayList<>(128);
		SortedSet<DateTime> rowDateHoursSet = new TreeSet<>();
		while ( !curr.isAfter(end) ) {
			rowDates.add(curr.minusMinutes(1));
			rowDates.add(curr.plusMinutes(1));
			rowDateHoursSet.add(curr.minusMinutes(1).hourOfDay().roundFloorCopy());
			rowDateHoursSet.add(curr.plusMinutes(1).hourOfDay().roundFloorCopy());
			GeneralNodeDatum d1 = getTestInstance(curr.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
			GeneralNodeDatum d2 = getTestInstance(curr.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
			dao.store(d1);
			dao.store(d2);
			curr = curr.plusMinutes(30);
			rawInsertedCount += 2;
		}
		List<DateTime> rowDateHours = new ArrayList<>(rowDateHoursSet);

		processAggregateStaleData();

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 30));
		filter.setLocalEndDate(new LocalDateTime(2018, 11, 1, 23, 30));
		long result = dao.deleteFiltered(filter);

		List<Map<String, Object>> rows = getDatumAggregateHourly();
		log.debug("Hour rows after delete:\n{}",
				rows.stream().map(e -> e.toString()).collect(joining("\n")));

		assertThat("Hour rows on or before/after start/end delete range should remain", rows,
				hasSize(5));
		int idx;
		for ( idx = 0; idx < 2; idx++ ) {
			assertThat("Remaining leading hour row date " + idx,
					((Timestamp) rows.get(idx).get("ts_start")),
					equalTo(new Timestamp(rowDateHours.get(idx).getMillis())));
		}
		int i;
		for ( i = 2, idx = rowDateHours.size() - 3; idx < rowDateHours.size(); idx++, i++ ) {
			assertThat("Remaining trailing hour row date " + i,
					((Timestamp) rows.get(i).get("ts_start")),
					equalTo(new Timestamp(rowDateHours.get(idx).getMillis())));
		}

		processAggregateStaleData();

		// then
		assertThat("Raw delete count leaves 3 leading, 7 trailing", result,
				equalTo(rawInsertedCount - 10));

		List<Map<String, Object>> rawData = getDatum();
		log.debug("Raw rows after delete:\n{}",
				rawData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Raw delete count leaves 3 leading, 7 trailing", rawData, hasSize(10));
		for ( i = 0; i < 3; i++ ) {
			assertThat("Remaining leading raw row date " + i, ((Timestamp) rawData.get(i).get("ts")),
					equalTo(new Timestamp(rowDates.get(i).getMillis())));
		}
		for ( i = 3, idx = rowDates.size() - 7; i < rows.size(); i++, idx++ ) {
			assertThat("Remaining trailing raw row date " + i, ((Timestamp) rawData.get(i).get("ts")),
					equalTo(new Timestamp(rowDates.get(idx).getMillis())));
		}

		List<Map<String, Object>> hourData = getDatumAggregateHourly();
		log.debug("Hour rows after delete:\n{}",
				hourData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining hour count", hourData, hasSize(5));
		assertThat("Hour 1 date", hourData.get(0).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(0).getMillis())));
		assertThat("Hour 2 date", hourData.get(1).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(1).getMillis())));
		assertThat("Hour 3 date", hourData.get(2).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(rowDateHours.size() - 3).getMillis())));
		assertThat("Hour 4 date", hourData.get(3).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(rowDateHours.size() - 2).getMillis())));
		assertThat("Hour 5 date", hourData.get(4).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(rowDateHours.size() - 1).getMillis())));

		List<Map<String, Object>> dayData = getDatumAggregateDaily();
		log.debug("Day rows after delete:\n{}",
				dayData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining day count", dayData, hasSize(3));
		assertThat("Day 1 date", dayData.get(0).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(0).dayOfMonth().roundFloorCopy().getMillis())));
		assertThat("Day 2 date", dayData.get(1).get("ts_start"), equalTo(new Timestamp(
				rowDateHours.get(0).dayOfMonth().roundFloorCopy().plusDays(1).getMillis())));
		assertThat("Day 3 date", dayData.get(2).get("ts_start"), equalTo(new Timestamp(
				rowDateHours.get(0).dayOfMonth().roundFloorCopy().plusDays(2).getMillis())));

		List<Map<String, Object>> monthData = getDatumAggregateMonthly();
		log.debug("Month rows after delete:\n{}",
				monthData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining month count", monthData, hasSize(2));
		assertThat("Month 1 date", monthData.get(0).get("ts_start"),
				equalTo(new Timestamp(rowDateHours.get(0).monthOfYear().roundFloorCopy().getMillis())));
		assertThat("Month 2 date", monthData.get(1).get("ts_start"), equalTo(new Timestamp(
				rowDateHours.get(rowDateHours.size() - 1).monthOfYear().roundFloorCopy().getMillis())));
	}

	@Test
	public void findDatumRecordCountsLocalMultipleNodesDifferentTimeZones() {
		// given
		DateTime ts = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		dao.store(d1);
		dao.store(d2);

		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");
		final Long nodeId2 = -19889L;
		setupTestLocation(-9889L, tz2.getID());
		setupTestNode(nodeId2, -9889L);

		DateTime ts2 = new DateTime(2018, 11, 1, 0, 0, 0, tz2);
		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), nodeId2, TEST_SOURCE_ID);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), nodeId2, TEST_SOURCE_ID);
		dao.store(d3);
		dao.store(d4);

		processAggregateStaleData();

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, nodeId2 });
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 12, 1, 0, 0));
		DatumRecordCounts result = dao.countDatumRecords(filter);

		// then
		assertDatumRecordCounts("Counts ", result, 2L /* d2, d4 */,
				2L /* 2018-11-01 00 z1, 2018-11-01 00 z2 */,
				2 /* 2018-11-01 z1, 2018-11-01 z2 */,
				2 /* 2018-11 z1, 2018-11 z2 */);
	}

	@Test
	public void deleteForFilterLocalMultipleNodesDifferentTimeZones() {
		// given
		findDatumRecordCountsLocalMultipleNodesDifferentTimeZones();

		final Long nodeId2 = -19889L;
		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, nodeId2 });
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(new LocalDateTime(2018, 11, 1, 0, 0));
		filter.setLocalEndDate(new LocalDateTime(2018, 12, 1, 0, 0));

		List<Map<String, Object>> rows = getDatum();
		log.debug("Raw rows before delete:\n{}",
				rows.stream().map(e -> e.toString()).collect(joining("\n")));

		long result = dao.deleteFiltered(filter);

		rows = getStaleDatumOrderedByNode(Aggregation.Hour);
		log.debug("Stale rows after delete:\n{}",
				rows.stream().map(e -> e.toString()).collect(joining("\n")));

		processAggregateStaleData();

		// then
		assertThat("Raw delete count", result, equalTo(2L));

		DateTime ts_z1 = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts_z2 = new DateTime(2018, 11, 1, 0, 0, 0, tz2);

		List<Map<String, Object>> rawData = getDatum();
		log.debug("Raw rows after delete:\n{}",
				rawData.stream().map(e -> e.toString()).collect(joining("\n")));

		assertThat("Remaining raw count", rawData, hasSize(2));
		assertThat("Raw 1 date", rawData.get(0).get("ts"),
				equalTo(new Timestamp(ts_z2.minusMinutes(1).getMillis())));
		assertThat("Raw 2 date", rawData.get(1).get("ts"),
				equalTo(new Timestamp(ts_z1.minusMinutes(1).getMillis())));

		List<Map<String, Object>> hourData = getDatumAggregateHourly();
		log.debug("Hour rows after delete:\n{}",
				hourData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining hour count", hourData, hasSize(2));
		assertThat("Hour 1 date", hourData.get(0).get("ts_start"),
				equalTo(new Timestamp(ts_z2.minusMinutes(1).hourOfDay().roundFloorCopy().getMillis())));
		assertThat("Hour 2 date", hourData.get(1).get("ts_start"),
				equalTo(new Timestamp(ts_z1.minusMinutes(1).hourOfDay().roundFloorCopy().getMillis())));

		List<Map<String, Object>> dayData = getDatumAggregateDaily();
		log.debug("Day rows after delete:\n{}",
				dayData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining day count", dayData, hasSize(2));
		assertThat("Day 1 date", dayData.get(0).get("ts_start"),
				equalTo(new Timestamp(ts_z2.minusMinutes(1).dayOfMonth().roundFloorCopy().getMillis())));
		assertThat("Day 2 date", dayData.get(1).get("ts_start"),
				equalTo(new Timestamp(ts_z1.minusMinutes(1).dayOfMonth().roundFloorCopy().getMillis())));

		List<Map<String, Object>> monthData = getDatumAggregateMonthly();
		log.debug("Month rows after delete:\n{}",
				monthData.stream().map(e -> e.toString()).collect(joining("\n")));
		assertThat("Remaining month count", monthData, hasSize(2));
		assertThat("Month 1 date", monthData.get(0).get("ts_start"), equalTo(
				new Timestamp(ts_z2.minusMinutes(1).monthOfYear().roundFloorCopy().getMillis())));
		assertThat("Month 2 date", monthData.get(1).get("ts_start"), equalTo(
				new Timestamp(ts_z1.minusMinutes(1).monthOfYear().roundFloorCopy().getMillis())));
	}

	private void createNodeAndSourceData(DateTime start, int numMinutes, Long[] nodes,
			String[] sources) {
		for ( int i = 0; i < numMinutes; i++ ) {
			for ( Long nodeId : nodes ) {
				for ( String sourceId : sources ) {
					GeneralNodeDatum d = getTestInstance(start.plusMinutes(i), nodeId, sourceId);
					dao.store(d);
				}
			}
		}
	}

	private DateTime createNodeAndSourceData() {
		final Long[] nodes = new Long[] { TEST_NODE_ID, TEST_2ND_NODE };
		final String[] sources = new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE };
		final DateTime start = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		final int numMinutes = 10;
		createNodeAndSourceData(start, numMinutes, nodes, sources);
		return start;
	}

	@Test
	public void deleteFilteredSpecificNodeAndSourceAndDateRange() {
		// given
		setupTestNode(TEST_2ND_NODE);
		final DateTime start = createNodeAndSourceData();
		final int totalRows = 40;

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_2ND_NODE);
		filter.setSourceId(TEST_2ND_SOURCE);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMinutes(5).toLocalDateTime());
		int result = (int) dao.deleteFiltered(filter);

		// then
		assertThat("Delete count", result, equalTo(5));

		List<Map<String, Object>> rawData = getDatum();
		assertThat("Remaining row count", rawData, hasSize(totalRows - result));

		List<Map<String, Object>> node1Data = rawData.stream()
				.filter(m -> m.get("node_id").equals(TEST_NODE_ID)).collect(Collectors.toList());
		assertThat("Remaining node 1 count", node1Data, hasSize(totalRows / 2));

		List<Map<String, Object>> node2Data = rawData.stream()
				.filter(m -> m.get("node_id").equals(TEST_2ND_NODE)).collect(Collectors.toList());
		assertThat("Remaining node 2 count", node2Data, hasSize(totalRows / 2 - result));

		List<Map<String, Object>> node2Sourcd1Data = node2Data.stream()
				.filter(m -> m.get("source_id").equals(TEST_SOURCE_ID)).collect(Collectors.toList());
		assertThat("Remaining node 2 source 1 count", node2Sourcd1Data, hasSize(totalRows / 2 / 2));

		List<Map<String, Object>> node2Sourcd2Data = node2Data.stream()
				.filter(m -> m.get("source_id").equals(TEST_2ND_SOURCE)).collect(Collectors.toList());
		assertThat("Remaining node 2 source 2 count", node2Sourcd2Data,
				hasSize(totalRows / 2 / 2 - result));
		for ( int i = 0; i < node2Sourcd2Data.size(); i++ ) {
			assertThat("Remaining node 2 source 2 date " + i, node2Sourcd2Data.get(i).get("ts"),
					equalTo(new Timestamp(start.plusMinutes(5 + i).getMillis())));
		}
	}

	@Test
	public void bulkExport() {
		DateTime date = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int i = 0; i < 10; i++ ) {
			GeneralNodeDatum d = getTestInstance(date.plusMinutes(i), TEST_NODE_ID, TEST_SOURCE_ID);
			dao.store(d);
		}

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(date);
		filter.setEndDate(date.plusDays(1));

		FilterableBulkExportOptions options = new FilterableBulkExportOptions("test", filter, null);

		List<Long> totalResultCountEstimates = new ArrayList<>(1);

		ExportResult result = dao.batchExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			private int count = 0;

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				totalResultCountEstimates.add(totalResultCountEstimate);
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				assertThat("Datum ts", d.getId().getCreated(), equalTo(date.plusMinutes(count)));
				assertThat("Datum node ID", d.getId().getNodeId(), equalTo(TEST_NODE_ID));
				assertThat("Datum source ID", d.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
				count++;
				return ExportCallbackAction.CONTINUE;
			}
		}, options);

		assertThat("Result available", result, notNullValue());
		assertThat("Num processed count", result.getNumProcessed(), equalTo(10L));
		assertThat("Total result count estimates", totalResultCountEstimates, contains(10L));
	}

	@Test
	public void bulkExportAggregate15Minute() {
		DateTime date = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int i = 0; i < 60; i += 2 ) {
			GeneralNodeDatum d = getTestInstance(date.plusMinutes(i), TEST_NODE_ID, TEST_SOURCE_ID);
			dao.store(d);
		}

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(date);
		filter.setEndDate(date.plusHours(1));
		filter.setAggregation(Aggregation.FifteenMinute);

		FilterableBulkExportOptions options = new FilterableBulkExportOptions("test", filter, null);

		List<Long> totalResultCountEstimates = new ArrayList<>(1);

		ExportResult result = dao.batchExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			private int count = 0;

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				totalResultCountEstimates.add(totalResultCountEstimate);
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				assertThat("Datum ts", d.getId().getCreated(), equalTo(date.plusMinutes(count * 15)));
				assertThat("Datum node ID", d.getId().getNodeId(), equalTo(TEST_NODE_ID));
				assertThat("Datum source ID", d.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
				count++;
				return ExportCallbackAction.CONTINUE;
			}
		}, options);

		assertThat("Result available", result, notNullValue());
		assertThat("Num processed count", result.getNumProcessed(), equalTo(4L));
		assertThat("Total result count estimates", totalResultCountEstimates, contains((Long) null));
	}

	@Test
	public void bulkExportAggregateHour() {
		DateTime date = new DateTime(2018, 11, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		for ( int i = 0; i < 300; i += 10 ) {
			GeneralNodeDatum d = getTestInstance(date.plusMinutes(i), TEST_NODE_ID, TEST_SOURCE_ID);
			dao.store(d);
		}

		processAggregateStaleData();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(date);
		filter.setEndDate(date.plusHours(5));
		filter.setAggregation(Aggregation.Hour);

		FilterableBulkExportOptions options = new FilterableBulkExportOptions("test", filter, null);

		List<Long> totalResultCountEstimates = new ArrayList<>(1);

		ExportResult result = dao.batchExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

			private int count = 0;

			@Override
			public void didBegin(Long totalResultCountEstimate) {
				totalResultCountEstimates.add(totalResultCountEstimate);
			}

			@Override
			public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
				assertThat("Datum ts", d.getId().getCreated(), equalTo(date.plusHours(count)));
				assertThat("Datum node ID", d.getId().getNodeId(), equalTo(TEST_NODE_ID));
				assertThat("Datum source ID", d.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
				count++;
				return ExportCallbackAction.CONTINUE;
			}
		}, options);

		assertThat("Result available", result, notNullValue());
		assertThat("Num processed count", result.getNumProcessed(), equalTo(5L));
		assertThat("Total result count estimates", totalResultCountEstimates, contains(5L));
	}
}
