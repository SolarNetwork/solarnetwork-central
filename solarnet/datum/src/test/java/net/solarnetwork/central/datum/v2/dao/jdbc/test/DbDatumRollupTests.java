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
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumAndAuxiliaryResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.arrayOfDecimals;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Tests for the database rollup stored procedures.
 *
 * @author matt
 * @version 2.1
 */
public class DbDatumRollupTests extends BaseDatumJdbcTestSupport {

	/**
	 * Rollup callback.
	 */
	public static interface RollupCallback {

		/**
		 * Handle the rollup result.
		 *
		 * @param datums
		 *        the datums
		 * @param meta
		 *        the metadata
		 * @param streamId
		 *        the stream ID
		 * @param results
		 *        the results
		 */
		public void doWithStream(List<GeneralNodeDatum> datums,
				Map<NodeSourcePK, ObjectDatumStreamMetadata> meta, UUID streamId,
				List<AggregateDatum> results);
	}

	/**
	 * CSV rollup callback.
	 */
	public static interface CsvRollupCallback {

		/**
		 * Handle the rollup result.
		 *
		 * @param datums
		 *        the datums
		 * @param meta
		 *        the mdatadata
		 * @param streamId
		 *        the stream ID
		 * @param results
		 *        the results
		 */
		public void doWithStream(List<Datum> datums, ObjectDatumStreamMetadata meta, UUID streamId,
				List<AggregateDatum> results);
	}

