/* ==================================================================
 * DbFindDatumAroundTests.java - 15/12/2022 9:10:57 am
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the "find datum around" database stored procedures.
 * 
 * @author matt
 * @version 1.1
 */
public class DbFindDatumAroundTests extends BaseDatumJdbcTestSupport {

	private static final String TOLERANCE_3MO = "3 months";

	private List<Datum> loadCsvStream(String resource, ObjectDatumStreamMetadata meta) {
		List<Datum> datum = datumResourceToList(getClass(), resource, staticProvider(singleton(meta)));
		log.debug("Got test data:\n{}", datum.stream().map(Object::toString).collect(joining("\n")));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);
		return datum;
	}

	private List<Datum> findAround(UUID streamId, Instant ts, String tolerance) {
		return jdbcTemplate.query("select * from solardatm.find_datm_around(?::uuid,?,?::interval)",
				DatumEntityRowMapper.INSTANCE, streamId.toString(), Timestamp.from(ts), tolerance);
	}

	private List<Datum> findAroundWithAccumulation(UUID streamId, Instant ts, String tolerance) {
		return jdbcTemplate.query("select * from solardatm.find_datm_around(?::uuid,?,?::interval,TRUE)",
				DatumEntityRowMapper.INSTANCE, streamId.toString(), Timestamp.from(ts), tolerance);
	}

	@Test
	public void exact() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAround(streamId, start.toInstant(), TOLERANCE_3MO);

		// THEN
		assertThat("Single datum returned for exact match", result, hasSize(1));
		assertThat("Datum is for given timestamp", result.get(0).getTimestamp(),
				is(equalTo(start.toInstant())));
	}

	@Test
	public void around() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 2, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAround(streamId, start.toInstant(), TOLERANCE_3MO);

		// THEN
		assertThat("Before/after datum returned for inexact match", result, hasSize(2));
		assertThat("Datum 1 is latest before", result.get(0).getTimestamp(),
				is(equalTo(start.toInstant().minusSeconds(60))));
		assertThat("Datum 2 is latest before", result.get(1).getTimestamp(),
				is(equalTo(start.toInstant().plusSeconds(60))));
	}

	@Test
	public void noneEarlier() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 10, 31, 0, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAround(streamId, start.toInstant(), TOLERANCE_3MO);

		// THEN
		assertThat("Single datum returned for earliest available row", result, hasSize(1));
		ZonedDateTime expected = ZonedDateTime.of(2022, 10, 31, 11, 28, 0, 0, ZoneOffset.UTC);
		assertThat("Datum is for earliest available", result.get(0).getTimestamp(),
				is(equalTo(expected.toInstant())));
	}

	@Test
	public void noneLater() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 22, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAround(streamId, start.toInstant(), TOLERANCE_3MO);

		// THEN
		assertThat("Single datum returned for earliest available row", result, hasSize(1));
		ZonedDateTime expected = ZonedDateTime.of(2022, 11, 9, 21, 1, 0, 0, ZoneOffset.UTC);
		assertThat("Datum is for latest available", result.get(0).getTimestamp(),
				is(equalTo(expected.toInstant())));
	}

	@Test
	public void nothingWithinTolerance() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "volume", "volume_less_guest" },
				null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-05-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 5, 0, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAround(streamId, start.toInstant(), "1 day");

		// THEN
		assertThat("No datum returned", result, hasSize(0));
	}

	@Test
	public void misingAccumulationProperties() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "flow" },
				new String[] { "volume", "volume_less_guest" }, null);
		final UUID streamId = meta.getStreamId();
		loadCsvStream("sample-raw-data-07-perfect-minutes.csv", meta);

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2022, 11, 9, 1, 0, 0, 0, ZoneOffset.UTC);
		List<Datum> result = findAroundWithAccumulation(streamId, start.toInstant(), "P1Y");

		// THEN
		assertThat("Surrounding rows with accumulation properties present returned", result, hasSize(2));

		ZonedDateTime expected = ZonedDateTime.of(2022, 10, 31, 11, 28, 0, 0, ZoneOffset.UTC);
		assertThat("Datum 1 is for latest available with accumulation properties present",
				result.get(0).getTimestamp(), is(equalTo(expected.toInstant())));
		expected = ZonedDateTime.of(2022, 11, 9, 1, 24, 0, 0, ZoneOffset.UTC);
		assertThat("Datum 2 is for earliest available with accumulation properties present",
				result.get(1).getTimestamp(), is(equalTo(expected.toInstant())));
	}

}
