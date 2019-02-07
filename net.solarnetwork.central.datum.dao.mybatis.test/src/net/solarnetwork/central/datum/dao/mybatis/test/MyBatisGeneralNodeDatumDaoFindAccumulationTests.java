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
		assertThat(msg + " " + propName + " accumulation", m.getSampleData().get(WH_PROP),
				equalTo(accumulation));
	}

	private List<GeneralNodeDatumPK> setupDefaultDatumAccumulationData(DateTime ts, DateTime ts2) {
		List<GeneralNodeDatumPK> result = new ArrayList<>(4);

		GeneralNodeDatum d1 = getTestInstance(ts.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d1.getSamples().putAccumulatingSampleValue(WH_PROP, 4002);
		GeneralNodeDatum d2 = getTestInstance(ts.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d2.getSamples().putAccumulatingSampleValue(WH_PROP, 4445);
		result.add(dao.store(d1));
		result.add(dao.store(d2));

		GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d3.getSamples().putAccumulatingSampleValue(WH_PROP, 8044);
		GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), TEST_NODE_ID, TEST_SOURCE_ID);
		d4.getSamples().putAccumulatingSampleValue(WH_PROP, 8344);
		result.add(dao.store(d3));
		result.add(dao.store(d4));

		// query depends on aggregate data
		processAggregateStaleData();

		DateTimeFormatter dateFormat = ISODateTimeFormat.date().withZone(ts.getZone());
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates(dateFormat.print(ts.minusDays(1)), dateFormat.print(ts),
						dateFormat.print(ts2.minusDays(1)), dateFormat.print(ts2))));

		return result;
	}

	@Test
	public void findDatumAccumulationOver() {
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
	public void findDatumAccumulationOverWithResetRecord() {
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
	public void findDatumAccumulationOverWithResetRecordsSequential() {
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
	public void findDatumAccumulationOverWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		// add reset record, closer to requested end start than d1
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
	public void findDatumAccumulationOverWithResetRecordAtEnd() {
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
	public void findDatumAccumulationOverWithResetRecordAtStartAndEnd() {
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
	public void findDatumAccumulationOverWithResetRecordAtStartAndEndAndMiddle() {
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

}
