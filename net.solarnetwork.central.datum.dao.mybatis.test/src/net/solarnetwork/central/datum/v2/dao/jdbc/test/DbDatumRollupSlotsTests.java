/* ==================================================================
 * DbDatumRollupSlotsTests.java - 10/11/2020 1:19:03 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Tests for the database slot rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumRollupSlotsTests extends BaseDatumJdbcTestSupport {

	private static interface RollupCallback {

		public void doWithStream(List<GeneralNodeDatum> datums,
				Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
				List<AggregateDatum> results);
	}

	private void loadStreamAndRollup(String resource, ZonedDateTime aggStart, ZonedDateTime aggEnd,
			Aggregation agg, RollupCallback callback) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		List<AggregateDatum> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			results = jdbcTemplate.query(
					"SELECT *, NULL AS read_a FROM solardatm.rollup_datm_for_time_span_slots(?::uuid,?,?,?) "
							+ "ORDER BY stream_id, ts_start",
					new AggregateDatumEntityRowMapper(agg), streamId.toString(),
					Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()),
					agg.getLevel());
			log.debug("Got {} rollup data:\n{}", agg.getKey(),
					results.stream().map(Object::toString).collect(Collectors.joining("\n")));
		}
		callback.doWithStream(datums, meta, streamId, results);
	}

	private void loadStreamWithAuxiliaryAndRollup(String resource, ZonedDateTime aggStart,
			ZonedDateTime aggEnd, Aggregation agg, RollupCallback callback) throws IOException {
		List<?> data = DatumTestUtils.loadJsonDatumAndAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		List<AggregateDatum> results = Collections.emptyList();
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				DatumTestUtils.insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
			results = jdbcTemplate.query(
					"SELECT *, NULL AS read_a FROM solardatm.rollup_datm_for_time_span_slots(?::uuid,?,?,?) "
							+ "ORDER BY stream_id, ts_start",
					new AggregateDatumEntityRowMapper(agg), streamId.toString(),
					Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()),
					agg.getLevel());
			log.debug("Got {} rollup data:\n{}", agg.getKey(),
					results.stream().map(Object::toString).collect(Collectors.joining("\n")));
		}
		callback.doWithStream(datums, meta, streamId, results);
	}

	private void assertAgg(String prefix, AggregateDatum result, AggregateDatum expected) {
		assertThat(prefix + " stream ID matches", result.getStreamId(), equalTo(expected.getStreamId()));
		assertThat(prefix + " timestamp", result.getTimestamp(), equalTo(expected.getTimestamp()));
		assertThat(prefix + " instantaneous", result.getProperties().getInstantaneous(),
				arrayContaining(expected.getProperties().getInstantaneous()));
		assertThat(prefix + " accumulating", result.getProperties().getAccumulating(),
				arrayContaining(expected.getProperties().getAccumulating()));
		assertThat(prefix + " stats instantaneous", result.getStatistics().getInstantaneous(),
				arrayContaining(expected.getStatistics().getInstantaneous()));
	}

	private static AggregateDatumEntity agg(UUID streamId, ZonedDateTime date, Aggregation agg,
			BigDecimal[] inst, BigDecimal[] acc, BigDecimal[][] stat_inst) {
		return new AggregateDatumEntity(streamId, date.toInstant(), agg,
				propertiesOf(inst, acc, null, null), statisticsOf(stat_inst, null));
	}

	@Test
	public void imperfectHour_10min() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-datum-31.txt", start, start.plusHours(1), Aggregation.TenMinute,
				new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned for 10min slots in hour", results, hasSize(6));

						assertAgg("10min slot 1", results.get(0),
								agg(streamId, start.plusSeconds(600 * 0), Aggregation.TenMinute,
										decimalArray("1.35", "2.35"), decimalArray("28.125"),
										new BigDecimal[][] { decimalArray("6", "1.1", "1.6"),
												decimalArray("6", "2.1", "2.6") }));
						assertAgg("10min slot 2", results.get(1),
								agg(streamId, start.plusSeconds(600 * 1), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("28.035714286"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 3", results.get(2),
								agg(streamId, start.plusSeconds(600 * 2), Aggregation.TenMinute,
										decimalArray("1.45", "2.45"), decimalArray("28.214285714"),
										new BigDecimal[][] { decimalArray("6", "1.2", "1.7"),
												decimalArray("6", "2.2", "2.7") }));
						assertAgg("10min slot 4", results.get(3),
								agg(streamId, start.plusSeconds(600 * 3), Aggregation.TenMinute,
										decimalArray("1.4", "2.4"), decimalArray("27.5"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 5", results.get(4),
								agg(streamId, start.plusSeconds(600 * 4), Aggregation.TenMinute,
										decimalArray("1.525", "2.525"), decimalArray("37"),
										new BigDecimal[][] { decimalArray("8", "1.0", "1.9"),
												decimalArray("8", "2.0", "2.9") }));
						assertAgg("10min slot 6", results.get(5),
								agg(streamId, start.plusSeconds(600 * 5), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("43.882352941"),
										new BigDecimal[][] { decimalArray("9", "1.1", "1.9"),
												decimalArray("9", "2.1", "2.9") }));
					}
				});
	}

	@Test
	public void reset_inMiddle_10min() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-32.txt", start, start.plusHours(1),
				Aggregation.TenMinute, new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned for 10min slots in hour", results, hasSize(6));

						assertAgg("10min slot 1", results.get(0),
								agg(streamId, start.plusSeconds(600 * 0), Aggregation.TenMinute,
										decimalArray("1.35", "2.35"), decimalArray("28.125"),
										new BigDecimal[][] { decimalArray("6", "1.1", "1.6"),
												decimalArray("6", "2.1", "2.6") }));
						assertAgg("10min slot 2", results.get(1),
								agg(streamId, start.plusSeconds(600 * 1), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("28.035714286"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 3", results.get(2),
								agg(streamId, start.plusSeconds(600 * 2), Aggregation.TenMinute,
										decimalArray("1.45", "2.45"), decimalArray("28.214285714"),
										new BigDecimal[][] { decimalArray("6", "1.2", "1.7"),
												decimalArray("6", "2.2", "2.7") }));
						assertAgg("10min slot 4", results.get(3),
								agg(streamId, start.plusSeconds(600 * 3), Aggregation.TenMinute,
										decimalArray("1.4", "2.4"), decimalArray("27.5"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 5", results.get(4),
								agg(streamId, start.plusSeconds(600 * 4), Aggregation.TenMinute,
										decimalArray("1.525", "2.525"), decimalArray("187"),
										new BigDecimal[][] { decimalArray("8", "1.0", "1.9"),
												decimalArray("8", "2.0", "2.9") }));
						assertAgg("10min slot 6", results.get(5),
								agg(streamId, start.plusSeconds(600 * 5), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("438.823529412"),
										new BigDecimal[][] { decimalArray("9", "1.1", "1.9"),
												decimalArray("9", "2.1", "2.9") }));
					}
				});
	}

	@Test
	public void reset_exactlyAtStart_10min() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-33.txt", start, start.plusHours(1),
				Aggregation.TenMinute, new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned for 10min slots in hour", results, hasSize(6));

						assertAgg("10min slot 1", results.get(0),
								agg(streamId, start.plusSeconds(600 * 0), Aggregation.TenMinute,
										decimalArray("1.35", "2.35"), decimalArray("28.125"),
										new BigDecimal[][] { decimalArray("6", "1.1", "1.6"),
												decimalArray("6", "2.1", "2.6") }));
						assertAgg("10min slot 2", results.get(1),
								agg(streamId, start.plusSeconds(600 * 1), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("28.035714286"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 3", results.get(2),
								agg(streamId, start.plusSeconds(600 * 2), Aggregation.TenMinute,
										decimalArray("1.45", "2.45"), decimalArray("28.214285714"),
										new BigDecimal[][] { decimalArray("6", "1.2", "1.7"),
												decimalArray("6", "2.2", "2.7") }));
						assertAgg("10min slot 4", results.get(3),
								agg(streamId, start.plusSeconds(600 * 3), Aggregation.TenMinute,
										decimalArray("1.4", "2.4"), decimalArray("24.5"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 5", results.get(4),
								agg(streamId, start.plusSeconds(600 * 4), Aggregation.TenMinute,
										decimalArray("1.525", "2.525"), decimalArray("370"),
										new BigDecimal[][] { decimalArray("8", "1.0", "1.9"),
												decimalArray("8", "2.0", "2.9") }));
						assertAgg("10min slot 6", results.get(5),
								agg(streamId, start.plusSeconds(600 * 5), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("438.823529412"),
										new BigDecimal[][] { decimalArray("9", "1.1", "1.9"),
												decimalArray("9", "2.1", "2.9") }));
					}
				});
	}

	@Test
	public void reset_twoInMiddle_10min() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		loadStreamWithAuxiliaryAndRollup("test-datum-34.txt", start, start.plusHours(1),
				Aggregation.TenMinute, new RollupCallback() {

					@Override
					public void doWithStream(List<GeneralNodeDatum> datums,
							Map<NodeSourcePK, NodeDatumStreamMetadata> meta, UUID streamId,
							List<AggregateDatum> results) {
						assertThat("Agg result returned for 10min slots in hour", results, hasSize(6));

						assertAgg("10min slot 1", results.get(0),
								agg(streamId, start.plusSeconds(600 * 0), Aggregation.TenMinute,
										decimalArray("1.35", "2.35"), decimalArray("28.125"),
										new BigDecimal[][] { decimalArray("6", "1.1", "1.6"),
												decimalArray("6", "2.1", "2.6") }));
						assertAgg("10min slot 2", results.get(1),
								agg(streamId, start.plusSeconds(600 * 1), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("28.035714286"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 3", results.get(2),
								agg(streamId, start.plusSeconds(600 * 2), Aggregation.TenMinute,
										decimalArray("1.45", "2.45"), decimalArray("28.214285714"),
										new BigDecimal[][] { decimalArray("6", "1.2", "1.7"),
												decimalArray("6", "2.2", "2.7") }));
						assertAgg("10min slot 4", results.get(3),
								agg(streamId, start.plusSeconds(600 * 3), Aggregation.TenMinute,
										decimalArray("1.4", "2.4"), decimalArray("27.5"),
										new BigDecimal[][] { decimalArray("5", "1.0", "1.9"),
												decimalArray("5", "2.0", "2.9") }));
						assertAgg("10min slot 5", results.get(4),
								agg(streamId, start.plusSeconds(600 * 4), Aggregation.TenMinute,
										decimalArray("1.525", "2.525"), decimalArray("99"),
										new BigDecimal[][] { decimalArray("8", "1.0", "1.9"),
												decimalArray("8", "2.0", "2.9") }));
						assertAgg("10min slot 6", results.get(5),
								agg(streamId, start.plusSeconds(600 * 5), Aggregation.TenMinute,
										decimalArray("1.5", "2.5"), decimalArray("43.882352941"),
										new BigDecimal[][] { decimalArray("9", "1.1", "1.9"),
												decimalArray("9", "2.1", "2.9") }));
					}
				});
	}
}
