/* ==================================================================
 * BillingUsageTests.java - 30/10/2026 3:30:00 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.dao.test;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.dao.AuditNodeServiceEntity.dailyAuditNodeService;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the
 * {@code solarbill.billing_usage(userid BIGINT, ts_min TIMESTAMP, ts_max TIMESTAMP)}
 * stored procedure.
 *
 * @author matt
 * @version 1.0
 */
public class BillingUsageTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final ZoneId TEST_ZONE = ZoneId.of("Pacific/Auckland");

	private static final ZonedDateTime TEST_MONTH = ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0,
			TEST_ZONE);

	private static final int TEST_DAY_COUNT = 10;

	private Map<Long, Map<String, Object>> billingUsage(Long userId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				select * from solarbill.billing_usage(?, ?, ?)
				""", userId, Timestamp.valueOf(TEST_MONTH.toLocalDateTime()),
				Timestamp.valueOf(TEST_MONTH.plusMonths(1).toLocalDateTime()));
		log.debug("Got billing_usage: [{}]",
				rows.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return rows.stream().collect(toMap(r -> (Long) r.get("node_id"), Function.identity()));
	}

	private void incrementQueryCount(Long nodeId, String sourceId, Instant ts, int count) {
		jdbcTemplate.execute((Connection con) -> {
			try (CallableStatement stmt = con
					.prepareCall("{call solardatm.audit_increment_datum_q_count(?,?,?,?)}")) {
				stmt.setObject(1, nodeId);
				stmt.setString(2, sourceId);
				stmt.setTimestamp(3, Timestamp.from(ts));
				stmt.setInt(4, count);
				stmt.execute();
			}
			return null;
		});
	}

	private ObjectDatumStreamMetadata setupTestStream(Long userId, Long locId, Long nodeId,
			String sourceId) {
		setupTestNode(nodeId, locId);
		setupTestUserNode(userId, nodeId);
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				TEST_ZONE.getId(), ObjectDatumKind.Node, nodeId, sourceId);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		return meta;
	}

	private ObjectDatumStreamAliasEntity setupTestAlias(@Nullable Long userId, Long locId,
			Long aliasNodeId, String aliasSourceId, ObjectDatumStreamMetadata orig) {
		setupTestNode(aliasNodeId, locId);
		if ( userId != null ) {
			setupTestUserNode(userId, aliasNodeId);
		}
		final Instant now = Instant.now();
		final var alias = new ObjectDatumStreamAliasEntity(UUID.randomUUID(), now, now,
				ObjectDatumKind.Node, aliasNodeId, aliasSourceId, orig.getObjectId(),
				orig.getSourceId());
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate, List.of(alias));
		return alias;
	}

	/**
	 * Create daily audit data for the original (non-alias) stream with all
	 * audit counts populated.
	 */
	private List<AuditDatum> auditDatumDaily(UUID streamId, long queryCount) {
		final var result = new ArrayList<AuditDatum>(TEST_DAY_COUNT);
		for ( int i = 0; i < TEST_DAY_COUNT; i++ ) {
			final Instant day = TEST_MONTH.plusDays(i).toInstant();
			// UUID streamId, Instant timestamp, Long datumCount,
			// Long datumHourlyCount, Integer datumDailyCount, Long datumPropertyCount,
			// Long datumQueryCount, Long datumPropertyUpdateCount, Long fluxDataInCount
			result.add(AuditDatumEntity.dailyAuditDatum(streamId, day, 100L, 2L, 1, 400L, queryCount,
					5L, 8_000L));
		}
		return result;
	}

	/**
	 * Create daily audit data for an alias stream: only the query count is
	 * accumulated on alias streams.
	 */
	private List<AuditDatum> aliasAuditDatumDaily(UUID streamId, long queryCount) {
		final var result = new ArrayList<AuditDatum>(TEST_DAY_COUNT);
		for ( int i = 0; i < TEST_DAY_COUNT; i++ ) {
			final Instant day = TEST_MONTH.plusDays(i).toInstant();
			result.add(AuditDatumEntity.dailyAuditDatum(streamId, day, 0L, 0L, 0, 0L, queryCount, 0L,
					0L));
		}
		return result;
	}

	private List<AuditDatum> auditAccDatumDaily(UUID streamId) {
		final var result = new ArrayList<AuditDatum>(TEST_DAY_COUNT);
		for ( int i = 0; i < TEST_DAY_COUNT; i++ ) {
			final Instant day = TEST_MONTH.plusDays(i).toInstant();
			// UUID streamId, Instant timestamp, Long datumCount, Long datumHourlyCount,
			// Integer datumDailyCount, Integer datumMonthlyCount
			result.add(AuditDatumEntity.accumulativeAuditDatum(streamId, day, 100_000L, 600L, 40, 2));
		}
		return result;
	}

	private static long sum(List<AuditDatum> data, Function<AuditDatum, @Nullable Long> prop) {
		return data.stream().map(prop).mapToLong(v -> v != null ? v : 0L).sum();
	}

	private void logAuditTables() {
		allTableData(log, jdbcTemplate, "solardatm.da_datm_meta", "node_id,source_id");
		allTableData(log, jdbcTemplate, "solardatm.da_datm_alias", "alias_node_id,alias_source_id");
		allTableData(log, jdbcTemplate, "solardatm.aud_datm_daily", "stream_id,ts_start");
		allTableData(log, jdbcTemplate, "solardatm.aud_acc_datm_daily", "stream_id,ts_start");
	}

	@Test
	public void usage_nodes() {
		// GIVEN
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		// create one datum stream per node
		final int nodeCount = 3;
		final var streams = new ArrayList<ObjectDatumStreamMetadata>(nodeCount);
		final var auditDatumDaily = new ArrayList<AuditDatum>();
		final var auditAccDatumDaily = new ArrayList<AuditDatum>();
		final var auditNodeServiceDaily = new ArrayList<AuditNodeServiceValue>();
		for ( int i = 0; i < nodeCount; i++ ) {
			ObjectDatumStreamMetadata meta = setupTestStream(userId, locId, randomLong(),
					"source/%d".formatted(i));
			streams.add(meta);
			auditDatumDaily.addAll(auditDatumDaily(meta.getStreamId(), 1_000L * (i + 1)));
			auditAccDatumDaily.addAll(auditAccDatumDaily(meta.getStreamId()));
			for ( int d = 0; d < TEST_DAY_COUNT; d++ ) {
				auditNodeServiceDaily.add(dailyAuditNodeService(meta.getObjectId(), "inst",
						TEST_MONTH.plusDays(d).toInstant(), 25L * (i + 1)));
			}
		}
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, auditDatumDaily);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, auditAccDatumDaily);
		DatumDbUtils.insertAuditNodeServiceValueDaily(log, jdbcTemplate, auditNodeServiceDaily);

		logAuditTables();

		// WHEN
		Map<Long, Map<String, Object>> result = billingUsage(userId);

		// THEN
		then(result).as("One row returned per node")
				.containsOnlyKeys(streams.stream().map(ObjectDatumStreamMetadata::getObjectId).toList());
		for ( ObjectDatumStreamMetadata meta : streams ) {
			final UUID streamId = meta.getStreamId();
			final List<AuditDatum> nodeAudit = auditDatumDaily.stream()
					.filter(a -> streamId.equals(a.getStreamId())).toList();
			final List<AuditDatum> nodeAccAudit = auditAccDatumDaily.stream()
					.filter(a -> streamId.equals(a.getStreamId())).toList();
			// @formatter:off
			then(result.get(meta.getObjectId()))
				.as("Properties in summed from node audit daily data datumPropertyCount")
				.containsEntry("prop_in", sum(nodeAudit, AuditDatum::getDatumPropertyCount))
				.as("Datum out summed from node audit daily data datumQueryCount")
				.containsEntry("datum_out", sum(nodeAudit, AuditDatum::getDatumQueryCount))
				.as("Flux data in summed from node audit daily data fluxDataInCount")
				.containsEntry("flux_data_in", sum(nodeAudit, AuditDatum::getFluxDataInCount))
				.as("Datum stored summed from node audit acc daily data datumTotalCount")
				.containsEntry("datum_stored", sum(nodeAccAudit, AuditDatum::getDatumTotalCount))
				.as("Instructions summed from node audit node service 'inst' daily data")
				.containsEntry("instr_issued", auditNodeServiceDaily.stream()
						.filter(a -> meta.getObjectId().equals(a.getNodeId()))
						.mapToLong(AuditNodeServiceValue::getCount).sum())
				;
			// @formatter:on
		}
	}

	@Test
	public void usage_alias_datumOut() {
		// GIVEN
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		final ObjectDatumStreamMetadata orig = setupTestStream(userId, locId, randomLong(), "orig");
		final ObjectDatumStreamAliasEntity alias = setupTestAlias(userId, locId, randomLong(),
				"alias", orig);

		final List<AuditDatum> origAudit = auditDatumDaily(orig.getStreamId(), 1_000L);
		final List<AuditDatum> origAccAudit = auditAccDatumDaily(orig.getStreamId());
		final List<AuditDatum> aliasAudit = aliasAuditDatumDaily(alias.getStreamId(), 3_000L);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, origAudit);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, origAccAudit);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, aliasAudit);

		logAuditTables();

		// WHEN
		Map<Long, Map<String, Object>> result = billingUsage(userId);

		// THEN
		then(result).as("One row returned for original node and alias node")
				.containsOnlyKeys(orig.getObjectId(), alias.getObjectId());

		// @formatter:off
		then(result.get(orig.getObjectId()))
			.as("Original node properties in summed from original stream audit data")
			.containsEntry("prop_in", sum(origAudit, AuditDatum::getDatumPropertyCount))
			.as("Original node datum out excludes alias stream query counts")
			.containsEntry("datum_out", sum(origAudit, AuditDatum::getDatumQueryCount))
			.as("Original node flux data in summed from original stream audit data")
			.containsEntry("flux_data_in", sum(origAudit, AuditDatum::getFluxDataInCount))
			.as("Original node datum stored summed from original stream acc audit data")
			.containsEntry("datum_stored", sum(origAccAudit, AuditDatum::getDatumTotalCount))
			;

		then(result.get(alias.getObjectId()))
			.as("Alias node has no properties in")
			.containsEntry("prop_in", 0L)
			.as("Alias node datum out summed from alias stream query counts")
			.containsEntry("datum_out", sum(aliasAudit, AuditDatum::getDatumQueryCount))
			.as("Alias node has no flux data in")
			.containsEntry("flux_data_in", 0L)
			.as("Alias node has no datum stored")
			.containsEntry("datum_stored", 0L)
			;
		// @formatter:on
	}

	@Test
	public void usage_alias_datumOut_otherUser() {
		// GIVEN
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		final Long aliasUserId = randomLong();
		setupTestUser(aliasUserId, locId);

		// alias node owned by a different user than the original node
		final ObjectDatumStreamMetadata orig = setupTestStream(userId, locId, randomLong(), "orig");
		final ObjectDatumStreamAliasEntity alias = setupTestAlias(aliasUserId, locId, randomLong(),
				"alias", orig);

		final List<AuditDatum> origAudit = auditDatumDaily(orig.getStreamId(), 1_000L);
		final List<AuditDatum> aliasAudit = aliasAuditDatumDaily(alias.getStreamId(), 3_000L);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, origAudit);
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, aliasAudit);

		logAuditTables();

		// WHEN
		Map<Long, Map<String, Object>> origResult = billingUsage(userId);
		Map<Long, Map<String, Object>> aliasResult = billingUsage(aliasUserId);

		// THEN
		// @formatter:off
		then(origResult)
			.as("Original user usage only includes original node")
			.containsOnlyKeys(orig.getObjectId())
			.extractingByKey(orig.getObjectId())
			.as("Original node datum out excludes alias stream query counts")
			.returns(sum(origAudit, AuditDatum::getDatumQueryCount), m -> m.get("datum_out"))
			;

		then(aliasResult)
			.as("Alias user usage only includes alias node")
			.containsOnlyKeys(alias.getObjectId())
			.extractingByKey(alias.getObjectId())
			.as("Alias node datum out summed from alias stream query counts")
			.returns(sum(aliasAudit, AuditDatum::getDatumQueryCount), m -> m.get("datum_out"))
			.as("Alias node has no properties in")
			.returns(0L, m -> m.get("prop_in"))
			;
		// @formatter:on
	}

	@Test
	public void usage_alias_datumOut_queryAudit() {
		// GIVEN
		final Long locId = randomLong();
		setupTestLocation(locId, TEST_ZONE.getId());

		final Long userId = randomLong();
		setupTestUser(userId, locId);

		final ObjectDatumStreamMetadata orig = setupTestStream(userId, locId, randomLong(), "orig");
		final ObjectDatumStreamAliasEntity alias = setupTestAlias(userId, locId, randomLong(),
				"alias", orig);

		// record query counts the way SolarQuery does, by node + source ID
		final int origQueryCount = 11;
		final int aliasQueryCount = 37;
		for ( int i = 0; i < TEST_DAY_COUNT; i++ ) {
			final Instant ts = TEST_MONTH.plusDays(i).plusHours(12).toInstant();
			incrementQueryCount(orig.getObjectId(), orig.getSourceId(), ts, origQueryCount);
			incrementQueryCount(alias.getObjectId(), alias.getSourceId(), ts, aliasQueryCount);
		}

		// roll up the hourly query counts into daily audit data
		DatumDbUtils.processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		logAuditTables();

		// WHEN
		Map<Long, Map<String, Object>> result = billingUsage(userId);

		// THEN
		then(result).as("One row returned for original node and alias node")
				.containsOnlyKeys(orig.getObjectId(), alias.getObjectId());

		// @formatter:off
		then(result.get(orig.getObjectId()))
			.as("Original node datum out only includes queries made on original node + source")
			.containsEntry("datum_out", (long) origQueryCount * TEST_DAY_COUNT)
			;

		then(result.get(alias.getObjectId()))
			.as("Alias node datum out includes queries made on alias node + source")
			.containsEntry("datum_out", (long) aliasQueryCount * TEST_DAY_COUNT)
			;
		// @formatter:on
	}

}
