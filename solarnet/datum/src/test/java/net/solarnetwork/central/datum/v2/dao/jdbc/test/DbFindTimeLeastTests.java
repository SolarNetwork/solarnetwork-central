/* ==================================================================
 * DbFindTimeGreatestTests.java - 13/11/2020 12:15:16 pm
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatum;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the "least recent" datum database stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindTimeLeastTests extends BaseDatumJdbcTestSupport {

	private List<Datum> findTimeLeast(UUID[] streamIds) {
		return jdbcTemplate.execute(new ConnectionCallback<List<Datum>>() {

			@Override
			public List<Datum> doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Finding time least for streams {}", Arrays.toString(streamIds));
				List<Datum> result = new ArrayList<>(streamIds.length);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_time_least(?::uuid[])}")) {
					Array array = con.createArrayOf("uuid", streamIds);
					stmt.setArray(1, array);
					array.free();
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								Datum d = DatumEntityRowMapper.INSTANCE.mapRow(rs, ++i);
								result.add(d);
							}
						}
					}
				}
				log.debug("Found time least for streams:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	@Test
	public void findTimeLeast_noData() {
		// GIVEN

		// WHEN
		List<Datum> results = findTimeLeast(new UUID[] { UUID.randomUUID() });

		// THEN
		assertThat("No result from no data", results, hasSize(0));
	}

	@Test
	public void findTimeLeast_oneStream() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		List<Datum> results = findTimeLeast(new UUID[] { streamId });

		// THEN
		assertThat("Result for one stream", results, hasSize(1));

		ZonedDateTime min = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertDatum("Greatest result returned", results.get(0),
				new DatumEntity(streamId, min.toInstant(), null,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));
	}

	@Test
	public void findTimeLeast_twoStreams() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums_a = loadJsonDatumResource("test-datum-01.txt", getClass());
		List<GeneralNodeDatum> datums_b = loadJsonDatumResource("test-datum-02.txt", getClass());
		datums_b.stream().forEach(d -> {
			d.setSourceId("b");
		});
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate,
				concat(datums_a.stream(), datums_b.stream()).collect(toList()), "UTC");
		UUID streamId_a = null;
		UUID streamId_b = null;
		for ( ObjectDatumStreamMetadata meta : metas.values() ) {
			if ( meta.getSourceId().equals("a") ) {
				streamId_a = meta.getStreamId();
			} else if ( meta.getSourceId().equals("b") ) {
				streamId_b = meta.getStreamId();
			}
		}

		// WHEN
		List<Datum> results = findTimeLeast(new UUID[] { streamId_a, streamId_b });

		// THEN
		assertThat("Results for two streams", results, hasSize(2));

		Datum datum_a = null;
		Datum datum_b = null;
		for ( Datum d : results ) {
			if ( d.getStreamId().equals(streamId_a) ) {
				datum_a = d;
			} else if ( d.getStreamId().equals(streamId_b) ) {
				datum_b = d;
			}
		}

		ZonedDateTime tsMax_a = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertDatum("Greatest result returned stream A", datum_a,
				new DatumEntity(streamId_a, tsMax_a.toInstant(), null,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));

		ZonedDateTime tsMax_b = ZonedDateTime.of(2020, 6, 1, 11, 59, 0, 0, ZoneOffset.UTC);
		assertDatum("Greatest result returned stream B", datum_b,
				new DatumEntity(streamId_b, tsMax_b.toInstant(), null,
						propertiesOf(decimalArray("1.1", "1.1"), decimalArray("100"), null, null)));
	}

	@Test
	public void findTimeLeast_twoStreams_oneFound() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums_a = loadJsonDatumResource("test-datum-01.txt", getClass());
		List<GeneralNodeDatum> datums_b = loadJsonDatumResource("test-datum-02.txt", getClass());
		datums_b.stream().forEach(d -> {
			d.setSourceId("b");
		});
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate,
				concat(datums_a.stream(), datums_b.stream()).collect(toList()), "UTC");
		UUID streamId_a = null;
		for ( ObjectDatumStreamMetadata meta : metas.values() ) {
			if ( meta.getSourceId().equals("a") ) {
				streamId_a = meta.getStreamId();
			}
		}

		// WHEN
		List<Datum> results = findTimeLeast(new UUID[] { streamId_a, UUID.randomUUID() });

		// THEN
		assertThat("Results for one stream given two in criteria", results, hasSize(1));

		Datum datum_a = null;
		for ( Datum d : results ) {
			if ( d.getStreamId().equals(streamId_a) ) {
				datum_a = d;
			}
		}

		ZonedDateTime tsMax_a = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertDatum("Greatest result returned stream A", datum_a,
				new DatumEntity(streamId_a, tsMax_a.toInstant(), null,
						propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));
	}

}
