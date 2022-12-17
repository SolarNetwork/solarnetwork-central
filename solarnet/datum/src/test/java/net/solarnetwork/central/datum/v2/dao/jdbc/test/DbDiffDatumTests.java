/* ==================================================================
 * DbDiffDatumTests.java - 17/11/2020 7:32:43 pm
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertOneDatumStreamWithAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.readingWith;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertReadingDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.ReadingDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@literal solardatm.diff_datm} database stored procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDiffDatumTests extends BaseDatumJdbcTestSupport {

	private ReadingDatum calcDiffDatum(UUID streamId, Instant from, Instant to) {
		return jdbcTemplate.execute(new ConnectionCallback<ReadingDatum>() {

			@Override
			public ReadingDatum doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement stmt = con.prepareStatement("""
						SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*
						FROM solardatm.find_datm_diff_rows(?::uuid,?,?) d
						HAVING (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).stream_id IS NOT NULL
						""")) {
					log.debug("Calculating datum diff {} from {} - {}", streamId, from, to);
					stmt.setString(1, streamId.toString());
					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							if ( rs.next() ) {
								ReadingDatum d = ReadingDatumEntityRowMapper.INSTANCE.mapRow(rs, 1);
								log.debug("Calculated datum diff: {}", d);
								return d;
							}
						}
					}
					return null;
				}
			}
		});
	}

	@Test
	public void calcDiffDatum_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		ReadingDatum d = calcDiffDatum(UUID.randomUUID(), start.toInstant(),
				start.plusHours(1).toInstant());

		// THEN
		assertThat("No result from no input", d, nullValue());
	}

	@Test
	public void calcDiffDatum_typical() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-02.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Typical readings just before start/end range", result, readingWith(streamId,
				null, start.minusMinutes(1), end.minusMinutes(1), decimalArray("30", "100", "130")));
	}

	@Test
	public void calcDiffDatum_nullProperties() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-38.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Null properties preserved within start/end range", result,
				readingWith(streamId, null, start.minusMinutes(1), end.minusMinutes(1),
						decimalArray(null, null, null), decimalArray("250", "1050", "1300")));
	}

	@Test
	public void calcDiffDatum_oneResetExactlyAtStart() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-15.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("One reset exactly at start", result,
				readingWith(streamId, null, start, end.minusMinutes(1), decimalArray("30", "10", "40")));
	}

	@Test
	public void calcDiffDatum_oneResetInMiddle() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-16.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("One reset in middle", result, readingWith(streamId, null,
				start.minusMinutes(1), end.minusMinutes(1), decimalArray("35", "100", "25")));
	}

	@Test
	public void calcDiffDatum_oneResetExactlyAtEnd() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-17.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("One reset exactly at end", result,
				readingWith(streamId, null, start.minusMinutes(1), end, decimalArray("31", "10", "41")));
	}

	@Test
	public void calcDiffDatum_oneResetExactlyAtStartWithoutLeading() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-18.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("One reset exactly at start without leading", result,
				readingWith(streamId, null, start, end.minusMinutes(1), decimalArray("30", "10", "40")));
	}

	@Test
	public void resetRecord_oneResetExactlyAtEndWithoutTrailing() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-19.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("One reset exactly at end without leading", result,
				readingWith(streamId, null, start.minusMinutes(1), end, decimalArray("31", "10", "41")));
	}

	@Test
	public void resetRecord_multiResetsWithin() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-20.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Multi resets within", result, readingWith(streamId, null,
				start.minusMinutes(1), end.minusMinutes(1), decimalArray("36", "100", "210")));
	}

	@Test
	public void resetRecord_adjacentResetsWithin() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-21.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Adjacent resets within", result, readingWith(streamId, null,
				start.minusMinutes(1), end.minusMinutes(1), decimalArray("36", "100", "220")));
	}

	@Test
	public void resetRecord_resetsExactlyAtStartAndEnd() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-22.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Resets exactly at start/end", result,
				readingWith(streamId, null, start, end, decimalArray("31", "10", "41")));
	}

	@Test
	public void resetRecord_resetsExactlyAtStartAndEndWithoutLeadingOrTrailing() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-23.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Resets exactly at start/end without leading/trailing", result,
				readingWith(streamId, null, start, end, decimalArray("31", "10", "41")));
	}

	@Test
	public void resetRecord_onlyOneReset() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-24.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		ZonedDateTime expected = end.minusMinutes(40);
		assertReadingDatum("Only one reset with way-back start", result,
				readingWith(streamId, null, expected, expected, decimalArray("0", "5", "5")));
	}

	@Test
	public void resetRecord_twoResetInMiddle() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-25.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Two reset with way-back start", result, readingWith(streamId, null,
				end.minusMinutes(40), end.minusMinutes(20), decimalArray("5", "5", "200")));
	}

	@Test
	public void resetRecord_resetJustBeforeStart() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-26.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Reset just before start", result, readingWith(streamId, null,
				start.minusMinutes(1), end.minusMinutes(1), decimalArray("30", "10", "40")));
	}

	@Test
	public void resetRecord_resetJustAfterEnd() throws IOException {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-27.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Reset just after end", result, readingWith(streamId, null,
				start.minusMinutes(1), end.minusMinutes(1), decimalArray("30", "10", "40")));
	}

	@Test
	public void calcDiffDatum_leadingWayBack() {
		// GIVEN
		UUID streamId = insertOneDatumStreamWithAuxiliary(log, jdbcTemplate, "test-datum-35.txt",
				getClass(), "UTC");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Leading long before start used", result,
				readingWith(streamId, null, start.minusMonths(4).minusMinutes(1), end.minusMinutes(1),
						decimalArray("30", "100", "130")));
	}

	@Test
	public void calcDiffDatum_perfectHourlyData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-04.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		// 2017-07-04T09:00:00.000Z - 2017-07-04T10:00:00.000Z
		ZonedDateTime start = ZonedDateTime.of(2017, 7, 4, 9, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertReadingDatum("Hour prior used because of perfect hourly data", result, readingWith(
				streamId, null, start, end, decimalArray("37", "12476432001", "12476432038")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_imperfectHours() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have perfect hour data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 1, 59, 0, 0, ZoneOffset.UTC);
		// end falls on perfect hour
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 2, 59, 0, 0, ZoneOffset.UTC);
		assertReadingDatum(
				"Hour with non-perfect start and perfect end uses prior minute start and perfect end",
				result, readingWith(streamId, null, expectedStart, expectedEnd,
						decimalArray("0", "45804", "45804"), decimalArray("0", "41005", "41005")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_sparseData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 1, 59, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("Sparse data:", result, readingWith(streamId, null, expectedStart,
				expectedEnd, decimalArray("0", "45804", "45804"), decimalArray("0", "41005", "41005")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_beforeMissingData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 3, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 1, 59, 0, 0, ZoneOffset.UTC);
		// no other data after start within end range, so end is start
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 2, 59, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("Before missing data:", result, readingWith(streamId, null, expectedStart,
				expectedEnd, decimalArray("0", "45804", "45804"), decimalArray("0", "41005", "41005")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_startOfMissingData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 3, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 4, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 2, 59, 0, 0, ZoneOffset.UTC);
		// no other data after start within end range, so end is start
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 2, 59, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("Start of missing data:", result, readingWith(streamId, null, expectedStart,
				expectedEnd, decimalArray("0", "45804", "45804"), decimalArray("0", "41005", "41005")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_afterMissingDataPerfectHourEnd() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 18, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 2, 59, 0, 0, ZoneOffset.UTC);
		// have perfect end date
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("After missing data perfect hour end:", result,
				readingWith(streamId, null, expectedStart, expectedEnd,
						decimalArray("132", "45804", "45936"), decimalArray("132", "41005", "41137")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_perfectHourStartAndEnd() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have perfect hour data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 19, 0, 0, 0, ZoneOffset.UTC);
		// end falls on perfect hour, so goes to prior minute
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("Hour with perfect start and perfect end uses perfect start and perfect end:",
				result, readingWith(streamId, null, expectedStart, expectedEnd,
						decimalArray("0", "45936", "45936"), decimalArray("0", "41137", "41137")));
	}

	@Test
	public void calcDiffDatum_perfectMinutelyData_perfectHourStart() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		List<Datum> datum = datumResourceToList(getClass(), "sample-raw-data-05-perfect-minutes.csv",
				staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = ZonedDateTime.of(2022, 11, 9, 21, 0, 0, 0, ZoneOffset.UTC);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		// start does not have perfect hour data, so goes to prior minute
		ZonedDateTime expectedStart = ZonedDateTime.of(2022, 11, 9, 20, 0, 0, 0, ZoneOffset.UTC);
		// end falls on perfect hour, so goes to prior minute
		ZonedDateTime expectedEnd = ZonedDateTime.of(2022, 11, 9, 20, 59, 0, 0, ZoneOffset.UTC);
		assertReadingDatum("Hour with perfect start:", result, readingWith(streamId, null, expectedStart,
				expectedEnd, decimalArray("2", "45936", "45938"), decimalArray("2", "41137", "41139")));
	}

}
