/* ==================================================================
 * DatumEntityRowMapperTests.java - 25/07/2025 11:22:21 am
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

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.Datum;

/**
 * Test cases for the {@link DatumEntityRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumEntityRowMapperTests extends BaseDatumJdbcTestSupport {

	private static final String LIST_ALL_SQL = """
			select stream_id,ts,received,data_i,data_a,data_s,data_t
			from solardatm.da_datm
			order by stream_id, ts
			""";

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

	@Test
	public void mapRow_NaN() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		// @formatter:off
		jdbcTemplate.update(DatumDbUtils.insertDatumMetaSql(Node),
				streamId.toString(),
				nodeId,
				randomString(),
				"{a}",
				null,
				null,
				null);

		final Instant ts = now().truncatedTo(MILLIS);

		jdbcTemplate.update(DatumDbUtils.insertDatumSql(),
				streamId.toString(),
				Timestamp.from(ts),
				"{NaN}",
				null,
				null,
				null,
				Timestamp.from(ts)
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");

		List<Datum> result = jdbcTemplate.query(LIST_ALL_SQL, DatumEntityRowMapper.INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(Datum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(Datum::getTimestamp))
			.satisfies(datum -> {
				then(datum.getProperties().getInstantaneous())
					.as("Has 1 instantaneous property, with NaN mapped to null")
					.containsExactly(decimalArray((String)null))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mapRow_Infinity() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		// @formatter:off
		jdbcTemplate.update(DatumDbUtils.insertDatumMetaSql(Node),
				streamId.toString(),
				nodeId,
				randomString(),
				"{a}",
				"{b}",
				null,
				null);

		final Instant ts = now().truncatedTo(MILLIS);

		jdbcTemplate.update(DatumDbUtils.insertDatumSql(),
				streamId.toString(),
				Timestamp.from(ts),
				"{Infinity}",
				"{-Infinity}",
				null,
				null,
				Timestamp.from(ts)
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");

		List<Datum> result = jdbcTemplate.query(LIST_ALL_SQL, DatumEntityRowMapper.INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(Datum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(Datum::getTimestamp))
			.satisfies(datum -> {
				then(datum.getProperties().getInstantaneous())
					.as("Has 1 instantaneous property, with Infinity mapped to null")
					.containsExactly(decimalArray((String)null))
					;
				then(datum.getProperties().getAccumulating())
					.as("Has 1 accumulating property, with Infinity mapped to null")
					.containsExactly(decimalArray((String)null))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mapRow_InfinityInMiddle() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		// @formatter:off
		jdbcTemplate.update(DatumDbUtils.insertDatumMetaSql(Node),
				streamId.toString(),
				nodeId,
				randomString(),
				"{a,b,c}",
				"{c,d,e}",
				null,
				null);

		final Instant ts = now().truncatedTo(MILLIS);

		jdbcTemplate.update(DatumDbUtils.insertDatumSql(),
				streamId.toString(),
				Timestamp.from(ts),
				"{12345,Infinity,34567}",
				"{10,-Infinity,30}",
				null,
				null,
				Timestamp.from(ts)
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");

		List<Datum> result = jdbcTemplate.query(LIST_ALL_SQL, DatumEntityRowMapper.INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(Datum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(Datum::getTimestamp))
			.satisfies(datum -> {
				then(datum.getProperties().getInstantaneous())
					.as("Has 3 instantaneous properties, with Infinity mapped to null")
					.containsExactly(decimalArray("12345", null, "34567"))
					;
				then(datum.getProperties().getAccumulating())
					.as("Has 3 accumulating properties, with Infinity mapped to null")
					.containsExactly(decimalArray("10", null, "30"))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mapRow_InfinityAtStart() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		// @formatter:off
		jdbcTemplate.update(DatumDbUtils.insertDatumMetaSql(Node),
				streamId.toString(),
				nodeId,
				randomString(),
				"{a,b,c}",
				"{c,d,e}",
				null,
				null);

		final Instant ts = now().truncatedTo(MILLIS);

		jdbcTemplate.update(DatumDbUtils.insertDatumSql(),
				streamId.toString(),
				Timestamp.from(ts),
				"{Infinity,23456,34567}",
				"{-Infinity,20,30}",
				null,
				null,
				Timestamp.from(ts)
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");

		List<Datum> result = jdbcTemplate.query(LIST_ALL_SQL, DatumEntityRowMapper.INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(Datum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(Datum::getTimestamp))
			.satisfies(datum -> {
				then(datum.getProperties().getInstantaneous())
					.as("Has 3 instantaneous properties, with Infinity mapped to null")
					.containsExactly(decimalArray(null, "23456", "34567"))
					;
				then(datum.getProperties().getAccumulating())
					.as("Has 3 accumulating properties, with Infinity mapped to null")
					.containsExactly(decimalArray(null, "20", "30"))
					;
			})
			;
		// @formatter:on
	}

}
