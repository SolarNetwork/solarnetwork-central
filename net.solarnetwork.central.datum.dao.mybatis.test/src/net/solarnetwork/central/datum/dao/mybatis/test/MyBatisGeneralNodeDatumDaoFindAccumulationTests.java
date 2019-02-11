/* ==================================================================
 * MyBatisGeneralNodeDatumDaoFindAccumulationTests.java - 8/02/2019 10:02:09 am
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

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for the
 * {@link MyBatisGeneralNodeDatumDao#findAccumulation(net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter, org.joda.time.DateTime, org.joda.time.DateTime, org.joda.time.Period)}
 * and
 * {@link {@link MyBatisGeneralNodeDatumDao#findAccumulation(net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter, org.joda.time.LocalDateTime, org.joda.time.LocalDateTime, org.joda.time.Period)}
 * methods.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoFindAccumulationTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final String WH_PROP = "watt_hours";

	private void verifyAccumulationResult(String msg, ReportingGeneralNodeDatumMatch m, DateTime endDate,
			DateTime startDate, Object endValue, Object startValue, Object accumulation) {
		verifyAccumulationResult(msg, m, TEST_NODE_ID, TEST_SOURCE_ID, WH_PROP, endDate, startDate,
				endValue, startValue, accumulation);
	}

	private void verifyAccumulationResult(String msg, ReportingGeneralNodeDatumMatch m, Long nodeId,
			String sourceId, String propName, DateTime endDate, DateTime startDate, Object endValue,
			Object startValue, Object accumulation) {
		assertThat(msg + " node ID", m.getId().getNodeId(), equalTo(nodeId));
		assertThat(msg + " source ID", m.getId().getSourceId(), equalTo(sourceId));
		assertThat(msg + " time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
		assertThat(msg + " start date", m.getId().getCreated().withZone(UTC),
				equalTo(startDate.withZone(UTC)));
		assertThat(msg + " end date", m.getSampleData().get("endDate"), equalTo(
				(Object) ISODateTimeFormat.dateTime().print(endDate.withZone(UTC)).replace('T', ' ')));
		assertThat(msg + " " + propName + " start value", m.getSampleData().get(propName + "_start"),
				equalTo(startValue));
		assertThat(msg + " " + propName + " end value", m.getSampleData().get(propName + "_end"),
				equalTo(endValue));
		assertThat(msg + " " + propName + " accumulation", m.getSampleData().get(propName),
				equalTo(accumulation));
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2) {
		return setupDefaultDatumAccumulationData(ts, ts2, true);
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2,
			boolean processAggregateStaleData) {
		List<GeneralNodeDatumPK> result = new ArrayList<>(4);

		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue(WH_PROP, 4002);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue(WH_PROP, 4445);
		result.add(dao.store(d1));
		result.add(dao.store(d2));

		if ( ts2 != null ) {
			GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
			d3.getSamples().putAccumulatingSampleValue(WH_PROP, 8044);
			GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
			d4.getSamples().putAccumulatingSampleValue(WH_PROP, 8344);
			result.add(dao.store(d3));
			result.add(dao.store(d4));

			if ( processAggregateStaleData ) {
				// query depends on aggregate data
				processAggregateStaleData();

				DateTimeFormatter dateFormat = ISODateTimeFormat.date().withZone(ts.getZone());
				assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
						contains(sqlDates(dateFormat.print(ts.minusDays(1)), dateFormat.print(ts),
								dateFormat.print(ts2.minusDays(1)), dateFormat.print(ts2))));
			}
		}

		return result;
	}

	@Test
	public void accumulationOver() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - d1) = (8044 - 4002)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 8044, 4002, 4042);
	}

	@Test
	public void accumulationOverWithResetRecord() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record
		Map<String, Number> finalSamples = Collections.singletonMap(WH_PROP, 5000);
		Map<String, Number> startSamples = Collections.singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(ts.plusDays(1), TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(0).getCreated(), 8044, 4002, 1042);
	}

	@Test
	public void accumulationOverWithResetRecordsSequential() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record 1
		Map<String, Number> finalSamples = Collections.singletonMap(WH_PROP, 5000);
		Map<String, Number> startSamples = Collections.singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(new DateTime(2018, 8, 2, 0, 0, 0, ts.getZone()), TEST_NODE_ID,
				TEST_SOURCE_ID, finalSamples, startSamples);

		// add reset record 2
		Map<String, Number> finalSamples2 = Collections.singletonMap(WH_PROP, 8010);
		Map<String, Number> startSamples2 = Collections.singletonMap(WH_PROP, 7000);
		insertResetDatumAuxiliaryRecord(new DateTime(2018, 8, 3, 0, 0, 0, ts.getZone()), TEST_NODE_ID,
				TEST_SOURCE_ID, finalSamples2, startSamples2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));
		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult(
				"(rF1 - d1) + (rF2 - rS1) + (d3 - rS2) == (5000 - 4002) + (8010 - 8000) + (8044 - 7000)",
				m, ids.get(2).getCreated(), ids.get(0).getCreated(), 8044, 4002, 2052);
	}

	@Test
	public void accumulationOverWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d1
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				8044, 4400, 3644);
	}

	@Test
	public void accumulationOverWithResetRecordAtEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts2.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) == (8100 - 4002)", m, resetDate, ids.get(0).getCreated(),
				8100, 4002, 4098);
	}

	@Test
	public void accumulationOverWithResetRecordAtStartAndEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// add reset record, closer to requested end date than d3
		DateTime resetDate2 = ts2.minusSeconds(30);
		Map<String, Number> finalSamples2 = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples2 = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate2, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples2,
				startSamples2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(r2F - rS) == (8100 - 4400)", m, resetDate2, resetDate, 8100, 4400,
				3700);
	}

	@Test
	public void accumulationOverWithResetRecordAtStartAndEndAndMiddle() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// add reset record in middle
		DateTime resetDateMid = ts.plusDays(1);
		Map<String, Number> finalSamplesMid = singletonMap(WH_PROP, 10000);
		Map<String, Number> startSamplesMid = singletonMap(WH_PROP, 7000);
		insertResetDatumAuxiliaryRecord(resetDateMid, TEST_NODE_ID, TEST_SOURCE_ID, finalSamplesMid,
				startSamplesMid);

		// add reset record, closer to requested end date than d3
		DateTime resetDate2 = ts2.minusSeconds(30);
		Map<String, Number> finalSamples2 = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples2 = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate2, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples2,
				startSamples2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rMidF - rS) + (r2F - rMidS) == (10000 - 4400) + (8100 - 7000)", m,
				resetDate2, resetDate, 8100, 4400, 6700);
	}

	/*-============================================================================================
	 * Accumulation over(no time constraint), local dates 
	 *-==========================================================================================*/

	@Test
	public void accumulationOverLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - d1) = (8044 - 4002)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 8044, 4002, 4042);
	}

	@Test
	public void accumulationOverLocalWithResetRecord() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record
		Map<String, Number> finalSamples = Collections.singletonMap("watt_hours", 5000);
		Map<String, Number> startSamples = Collections.singletonMap("watt_hours", 8000);
		insertResetDatumAuxiliaryRecord(new DateTime(2018, 8, 2, 0, 0, 0, ts.getZone()), TEST_NODE_ID,
				TEST_SOURCE_ID, finalSamples, startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(0).getCreated(), 8044, 4002, 1042);
	}

	@Test
	public void accumulationOverLocalWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d1
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				8044, 4400, 3644);
	}

	@Test
	public void accumulationOverLocalWithResetRecordAtEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts2.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) == (8100 - 4002)", m, resetDate, ids.get(0).getCreated(),
				8100, 4002, 4098);
	}

	@Test
	public void accumulationOverLocalExactTimeBoundaries() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		dao.store(d1);

		DateTime tsMid = new DateTime(2018, 8, 15, 0, 0, 0, ts.getZone());
		GeneralNodeDatum dMid = getTestInstance(tsMid, TEST_NODE_ID, TEST_SOURCE_ID);
		dMid.getSamples().putAccumulatingSampleValue("watt_hours", 6000);
		dao.store(dMid);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d2 = getTestInstance(ts2, TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		dao.store(d2);

		// query depends on aggregate data
		processAggregateStaleData();
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates("2018-08-01", "2018-08-15", "2018-09-01")));

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("First date latest before start", m.getId().getCreated().withZone(ts.getZone()),
				equalTo(d1.getCreated()));
		assertThat("Last date latest before end", m.getSampleData().get("endDate"),
				equalTo((Object) ISODateTimeFormat.dateTime()
						.print(d2.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation between d1 - d2", m.getSampleData().get("watt_hours"),
				equalTo((Object) 4042));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 4002));
		assertThat("Watt hours end d2", m.getSampleData().get("watt_hours_end"), equalTo((Object) 8044));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	@Test
	public void accumulationOverLocalNoDataBeforeStartDate() {
		// given
		DateTime ts = new DateTime(2018, 8, 2, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		dao.store(d1);

		DateTime tsMid = new DateTime(2018, 8, 15, 0, 0, 0, ts.getZone());
		GeneralNodeDatum dMid = getTestInstance(tsMid, TEST_NODE_ID, TEST_SOURCE_ID);
		dMid.getSamples().putAccumulatingSampleValue("watt_hours", 6000);
		dao.store(dMid);

		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d2 = getTestInstance(ts2, TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		dao.store(d2);

		// query depends on aggregate data
		processAggregateStaleData();
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates("2018-08-02", "2018-08-15", "2018-09-01")));

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("First date latest before start", m.getId().getCreated().withZone(ts.getZone()),
				equalTo(d1.getCreated()));
		assertThat("Last date latest before end", m.getSampleData().get("endDate"),
				equalTo((Object) ISODateTimeFormat.dateTime()
						.print(d2.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation between d1 - d2", m.getSampleData().get("watt_hours"),
				equalTo((Object) 4042));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 4002));
		assertThat("Watt hours end d2", m.getSampleData().get("watt_hours_end"), equalTo((Object) 8044));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	@Test
	public void accumulationOverLocalNoDataAfterStartDate() {
		// given
		DateTime ts = new DateTime(2018, 7, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		GeneralNodeDatum d1 = getTestInstance(ts, TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue("watt_hours", 4002);
		dao.store(d1);

		DateTime tsMid = new DateTime(2018, 7, 15, 0, 0, 0, ts.getZone());
		GeneralNodeDatum dMid = getTestInstance(tsMid, TEST_NODE_ID, TEST_SOURCE_ID);
		dMid.getSamples().putAccumulatingSampleValue("watt_hours", 6000);
		dao.store(dMid);

		DateTime ts2 = new DateTime(2018, 7, 31, 0, 0, 0, ts.getZone());
		GeneralNodeDatum d2 = getTestInstance(ts2, TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue("watt_hours", 8044);
		dao.store(d2);

		// query depends on aggregate data
		processAggregateStaleData();
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates("2018-07-01", "2018-07-15", "2018-07-31")));

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		assertThat("First date latest before start", m.getId().getCreated().withZone(ts.getZone()),
				equalTo(d2.getCreated()));
		assertThat("Last date latest before end", m.getSampleData().get("endDate"),
				equalTo((Object) ISODateTimeFormat.dateTime()
						.print(d2.getCreated().withZone(DateTimeZone.UTC)).replace('T', ' ')));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours accumulation between d2 - d2", m.getSampleData().get("watt_hours"),
				equalTo((Object) 0));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"),
				equalTo((Object) 8044));
		assertThat("Watt hours end d2", m.getSampleData().get("watt_hours_end"), equalTo((Object) 8044));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	/*-============================================================================================
	 * Accumulation limited (by time constraint, e.g. interval '1 month')
	 *-==========================================================================================*/

	@Test
	public void accumulationLimited() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - d1) == (8044 - 4002)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 8044, 4002, 4042);
	}

	@Test
	public void accumulationLimitedWithResetRecord() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record
		Map<String, Number> finalSamples = Collections.singletonMap("watt_hours", 5000);
		Map<String, Number> startSamples = Collections.singletonMap("watt_hours", 8000);
		insertResetDatumAuxiliaryRecord(ts.plusDays(1), TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(0).getCreated(), 8044, 4002, 1042);
	}

	@Test
	public void accumulationLimitedWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested start date than d1
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				8044, 4400, 3644);
	}

	@Test
	public void accumulationLimitedWithResetRecordAtEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts2.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				Period.months(1));

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d1) == (8100 - 4002)", m, resetDate, ids.get(0).getCreated(),
				8100, 4002, 4098);
	}

	/*-============================================================================================
	 * Accumulation limited (by time constraint, e.g. interval '1 month'), local dates
	 *-==========================================================================================*/

	@Test
	public void accumulationLimitedLocalNoData() {
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
	public void accumulationLimitedLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

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
		verifyAccumulationResult("(d3 - d1) == (8044 - 4002)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 8044, 4002, 4042);
	}

	@Test
	public void accumulationLimitedLocalWithResetRecord() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record
		Map<String, Number> finalSamples = Collections.singletonMap("watt_hours", 5000);
		Map<String, Number> startSamples = Collections.singletonMap("watt_hours", 8000);
		insertResetDatumAuxiliaryRecord(new DateTime(2018, 8, 2, 0, 0, 0, ts.getZone()), TEST_NODE_ID,
				TEST_SOURCE_ID, finalSamples, startSamples);

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
		verifyAccumulationResult("(rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(0).getCreated(), 8044, 4002, 1042);
	}

	@Test
	public void accumulationLimitedLocalWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested start date than d1
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

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
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				8044, 4400, 3644);
	}

	@Test
	public void accumulationLimitedLocalWithResetRecordAtEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts2.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

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
		verifyAccumulationResult("(rF - d1) == (8100 - 4002)", m, resetDate, ids.get(0).getCreated(),
				8100, 4002, 4098);
	}

	@Test
	public void accumulationLimitedLocalOnlyStart() {
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
		verifyAccumulationResult("(d1 - d1) == (4002 - 4002)", m, d1.getCreated(), d1.getCreated(), 4002,
				4002, 0);
	}

	@Test
	public void accumulationLimitedLocalOnlyEnd() {
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
		verifyAccumulationResult("(d1 - d1) == (4002 - 4002)", m, d1.getCreated(), d1.getCreated(), 4002,
				4002, 0);
	}

}
