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

import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.arrayOfDecimals;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
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
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.jdbc.ReadingDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;

/**
 * Test cases for the {@literal solardatm.diff_datm} database stored procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDiffDatumTests extends BaseDatumJdbcTestSupport {

	public ReadingDatum calcDiffDatum(UUID streamId, Instant from, Instant to) {
		return jdbcTemplate.execute(new ConnectionCallback<ReadingDatum>() {

			@Override
			public ReadingDatum doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement stmt = con.prepareStatement(
				// @formatter:off
								"SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).* "
								+"FROM solardatm.find_datm_diff_rows(?::uuid,?,?) d "
								+"HAVING (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).stream_id IS NOT NULL"
								// @formatter:on
				)) {
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

	private UUID loadStreamWithAuxiliary(String resource) throws IOException {
		List<?> data = DatumTestUtils.loadJsonDatumAndAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				DatumTestUtils.insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return streamId;
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

	@SuppressWarnings("unchecked")
	@Test
	public void calcDiffDatum_typical() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-02.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Timestamp is reading start", result.getTimestamp(),
				equalTo(start.minusMinutes(1).toInstant()));
		assertThat("End timestamp is reading end", result.getEndTimestamp(),
				equalTo(end.minusMinutes(1).toInstant()));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("30"));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "30", "100", "130" })));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void calcDiffDatum_oneResetExactlyAtStart() throws IOException {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-15.txt");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Timestamp is reading start reset", result.getTimestamp(),
				equalTo(start.toInstant()));
		assertThat("End timestamp is reading end", result.getEndTimestamp(),
				equalTo(end.minusMinutes(1).toInstant()));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("30"));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void calcDiffDatum_oneResetInMiddle() throws IOException {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-16.txt");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Timestamp is reading start", result.getTimestamp(),
				equalTo(start.minusMinutes(1).toInstant()));
		assertThat("End timestamp is reading end", result.getEndTimestamp(),
				equalTo(end.minusMinutes(1).toInstant()));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("35"));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "35", "100", "25" })));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void calcDiffDatum_oneResetExactlyAtEnd() throws IOException {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-17.txt");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Timestamp is reading start", result.getTimestamp(),
				equalTo(start.minusMinutes(1).toInstant()));
		assertThat("End timestamp is reading end reset", result.getEndTimestamp(),
				equalTo(end.toInstant()));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("31"));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "31", "10", "41" })));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void calcDiffDatum_oneResetExactlyAtStartWithoutLeading() throws IOException {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-18.txt");

		// WHEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		ZonedDateTime end = start.plusHours(1);
		ReadingDatum result = calcDiffDatum(streamId, start.toInstant(), end.toInstant());

		// THEN
		assertThat("Result returned", result, notNullValue());
		assertThat("Timestamp is reading start reset", result.getTimestamp(),
				equalTo(start.toInstant()));
		assertThat("End timestamp is reading end", result.getEndTimestamp(),
				equalTo(end.minusMinutes(1).toInstant()));
		assertThat("Agg instantaneous", result.getProperties().getInstantaneous(), nullValue());
		assertThat("Agg accumulating", result.getProperties().getAccumulating(), arrayOfDecimals("30"));
		assertThat("Stats accumulating", result.getStatistics().getAccumulating(),
				arrayContaining(arrayOfDecimals(new String[] { "30", "10", "40" })));
	}
}
