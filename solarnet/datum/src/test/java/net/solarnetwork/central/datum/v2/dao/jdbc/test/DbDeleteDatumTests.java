/* ==================================================================
 * DbDeleteDatumTests.java - 23/11/2020 3:58:47 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertStaleAggregateDatum;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@code solardatm.delete_datm} database procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDeleteDatumTests extends BaseDatumJdbcTestSupport {

	private int deleteDatum(UUID streamId, LocalDateTime start, LocalDateTime end, String timeZoneId) {
		return jdbcTemplate.execute(new ConnectionCallback<Long>() {

			@Override
			public Long doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.delete_datm(?, ?, ?, ?)}")) {
					cs.registerOutParameter(1, Types.BIGINT);
					cs.setObject(2, streamId, Types.OTHER);
					cs.setObject(3, start, Types.TIMESTAMP);
					cs.setObject(4, end, Types.TIMESTAMP);
					cs.setString(5, timeZoneId);
					cs.execute();
					return cs.getLong(1);
				}
			}

		}).intValue();
	}

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> populateTestData(final long start,
			final int count, final long step, final Long nodeId, final String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(count);
		long ts = start;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(Instant.ofEpochMilli(ts));
			d.setNodeId(nodeId);
			d.setSourceId(sourceId);
			DatumSamples s = new DatumSamples();
			s.putInstantaneousSampleValue("watts", 125);
			s.putAccumulatingSampleValue("wattHours", 10);
			d.setSamples(s);
			data.add(d);
			ts += step;
		}
		return DatumDbUtils.ingestDatumStream(log, jdbcTemplate, data, TEST_TZ);
	}

	@Before
	public void setup() {
		setupTestNode();
	}

	@Test
	public void deleteDatumAddStaleRow() {
		// given
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 15, 05, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();
		processStaleAggregateDatum(log, jdbcTemplate);

		// when
		int updateCount = deleteDatum(streamId, ts1.toLocalDateTime(),
				ts1.plusMinutes(1).toLocalDateTime(), TEST_TZ);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		assertThat("Stale row inserted", staleRows, hasSize(1));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void deleteDatumAddStaleRowAfterPrevHour() {
		// given
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 14, 55, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();
		processStaleAggregateDatum(log, jdbcTemplate);

		// when

		// delete 2nd datum, in 2nd hour
		int updateCount = deleteDatum(streamId, ts1.plusMinutes(10).toLocalDateTime(),
				ts1.plusMinutes(11).toLocalDateTime(), TEST_TZ);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));

		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale rows inserted", staleRows, hasSize(2));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
		assertStaleAggregateDatum("2", staleRows.get(1), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void deleteDatumAddStaleRowBeforeNextHour() {
		// given
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 14, 55, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();
		processStaleAggregateDatum(log, jdbcTemplate);

		// when

		// delete 1st datum, in 1st hour
		int updateCount = deleteDatum(streamId, ts1.toLocalDateTime(),
				ts1.plusMinutes(1).toLocalDateTime(), TEST_TZ);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));

		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale rows inserted", staleRows, hasSize(2));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
		assertStaleAggregateDatum("2", staleRows.get(1), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant(), Aggregation.Hour, null));
	}

}
