/* ==================================================================
 * DbDatumRollupTests.java - 30/10/2020 3:12:24 pm
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumAndAuxiliaryResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.arrayOfDecimals;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Tests for the database rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumRollupTests extends BaseDatumJdbcTestSupport {

	private static interface RollupCallback {

		public void doWithStream(List<GeneralNodeDatum> datums,
				Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
				List<AggregateDatum> results);
	}

	private void loadStreamAndRollup(String resource, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		List<AggregateDatum> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			results = jdbcTemplate.query(
					"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
					AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
					Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));
		}
		callback.doWithStream(datums, meta, streamId, results);
	}

	private void rollup(UUID streamId, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) {
		List<AggregateDatum> results = Collections.emptyList();
		results = jdbcTemplate.query("select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));

		callback.doWithStream(null, null, streamId, results);
	}

	private void loadStreamWithAuxiliaryAndRollup(String resource, ZonedDateTime aggStart,
			ZonedDateTime aggEnd, RollupCallback callback) throws IOException {
		List<?> data = loadJsonDatumAndAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		List<AggregateDatum> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
			results = jdbcTemplate.query(
					"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
					AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
					Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));
		}
		callback.doWithStream(datums, meta, streamId, results);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void regularHour() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-01.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "4.6"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("25"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
								arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "20", "100", "120" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void imperfectHour() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-02.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "4.6"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("30"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
								arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "30", "100", "130" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void incompleteHour_noTrailing() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-03.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.35", "3.6"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("19.5"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "4", "1.2", "1.5" }),
								arrayOfDecimals(new String[] { "4", "2.1", "5.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "20", "100", "120" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void incompleteHour_noLeading() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-04.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.5", "5.1"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("20.5"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "5", "1.3", "1.7" }),
								arrayOfDecimals(new String[] { "5", "3.1", "7.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "20", "110", "130" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void incompleteHour_noLeadingOrTrailing() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-05.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "4.6"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("15"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "4", "1.3", "1.6" }),
								arrayOfDecimals(new String[] { "4", "3.1", "6.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "15", "110", "125" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void incompleteHour_oneRow() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-06.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.3", "3.1"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("0"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "1", "1.3", "1.3" }),
								arrayOfDecimals(new String[] { "1", "3.1", "3.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "0", "110", "110" })));
			}
		});
	}

	@Test
	public void incompleteHour_noData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-07.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(0));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void noAccumulatingData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-08.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "4.6"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(), nullValue());
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "4", "1.3", "1.6" }),
								arrayOfDecimals(new String[] { "4", "3.1", "6.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(), nullValue());
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void inconsistentData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-14.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "5.1", "45.1"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("20", "200"));
				assertThat("Agg tags", result.getProperties().getTags(), arrayContaining("Ohboy"));
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
								arrayOfDecimals(new String[] { "4", "2.1", "7.1" }),
								arrayOfDecimals(new String[] { "2", "40.1", "50.1" })));
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
						arrayContaining(arrayOfDecimals(new String[] { "20", "105", "125" }),
								arrayOfDecimals(new String[] { "200", "1100", "1300" })));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_oneResetExactlyAtStart() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-15.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30.5"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_oneResetInMiddle() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-16.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("35"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "35", "100", "25" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_oneResetExactlyAtEnd() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-17.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30.5"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "31", "10", "41" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_oneResetExactlyAtStartWithoutLeading() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-18.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30.5"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_oneResetExactlyAtEndWithoutTrailing() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-19.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30.5"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "31", "10", "41" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_multiResetsWithin() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-20.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("36"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "36", "100", "210" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_adjacentResetsWithin() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-21.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("36"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "36", "100", "220" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_resetsExactlyAtStartAndEnd() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-22.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("31"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "31", "10", "41" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_resetsExactlyAtStartAndEndWithoutLeadingOrTrailing() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-23.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("31"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "31", "10", "41" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_onlyOneReset() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-24.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								nullValue());
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("0"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								nullValue());
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "0", "115", "5" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_twoResetInMiddle() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-25.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								nullValue());
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("5"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								nullValue());
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "5", "115", "200" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_resetJustBeforeStart() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-26.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
					}
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void resetRecord_resetJustAfterEnd() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-27.txt", start, start.plusHours(1),
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
								arrayOfDecimals("1.45", "4.6"));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("30"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
					}
				});
	}

	@Test
	public void status_coEqualMostFrequent() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-28.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));
				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
				assertThat("Agg accumulating", result.getProperties().getAccumulating(), nullValue());
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						nullValue());
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(), nullValue());
				assertThat("Agg status has some co-equal most-frequent value",
						result.getProperties().getStatus(),
						anyOf(arrayContaining("A"), arrayContaining("C")));
			}
		});
	}

	@Test
	public void status_mostFrequent() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-29.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));
				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
				assertThat("Agg accumulating", result.getProperties().getAccumulating(), nullValue());
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						nullValue());
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(), nullValue());
				assertThat("Agg status is most frequent value", result.getProperties().getStatus(),
						arrayContaining("B"));
			}
		});
	}

	@Test
	public void status_multiPropMostFrequent() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-30.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));
				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
				assertThat("Agg accumulating", result.getProperties().getAccumulating(), nullValue());
				assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
						nullValue());
				assertThat("Stats accumulating", result.getStatistics().getAccumulating(), nullValue());
				assertThat("Agg status is most frequent value", result.getProperties().getStatus(),
						arrayContaining("B", "DD"));
			}
		});
	}

	@Test
	public void hour_withTag() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-14.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg instantaneous", result.getProperties().getInstantaneous(),
						arrayOfDecimals("1.45", "5.1", "45.1"));
				assertThat("Agg accumulating", result.getProperties().getAccumulating(),
						arrayOfDecimals("20", "200"));
				assertThat("Agg tags", result.getProperties().getTags(), arrayContaining("Ohboy"));
			}
		});
	}

	@Test
	public void hour_withTags_repeatedTag() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-36.txt", start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
				assertThat("Agg tags", result.getProperties().getTags(),
						arrayContainingInAnyOrder("Yeehaw", "Blamo", "Ohboy"));
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void gapsInAccumulating() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "temp", "watts", "dcPower", "dcVoltage",
						"frequency", "ambientTemp", "apparentPower" },
				new String[] { "wattHours" }, null);
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-03.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2021, 2, 10, 21, 35, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Stats accumulating ignores gaps", result.getStatistics().getAccumulating(),
						arrayContaining(
								arrayOfDecimals(new String[] { "-112", "1067564247", "1067564135" })));
			}
		});
	}

}
