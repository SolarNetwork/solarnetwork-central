/* ==================================================================
 * DbDeleteDatumByIdTests.java - 12/02/2025 7:31:21 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlReturnResultSet;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.ObjectDatumIdRowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the
 * {@code solardatm.delete_datm(BIGINT,TIMESTAMP,UUID,BIGINT,TEXT,BOOLEAN}
 * database procedure.
 *
 * @author matt
 * @version 1.0
 */
public class DbDeleteDatumByIdTests extends BaseDatumJdbcTestSupport {

	private static final String SQL_DELETE_DATUM_BY_ID = "{call solardatm.delete_datm(?,?,?::UUID,?,?,?)}";

	private Long userId;
	private Long nodeId;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		userId = randomLong();
		setupTestUser(userId);
		nodeId = randomLong();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> populateTestData(final Instant start,
			final int count, final long step, final Long nodeId, final String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(start.plusMillis(i * step));
			d.setNodeId(nodeId);
			d.setSourceId(sourceId);
			DatumSamples s = new DatumSamples();
			s.putInstantaneousSampleValue("watts", 125);
			s.putAccumulatingSampleValue("wattHours", 10);
			d.setSamples(s);
			data.add(d);
		}
		return DatumDbUtils.ingestDatumStream(log, jdbcTemplate, data, TEST_TZ);
	}

	@SuppressWarnings("unchecked")
	private List<ObjectDatumId> deleteDatum(Long userId, Instant timestamp, UUID streamId, Long nodeId,
			String sourceId, boolean track) {
		Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con.prepareCall(SQL_DELETE_DATUM_BY_ID);
				stmt.setObject(1, userId);
				stmt.setTimestamp(2, Timestamp.from(timestamp));
				if ( streamId != null ) {
					stmt.setString(3, streamId.toString());
				} else {
					stmt.setNull(3, Types.VARCHAR);
				}
				if ( nodeId != null ) {
					stmt.setObject(4, nodeId);
				} else {
					stmt.setNull(4, Types.BIGINT);
				}
				if ( sourceId != null ) {
					stmt.setString(5, sourceId);
				} else {
					stmt.setNull(5, Types.VARCHAR);
				}
				stmt.setBoolean(6, track);
				return stmt;
			}
		}, List.of(new SqlReturnResultSet("data", ObjectDatumIdRowMapper.INSTANCE)));
		if ( result.get("data") instanceof List<?> l ) {
			return (List<ObjectDatumId>) l;
		}
		return Collections.emptyList();
	}

	@Test
	public void deleteDatumAddStaleRow() {
		// GIVEN
		final int rowCount = 10;
		final long step = 1000;
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 15, 05, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(ts1.toInstant(), rowCount,
				step, nodeId, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();
		processStaleAggregateDatum(log, jdbcTemplate);

		// WHEN
		Instant tsToDelete = ts1.plusSeconds(CommonTestUtils.RNG.nextLong(rowCount)).toInstant();
		List<ObjectDatumId> result = deleteDatum(userId, tsToDelete, streamId, null, null, true);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned deleted datum row identifier")
			.containsExactly(ObjectDatumId.nodeId(streamId, nodeId, TEST_SOURCE_ID, tsToDelete, Aggregation.None));
		// @formatter:on

		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		// @formatter:off
		then(staleRows)
			.as("Stale row inserted")
			.containsExactly(new StaleAggregateDatumEntity(streamId, tsToDelete.truncatedTo(HOURS), Aggregation.Hour, null))
			;
		// @formatter:on
	}

}
