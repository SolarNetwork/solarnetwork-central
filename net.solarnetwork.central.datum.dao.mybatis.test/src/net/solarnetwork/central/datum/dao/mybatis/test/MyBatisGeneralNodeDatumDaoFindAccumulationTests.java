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
import static org.hamcrest.Matchers.hasSize;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertThat;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

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

	private void verifyAggregateReadings(String msg, List<GeneralNodeDatumReadingAggregate> aggData,
			String propName, Object[][] expected) {
		assertThat(msg + " count", aggData, hasSize(expected.length));
		DateTimeZone zone = DateTimeZone.forID(TEST_TZ);
		for ( int i = 0; i < expected.length; i++ ) {
			GeneralNodeDatumReadingAggregate m = aggData.get(i);
			assertThat(msg + " [" + i + "] " + propName + " date", m.getDate().withZone(zone),
					equalTo(expected[i][0]));
			assertThat(msg + " [" + i + "] " + propName + " start value", m.getAs().get(propName),
					equalTo(expected[i][1]));
			assertThat(msg + " [" + i + "] " + propName + " end value", m.getAf().get(propName),
					equalTo(expected[i][2]));
			assertThat(msg + " [" + i + "] " + propName + " accumulation", m.getA().get(propName),
					equalTo(expected[i][3]));
		}
	}

	private void verifyAggregateReadings(String msg, String propName, Object[][] expectedHourly,
			Object[][] expectedDaily, Object[][] expectedMonthly) {
		verifyAggregateReadings(msg + " hourly", getDatumReadingAggregteHourly(), propName,
				expectedHourly);
		verifyAggregateReadings(msg + " daily", getDatumReadingAggregteDaily(), propName, expectedDaily);
		verifyAggregateReadings(msg + " monthly", getDatumReadingAggregteMonthly(), propName,
				expectedMonthly);
	}

	private void verifyCalculateDatumDiffOverResult(String msg, GeneralNodeDatumReadingAggregate m,
			Object startValue, Object endValue, Object accumulation) {
		verifyCalculateDatumDiffOverResult(msg, m, WH_PROP, startValue, endValue, accumulation);
	}

	private void verifyCalculateDatumDiffOverResult(String msg, GeneralNodeDatumReadingAggregate m,
			String propName, Object startValue, Object endValue, Object accumulation) {
		assertThat(msg + " " + propName + " start value", m.getAs().get(propName), equalTo(startValue));
		assertThat(msg + " " + propName + " end value", m.getAf().get(propName), equalTo(endValue));
		assertThat(msg + " " + propName + " accumulation", m.getA().get(propName),
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

		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = DatumTestUtils.ingestDatumStream(log,
				jdbcTemplate, Arrays.asList(d1, d2), "UTC");
		result.add(d1.getId());
		result.add(d2.getId());

		if ( ts2 != null ) {
			GeneralNodeDatum d3 = getTestInstance(ts2.minusMinutes(1), nodeId, sourceId);
			d3.getSamples().putAccumulatingSampleValue(WH_PROP, data[2]);
			GeneralNodeDatum d4 = getTestInstance(ts2.plusMinutes(1), nodeId, sourceId);
			d4.getSamples().putAccumulatingSampleValue(WH_PROP, data[3]);

			DatumTestUtils.ingestDatumStream(log, jdbcTemplate, Arrays.asList(d3, d4), "UTC");
			result.add(d3.getId());
			result.add(d4.getId());

			if ( processAggregateStaleData ) {
				// query depends on aggregate data
				DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate);

				UUID streamId = metas.get(new NodeSourcePK(nodeId, sourceId)).getStreamId();
				List<AggregateDatum> aggs = DatumTestUtils
						.listAggregateDatum(jdbcTemplate, Aggregation.Day).stream()
						.filter(e -> streamId.equals(e.getStreamId())).collect(Collectors.toList());
				List<Instant> days = aggs.stream().map(AggregateDatum::getTimestamp)
						.collect(Collectors.toList());
				assertThat("Aggregate days", days,
						contains(
								ZonedDateTime
										.ofInstant(Instant.ofEpochMilli(ts.getMillis()),
												ZoneId.of(TEST_TZ))
										.truncatedTo(ChronoUnit.DAYS).minusDays(1).toInstant(),
								ZonedDateTime
										.ofInstant(Instant.ofEpochMilli(ts.getMillis()),
												ZoneId.of(TEST_TZ))
										.truncatedTo(ChronoUnit.DAYS).toInstant(),
								ZonedDateTime
										.ofInstant(Instant.ofEpochMilli(ts2.getMillis()),
												ZoneId.of(TEST_TZ))
										.truncatedTo(ChronoUnit.DAYS).minusDays(1).toInstant(),
								ZonedDateTime
										.ofInstant(Instant.ofEpochMilli(ts2.getMillis()),
												ZoneId.of(TEST_TZ))
										.truncatedTo(ChronoUnit.DAYS).toInstant()));
			}
		}

		return result;
	}

	/*-============================================================================================
	 * Accumulation over(no time constraint)
	 *-==========================================================================================*/

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
				ids.get(0).getCreated(), 4002, 8044, 4042);
	}

	@Test
	public void accumulationOverMultiIgnoreOtherData() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData2(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_2ND_NODE);
		filter.setSourceId(TEST_2ND_SOURCE);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(1));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("(d3 - d1) = (8004 - 4020)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 4020, 8004, 3984);
	}

	@Test
	public void accumulationOverMulti() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2, false);
		List<GeneralNodeDatumPK> ids2 = setupDefaultDatumAccumulationData2(ts, ts2);

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_2ND_NODE });
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_2ND_SOURCE });
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("N2 (d3 - d1) = (8004 - 4020)", m, ids2.get(2).getCreated(),
				ids2.get(0).getCreated(), 4020, 8004, 3984);
		m = itr.next();
		verifyAccumulationResult("N1 (d3 - d1) = (8044 - 4002)", m, ids.get(2).getCreated(),
				ids.get(0).getCreated(), 4002, 8044, 4042);
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
				ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 1042);
	}

	@Test
	public void accumulationOverWithResetRecordMulti() {
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
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter, ts, ts2,
				null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(2));

		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();
		verifyAccumulationResult2("N2 (rF - d1) + (d3 - rS) == (5000 - 4020) + (8004 - 8000)", m,
				ids2.get(2).getCreated(), ids2.get(0).getCreated(), 4020, 8004, 984);
		m = itr.next();
		verifyAccumulationResult("N1 (rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)", m,
				ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 1042);
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
				m, ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 2052);
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
				4400, 8044, 3644);
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
				4002, 8100, 4098);
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
		verifyAccumulationResult("(r2F - rS) == (8100 - 4400)", m, resetDate2, resetDate, 4400, 8100,
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
				resetDate2, resetDate, 4400, 8100, 6700);
	}

	/*-============================================================================================
	 * Low-level accumulation over single node (no time constraint)
	 *-==========================================================================================*/

	@Test
	public void calculateDatumDiffOver() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(d3 - d1) = (8044 - 4002)";
		verifyCalculateDatumDiffOverResult(msg, m, 4002, 8044, 4042);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),  4002, 4002, 0 },
			new Object[] { ts,                4002, 4445, 443 },
			new Object[] { ts2.minusHours(1), 4445, 8044, 3599 },
			new Object[] { ts2,               8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),  4002, 4002, 0 },
			new Object[] { ts,               4002, 4445, 443 },
			new Object[] { ts2.minusDays(1), 4445, 8044, 3599 },
			new Object[] { ts2,              8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1), 4002, 4002, 0 },
			new Object[] { ts,                4002, 8044, 4042 },
			new Object[] { ts.plusMonths(1),  8044, 8344, 300 },
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecord() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record
		DateTime resetDate = ts.plusDays(1);
		Map<String, Number> finalSamples = Collections.singletonMap(WH_PROP, 5000);
		Map<String, Number> startSamples = Collections.singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(rF - d1) + (d3 - rS) == (5000 - 4002) + (8044 - 8000)";
		verifyCalculateDatumDiffOverResult(msg, m, 4002, 8044, 1042);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { resetDate,			4445, 5000, 555 },
			new Object[] { ts2.minusHours(1),	8000, 8044, 44 }, 
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { resetDate,			4445, 5000, 555 },
			new Object[] { ts2.minusDays(1),	8000, 8044, 44 }, 
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 8044, 1042 },
			new Object[] { ts.plusMonths(1),	8044, 8344, 300 }, 
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecordsSequential() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record 1
		DateTime resetDate = new DateTime(2018, 8, 2, 0, 0, 0, ts.getZone());
		Map<String, Number> finalSamples = Collections.singletonMap(WH_PROP, 5000);
		Map<String, Number> startSamples = Collections.singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// add reset record 2
		DateTime resetDate2 = new DateTime(2018, 8, 3, 0, 0, 0, ts.getZone());
		Map<String, Number> finalSamples2 = Collections.singletonMap(WH_PROP, 8010);
		Map<String, Number> startSamples2 = Collections.singletonMap(WH_PROP, 7000);
		insertResetDatumAuxiliaryRecord(resetDate2, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples2,
				startSamples2);

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(rF1 - d1) + (rF2 - rS1) + (d3 - rS2) == (5000 - 4002) + (8010 - 8000) + (8044 - 7000)";
		verifyCalculateDatumDiffOverResult(msg, m, 4002, 8044, 2052);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { resetDate,			4445, 5000, 555 },
			new Object[] { resetDate2,			8000, 8010, 10 },
			new Object[] { ts2.minusHours(1),	7000, 8044, 1044 }, 
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { resetDate,			4445, 5000, 555 },
			new Object[] { resetDate2,			8000, 8010, 10 },
			new Object[] { ts2.minusDays(1),	7000, 8044, 1044 }, 
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 8044, 2052 },
			new Object[] { ts.plusMonths(1),	8044, 8344, 300 }, 
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecordAtStart() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested start date than d1
		DateTime resetDate = ts.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8000);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 4400);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(d3 - rS) == (8044 - 4400)";
		verifyCalculateDatumDiffOverResult(msg, m, 4400, 8044, 3644);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 8000, 3998 },
			new Object[] { ts,					4400, 4445, 45 },
			new Object[] { ts2.minusHours(1),	4445, 8044, 3599 },
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 8000, 3998 },
			new Object[] { ts,					4400, 4445, 45 },
			new Object[] { ts2.minusDays(1),	4445, 8044, 3599 },
			new Object[] { ts2,					8044, 8344, 300 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 8000, 3998 },
			new Object[] { ts,					4400, 8044, 3644 },
			new Object[] { ts.plusMonths(1),	8044, 8344, 300 },
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecordAtEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

		// add reset record, closer to requested end date than d3
		DateTime resetDate = ts2.minusSeconds(30);
		Map<String, Number> finalSamples = singletonMap(WH_PROP, 8100);
		Map<String, Number> startSamples = singletonMap(WH_PROP, 8000);
		insertResetDatumAuxiliaryRecord(resetDate, TEST_NODE_ID, TEST_SOURCE_ID, finalSamples,
				startSamples);

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(rF - d1) == (8100 - 4002)";
		verifyCalculateDatumDiffOverResult(msg, m, 4002, 8100, 4098);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { ts2.minusHours(1),	4445, 8100, 3655 },
			new Object[] { ts2,					8000, 8344, 344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 4002, 0 },
			new Object[] { ts,					4002, 4445, 443 },
			new Object[] { ts2.minusDays(1),	4445, 8100, 3655 },
			new Object[] { ts2,					8000, 8344, 344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 4002, 0 },
			new Object[] { ts,					4002, 8100, 4098 },
			new Object[] { ts.plusMonths(1),	8000, 8344, 344 },
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecordAtStartAndEnd() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

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

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(r2F - rS) == (8100 - 4400)";
		verifyCalculateDatumDiffOverResult(msg, m, 4400, 8100, 3700);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 8000, 3998 },
			new Object[] { ts,					4400, 4445, 45 },
			new Object[] { ts2.minusHours(1),	4445, 8100, 3655 },
			new Object[] { ts2,					8000, 8344, 344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 8000, 3998 },
			new Object[] { ts,					4400, 4445, 45 },
			new Object[] { ts2.minusDays(1),	4445, 8100, 3655 },
			new Object[] { ts2,					8000, 8344, 344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 8000, 3998 },
			new Object[] { ts,					4400, 8100, 3700 },
			new Object[] { ts.plusMonths(1),	8000, 8344, 344 },
			// @formatter:on
		});
	}

	@Test
	public void calculateDatumDiffOverWithResetRecordAtStartAndEndAndMiddle() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2, false);

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

		// process aggregates (with reset data)
		processAggregateStaleData();

		// when
		GeneralNodeDatumReadingAggregate m = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, ts,
				ts2);

		// then
		String msg = "(rMidF - rS) + (r2F - rMidS) == (10000 - 4400) + (8100 - 7000)";
		verifyCalculateDatumDiffOverResult(msg, m, 4400, 8100, 6700);

		verifyAggregateReadings(msg, WH_PROP, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusHours(1),	4002, 8000,  3998 },
			new Object[] { ts,					4400, 4445,  45 },
			new Object[] { resetDateMid,		4445, 10000, 5555 },
			new Object[] { ts2.minusHours(1),	7000, 8100,  1100 }, 
			new Object[] { ts2,					8000, 8344,  344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusDays(1),		4002, 8000,  3998 },
			new Object[] { ts,					4400, 4445,  45 },
			new Object[] { resetDateMid,		4445, 10000, 5555 },
			new Object[] { ts2.minusDays(1),	7000, 8100,  1100 }, 
			new Object[] { ts2,					8000, 8344,  344 },
			// @formatter:on
		}, new Object[][] {
			// @formatter:off
			new Object[] { ts.minusMonths(1),	4002, 8000, 3998 },
			new Object[] { ts,					4400, 8100, 6700 },
			new Object[] { ts.plusMonths(1),	8000, 8344, 344 }, 
			// @formatter:on
		});
	}

	/*-============================================================================================
	 * Accumulation over (no time constraint), local dates 
	 *-==========================================================================================*/

	@Test
	public void accumulationOverLocal() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		List<GeneralNodeDatumPK> ids = setupDefaultDatumAccumulationData(ts, ts2);

		log.debug("Day data: {}", getDatumAggregateDaily());

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
				ids.get(0).getCreated(), 4002, 8044, 4042);
	}

	@Test
	public void accumulationOverLocal_IgnoreNullStartAccumlation() {
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));

		// add a NULL record that is actually closer in time to start date
		GeneralNodeDatum null1 = new GeneralNodeDatum();
		null1.setNodeId(TEST_NODE_ID);
		null1.setCreated(ts.minusSeconds(30));
		null1.setSourceId(TEST_SOURCE_ID);
		null1.setSamples(new GeneralNodeDatumSamples());
		null1.getSamples().putStatusSampleValue("alert", "foo");
		dao.store(null1);

		accumulationOverLocal();
	}

	@Test
	public void accumulationOverLocal_IgnoreNullStartAccumlationFirst() {
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));

		// add a NULL record that is actually the start of data
		GeneralNodeDatum null1 = new GeneralNodeDatum();
		null1.setNodeId(TEST_NODE_ID);
		null1.setCreated(ts.minusSeconds(90));
		null1.setSourceId(TEST_SOURCE_ID);
		null1.setSamples(new GeneralNodeDatumSamples());
		null1.getSamples().putStatusSampleValue("alert", "foo");
		dao.store(null1);

		accumulationOverLocal();
	}

	@Test
	public void accumulationOverLocal_IgnoreNullEndAccumlation() {
		DateTime ts = new DateTime(2018, 9, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));

		// add a NULL record that is actually closer in time to end date
		GeneralNodeDatum null1 = new GeneralNodeDatum();
		null1.setNodeId(TEST_NODE_ID);
		null1.setCreated(ts.minusSeconds(30));
		null1.setSourceId(TEST_SOURCE_ID);
		null1.setSamples(new GeneralNodeDatumSamples());
		null1.getSamples().putStatusSampleValue("alert", "foo");
		dao.store(null1);

		accumulationOverLocal();
	}

	@Test
	public void accumulationOverLocal_OnlyNullAccumulation() {
		// given
		DateTime ts = new DateTime(2018, 8, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		DateTime ts2 = new DateTime(2018, 9, 1, 0, 0, 0, ts.getZone());
		setupDefaultDatumAccumulationData(ts, ts2);

		jdbcTemplate.update("update solardatum.da_datum set jdata_a = NULL");
		processAggregateStaleData();

		log.debug("Raw data: {}", getDatum());
		log.debug("Day data: {}", getDatumAggregateDaily());

		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2018, 8, 1, 0, 0), new LocalDateTime(2018, 9, 1, 0, 0), null);

		// then
		assertThat("Datum at rows returned", results.getReturnedResultCount(), equalTo(0));
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
				ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 1042);
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
				4400, 8044, 3644);
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
				4002, 8100, 4098);
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
		log.debug("Raw data: {}", getDatum());
		log.debug("Day data: {}", getDatumAggregateDaily());
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
				ids.get(0).getCreated(), 4002, 8044, 4042);
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
				ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 1042);
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
				4400, 8044, 3644);
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
				4002, 8100, 4098);
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
				ids.get(0).getCreated(), 4002, 8044, 4042);
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
				ids.get(2).getCreated(), ids.get(0).getCreated(), 4002, 8044, 1042);
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
				4400, 8044, 3644);
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
				4002, 8100, 4098);
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

	@Test
	public void accumulationOverMonth_LateDataStart() throws Exception {
		// given
		final DateTimeZone tz = DateTimeZone.forID(TEST_TZ);
		int count = 0;
		DateTime start = null;
		DateTime end = null;
		DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ").withZone(tz);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("sample-raw-data-02.csv"), "UTF-8"))) {
			String line = null;
			while ( (line = in.readLine()) != null ) {
				count++;
				if ( count == 1 ) {
					continue;
				}
				String[] data = commaDelimitedListToStringArray(line);
				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setCreated(dtf.parseDateTime(data[0]));

				GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
				samples.putInstantaneousSampleValue("watts", Integer.valueOf(data[3]));
				samples.putAccumulatingSampleValue("watt_hours", Long.valueOf(data[4]));
				d.setSamples(samples);
				dao.store(d);

				if ( start == null ) {
					start = d.getCreated();
				}
				end = d.getCreated();
			}
		}
		log.debug("Loaded {} datum from {} to {}", count, start, end);

		// query depends on aggregate data
		processAggregateStaleData();
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates("2019-07-25", "2019-07-26", "2019-07-27", "2019-07-28", "2019-07-29",
						"2019-07-30", "2019-08-02")));
		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				start.monthOfYear().roundFloorCopy(), end.monthOfYear().roundFloorCopy(), null);

		// then
		assertThat("Datum row returned", results.getReturnedResultCount(), equalTo(1));
		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();

		assertThat("First date start of data", m.getId().getCreated().withZone(tz), equalTo(start));
		assertThat("Last date end of month", m.getSampleData().get("endDate"),
				equalTo((Object) ISODateTimeFormat.dateTime().withZoneUTC()
						.print(dtf.parseDateTime("2019-07-30 12:54:00+12")).replace('T', ' ')));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"), equalTo(4000));
		assertThat("Watt hours end d2", m.getSampleData().get("watt_hours_end"), equalTo(16183000));
		assertThat("Watt hours accumulation between d1 - d2", m.getSampleData().get("watt_hours"),
				equalTo(16179000));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

	@Test
	public void accumulationOverLocalMonth_LateDataStart() throws Exception {
		// given
		final DateTimeZone tz = DateTimeZone.forID(TEST_TZ);
		int count = 0;
		DateTime start = null;
		DateTime end = null;
		DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ").withZone(tz);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				getClass().getResourceAsStream("sample-raw-data-02.csv"), "UTF-8"))) {
			String line = null;
			while ( (line = in.readLine()) != null ) {
				count++;
				if ( count == 1 ) {
					continue;
				}
				String[] data = commaDelimitedListToStringArray(line);
				GeneralNodeDatum d = new GeneralNodeDatum();
				d.setNodeId(TEST_NODE_ID);
				d.setSourceId(TEST_SOURCE_ID);
				d.setCreated(dtf.parseDateTime(data[0]));

				GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
				samples.putInstantaneousSampleValue("watts", Integer.valueOf(data[3]));
				samples.putAccumulatingSampleValue("watt_hours", Long.valueOf(data[4]));
				d.setSamples(samples);
				dao.store(d);

				if ( start == null ) {
					start = d.getCreated();
				}
				end = d.getCreated();
			}
		}
		log.debug("Loaded {} datum from {} to {}", count, start, end);

		// query depends on aggregate data
		processAggregateStaleData();
		assertThat("Aggregate days", sqlDatesFromLocalDates(getDatumAggregateDaily()),
				contains(sqlDates("2019-07-25", "2019-07-26", "2019-07-27", "2019-07-28", "2019-07-29",
						"2019-07-30", "2019-08-02")));
		// when
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAccumulation(filter,
				new LocalDateTime(2019, 7, 1, 0, 0), new LocalDateTime(2019, 8, 1, 0, 0), null);

		// then
		assertThat("Datum row returned", results.getReturnedResultCount(), equalTo(1));
		Iterator<ReportingGeneralNodeDatumMatch> itr = results.iterator();
		ReportingGeneralNodeDatumMatch m = itr.next();

		assertThat("First date start of data", m.getId().getCreated().withZone(tz), equalTo(start));
		assertThat("Last date end of month", m.getSampleData().get("endDate"),
				equalTo((Object) ISODateTimeFormat.dateTime().withZoneUTC()
						.print(dtf.parseDateTime("2019-07-30 12:54:00+12")).replace('T', ' ')));
		assertThat("Node ID", m.getId().getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("Source ID", m.getId().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Watt hours start d1", m.getSampleData().get("watt_hours_start"), equalTo(4000));
		assertThat("Watt hours end d2", m.getSampleData().get("watt_hours_end"), equalTo(16183000));
		assertThat("Watt hours accumulation between d1 - d2", m.getSampleData().get("watt_hours"),
				equalTo(16179000));
		assertThat("Time zone", m.getSampleData().get("timeZone"), equalTo((Object) TEST_TZ));
	}

}
