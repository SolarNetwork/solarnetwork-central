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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.arrayOfDecimals;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.arrayContaining;
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
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;

/**
 * Tests for the database rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumRollupTests extends BaseDatumJdbcTestSupport {

	private static interface RollupCallback {

		public void doWithStream(List<GeneralNodeDatum> datums,
				Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
				List<AggregateDatumEntity> results);
	}

	private void loadStreamAndRollup(String resource, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			RollupCallback callback) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = null;
		List<AggregateDatumEntity> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			results = jdbcTemplate.query(
					"select * from solardatm.rollup_datm_for_time_span(?::uuid,?,?)",
					AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
					Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()));
		}
		callback.doWithStream(datums, meta, streamId, results);
	}

	private void loadStreamWithAuxiliaryAndRollup(String resource, ZonedDateTime aggStart,
			ZonedDateTime aggEnd, RollupCallback callback) throws IOException {
		List<?> data = DatumTestUtils.loadJsonDatumAndAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = null;
		List<AggregateDatumEntity> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				DatumTestUtils.insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
					Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
					List<AggregateDatumEntity> results) {
				assertThat("Agg result returned", results, hasSize(1));

				AggregateDatumEntity result = results.get(0);
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
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatumEntity> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatumEntity result = results.get(0);
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
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatumEntity> results) {
						assertThat("Agg result returned", results, hasSize(1));

						AggregateDatumEntity result = results.get(0);
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
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatumEntity> results) {
						assertThat("Agg result returned", results, hasSize(1));
						AggregateDatumEntity result = results.get(0);
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

}
