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
 * {@link MyBatisGeneralNodeDatumDao#findAccumulationWithin(net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter, DateTime, DateTime, Period)}
 * and
 * {@link {@link MyBatisGeneralNodeDatumDao#findAccumulationWithin(net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}
 * methods.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoFindAccumulationWithinTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	private static final String WH_PROP = "watt_hours";

	@Override
	public void setup() {
		super.setup();
		setupTestNode(TEST_2ND_NODE);
	}

	private void verifyAccumulationResult(String msg, ReportingGeneralNodeDatumMatch m, DateTime endDate,
			DateTime startDate, Object startValue, Object endValue, Object accumulation) {
		verifyAccumulationResult(msg, m, TEST_NODE_ID, TEST_SOURCE_ID, WH_PROP, endDate, startDate,
				startValue, endValue, accumulation);
	}

	private void verifyAccumulationResult2(String msg, ReportingGeneralNodeDatumMatch m,
			DateTime endDate, DateTime startDate, Object startValue, Object endValue,
			Object accumulation) {
		verifyAccumulationResult(msg, m, TEST_2ND_NODE, TEST_2ND_SOURCE, WH_PROP, endDate, startDate,
				startValue, endValue, accumulation);
	}

	private void verifyAccumulationResult(String msg, ReportingGeneralNodeDatumMatch m, Long nodeId,
			String sourceId, String propName, DateTime endDate, DateTime startDate, Object startValue,
			Object endValue, Object accumulation) {
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

	private static final Long[] DATUM_ACCUMULATION_WH_1 = new Long[] { 4002L, 4445L, 8044L, 8344L };
	private static final Long[] DATUM_ACCUMULATION_WH_2 = new Long[] { 4020L, 4454L, 8004L, 8234L };

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2) {
		return setupDefaultDatumAccumulationData(ts, ts2, true);
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData2(DateTime ts, DateTime ts2) {
		return setupDefaultDatumAccumulationData2(ts, ts2, true);
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2,
			boolean processAggregateStaleData) {
		return setupDefaultDatumAccumulationData(ts, ts2, TEST_NODE_ID, TEST_SOURCE_ID,
				processAggregateStaleData, DATUM_ACCUMULATION_WH_1);
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData2(DateTime ts, DateTime ts2,
			boolean processAggregateStaleData) {
		return setupDefaultDatumAccumulationData(ts, ts2, TEST_2ND_NODE, TEST_2ND_SOURCE,
				processAggregateStaleData, DATUM_ACCUMULATION_WH_2);
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2,
			Long nodeId, String sourceId, boolean processAggregateStaleData, Long[] data) {
		assert data != null && data.length > 3;
		List<GeneralNodeDatumPK> result = new ArrayList<>(4);

		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), nodeId, sourceId);
		d1.getSamples().putAccumulatingSampleValue(WH_PROP, data[0]);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), nodeId, sourceId);
		d2.getSamples().putAccumulatingSampleValue(WH_PROP, data[1]);
		result.add(dao.store(d1));
		result.add(dao.store(d2));

		if ( ts2 != null ) {
			GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), nodeId, sourceId);
			d3.getSamples().putAccumulatingSampleValue(WH_PROP, data[2]);
			GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), nodeId, sourceId);
			d4.getSamples().putAccumulatingSampleValue(WH_PROP, data[3]);
			result.add(dao.store(d3));
			result.add(dao.store(d4));

			if ( processAggregateStaleData ) {
				// query depends on aggregate data
				processAggregateStaleData();

				DateTimeFormatter dateFormat = ISODateTimeFormat.date().withZone(ts.getZone());
				assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily(nodeId)),
						contains(sqlDates(dateFormat.print(ts.minusDays(1)), dateFormat.print(ts),
								dateFormat.print(ts2.minusDays(1)), dateFormat.print(ts2))));
			}
		}

		return result;
	}

	/*-============================================================================================
	 * Accumulation within (exact time constraint)
	 *-==========================================================================================*/

	@Test
	public void accumulationWithin() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - d2) = (8044 - 4445)", m, ids.get(2).getCreated(),
				ids.get(1).getCreated(), 4445, 8044, 3599);
	}

	@Test
	public void accumulationWithinMultiIgnoreOtherData() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData2(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_2ND_NODE);
		filter.setSourceId(TEST_2ND_SOURCE);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("(d3 - d2) = (8004 - 4454)", m, ids.get(2).getCreated(),
				ids.get(1).getCreated(), 4454, 8004, 3550);
	}

	@Test
	public void accumulationWithinMulti() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);
		List<GeneralNodeDatumPK> ids2 = setupDefaultDatumAccumulationData2(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("N2 (d3 - d2) = (8004 - 4454)", m, ids2.get(2).getCreated(),
				ids2.get(1).getCreated(), 4454, 8004, 3550);
		m = itr.next();
		verifyAccumulationResult("N1 (d3 - d2) = (8044 - 4445)", m, ids.get(2).getCreated(),
				ids.get(1).getCreated(), 4445, 8044, 3599);
	}

	@Test
	public void accumulationWithinWithResetRecord() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d2) + (d3 - rS) == (5000 - 4445) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(1).getCreated(), 4445, 8044, 599);
	}

	@Test
	public void accumulationWithinWithResetRecordMulti() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);
		List<GeneralNodeDatumPK> ids2 = setupDefaultDatumAccumulationData2(ts, ts2);

		// add reset record
		Map<String, Number> finalSamples = Collections.singletonMap(WH_PROP, 5000);
		Map<String, Number> startSamples = Collections.singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(ts.plusDays(1), TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);
		insertResetDatumAuxiliaryRecord(ts.plusDays(1), TEST_2ND_NODE, TEST_2ND_SOURCE, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("N2 (rF - d2) + (d3 - rS) == (5000 - 4454) + (8004 - 8000)", m,
				ids2.get(2).getCreated(), ids2.get(1).getCreated(), 4454, 8004, 550);
		m = itr.next();
		verifyAccumulationResult("N1 (rF - d2) + (d3 - rS) == (5000 - 4445) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(1).getCreated(), 4445, 8044, 599);
	}

	@Test
	public void accumulationWithinWithResetRecordsSequential() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));
		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult(
				"(rF1 - d2) + (rF2 - rS1) + (d3 - rS2) == (5000 - 4445) + (8010 - 8000) + (8044 - 7000)",
				m, ids.get(2).getCreated(), ids.get(1).getCreated(), 4445, 8044, 1609);
	}

	@Test
	public void accumulationWithinWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d2
		DateTime resetDate = ts.plusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				4400, 8044, 3644);
	}

	@Test
	public void accumulationWithinWithResetRecordAtEnd() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d2) == (8100 - 4445)", m, resetDate, ids.get(1).getCreated(),
				4445, 8100, 3655);
	}

	@Test
	public void accumulationWithinWithResetRecordAtStartAndEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d2
		DateTime resetDate = ts.plusSeconds(30);
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(r2F - rS) == (8100 - 4400)", m, resetDate2, resetDate, 4400, 8100,
				3700);
	}

	@Test
	public void accumulationWithinWithResetRecordAtStartAndEndAndMiddle() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d2
		DateTime resetDate = ts.plusSeconds(30);
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter, ts,
				ts2, null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rMidF - rS) + (r2F - rMidS) == (10000 - 4400) + (8100 - 7000)", m,
				resetDate2, resetDate, 4400, 8100, 6700);
	}

	/*-============================================================================================
	 * Accumulation within (exact time constraint), local dates 
	 *-==========================================================================================*/

	@Test
	public void accumulationWithinLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - d2) = (8044 - 4445)", m, ids.get(2).getCreated(),
				ids.get(1).getCreated(), 4445, 8044, 3599);
	}

	@Test
	public void accumulationWithinLocalWithResetRecord() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d2) + (d3 - rS) == (5000 - 4445) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(1).getCreated(), 4445, 8044, 599);
	}

	@Test
	public void accumulationWithinLocalWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested start date than d2
		DateTime resetDate = ts.plusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(d3 - rS) == (8044 - 4400)", m, ids.get(2).getCreated(), resetDate,
				4400, 8044, 3644);
	}

	@Test
	public void accumulationWithinLocalWithResetRecordAtEnd() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult("(rF - d2) == (8100 - 4445)", m, resetDate, ids.get(1).getCreated(),
				4445, 8100, 3655);
	}

	@Test
	public void accumulationWithinLocalExactTimeBoundaries() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
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
	public void accumulationWithinLocalNoDataBeforeStartDate() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
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
	public void accumulationWithinLocalNoDataAfterStartDate() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulationWithin(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
	}

}
