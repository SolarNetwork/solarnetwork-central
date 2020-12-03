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
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
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
				UUID streamId, List<AggregateDatum> results, List<AggregateDatum> fnResults);
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
				"select * from solardatm.rollup_agg_data_for_time_span(?::uuid,?,?,?)",
				AggregateDatumEntityRowMapper.INSTANCE, streamId.toString(),
				Timestamp.from(aggStart.toInstant()), Timestamp.from(aggEnd.toInstant()), kind.getKey());
		List<AggregateDatum> fnResults = executeAggregateRollup(streamId, kind, aggStart, aggEnd);
		callback.doWithStream(datums, meta, streamId, results, fnResults);
	}

	private List<AggregateDatum> executeAggregateRollup(UUID streamId, Aggregation kind,
			ZonedDateTime aggStart, ZonedDateTime aggEnd) {
		final String table = (kind == Aggregation.Month ? "monthly"
				: kind == Aggregation.Day ? "daily" : "hourly");
		// @formatter:off
		List<AggregateDatum> results = jdbcTemplate.query(
				"SELECT datum.stream_id,\n"
				+ "		?::timestamptz,\n"
				+ "		(solardatm.rollup_agg_data(\n"
				+ "			(datum.data_i, datum.data_a, datum.data_s, datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_data\n"
				+ "		ORDER BY datum.ts_start)).*\n"
				+ "FROM solardatm.agg_datm_" +table +" datum\n"
				+ "WHERE datum.stream_id = ?::uuid AND datum.ts_start >= ? AND datum.ts_start < ?\n"
				+ "GROUP BY datum.stream_id\n"
				+ "HAVING count(*) > 0",
				AggregateDatumEntityRowMapper.INSTANCE, 
				Timestamp.from(aggStart.toInstant()), 
				streamId.toString(),
				Timestamp.from(aggStart.toInstant()), 
				Timestamp.from(aggEnd.toInstant()));
		// @formatter:on
		return results;
	}

	@Test
	public void regularHour() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-01.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results,
							List<AggregateDatum> fnResults) {
						assertThat("Agg result returned", results, hasSize(1));
						assertThat("Agg function result returned", results, hasSize(results.size()));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						AggregateDatum expected = new AggregateDatumEntity(streamId, start.toInstant(),
								Aggregation.Hour,
								propertiesOf(decimalArray("1.55", "5.6"), decimalArray("3600"), null,
										null),
								statisticsOf(
										new BigDecimal[][] { decimalArray("48", "1.1", "3.8"),
												decimalArray("48", "2.0", "7.8") },
										new BigDecimal[][] { decimalArray("100", "928", "828") }));

						assertAggregateDatum("Function results same", results.get(0), expected);
						assertAggregateDatum("Function results same", fnResults.get(0), expected);
					}
				});

	}

	@Test
	public void oneRow() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-02.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results,
							List<AggregateDatum> fnResults) {
						assertThat("Agg result returned", results, hasSize(1));
						assertThat("Agg function result returned", results, hasSize(results.size()));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						AggregateDatum expected = new AggregateDatumEntity(streamId, start.toInstant(),
								Aggregation.Hour,
								propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null,
										null),
								statisticsOf(
										new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
												decimalArray("6", "2.0", "7.1") },
										new BigDecimal[][] { decimalArray("100", "201", "101") }));

						assertAggregateDatum("Function results same", results.get(0), expected);
						assertAggregateDatum("Function results same", fnResults.get(0), expected);
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
							UUID streamId, List<AggregateDatum> results,
							List<AggregateDatum> fnResults) {
						assertThat("Agg result returned", results, hasSize(0));
						assertThat("Agg function result returned", results, hasSize(results.size()));
					}
				});
	}

	@Test
	public void noAccumulatingData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-04.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results,
							List<AggregateDatum> fnResults) {
						assertThat("Agg result returned", results, hasSize(1));
						assertThat("Agg function result returned", results, hasSize(results.size()));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						AggregateDatum expected = new AggregateDatumEntity(streamId, start.toInstant(),
								Aggregation.Hour,
								propertiesOf(decimalArray("1.55", "5.6"), null, null, null),
								statisticsOf(new BigDecimal[][] { decimalArray("48", "1.1", "3.8"),
										decimalArray("48", "2.0", "7.8") }, null));

						assertAggregateDatum("Function results same", results.get(0), expected);
						assertAggregateDatum("Function results same", fnResults.get(0), expected);
					}
				});
	}

	@Test
	public void inconsistentData() throws IOException {
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		loadStreamAndRollup("test-agg-hour-datum-05.txt", Aggregation.Hour, start, start.plusHours(24),
				new RollupCallback() {

					@Override
					public void doWithStream(List<AggregateDatum> datums, ObjectDatumStreamMetadata meta,
							UUID streamId, List<AggregateDatum> results,
							List<AggregateDatum> fnResults) {
						assertThat("Agg result returned", results, hasSize(1));
						assertThat("Agg function result returned", results, hasSize(results.size()));

						AggregateDatum result = results.get(0);
						log.debug("Got result: {}", result);

						AggregateDatum expected = new AggregateDatumEntity(streamId, start.toInstant(),
								Aggregation.Hour,
								propertiesOf(decimalArray("1.5", "5.266666667", "60.1"),
										decimalArray("2700", "2400"), null, null),
								statisticsOf(
										new BigDecimal[][] { decimalArray("42", "1.1", "3.7"),
												decimalArray("36", "2.0", "7.7"),
												decimalArray("18", "40.0", "100.1") },
										new BigDecimal[][] { decimalArray("100", "928", "621"),
												decimalArray("1000", "3402", "2402") }));

						assertAggregateDatum("Function results same", results.get(0), expected);
						assertAggregateDatum("Function results same", fnResults.get(0), expected);
					}
				});
	}

}
