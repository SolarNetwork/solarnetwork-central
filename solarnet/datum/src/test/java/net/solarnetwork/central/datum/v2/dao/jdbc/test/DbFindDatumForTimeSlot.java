/* ==================================================================
 * DbFindDatumForTimeSpan.java - 15/12/2022 2:02:00 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the "find datum for time span" database stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindDatumForTimeSpan extends BaseDatumJdbcTestSupport {

	private static final String TOLERANCE_3MO = "3 months";

	private List<Datum> loadCsvStream(String resource, ObjectDatumStreamMetadata meta) {
		List<Datum> datum = datumResourceToList(getClass(), resource, staticProvider(singleton(meta)));
		log.debug("Got test data:\n{}", datum.stream().map(Object::toString).collect(joining("\n")));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);
		return datum;
	}

	private List<Datum> findDatum(UUID streamId, Instant from, Instant to, String tolerance) {
		return jdbcTemplate.execute(new ConnectionCallback<List<Datum>>() {

			@Override
			public List<Datum> doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement stmt = con.prepareStatement("""
						SELECT
							  d.stream_id
							, d.ts
							, d.ts AS received
							, d.data_i
							, d.data_a
							, d.data_s
							, d.data_t
						FROM solardatm.find_datm_for_time_span(?::uuid,?,?,?::interval) d
						ORDER BY ts
						""")) {
					log.debug("Finding {} datum from {} - {}", streamId, from, to);
					stmt.setString(1, streamId.toString());
					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					stmt.setString(4, tolerance);
					List<Datum> result = new ArrayList<>();
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							while ( rs.next() ) {
								Datum d = DatumEntityRowMapper.INSTANCE.mapRow(rs, 1);
								log.debug("Found datum: {}", d);
								result.add(d);
							}
						}
					}
					return result;
				}
			}
		});
	}

	@Test
	public void perfectHourlyData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);
		loadCsvStream("sample-raw-data-04.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2017, 7, 4, 9, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned on exact hours", result, hasSize(2));
		assertThat("First result", result.get(0).getTimestamp(), is(equalTo(start.toInstant())));
		assertThat("Last result", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(start.plusHours(1).toInstant())));
	}

	@Test
	public void imperfectStartAndEnd() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A",
				new String[] { "watts", "frequency", "voltage_a", "voltage_b", "voltage_c" },
				new String[] { "wattHours" }, null);

		loadCsvStream("sample-raw-data-01.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2019, 8, 26, 7, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned on exact hours", result, hasSize(62));
		assertThat("First result is one before start date", result.get(0).getTimestamp(), is(equalTo(
				DateTimeFormatter.ISO_INSTANT.parse("2019-08-26T06:59:33.011Z", Instant::from))));
		assertThat("Last result is one past end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2019-08-26T08:00:33.014Z",
						Instant::from))));
	}

}