	/**
	 * Load CSV stream datum and rollup.
	 *
	 * @param log
	 *        the logger
	 * @param jdbcTemplate
	 *        the JDBC operations
	 * @param resourceClass
	 *        the class to load {@code reesource} from
	 * @param resource
	 *        the CSV resource to load
	 * @param meta
	 *        the metadata
	 * @param aggStart
	 *        the aggregation start
	 * @param aggEnd
	 *        the aggregation end
	 * @param callback
	 *        the callback
	 */
	public static void loadCsvStreamAndRollup(Logger log, JdbcOperations jdbcTemplate,
			Class<?> resourceClass, String resource, ObjectDatumStreamMetadata meta,
			ZonedDateTime aggStart, ZonedDateTime aggEnd, CsvRollupCallback callback) {
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(resourceClass, resource,
				staticProvider(singleton(meta)));
		log.debug("Got test data:\n{}", datum.stream().map(Object::toString).collect(joining("\n")));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<AggregateDatum> results = jdbcTemplate.query(
				"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));

		callback.doWithStream(datum, meta, streamId, results);
	}

	public void loadCsvStreamAndRollup(String resource, ObjectDatumStreamMetadata meta,
			ZonedDateTime aggStart, ZonedDateTime aggEnd, CsvRollupCallback callback) {
		loadCsvStreamAndRollup(log, jdbcTemplate, getClass(), resource, meta, aggStart, aggEnd,
				callback);
	}

	/**
	 *
	 * @param log
	 *        the logger
	 * @param jdbcTemplate
	 *        the JDBC operations
	 * @param resourceClass
	 *        the class to load {@code reesource} from
	 * @param resource
	 *        the CSV resource to load
	 * @param aggStart
	 *        the aggregation start
	 * @param aggEnd
	 *        the aggregation end
	 * @param callback
	 *        the callback
	 * @throws IOException
	 *         if an IO error occurs
	 */
	public static void loadStreamAndRollup(Logger log, JdbcOperations jdbcTemplate,
			Class<?> resourceClass, String resource, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, resourceClass);
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

	private void loadStreamAndRollup(String resource, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) throws IOException {
		loadStreamAndRollup(log, jdbcTemplate, getClass(), resource, aggStart, aggEnd, callback);
	}

	/**
	 * Rollup.
	 *
	 * @param jdbcTemplate
	 *        the JDBC operations
	 * @param streamId
	 *        the stream ID
	 * @param aggStart
	 *        the aggregation start
	 * @param aggEnd
	 *        the aggregation end
	 * @param callback
	 *        the callback
	 */
	public static void rollup(JdbcOperations jdbcTemplate, UUID streamId, ZonedDateTime aggStart,
			ZonedDateTime aggEnd, RollupCallback callback) {
		List<AggregateDatum> results = Collections.emptyList();
		results = jdbcTemplate.query("select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));

		callback.doWithStream(null, null, streamId, results);
	}

	private void rollup(UUID streamId, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) {
		rollup(jdbcTemplate, streamId, aggStart, aggEnd, callback);
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
						arrayContaining(arrayOfDecimals(new String[] { "25", "100", "125" })));
			}
		});
	}

	@Test
	public void calcDiffDatum_perfectHourlyData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);

		ZonedDateTime start = ZonedDateTime.of(2017, 7, 4, 9, 0, 0, 0, ZoneOffset.UTC);
		loadCsvStreamAndRollup("sample-raw-data-04.csv", meta, start, start.plusHours(1),
				new CsvRollupCallback() {

					@Override
					public void doWithStream(List<Datum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);
						assertThat("Stream ID matches", result.getStreamId(), equalTo(streamId));
						assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));
						assertThat("Agg accumulating", result.getProperties().getAccumulating(),
								arrayOfDecimals("37"));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(
										new String[] { "37", "12476432001", "12476432038" })));
					}
				});
	}

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

	@Test
	public void resetRecord_oneResetInMiddle_offsetProperty() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-40.txt", start, start.plusHours(1),
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
								arrayOfDecimals("0", "35"));
						assertThat("Stats instantaneous", result.getStatistics().getInstantaneous(),
								arrayContaining(arrayOfDecimals(new String[] { "6", "1.2", "1.7" }),
										arrayOfDecimals(new String[] { "6", "2.1", "7.1" })));
						assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
								arrayContaining(arrayOfDecimals(new String[] { "0", "1000", "1000" }),
										arrayOfDecimals(new String[] { "35", "100", "25" })));
					}
				});
	}

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

	private ObjectDatumStreamMetadata load_raw05() {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		return meta;
	}

	@Test
	public void raw05_1109_01() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Sparse data:", result.getStatistics().getAccumulating(), arrayContaining(
						decimalArray("0", "45804", "45804"), decimalArray("0", "41005", "41005")));
			}
		});
	}

	@Test
	public void raw05_1109_02() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Before mising data:", result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("0", "45804", "45804"),
								decimalArray("0", "41005", "41005")));
			}
		});
	}

	@Test
	public void raw05_1109_03() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 3, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("No data in range", results, hasSize(0));
			}
		});
	}

	@Test
	public void raw05_1109_18() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 18, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned for adjacent earlier hour", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Pick up accumulation from previous gap:",
						result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("132", "45804", "45936"),
								decimalArray("132", "41005", "41137")));
			}
		});
	}

	@Test
	public void raw05_1109_19() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Hour with perfect start:", result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("0", "45936", "45936"),
								decimalArray("0", "41137", "41137")));
			}
		});
	}

	@Test
	public void raw05_1109_20() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		rollup(meta.getStreamId(), start, start.plusHours(1), new RollupCallback() {

			@Override
			public void doWithStream(List<GeneralNodeDatum> datums,
					Map<NodeSourcePK, ObjectDatumStreamMetadata> metas, UUID sid,
					List<AggregateDatum> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatum result = results.get(0);
				log.debug("Got result: {}", result);
				assertThat("Stream ID matches", result.getStreamId(), equalTo(meta.getStreamId()));
				assertThat("Agg timestamp", result.getTimestamp(), equalTo(start.toInstant()));

				assertThat("Hour with perfect start:", result.getStatistics().getAccumulating(),
						arrayContaining(decimalArray("2", "45936", "45938"),
								decimalArray("2", "41137", "41139")));
			}
		});
	}

}
