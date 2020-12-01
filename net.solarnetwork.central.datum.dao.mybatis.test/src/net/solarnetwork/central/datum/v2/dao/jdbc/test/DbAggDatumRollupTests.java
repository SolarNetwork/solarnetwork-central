/* ==================================================================
 * DbAggDatumRollupTests.java - 5/11/2020 6:45:06 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.arrayOfDecimals;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Tests for the database aggregate rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbAggDatumRollupTests extends BaseDatumJdbcTestSupport {

	private static interface RollupCallback {

		public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
				UUID streamId, List<AggregateDatum> results);
	}

	private BasicObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a", TEST_TZ);
	}

	private BasicObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), timeZoneId, ObjectDatumKind.Node,
				nodeId, sourceId, new String[] { "x", "y", "z" }, new String[] { "w", "ww" },
				new String[] { "st" });
	}

	private void loadStreamAndRollup(String resource, Aggregation kind, ZonedDateTime aggStart,
			ZonedDateTime aggEnd, RollupCallback callback) throws IOException {
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = DatumDbUtils.loadJsonAggregateDatumResource(resource, getClass(),
				staticProvider(singleton(meta)));
		log.debug("Got test data: {}", datums);
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		List<AggregateDatum> results = jdbcTemplate.query(
				"select * from solardatm.rollup_agg_datm_for_time_span(?::uuid,?,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()), kind.getKey());
		callback.doWithStream(datums, meta, streamId, results);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void regularHour() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-01.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.55", "5.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("3600"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "48", "1.1", "3.8" }),
										arrayOfDecimals(new String[] { "48", "2.0", "7.8" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "100", "928", "828" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void oneRow() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-02.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.2", "2.1"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("100"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.1", "3.1" }),
										arrayOfDecimals(new String[] { "6", "2.0", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "100", "201", "101" })));
					}
				});
	}

	@Test
	public void noRow() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-03.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(0));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void noAccumulatingData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-04.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.55", "5.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								nullValue());
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "48", "1.1", "3.8" }),
										arrayOfDecimals(new String[] { "48", "2.0", "7.8" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								nullValue());
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void inconsistentData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-05.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.5", "5.266666667", "60.1"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("2700", "2400"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "42", "1.1", "3.7" }),
										arrayOfDecimals(new String[] { "36", "2.0", "7.7" }),
										arrayOfDecimals(new String[] { "18", "40.0", "100.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "100", "928", "621" }),
										arrayOfDecimals(new String[] { "1000", "3402", "2402" })));
					}
				});
	}

}
