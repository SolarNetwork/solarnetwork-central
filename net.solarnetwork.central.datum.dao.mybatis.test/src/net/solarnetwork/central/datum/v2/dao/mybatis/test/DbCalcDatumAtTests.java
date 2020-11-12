/* ==================================================================
 * DbCalcDatumAtTests.java - 13/11/2020 9:55:35 am
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
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
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;

/**
 * Test cases for the database aggregate function to interpolate datum at a
 * point in time.
 * 
 * @author matt
 * @version 1.0
 */
public class DbCalcDatumAtTests extends BaseDatumJdbcTestSupport {

	public DatumEntity calcDatumAt(UUID streamId, Instant at) {
		return jdbcTemplate.execute(new ConnectionCallback<DatumEntity>() {

			@Override
			public DatumEntity doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.calc_datm_at(?::uuid,?)}")) {
					log.debug("Calculating datum stream {} at {}", streamId, at);
					stmt.setString(1, streamId.toString());
					stmt.setTimestamp(2, Timestamp.from(at));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							if ( rs.next() ) {
								DatumEntity d = DatumEntityRowMapper.INSTANCE.mapRow(rs, 1);
								log.debug("Calculated datum at {}: {}", at, d);
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
	public void calcDatumAt_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		DatumEntity d = calcDatumAt(UUID.randomUUID(), start.toInstant());

		// THEN
		assertThat("No result from no input", d, nullValue());
	}

	@Test
	public void calcDatumAt_wayBeforeMatch() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		DatumEntity d = calcDatumAt(streamId, start.minusYears(1).toInstant());

		// THEN
		assertThat("No result from too-early date", d, nullValue());
	}

	@Test
	public void calcDatumAt_wayAfterMatch() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		DatumEntity d = calcDatumAt(streamId, start.plusYears(1).toInstant());

		// THEN
		assertThat("No result from too-early date", d, nullValue());
	}

	@Test
	public void calcDatumAt_oneEigth() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		Instant at = start.plusSeconds(75).toInstant();
		DatumEntity d = calcDatumAt(streamId, at);

		// THEN
		assertThat("Result between datum available", d, notNullValue());
		assertThat("Stream ID matches", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp matches given time", d.getTimestamp(), equalTo(at));
		assertThat("Instantaneous 1/8 difference [1.2,2.1] -> [1.3,3.1]",
				d.getProperties().getInstantaneous(), arrayContaining(decimalArray("1.2125", "2.225")));
		assertThat("Accumulated 1/8 difference [100] -> [105]", d.getProperties().getAccumulating(),
				arrayContaining(decimalArray("100.625")));
	}

	@Test
	public void calcDatumAt_fourFifths() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		Instant at = start.plusMinutes(8).toInstant();
		DatumEntity d = calcDatumAt(streamId, at);

		// THEN
		assertThat("Result between datum available", d, notNullValue());
		assertThat("Stream ID matches", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp matches given time", d.getTimestamp(), equalTo(at));
		assertThat("Instantaneous 4/5 difference [1.2,2.1] -> [1.3,3.1]",
				d.getProperties().getInstantaneous(), arrayContaining(decimalArray("1.28", "2.9")));
		assertThat("Accumulated 4/5 difference [100] -> [105]", d.getProperties().getAccumulating(),
				arrayContaining(decimalArray("104")));
	}

	@Test
	public void calcDatumAt_exactDatum() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// WHEN
		Instant at = start.plusMinutes(10).toInstant();
		DatumEntity d = calcDatumAt(streamId, at);

		// THEN
		assertThat("Result between datum available", d, notNullValue());
		assertThat("Stream ID matches", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp matches given time", d.getTimestamp(), equalTo(at));
		assertThat("Instantaneous exact", d.getProperties().getInstantaneous(),
				arrayContaining(decimalArray("1.3", "3.1")));
		assertThat("Accumulated exact", d.getProperties().getAccumulating(),
				arrayContaining(decimalArray("105")));
	}
}
