/* ==================================================================
 * DbDatumDiffRowsTests.java - 17/11/2020 7:34:57 pm
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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.SORT_TYPED_DATUM_BY_TS;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumAndAuxiliaryResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.TypedDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.TypedDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@code solardatm.find_datm_diff_rows} database stored
 * procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumDiffRowsTests extends BaseDatumJdbcTestSupport {

	private List<TypedDatumEntity> calcDiffDatum(UUID streamId, Instant from, Instant to) {
		return jdbcTemplate.execute(new ConnectionCallback<List<TypedDatumEntity>>() {

			@Override
			public List<TypedDatumEntity> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				List<TypedDatumEntity> result = new ArrayList<>();
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_datm_diff_rows(?::uuid,?,?)}")) {
					log.debug("Querying for stream {} diff rows between {} - {}", streamId, from, to);
					stmt.setString(1, streamId.toString());
					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							while ( rs.next() ) {
								TypedDatumEntity d = TypedDatumEntityRowMapper.INSTANCE.mapRow(rs, 1);
								result.add(d);
							}
						}
					}
				}
				Collections.sort(result, SORT_TYPED_DATUM_BY_TS);
				log.debug("Got diff rows:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	private UUID loadStreamWithAuxiliary(String resource) throws IOException {
		List<?> data = loadJsonDatumAndAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return streamId;
	}

	@Test
	public void calcDiffDatum_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		List<TypedDatumEntity> results = calcDiffDatum(UUID.randomUUID(), start.toInstant(),
				start.plusDays(1).toInstant());

		// THEN
		assertThat("No results from no input", results, hasSize(0));
	}

	@Test
	public void calcDiffDatum_typical() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-02.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		List<TypedDatumEntity> results = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Start/end results returned", results, hasSize(2));
		assertThat("Start", results.get(0).getTimestamp(), equalTo(start.minusMinutes(1).toInstant()));
		assertThat("End", results.get(1).getTimestamp(), equalTo(end.minusMinutes(1).toInstant()));
	}

	@Test
	public void calcDiffDatum_resetInMiddle() throws IOException {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-16.txt");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		List<TypedDatumEntity> results = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Start/reset_f/reset_s/end results returned", results, hasSize(4));
		assertThat("Start timestamp", results.get(0).getTimestamp(),
				equalTo(start.minusMinutes(1).toInstant()));
		assertThat("Reset final timestamp", results.get(1).getTimestamp(),
				equalTo(start.plusMinutes(20).toInstant()));
		assertThat("Reset start timestamp", results.get(2).getTimestamp(),
				equalTo(start.plusMinutes(20).toInstant()));
		assertThat("End timestamp", results.get(3).getTimestamp(),
				equalTo(end.minusMinutes(1).toInstant()));
	}

	@Test
	public void calcDiffDatum_perfectHourlyData() throws IOException {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", null, new String[] { "wattHours" }, null);
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
		List<TypedDatumEntity> results = calcDiffDatum(meta.getStreamId(), start.toInstant(),
				end.toInstant());

		// THEN
		assertThat("Start/end results returned", results, hasSize(2));
		assertThat("Start timestamp", results.get(0).getTimestamp(),
				equalTo(ISO_INSTANT.parse("2017-07-04T08:00:00.000Z", Instant::from)));
		assertThat("End timestamp", results.get(1).getTimestamp(),
				equalTo(ISO_INSTANT.parse("2017-07-04T09:00:00.000Z", Instant::from)));
	}

}
