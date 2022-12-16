/* ==================================================================
 * DbFindDatumForTimeSlot.java - 15/12/2022 2:02:00 pm
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
public class DbFindDatumForTimeSlot extends BaseDatumJdbcTestSupport {

	private static final String TOLERANCE_3MO = "P3M";
	private static final String TARGET_1H = "PT1H";

	private List<Datum> loadCsvStream(String resource, ObjectDatumStreamMetadata meta) {
		List<Datum> datum = datumResourceToList(getClass(), resource, staticProvider(singleton(meta)));
		log.debug("Got test data:\n{}", datum.stream().map(Object::toString).collect(joining("\n")));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);
		return datum;
	}

	private List<Datum> findDatum(UUID streamId, Instant from, Instant to, String tolerance,
			String agg) {
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
							, d.rtype
						FROM solardatm.find_datm_for_time_slot(?::uuid,?,?,?::interval,?::interval) d
						ORDER BY ts
						""")) {
					log.debug("Finding {} datum from {} - {}", streamId, from, to);
					stmt.setString(1, streamId.toString());
					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					stmt.setString(4, tolerance);
					stmt.setString(5, agg);
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
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);
		loadCsvStream("sample-raw-data-04.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2017, 7, 4, 9, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned on exact hours", result, hasSize(2));
		assertThat("First result", result.get(0).getTimestamp(), is(equalTo(start.toInstant())));
		assertThat("Last result", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(start.plusHours(1).toInstant())));
	}

	@Test
	public void imperfectStartAndEnd() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A",
				new String[] { "watts", "frequency", "voltage_a", "voltage_b", "voltage_c" },
				new String[] { "wattHours" }, null);

		loadCsvStream("sample-raw-data-01.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2019, 8, 26, 7, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned on exact hours", result, hasSize(62));
		assertThat("First result is one before start date", result.get(0).getTimestamp(), is(equalTo(
				DateTimeFormatter.ISO_INSTANT.parse("2019-08-26T06:59:33.011Z", Instant::from))));
		assertThat("Last result is one past end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2019-08-26T08:00:33.014Z",
						Instant::from))));
	}

	private ObjectDatumStreamMetadata load_raw05() {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);

		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		return meta;
	}

	@Test
	public void raw05_1031_11() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 10, 31, 11, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(2));
		assertThat("First result is first available", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-10-31T11:28:00Z", Instant::from))));
		assertThat("Last result is one past end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T00:49:00Z", Instant::from))));
	}

	@Test
	public void raw05_1031_12() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("No rows in range", result, hasSize(0));
	}

	@Test
	public void raw05_110823() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 8, 23, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("No rows in range", result, hasSize(0));
	}

	@Test
	public void raw05_1109_00() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 0, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(12));
		assertThat("First result is one before first available", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-10-31T11:28:00Z", Instant::from))));
		assertThat("Last result is one at end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T01:00:00Z", Instant::from))));
	}

	@Test
	public void raw05_1109_01() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(58));
		assertThat("First result is exactly on hour because prior included in previous hour",
				result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T01:00:00Z", Instant::from))));
		assertThat("Last result is one past end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T02:01:00Z", Instant::from))));
	}

	@Test
	public void raw05_1109_02() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(59));
		assertThat("First result is one before start date", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T01:59:00Z", Instant::from))));
		assertThat("Last result is one past end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T19:00:00Z", Instant::from))));
	}

	@Test
	public void raw05_1109_03() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 3, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("No rows in range", result, hasSize(0));
	}

	@Test
	public void raw05_1109_18() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 18, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("No rows in range", result, hasSize(0));
	}

	@Test
	public void raw05_1109_19() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(57));
		assertThat("First result is one before start date", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T02:59:00Z", Instant::from))));
		assertThat("Last result is exact end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T20:00:00Z", Instant::from))));
	}

	@Test
	public void raw05_1109_20() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(56));
		assertThat("First result is exact start date", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T20:00:00Z", Instant::from))));
		assertThat("Last result is one after end date", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T21:01:00Z", Instant::from))));
	}

	@Test
	public void raw05_1109_21() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = load_raw05();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 21, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findDatum(meta.getStreamId(), start.toInstant(),
				start.plusHours(1).toInstant(), TOLERANCE_3MO, TARGET_1H);

		// THEN
		log.debug("Got result: {}", result);
		assertThat("Expected count returned", result, hasSize(2));
		assertThat("First result is one before start date", result.get(0).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T20:59:00Z", Instant::from))));
		assertThat("Last result is last available", result.get(result.size() - 1).getTimestamp(),
				is(equalTo(DateTimeFormatter.ISO_INSTANT.parse("2022-11-09T21:01:00Z", Instant::from))));
	}

}
