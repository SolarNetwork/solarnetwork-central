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
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the datum stream alias database objects.
 *
 * @author matt
 * @version 1.1
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
		return stream(nodeId, "s/orig");
	}

	private ObjectDatumStreamMetadata stream(Long nodeId, String sourceId) {
		final ObjectDatumStreamMetadata meta = emptyMeta(UUID.randomUUID(), TEST_TZ, Node, nodeId,
				sourceId);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
		return meta;
	}

	private ObjectDatumStreamAliasEntity alias(UUID aliasId, ObjectDatumStreamMetadata orig,
			Long aliasNodeId) {
		return alias(aliasId, orig, aliasNodeId, "s/alias");
	}

	private ObjectDatumStreamAliasEntity alias(UUID aliasId, ObjectDatumStreamMetadata orig,
			Long aliasNodeId, String aliasSourceId) {
		final Instant now = Instant.now();
		final var alias = new ObjectDatumStreamAliasEntity(aliasId, now, now, Node, aliasNodeId,
				aliasSourceId, orig.getObjectId(), orig.getSourceId());
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

	private UserLongCompositePK grant(Long userId, Long granteeId, boolean enabled, Long[] nodeIds,
			String[] sourceIds) {
		return grant(userId, granteeId, enabled, false, nodeIds, sourceIds);
	}

	private UserLongCompositePK publicGrant(Long userId, Long granteeId, Long[] nodeIds,
			String[] sourceIds) {
		return grant(userId, granteeId, true, true, nodeIds, sourceIds);
	}

	private UserLongCompositePK grant(Long userId, Long granteeId, boolean enabled, boolean allowPublic,
			Long[] nodeIds, String[] sourceIds) {
		final var grant = new ObjectDatumStreamAliasGrantEntity(userId, 0L, Instant.now(), granteeId);
		grant.setEnabled(enabled);
		grant.setAllowPublic(allowPublic);
		grant.setNodeIds(nodeIds);
		grant.setSourceIds(sourceIds);
		return new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).create(userId, grant);
	}

	private UserLongCompositePK grant(Long userId, Long granteeId, Long... nodeIds) {
		return grant(userId, granteeId, true, nodeIds, null);
	}

	private ObjectDatumStreamAliasGrantEntity savedGrant(UserLongCompositePK id) {
		return new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).get(id);
	}

	private Long privateNode(Long userId) {
		final Long nodeId = ownedNode(userId);
		jdbcTemplate.update("UPDATE solaruser.user_node SET private = TRUE WHERE node_id = ?", nodeId);
		return nodeId;
	}

	private void setPublic(Long nodeId) {
		jdbcTemplate.update("UPDATE solaruser.user_node SET private = FALSE WHERE node_id = ?", nodeId);
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

	@Test
	public void grant_none() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias of another user's stream requires a grant").isEmpty();
	}

	@Test
	public void grant_node() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		// @formatter:off
		then(result)
			.as("Alias resolves with grant of its node")
			.hasSize(1)
			.element(0)
			.as("Alias resolves to original stream")
			.returns(orig.getStreamId(), m -> m.get("orig_stream_id"))
			.as("Original node available")
			.returns(orig.getObjectId(), m -> m.get("orig_node_id"))
			.as("Node is the alias node")
			.returns(alias.getObjectId(), m -> m.get("node_id"))
			;
		// @formatter:on
	}

	@Test
	public void grant_disabled() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, false, new Long[] { orig.getObjectId() }, null);

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Disabled grant has no effect").isEmpty();
	}

	@Test
	public void grant_otherGrantee() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_3, orig.getObjectId());

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Grant to another user has no effect").isEmpty();
	}

	@Test
	public void grant_otherNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, ownedNode(TEST_USER_1));

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Grant of another node has no effect").isEmpty();
	}

	@Test
	public void grant_nodeNotOwnedByGrantor() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_3, TEST_USER_2, orig.getObjectId());

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("A grant cannot give access to a node its owner does not own").isEmpty();
	}

	@Test
	public void grant_sourceWithoutNodes_otherOwnersNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_3, TEST_USER_2, true, null, new String[] { "**" });

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("A source grant only applies to nodes its owner owns").isEmpty();
	}

	@Test
	public void grant_publicAliasNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		setPublic(aliasNodeId);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias on public node would publish a private stream").isEmpty();
	}

	@Test
	public void grant_publicAliasNode_allowPublic() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		publicGrant(TEST_USER_1, TEST_USER_2, new Long[] { orig.getObjectId() }, null);

		// WHEN
		setPublic(aliasNodeId);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Owner agreed to alias on public node").hasSize(1);
	}

	@Test
	public void grant_privateAliasNode_allowPublic() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		publicGrant(TEST_USER_1, TEST_USER_2, new Long[] { orig.getObjectId() }, null);

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Allow public does not require a public node").hasSize(1);
	}

	@Test
	public void grant_publicAliasNode_allowPublicRemoved() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		final UserLongCompositePK id = publicGrant(TEST_USER_1, TEST_USER_2,
				new Long[] { orig.getObjectId() }, null);
		setPublic(aliasNodeId);

		// WHEN
		final ObjectDatumStreamAliasGrantEntity grant = savedGrant(id);
		grant.setAllowPublic(false);
		new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate).save(grant);

		// THEN
		// @formatter:off
		then(metaAliased(alias.getStreamId()))
			.as("Alias on public node stops resolving")
			.isEmpty()
			;
		// @formatter:on

		// WHEN
		jdbcTemplate.update("UPDATE solaruser.user_node SET private = TRUE WHERE node_id = ?",
				aliasNodeId);

		// THEN
		// @formatter:off
		then(metaAliased(alias.getStreamId()))
			.as("The grant itself still applies to a private node")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void grant_publicAliasNode_allowPublicOnOtherGrant() {
		// GIVEN
		final Long nodeId = ownedNode(TEST_USER_1);
		final ObjectDatumStreamMetadata shared = stream(nodeId, "shared/1");
		final ObjectDatumStreamMetadata secret = stream(nodeId, "secret/1");
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var sharedAlias = alias(UUID.randomUUID(), shared, aliasNodeId, "s/alias/shared");
		final var secretAlias = alias(UUID.randomUUID(), secret, aliasNodeId, "s/alias/secret");
		grant(TEST_USER_1, TEST_USER_2, nodeId);
		publicGrant(TEST_USER_1, TEST_USER_2, null, new String[] { "shared/*" });

		// WHEN
		setPublic(aliasNodeId);

		// THEN
		// @formatter:off
		then(metaAliased(sharedAlias.getStreamId()))
			.as("Stream matched by a grant that allows public resolves")
			.hasSize(1)
			;
		then(metaAliased(secretAlias.getStreamId()))
			.as("Allow public on one grant does not extend to streams of another grant")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void grant_publicAliasNode_allowPublic_disabled() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		grant(TEST_USER_1, TEST_USER_2, false, true, new Long[] { orig.getObjectId() }, null);

		// WHEN
		setPublic(aliasNodeId);

		// THEN
		then(metaAliased(alias.getStreamId())).as("Disabled grant ignored").isEmpty();
	}

	@Test
	public void grant_publicAliasNode_allowPublic_otherGrantee() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		publicGrant(TEST_USER_1, TEST_USER_3, new Long[] { orig.getObjectId() }, null);

		// WHEN
		setPublic(aliasNodeId);

		// THEN
		then(metaAliased(alias.getStreamId())).as("Allow public is not for any user").isEmpty();
	}

	@Test
	public void grant_publicAliasNode_allowPublic_archivedOriginalNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		publicGrant(TEST_USER_1, TEST_USER_2, new Long[] { orig.getObjectId() }, null);
		setPublic(aliasNodeId);

		// WHEN
		jdbcTemplate.update("UPDATE solaruser.user_node SET archived = TRUE WHERE node_id = ?",
				orig.getObjectId());

		// THEN
		then(metaAliased(alias.getStreamId())).as("Archived node hidden").isEmpty();
	}

	@Test
	public void grant_publicAliasNode_allowPublic_originalNodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		publicGrant(TEST_USER_1, TEST_USER_2, null, new String[] { "s/*" });
		setPublic(aliasNodeId);

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_3);

		// THEN
		then(metaAliased(alias.getStreamId())).as("Grant follows ownership").isEmpty();
	}

	@Test
	public void grant_archivedOriginalNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		jdbcTemplate.update("UPDATE solaruser.user_node SET archived = TRUE WHERE node_id = ?",
				orig.getObjectId());
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Archived nodes are not shared").isEmpty();
	}

	@Test
	public void grant_sourcePattern() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, true, null, new String[] { "x/*", "s/*" });

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Source pattern matches stream on any node of the owner").hasSize(1);
	}

	@Test
	public void grant_sourcePattern_noMatch() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, true, null, new String[] { "x/*" });

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Source pattern does not match stream").isEmpty();
	}

	@Test
	public void grant_nodeAndSourcePattern() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, true, new Long[] { orig.getObjectId() }, new String[] { "x/*" });

		// WHEN
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Both the node and source must match").isEmpty();
	}

	@Test
	public void grant_originalNodeTransferred() {
		// GIVEN
		final Long otherNodeId = ownedNode(TEST_USER_1);
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		final var grantId = grant(TEST_USER_1, TEST_USER_2, orig.getObjectId(), otherNodeId);

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_3);

		// THEN
		// @formatter:off
		then(metaAliased(alias.getStreamId()))
			.as("Access lost when node transferred")
			.isEmpty()
			;
		then(metaForStream("find_metadata_for_stream", alias.getStreamId()))
			.as("Metadata not available")
			.isEmpty()
			;
		then(metaForStream("find_metadata_for_stream_all", alias.getStreamId()))
			.as("Accounting still resolves")
			.hasSize(1)
			;
		then(savedGrant(grantId).getNodeIds())
			.as("Node removed from grant of old owner")
			.containsExactly(otherNodeId)
			;
		// @formatter:on
	}

	@Test
	public void grant_originalNodeTransferred_newOwnerGrants() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());
		transferNode(orig.getObjectId(), TEST_USER_3);

		// WHEN
		grant(TEST_USER_3, TEST_USER_2, orig.getObjectId());
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Alias resolves when new owner grants access").hasSize(1);
	}

	@Test
	public void grant_originalNodeTransferredBack() {
		// GIVEN
		final Long otherNodeId = ownedNode(TEST_USER_1);
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId(), otherNodeId);
		transferNode(orig.getObjectId(), TEST_USER_3);

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_1);
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Grant does not apply again when node is returned").isEmpty();
	}

	@Test
	public void grant_lastNodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var grantId = grant(TEST_USER_1, TEST_USER_2, orig.getObjectId(), orig.getObjectId());

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_3);

		// THEN
		then(savedGrant(grantId)).as("Grant without nodes deleted, not widened to any node").isNull();
	}

	@Test
	public void grant_lastNodeTransferred_withSources() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var grantId = grant(TEST_USER_1, TEST_USER_2, true, new Long[] { orig.getObjectId() },
				new String[] { "s/*" });

		// WHEN
		transferNode(orig.getObjectId(), TEST_USER_3);

		// THEN
		then(savedGrant(grantId)).as("Grant without nodes deleted, not widened to any node").isNull();
	}

	@Test
	public void grant_aliasNodeTransferred() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final Long aliasNodeId = privateNode(TEST_USER_2);
		final var alias = alias(orig, aliasNodeId);
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		transferNode(aliasNodeId, TEST_USER_3);
		final List<Map<String, Object>> transferred = metaAliased(alias.getStreamId());
		transferNode(aliasNodeId, TEST_USER_2);
		final List<Map<String, Object>> returned = metaAliased(alias.getStreamId());

		// THEN
		// @formatter:off
		then(transferred)
			.as("New owner of alias node is not the grantee")
			.isEmpty()
			;
		then(returned)
			.as("Grant applies again to grantee")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void grant_originalNodeWithoutOwner() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		jdbcTemplate.update("DELETE FROM solaruser.user_node WHERE node_id = ?", orig.getObjectId());
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Grant has no effect on a node without an owner").isEmpty();
	}

	@Test
	public void grant_streamMovedOffNode() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		jdbcTemplate.update("UPDATE solardatm.da_datm_meta SET node_id = ? WHERE stream_id = ?::uuid",
				ownedNode(TEST_USER_1), orig.getStreamId().toString());
		final List<Map<String, Object>> result = metaAliased(alias.getStreamId());

		// THEN
		then(result).as("Grant is evaluated against the node the stream moved to").isEmpty();
	}

	@Test
	public void grant_userView() {
		// GIVEN
		final ObjectDatumStreamMetadata orig = stream(ownedNode(TEST_USER_1));
		final var alias = alias(orig, privateNode(TEST_USER_2));
		grant(TEST_USER_1, TEST_USER_2, orig.getObjectId());

		// WHEN
		final List<Map<String, Object>> result = jdbcTemplate.queryForList(
				"SELECT user_id FROM solaruser.da_datm_meta_aliased WHERE stream_id = ?::uuid",
				alias.getStreamId().toString());

		// THEN
		// @formatter:off
		then(result)
			.as("Alias is owned by owner of alias node")
			.containsExactly(Map.of("user_id", TEST_USER_2))
			;
		// @formatter:on
	}

	@Test
	public void grant_self() {
		thenThrownBy(() -> grant(TEST_USER_1, TEST_USER_1, 1L)).as("Cannot grant to self")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void grant_emptyNodes() {
		thenThrownBy(() -> grant(TEST_USER_1, TEST_USER_2, true, new Long[0], new String[] { "a" }))
				.as("Empty node list is not a wildcard")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void grant_emptySources() {
		thenThrownBy(() -> grant(TEST_USER_1, TEST_USER_2, true, new Long[] { 1L }, new String[0]))
				.as("Empty source list is not a wildcard")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void grant_noNodesOrSources() {
		thenThrownBy(() -> grant(TEST_USER_1, TEST_USER_2, true, null, null))
				.as("Cannot grant an entire account")
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	public void aliasAllowed_parallelSafe() {
		// WHEN
		final String result = jdbcTemplate.queryForObject("""
				SELECT proparallel FROM pg_proc WHERE oid =
					'solardatm.da_datm_alias_allowed(bigint,text,bigint)'::regprocedure
				""", String.class);

		// THEN
		then(result).as("Function must not disable parallel query plans").isEqualTo("s");
	}

	private List<ObjectDatumStreamAliasGrantStream> availableStreams(Long granteeId, Long aliasNodeId,
			BasicObjectDatumStreamAliasGrantFilter filter) {
		final var results = new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate)
				.findAvailableStreams(granteeId, aliasNodeId, filter);
		return StreamSupport.stream(results.spliterator(), false).toList();
	}

	private List<ObjectDatumStreamAliasGrantStream> availableStreams(Long granteeId, Long aliasNodeId) {
		return availableStreams(granteeId, aliasNodeId, null);
	}

	private static ObjectDatumStreamAliasGrantStream available(Long userId,
			ObjectDatumStreamMetadata meta) {
		return new ObjectDatumStreamAliasGrantStream(userId, meta.getObjectId(), meta.getSourceId());
	}

	@Test
	public void availableStreams_nodeGrant() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		final ObjectDatumStreamMetadata meta1 = stream(nodeId, "s/1");
		final ObjectDatumStreamMetadata meta2 = stream(nodeId, "s/2");
		stream(ownedNode(TEST_USER_2), "s/other-node");
		grant(TEST_USER_2, TEST_USER_1, nodeId);

		// WHEN
		final var result = availableStreams(TEST_USER_1, hostNodeId);

		// THEN
		// @formatter:off
		then(result)
			.as("Only streams of granted node available")
			.containsExactly(available(TEST_USER_2, meta1), available(TEST_USER_2, meta2))
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_sourceGrant() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		final ObjectDatumStreamMetadata meta1 = stream(nodeId, "shared/1");
		stream(nodeId, "secret/1");
		final ObjectDatumStreamMetadata meta2 = stream(ownedNode(TEST_USER_2), "shared/2");
		grant(TEST_USER_2, TEST_USER_1, true, null, new String[] { "shared/*" });

		// WHEN
		final var result = availableStreams(TEST_USER_1, hostNodeId);

		// THEN
		// @formatter:off
		then(result)
			.as("Only streams matching source pattern available, on any node of the owner")
			.containsExactlyInAnyOrder(available(TEST_USER_2, meta1), available(TEST_USER_2, meta2))
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_noGrant() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		stream(ownedNode(TEST_USER_2));

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Nothing available without grant").isEmpty();
	}

	@Test
	public void availableStreams_disabledGrant() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, false, new Long[] { nodeId }, null);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Disabled grant ignored").isEmpty();
	}

	@Test
	public void availableStreams_otherGrantee() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_3, nodeId);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Grant to other user ignored").isEmpty();
	}

	@Test
	public void availableStreams_publicAliasNode() {
		// GIVEN
		final Long hostNodeId = ownedNode(TEST_USER_1);
		setPublic(hostNodeId);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, nodeId);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Public node cannot host").isEmpty();
	}

	@Test
	public void availableStreams_publicAliasNode_allowPublic() {
		// GIVEN
		final Long hostNodeId = ownedNode(TEST_USER_1);
		setPublic(hostNodeId);
		final Long nodeId = ownedNode(TEST_USER_2);
		final ObjectDatumStreamMetadata shared = stream(nodeId, "shared/1");
		stream(nodeId, "secret/1");
		grant(TEST_USER_2, TEST_USER_1, nodeId);
		publicGrant(TEST_USER_2, TEST_USER_1, null, new String[] { "shared/*" });

		// THEN
		// @formatter:off
		then(availableStreams(TEST_USER_1, hostNodeId))
			.as("Only streams of grants that allow public available for public node")
			.containsExactly(available(TEST_USER_2, shared))
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_otherUserAliasNode() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_3);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, nodeId);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Alias node of other user").isEmpty();
	}

	@Test
	public void availableStreams_archivedOriginalNode() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, nodeId);
		jdbcTemplate.update("UPDATE solaruser.user_node SET archived = TRUE WHERE node_id = ?", nodeId);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Archived node hidden").isEmpty();
	}

	@Test
	public void availableStreams_grantNamesNodeOfOtherOwner() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_3);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, nodeId);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Cannot grant unowned node").isEmpty();
	}

	@Test
	public void availableStreams_originalNodeTransferred() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, true, null, new String[] { "s/*" });

		// WHEN
		transferNode(nodeId, TEST_USER_3);

		// THEN
		then(availableStreams(TEST_USER_1, hostNodeId)).as("Grant follows ownership").isEmpty();
	}

	@Test
	public void availableStreams_overlappingGrants() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		final ObjectDatumStreamMetadata meta = stream(nodeId);
		grant(TEST_USER_2, TEST_USER_1, nodeId);
		grant(TEST_USER_2, TEST_USER_1, true, null, new String[] { "s/*" });

		// THEN
		// @formatter:off
		then(availableStreams(TEST_USER_1, hostNodeId))
			.as("Stream listed once when matched by several grants")
			.containsExactly(available(TEST_USER_2, meta))
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_filtered() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId2 = ownedNode(TEST_USER_2);
		final Long nodeId3 = ownedNode(TEST_USER_3);
		final ObjectDatumStreamMetadata meta2 = stream(nodeId2);
		final ObjectDatumStreamMetadata meta3 = stream(nodeId3);
		final UserLongCompositePK grant2 = grant(TEST_USER_2, TEST_USER_1, nodeId2);
		grant(TEST_USER_3, TEST_USER_1, nodeId3);

		// WHEN
		final var byOwner = new BasicObjectDatumStreamAliasGrantFilter();
		byOwner.setUserId(TEST_USER_3);
		final var byGrant = new BasicObjectDatumStreamAliasGrantFilter();
		byGrant.setGrantId(grant2.getEntityId());

		// THEN
		// @formatter:off
		then(availableStreams(TEST_USER_1, hostNodeId, byOwner))
			.as("Limited to owner")
			.containsExactly(available(TEST_USER_3, meta3))
			;
		then(availableStreams(TEST_USER_1, hostNodeId, byGrant))
			.as("Limited to grant")
			.containsExactly(available(TEST_USER_2, meta2))
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_paged() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		stream(nodeId, "s/1");
		stream(nodeId, "s/2");
		final ObjectDatumStreamMetadata meta3 = stream(nodeId, "s/3");
		grant(TEST_USER_2, TEST_USER_1, nodeId);

		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setMax(2);
		filter.setOffset(2L);

		// WHEN
		final var result = new JdbcObjectDatumStreamAliasGrantEntityDao(jdbcTemplate)
				.findAvailableStreams(TEST_USER_1, hostNodeId, filter);

		// THEN
		// @formatter:off
		then(result.getResults())
			.as("Page of results returned")
			.containsExactly(available(TEST_USER_2, meta3))
			;
		then(result.getTotalResults())
			.as("Total counts all streams")
			.isEqualTo(3L)
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_agreesWithAliasAllowed() {
		// GIVEN
		final Long hostNodeId = privateNode(TEST_USER_1);
		final Long nodeId = ownedNode(TEST_USER_2);
		final ObjectDatumStreamMetadata shared = stream(nodeId, "shared/1");
		final ObjectDatumStreamMetadata secret = stream(nodeId, "secret/1");
		grant(TEST_USER_2, TEST_USER_1, true, new Long[] { nodeId }, new String[] { "shared/*" });

		final var aliasDao = new JdbcObjectDatumStreamAliasEntityDao(jdbcTemplate);

		// THEN
		// @formatter:off
		then(availableStreams(TEST_USER_1, hostNodeId))
			.as("Listed stream")
			.containsExactly(available(TEST_USER_2, shared))
			;
		then(aliasDao.aliasAllowed(nodeId, shared.getSourceId(), hostNodeId))
			.as("Listed stream can be aliased")
			.isTrue()
			;
		then(aliasDao.aliasAllowed(nodeId, secret.getSourceId(), hostNodeId))
			.as("Unlisted stream cannot be aliased")
			.isFalse()
			;
		// @formatter:on
	}

}
