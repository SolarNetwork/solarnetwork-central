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

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

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

	private List<Map<String, Object>> selectAllDatumMostRecent() {
		return jdbcTemplate
				.queryForList("select * from solardatum.da_datum_range ORDER BY node_id, source_id");
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
}
