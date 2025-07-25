/* ==================================================================
 * AggregateDatumEntityRowMapperTests.java - 25/07/2025 2:25:21 pm
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
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.atIndex;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link AggregateDatumEntityRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
public class AggregateDatumEntityRowMapperTests extends BaseDatumJdbcTestSupport {

	private static final String LIST_ALL_HOUR_SQL = """
			select stream_id,ts_start,data_i,data_a,data_s,data_t,stat_i,read_a
			from solardatm.agg_datm_hourly
			order by stream_id, ts_start
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

		final Instant ts = now().truncatedTo(HOURS);

		jdbcTemplate.update(DatumDbUtils.insertAggDatumSql(Aggregation.Hour),
				streamId.toString(),
				Timestamp.from(ts),
				"{NaN}",
				null,
				null,
				null,
				"{{NaN,NaN,NaN}}",
				null
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.agg_datm_hourly", "stream_id,ts_start");

		List<AggregateDatum> result = jdbcTemplate.query(LIST_ALL_HOUR_SQL,
				AggregateDatumEntityRowMapper.HOUR_INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(AggregateDatum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(AggregateDatum::getTimestamp))
			.satisfies(agg -> {
				then(agg.getProperties().getInstantaneous())
					.as("Has 1 instantaneous property, with NaN mapped to null")
					.containsExactly(decimalArray((String)null))
					;
				then(agg.getStatistics().getInstantaneous())
					.as("Has 1 instantaneous stat, with NaN mapped to null")
					.contains(decimalArray((String)null,  null, null), atIndex(0))
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

		final Instant ts = now().truncatedTo(HOURS);

		jdbcTemplate.update(DatumDbUtils.insertAggDatumSql(Aggregation.Hour),
				streamId.toString(),
				Timestamp.from(ts),
				"{Infinity}",
				"{-Infinity}",
				null,
				null,
				"{{Infinity,Infinity,Infinity}}",
				"{{-Infinity,-Infinity,-Infinity}}"
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.agg_datm_hourly", "stream_id,ts_start");

		List<AggregateDatum> result = jdbcTemplate.query(LIST_ALL_HOUR_SQL,
				AggregateDatumEntityRowMapper.HOUR_INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(AggregateDatum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(AggregateDatum::getTimestamp))
			.satisfies(agg -> {
				then(agg.getProperties().getInstantaneous())
					.as("Has 1 instantaneous property, with Infinity mapped to null")
					.containsExactly(decimalArray((String)null))
					;
				then(agg.getProperties().getAccumulating())
					.as("Has 1 accumulating property, with Infinity mapped to null")
					.containsExactly(decimalArray((String)null))
					;
				then(agg.getStatistics().getInstantaneous())
					.as("Has 1 instantaneous stat, with Infinity mapped to null")
					.contains(decimalArray((String)null,  null, null), atIndex(0))
					;
				then(agg.getStatistics().getAccumulating())
					.as("Has 1 accumulating stat, with Infinity mapped to null")
					.contains(decimalArray((String)null,  null, null), atIndex(0))
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
				"{d,e,f}",
				null,
				null);

		final Instant ts = now().truncatedTo(HOURS);

		jdbcTemplate.update(DatumDbUtils.insertAggDatumSql(Aggregation.Hour),
				streamId.toString(),
				Timestamp.from(ts),
				"{1.23,Infinity,2.34}",
				"{30,-Infinity,31}",
				null,
				null,
				"{{10,1.0,2.0},{20,-Infinity,2.5},{30,2.0,3.0}}",
				"{{40,4.0,140},{50,Infinity,150},{60,6.0,160}}"
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.agg_datm_hourly", "stream_id,ts_start");

		List<AggregateDatum> result = jdbcTemplate.query(LIST_ALL_HOUR_SQL,
				AggregateDatumEntityRowMapper.HOUR_INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(AggregateDatum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(AggregateDatum::getTimestamp))
			.satisfies(agg -> {
				then(agg.getProperties().getInstantaneous())
					.as("Has 3 instantaneous properties, with Infinity mapped to null")
					.containsExactly(decimalArray("1.23", null, "2.34"))
					;
				then(agg.getProperties().getAccumulating())
					.as("Has 3 accumulating properties, with Infinity mapped to null")
					.containsExactly(decimalArray("30", null, "31"))
					;
				then(agg.getStatistics().getInstantaneous())
					.as("Has 3 instantaneous stats, with Infinity mapped to null")
					.contains(decimalArray("10", "1.0", "2.0"), atIndex(0))
					.contains(decimalArray("20",  null, "2.5"), atIndex(1))
					.contains(decimalArray("30", "2.0", "3.0"), atIndex(2))
					;
				then(agg.getStatistics().getAccumulating())
					.as("Has 3 accumulating stats, with Infinity mapped to null")
					.contains(decimalArray("40", "4.0", "140"), atIndex(0))
					.contains(decimalArray("50",  null, "150"), atIndex(1))
					.contains(decimalArray("60", "6.0", "160"), atIndex(2))
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
				"{d,e,f}",
				null,
				null);

		final Instant ts = now().truncatedTo(HOURS);

		jdbcTemplate.update(DatumDbUtils.insertAggDatumSql(Aggregation.Hour),
				streamId.toString(),
				Timestamp.from(ts),
				"{Infinity,1.23,2.34}",
				"{-Infinity,3.0,31}",
				null,
				null,
				"{{10,1.0,2.0},{-Infinity,2.0,2.5},{30,2.0,3.0}}",
				"{{40,4.0,140},{Infinity,5.0,150},{60,6.0,160}}"
				);
		// @formatter:on

		// WHEN
		allTableData(log, jdbcTemplate, "solardatm.agg_datm_hourly", "stream_id,ts_start");

		List<AggregateDatum> result = jdbcTemplate.query(LIST_ALL_HOUR_SQL,
				AggregateDatumEntityRowMapper.HOUR_INSTANCE);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned datum row")
			.hasSize(1)
			.first()
			.as("Stream ID mapped")
			.returns(streamId, from(AggregateDatum::getStreamId))
			.as("Timestamp mapped")
			.returns(ts, from(AggregateDatum::getTimestamp))
			.satisfies(agg -> {
				then(agg.getProperties().getInstantaneous())
					.as("Has 3 instantaneous properties, with Infinity mapped to null")
					.containsExactly(decimalArray(null, "1.23", "2.34"))
					;
				then(agg.getProperties().getAccumulating())
					.as("Has 3 accumulating properties, with Infinity mapped to null")
					.containsExactly(decimalArray(null, "3.0", "31"))
					;
				then(agg.getStatistics().getInstantaneous())
					.as("Has 3 instantaneous stats, with Infinity mapped to null")
					.contains(decimalArray("10", "1.0", "2.0"), atIndex(0))
					.contains(decimalArray(null, "2.0", "2.5"), atIndex(1))
					.contains(decimalArray("30", "2.0", "3.0"), atIndex(2))
					;
				then(agg.getStatistics().getAccumulating())
					.as("Has 3 accumulating stats, with Infinity mapped to null")
					.contains(decimalArray("40", "4.0", "140"), atIndex(0))
					.contains(decimalArray(null, "5.0", "150"), atIndex(1))
					.contains(decimalArray("60", "6.0", "160"), atIndex(2))
					;
			})
			;
		// @formatter:on
	}

}
