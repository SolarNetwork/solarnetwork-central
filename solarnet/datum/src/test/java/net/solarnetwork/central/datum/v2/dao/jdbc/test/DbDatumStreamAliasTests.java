/* ==================================================================
 * DbDatumStreamAliasTests.java - 31/10/2026 3:12:44 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.ioAuditDatum;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.domain.datum.Aggregation.Day;
import static net.solarnetwork.domain.datum.Aggregation.Hour;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Location;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the datum stream alias database objects.
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumStreamAliasTests extends BaseDatumJdbcTestSupport {

	private static final Long TEST_USER_1 = -9001L;
	private static final Long TEST_USER_2 = -9002L;
	private static final Long TEST_USER_3 = -9003L;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		for ( Long userId : List.of(TEST_USER_1, TEST_USER_2, TEST_USER_3) ) {
			setupTestUser(userId, "test%d@localhost".formatted(-userId));
		}
	}

	private Long ownedNode(Long userId) {
		final Long nodeId = randomLong();
		setupTestNode(nodeId);
		if ( userId != null ) {
			setupTestUserNode(userId, nodeId);
		}
		return nodeId;
	}

	private ObjectDatumStreamMetadata stream(Long nodeId) {
		final ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), TEST_TZ, Node, nodeId,
				"s/orig");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		return meta;
	}

	private ObjectDatumStreamAliasEntity alias(UUID aliasId, ObjectDatumStreamMetadata orig,
			Long aliasNodeId) {
		final Instant now = Instant.now();
		final var alias = new ObjectDatumStreamAliasEntity(aliasId, now, now, Node, aliasNodeId,
				"s/alias", orig.getObjectId(), orig.getSourceId());
		DatumDbUtils.insertObjectDatumStreamAliases(log, jdbcTemplate, List.of(alias));
		return alias;
	}

	private ObjectDatumStreamAliasEntity alias(ObjectDatumStreamMetadata orig, Long aliasNodeId) {
		return alias(UUID.randomUUID(), orig, aliasNodeId);
	}

	private void transferNode(Long nodeId, Long userId) {
		jdbcTemplate.update("UPDATE solaruser.user_node SET user_id = ? WHERE node_id = ?", userId,
				nodeId);
	}

	private List<Map<String, Object>> metaAliased(UUID streamId) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solardatm.da_datm_meta_aliased WHERE stream_id = ?::uuid",
				streamId.toString());
	}

	private List<Map<String, Object>> metaForStream(String functionName, UUID streamId) {
		return jdbcTemplate.queryForList("SELECT * FROM solardatm.%s(?::uuid)".formatted(functionName),
				streamId.toString());
	}

	private List<Map<String, Object>> deletedAliases() {
		return jdbcTemplate.queryForList("SELECT * FROM solardatm.da_datm_alias_del");
	}

	@Test
	public void view_realStream_withoutOwner() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(null));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(orig.getStreamId());

		// THEN
		then(result).as("Real stream does not depend on node ownership").hasSize(1);
	}

	@Test
	public void view_alias_sameOwner() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		// @formatter:off
		then(result)
			.as("Alias between nodes of one owner resolves")
			.hasSize(1)
			.element(0)
			.as("Alias resolves to original stream")
			.returns(orig.getStreamId(), m -> m.get("orig_stream_id"))
			;
		// @formatter:on
	}

	@Test
	public void view_alias_sameNode_withoutOwner() {
		// GIVEN
		final Long nodeId = ownedNode(null);
		final ObjectDatumStreamMetadata orig = stream(nodeId);
		final var alias = alias(orig, nodeId);

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias on the original node resolves").hasSize(1);
	}

	@Test
	public void view_alias_originalNodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_2);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias does not resolve once original node owned by other user").isEmpty();
	}

	@Test
	public void view_alias_aliasNodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = ownedNode(TEST_USER_1);
		final var alias = alias(orig, aliasNodeId);

		// WHEN
		transferNode(aliasNodeId, TEST_USER_2);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias does not resolve once alias node owned by other user").isEmpty();
	}

	@Test
	public void view_alias_bothNodesTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = ownedNode(TEST_USER_1);
		final var alias = alias(orig, aliasNodeId);

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_2);
		transferNode(aliasNodeId, TEST_USER_2);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias resolves once both nodes share an owner again").hasSize(1);
	}

	@Test
	public void view_alias_originalNodeWithoutOwner() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(null));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias does not resolve without an original node owner").isEmpty();
	}

	@Test
	public void view_alias_aliasNodeWithoutOwner() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(null));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias does not resolve without an alias node owner").isEmpty();
	}

	@Test
	public void metadata_alias_nodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_2);
		final List<Map<String, Object>> result = metaForStream("find_metadata_for_stream",
				alias.getStreamId());
		final List<Map<String, Object>> accountingResult = metaForStream("find_metadata_for_stream_all",
				alias.getStreamId());

		// THEN
		then(result).as("Metadata not available for alias that does not resolve").isEmpty();
		then(accountingResult).as("Accounting metadata still available for alias").hasSize(1);
	}

	@Test
	public void audit_alias_nodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));
		transferNode(orig.getObjectId(), TEST_USER_2);

		// WHEN
		final Instant ts = Instant.now();
		jdbcTemplate.queryForList("SELECT solardatm.audit_increment_datum_q_count(?,?,?::timestamptz,?)",
				alias.getObjectId(), alias.getSourceId(), ts.toString(), 123);

		// THEN
		// @formatter:off
		then(DatumDbUtils.listAuditDatum(jdbcTemplate, Hour))
			.as("Query usage still recorded on alias stream")
			.singleElement()
			.returns(alias.getStreamId(), AuditDatum::getStreamId)
			.returns(123L, AuditDatum::getDatumQueryCount)
			;
		// @formatter:on
	}

	@Test
	public void auditRollup_alias_nodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		final ZoneId zone = ZoneId.of(TEST_TZ);
		final Instant day = Instant.now().atZone(zone).truncatedTo(DAYS).toInstant();
		DatumDbUtils.insertAuditDatum(log, jdbcTemplate, List
				.of(ioAuditDatum(alias.getStreamId(), day.truncatedTo(HOURS), 0L, 0L, 123L, 0L, 0L)));
		DatumDbUtils.insertStaleAuditDatum(log, jdbcTemplate,
				List.of(new StaleAuditDatumEntity(alias.getStreamId(), day, Day, Instant.now())));
		transferNode(orig.getObjectId(), TEST_USER_2);

		// WHEN
		DatumDbUtils.processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Day));

		// THEN
		// @formatter:off
		then(DatumDbUtils.listAuditDatum(jdbcTemplate, Day))
			.as("Daily rollup uses the alias node time zone, not a UTC fallback")
			.singleElement()
			.returns(alias.getStreamId(), AuditDatum::getStreamId)
			.returns(day, AuditDatum::getTimestamp)
			.returns(123L, AuditDatum::getDatumQueryCount)
			;
		// @formatter:on
	}

	@Test
	public void save_alias_nodeStreamId() {
		// GIVEN
		final ObjectDatumStreamMetadata otherUserStream = stream(ownedNode(TEST_USER_2));
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));

		// WHEN/THEN
		thenThrownBy(() -> alias(otherUserStream.getStreamId(), orig, orig.getObjectId()))
				.as("Alias cannot use the stream ID of a real node stream")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void save_alias_locationStreamId() {
		// GIVEN
		final ObjectDatumStreamMetadata locStream = emptyMeta(UUID.randomUUID(), TEST_TZ, Location,
				TEST_LOC_ID, "s/loc");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(locStream));
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));

		// WHEN/THEN
		thenThrownBy(() -> alias(locStream.getStreamId(), orig, orig.getObjectId()))
				.as("Alias cannot use the stream ID of a real location stream")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void update_alias_nodeStreamId() {
		// GIVEN
		final ObjectDatumStreamMetadata otherUserStream = stream(ownedNode(TEST_USER_2));
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, orig.getObjectId());

		// WHEN/THEN
		thenThrownBy(() -> jdbcTemplate.update(
				"UPDATE solardatm.da_datm_alias SET stream_id = ?::uuid WHERE stream_id = ?::uuid",
				otherUserStream.getStreamId().toString(), alias.getStreamId().toString()))
						.as("Alias cannot change to the stream ID of a real stream")
						.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void delete_alias_recorded() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		jdbcTemplate.update("DELETE FROM solardatm.da_datm_alias WHERE stream_id = ?::uuid",
				alias.getStreamId().toString());

		// THEN
		// @formatter:off
		then(deletedAliases())
			.as("Deleted alias recorded")
			.singleElement()
			.returns(alias.getStreamId(), m -> m.get("stream_id"))
			.returns(alias.getObjectId(), m -> m.get("alias_node_id"))
			;
		// @formatter:on
	}

	@Test
	public void delete_originalStream_aliasRecorded() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));

		// WHEN
		jdbcTemplate.update("DELETE FROM solardatm.da_datm_meta WHERE stream_id = ?::uuid",
				orig.getStreamId().toString());

		// THEN
		// @formatter:off
		then(deletedAliases())
			.as("Alias removed by cascade recorded")
			.singleElement()
			.returns(alias.getStreamId(), m -> m.get("stream_id"))
			;
		// @formatter:on
	}

	@Test
	public void delete_alias_recreatedThenDeleted() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, ownedNode(TEST_USER_1));
		final String aliasId = alias.getStreamId().toString();
		jdbcTemplate.update("DELETE FROM solardatm.da_datm_alias WHERE stream_id = ?::uuid", aliasId);
		final Long newAliasNodeId = ownedNode(TEST_USER_1);
		alias(alias.getStreamId(), orig, newAliasNodeId);

		// WHEN
		jdbcTemplate.update("DELETE FROM solardatm.da_datm_alias WHERE stream_id = ?::uuid", aliasId);

		// THEN
		// @formatter:off
		then(deletedAliases())
			.as("Latest alias node recorded")
			.singleElement()
			.returns(newAliasNodeId, m -> m.get("alias_node_id"))
			;
		// @formatter:on
	}

}
