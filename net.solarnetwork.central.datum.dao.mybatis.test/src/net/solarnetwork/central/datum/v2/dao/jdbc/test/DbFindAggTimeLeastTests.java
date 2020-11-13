/* ==================================================================
 * DbFindTimeLeastTests.java - 13/11/2020 12:15:16 pm
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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the "most recent" datum database stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindAggTimeLeastTests extends BaseDatumJdbcTestSupport {

	private List<AggregateDatumEntity> findAggTimeLeast(UUID[] streamIds, Aggregation kind) {
		return jdbcTemplate.execute(new ConnectionCallback<List<AggregateDatumEntity>>() {

			@Override
			public List<AggregateDatumEntity> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding time greatest for streams {}", Arrays.toString(streamIds));
				List<AggregateDatumEntity> result = new ArrayList<>(streamIds.length);
				RowMapper<AggregateDatumEntity> mapper = new AggregateDatumEntityRowMapper(kind);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_agg_time_least(?::uuid[],?)}")) {
					Array array = con.createArrayOf("uuid", streamIds);
					stmt.setArray(1, array);
					array.free();
					stmt.setString(2, kind.getKey());
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								AggregateDatumEntity d = mapper.mapRow(rs, ++i);
								result.add(d);
							}
						}
					}
				}
				log.debug("Found agg time greatest for streams:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), nodeId, sourceId,
				new String[] { "x", "y" }, new String[] { "w" }, null);
	}

	private static final Set<Aggregation> aggs() {
		return new LinkedHashSet<>(Arrays.asList(Aggregation.Hour, Aggregation.Day, Aggregation.Month));
	}

	@Test
	public void findAggTimeLeast_noData() {
		// GIVEN
		UUID streamId = UUID.randomUUID();

		// WHEN
		for ( Aggregation kind : aggs() ) {
			List<AggregateDatumEntity> results = findAggTimeLeast(new UUID[] { streamId }, kind);

			// THEN
			assertThat(format("No %s result from no data", kind), results, hasSize(0));
		}
	}

	@Test
	public void findAggTimeLeast_hour_oneStream() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		List<AggregateDatumEntity> results = findAggTimeLeast(new UUID[] { streamId }, Aggregation.Hour);

		// THEN
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertThat("Result for one stream", results, hasSize(1));
		assertAggregateDatum("Least result returned", results.get(0),
				new AggregateDatumEntity(streamId, date.toInstant(), Aggregation.Hour,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
										decimalArray("6", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray("100", "200", "100") })));
	}

	@Test
	public void findAggTimeLeast_day_oneStream() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-day-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		List<AggregateDatumEntity> results = findAggTimeLeast(new UUID[] { streamId }, Aggregation.Day);

		// THEN
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertThat("Result for one stream", results, hasSize(1));
		assertAggregateDatum("Least result returned", results.get(0),
				new AggregateDatumEntity(streamId, date.toInstant(), Aggregation.Day,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
										decimalArray("6", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray("100", "200", "100") })));
	}

	@Test
	public void findAggTimeLeast_month_oneStream() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-month-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		List<AggregateDatumEntity> results = findAggTimeLeast(new UUID[] { streamId },
				Aggregation.Month);

		// THEN
		ZonedDateTime date = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertThat("Result for one stream", results, hasSize(1));
		assertAggregateDatum("Least result returned", results.get(0),
				new AggregateDatumEntity(streamId, date.toInstant(), Aggregation.Month,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
										decimalArray("6", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray("100", "200", "100") })));
	}

	@Test
	public void findAggTimeLeast_hour_twoStreams() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta_a = testStreamMetadata(1L, "a");
		BasicNodeDatumStreamMetadata meta_b = testStreamMetadata(1L, "b");
		ObjectDatumStreamMetadataProvider metaProvider = staticProvider(asList(meta_a, meta_b));

		List<AggregateDatum> datums_a = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), metaProvider);
		List<AggregateDatum> datums_b = loadJsonAggregateDatumResource("test-agg-hour-datum-02.txt",
				getClass(), metaProvider, e -> {
					// map data to stream b
					return new AggregateDatumEntity(meta_b.getStreamId(), e.getTimestamp(),
							e.getAggregation(), e.getProperties(), e.getStatistics());
				});
		insertAggregateDatum(log, jdbcTemplate, datums_a);
		insertAggregateDatum(log, jdbcTemplate, datums_b);

		// WHEN
		List<AggregateDatumEntity> results = findAggTimeLeast(
				new UUID[] { meta_a.getStreamId(), meta_b.getStreamId() }, Aggregation.Hour);

		// THEN
		assertThat("Results for two streams", results, hasSize(2));

		AggregateDatumEntity datum_a = null;
		AggregateDatumEntity datum_b = null;
		for ( AggregateDatumEntity d : results ) {
			if ( d.getStreamId().equals(meta_a.getStreamId()) ) {
				datum_a = d;
			} else if ( d.getStreamId().equals(meta_b.getStreamId()) ) {
				datum_b = d;
			}
		}

		ZonedDateTime date_a = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertAggregateDatum("Least result returned A", datum_a,
				new AggregateDatumEntity(meta_a.getStreamId(), date_a.toInstant(), Aggregation.Hour,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
										decimalArray("6", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray("100", "200", "100") })));

		ZonedDateTime date_b = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertAggregateDatum("Least result returned B", datum_b,
				new AggregateDatumEntity(meta_b.getStreamId(), date_b.toInstant(), Aggregation.Hour,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("6", "1.1", "3.1"),
										decimalArray("6", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray("100", "201", "101") })));
	}

}
