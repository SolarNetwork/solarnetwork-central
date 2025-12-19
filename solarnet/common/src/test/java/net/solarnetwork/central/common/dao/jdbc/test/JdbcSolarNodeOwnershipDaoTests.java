/* ==================================================================
 * JdbcSolarNodeOwnershipDaoTests.java - 28/02/2020 3:12:46 pm
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static java.lang.String.format;
import static net.solarnetwork.central.common.dao.jdbc.CommonDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.domain.ObjectDatumStreamMetadataId.idForMetadata;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenStatus.Disabled;
import static net.solarnetwork.central.security.SecurityTokenType.ReadNodeData;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link JdbcSolarNodeOwnershipDao} class.
 * 
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcSolarNodeOwnershipDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final Long TEST_USER_ID = Long.valueOf(-9999);
	private static final String TEST_USERNAME = "unittest@localhost";
	private static final Long TEST_USER_ID_2 = Long.valueOf(-9998);
	private static final String TEST_USERNAME_2 = "unittest2@localhost";

	private JdbcSolarNodeOwnershipDao dao;

	@Mock
	private Cache<Long, SolarNodeOwnership> cache;

	@Mock
	private Cache<UUID, ObjectDatumStreamMetadataId> streamIdCache;

	@Captor
	private ArgumentCaptor<SolarNodeOwnership> ownershipCaptor;

	@Captor
	private ArgumentCaptor<UUID> uuidCaptor;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamMetadataId> streamIdCaptor;

	private void setupTestUserInternal(Long id, String username) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id,email,password,disp_name,enabled) values (?,?,?,?,?)",
				id, username, DigestUtils.sha256Hex("password"), "Unit Test", Boolean.TRUE);
	}

	private void setupTestUserNode(Long userId, Long nodeId, String name) {
		jdbcTemplate.update("insert into solaruser.user_node (user_id,node_id,disp_name) values (?,?,?)",
				userId, nodeId, name);
	}

	@BeforeEach
	public void setup() {
		dao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
	}

	@Test
	public void ownershipForNodeId_noMatch() {
		// GIVEN

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		and.then(ownership).as("Null returned when no match").isNull();
	}

	@Test
	public void ownershipForNodeId_noMatch_cache() {
		// GIVEN
		dao.setNodeOwnershipCache(cache);
		given(cache.get(TEST_NODE_ID)).willReturn(null);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		and.then(ownership).as("Null returned when no match").isNull();
	}

	@Test
	public void ownershipForNodeId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		and.then(ownership).as("Ownership found").isEqualTo(expected);
	}

	@Test
	public void ownershipForNodeId_match_cacheMiss() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		dao.setNodeOwnershipCache(cache);

		// test cache first
		given(cache.get(TEST_NODE_ID)).willReturn(null);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		then(cache).should().put(eq(TEST_NODE_ID), ownershipCaptor.capture());
		and.then(ownershipCaptor.getValue()).as("Ownership info cached").isSameAs(ownership);

		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		and.then(ownership).as("Ownership found").isEqualTo(expected);
	}

	@Test
	public void ownershipForNodeId_match_cacheHit() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		dao.setNodeOwnershipCache(cache);

		// test cache first
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		given(cache.get(TEST_NODE_ID)).willReturn(expected);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		and.then(ownership).as("Ownership found in cache").isSameAs(expected);
	}

	@Test
	public void ownershipsForUserId_noMatch() {
		// GIVEN

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		and.then(ownerships).as("Null returned when no match").isNull();
	}

	@Test
	public void ownershipsForUserId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		// @formatter:off
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		and.then(ownerships)
			.as("One match returned")
			.hasSize(1)
			.singleElement()
			.as("Ownership returned when single match")
			.isEqualTo(expected)
			;
		// @formatter:on
	}

	@Test
	public void ownershipsForUserId_matchMulti() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		List<BasicSolarNodeOwnership> expected = new ArrayList<>(5);
		SecureRandom r = new SecureRandom();
		for ( int i = 0; i < 5; i++ ) {
			Long nodeId = r.nextLong();
			setupTestNode(nodeId, TEST_LOC_ID);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
			expected.add(BasicSolarNodeOwnership.ownershipFor(nodeId, TEST_USER_ID, TEST_LOC_COUNTRY,
					"Pacific/Auckland"));
		}
		// toss in some nodes for a different user, to verify they are NOT returned
		setupTestUserInternal(TEST_USER_ID_2, TEST_USERNAME_2);
		for ( int i = 0; i < 5; i++ ) {
			Long nodeId = r.nextLong();
			setupTestNode(nodeId, TEST_LOC_ID);
			setupTestUserNode(TEST_USER_ID_2, nodeId, "User 2 Test Node " + i);
		}
		Collections.sort(expected, Comparator.comparing(BasicSolarNodeOwnership::getNodeId));

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		// @formatter:off
		and.then(ownerships)
			.as("Ownership returned in node order for multi match")
			.containsExactlyElementsOf(expected)
			;
		// @formatter:on
	}

	private static String randomTokenId() {
		return java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
	}

	private void setupTestUserNode() {
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");
	}

	private String setupTestToken(Long userId, SecurityTokenType type) {
		return setupTestToken(userId, type, Active);
	}

	private String setupTestToken(Long userId, SecurityTokenType type, SecurityTokenStatus status) {
		return setupTestToken(userId, type, status, null);
	}

	private String setupTestToken(Long userId, SecurityTokenType type, SecurityTokenStatus status,
			SecurityPolicy policy) {
		final String tokenId = randomTokenId();
		String policyJson = JsonUtils.getJSONString(policy, null);
		insertSecurityToken(jdbcTemplate, tokenId, "pw", userId, status.name(), type.name(), policyJson);
		return tokenId;
	}

	@Test
	public void findNodeIdsForUserTokenNoNodes() {
		// create some OTHER user with a node, to be sure
		setupTestUserNode();

		// create a new user without any nodes
		setupTestUserInternal(TEST_USER_ID_2, TEST_USERNAME_2);
		final String tokenId = setupTestToken(TEST_USER_ID_2, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("No nodes returned for user with no nodes")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void findNodeIdsForUserTokenSingleNode() {
		setupTestUserNode();
		final String tokenId = setupTestToken(TEST_USER_ID, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("Node returned for token")
			.containsExactly(TEST_NODE_ID)
			;
		// @formatter:on
	}

	@Test
	public void findNodeIdsForUserTokenSingleNodeTokenDisabled() {
		setupTestUserNode();
		final String tokenId = setupTestToken(TEST_USER_ID, User, Disabled);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("No nodes returned because token disabled")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodes() {
		SortedSet<Long> expectedNodeIds = new TreeSet<>();
		setupTestLocation();
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_NODE_ID - i;
			setupTestNode(nodeId);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
			expectedNodeIds.add(nodeId);
		}

		final String tokenId = setupTestToken(TEST_USER_ID, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("All nodes for user returned in node ID order")
			.containsExactlyElementsOf(expectedNodeIds)
			;
		// @formatter:on
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodesFilteredByPolicy() {
		setupTestLocation();
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_NODE_ID - i;
			setupTestNode(nodeId);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
		}

		final String tokenId = setupTestToken(TEST_USER_ID, User, Active, BasicSecurityPolicy.builder()
				.withNodeIds(new HashSet<>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID - 1))).build());

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("Policy filtered user nodes, in node ID order")
			.containsExactly(TEST_NODE_ID - 1, TEST_NODE_ID)
			;
		// @formatter:on
	}

	@Test
	public void findNodeIdsForReadNodeDataTokenMultipleNodesFilteredByPolicy() {
		setupTestLocation();
		setupTestUserInternal(TEST_USER_ID, TEST_USERNAME);
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_NODE_ID - i;
			setupTestNode(nodeId);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
		}

		final String tokenId = setupTestToken(TEST_USER_ID, ReadNodeData, Active,
				BasicSecurityPolicy.builder()
						.withNodeIds(new HashSet<>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID - 1)))
						.build());

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);

		// @formatter:off
		and.then(nodeIds)
			.as("Policy filtered user nodes, in node ID order")
			.containsExactly(TEST_NODE_ID - 1, TEST_NODE_ID)
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					format("s%d", i), new String[] { "a", "b", "c" }, new String[] { "d", "e" },
					new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		Map<UUID, ObjectDatumStreamMetadataId> results = dao
				.getDatumStreamMetadataIds(streamIds.toArray(new UUID[streamIds.size()]));

		// THEN
		final List<ObjectDatumStreamMetadataId> ids = data.stream()
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		// @formatter:off
		and.then(results)
			.as("Stream IDs same")
			.containsOnlyKeys(streamIds)
			.values()
			.as("Metadata IDs same and ordered")
			.containsExactlyElementsOf(ids)
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds_someMissing() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			if ( i < 3 ) {
				data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
						(long) i, format("s%d", i), new String[] { "a", "b", "c" },
						new String[] { "d", "e" }, new String[] { "f" }));
			}

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		Map<UUID, ObjectDatumStreamMetadataId> results = dao
				.getDatumStreamMetadataIds(streamIds.toArray(new UUID[streamIds.size()]));

		// THEN
		final List<ObjectDatumStreamMetadataId> ids = data.stream()
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		// @formatter:off
		and.then(results)
			.as("Stream IDs for all available")
			.containsOnlyKeys(data.get(0).getStreamId(), data.get(1).getStreamId())
			.values()
			.as("Metadata IDs same and ordered")
			.containsExactlyElementsOf(ids)
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds_cacheMiss() {
		// GIVEN
		dao.setStreamMetadataIdCache(streamIdCache);

		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);

		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					format("s%d", i), new String[] { "a", "b", "c" }, new String[] { "d", "e" },
					new String[] { "f" }));
		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		Map<UUID, ObjectDatumStreamMetadataId> results = dao
				.getDatumStreamMetadataIds(streamIds.toArray(new UUID[streamIds.size()]));

		// THEN
		final List<ObjectDatumStreamMetadataId> ids = data.stream()
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		// @formatter:off
		then(streamIdCache).should(times(3)).put(uuidCaptor.capture(), streamIdCaptor.capture());
		and.then(uuidCaptor.getAllValues())
			.as("Cached stream ID keys")
			.containsExactlyElementsOf(streamIds)
			;
		and.then(streamIdCaptor.getAllValues())
			.as("Cached stream ID values")
			.containsExactlyElementsOf(ids)
			;
		
		and.then(results)
			.as("Stream IDs same")
			.containsOnlyKeys(streamIds)
			.values()
			.as("Metadata IDs same and ordered")
			.containsExactlyElementsOf(ids)
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds_cacheHit() {
		dao.setStreamMetadataIdCache(streamIdCache);

		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);

		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
					ObjectDatumKind.Node, (long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" });
			data.add(meta);
			given(streamIdCache.get(streamId)).willReturn(idForMetadata(meta));
		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		final List<ObjectDatumStreamMetadataId> ids = data.stream()
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		// WHEN
		Map<UUID, ObjectDatumStreamMetadataId> results = dao
				.getDatumStreamMetadataIds(streamIds.toArray(new UUID[streamIds.size()]));

		// THEN
		// @formatter:off
		and.then(results)
			.as("Stream IDs same")
			.containsOnlyKeys(streamIds)
			.values()
			.as("Metadata IDs same and ordered")
			.containsExactlyElementsOf(ids)
			;
		// @formatter:on
	}

	@Test
	public void getDatumStreamMetadataIds_cacheMix() {
		dao.setStreamMetadataIdCache(streamIdCache);

		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);

		final List<UUID> streamIdsAddedToCache = new ArrayList<>();
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
					ObjectDatumKind.Node, (long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" });
			data.add(meta);
			ObjectDatumStreamMetadataId foundId = (i == 2 ? idForMetadata(meta) : null);
			given(streamIdCache.get(streamId)).willReturn(foundId);
			if ( foundId == null ) {
				streamIdsAddedToCache.add(streamId);
			}
		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		Map<UUID, ObjectDatumStreamMetadataId> results = dao
				.getDatumStreamMetadataIds(streamIds.toArray(new UUID[streamIds.size()]));

		// THEN
		final List<ObjectDatumStreamMetadataId> ids = data.stream()
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		final List<ObjectDatumStreamMetadataId> idsAddedToCache = data.stream()
				.filter(meta -> streamIdsAddedToCache.contains(meta.getStreamId()))
				.map(ObjectDatumStreamMetadataId::idForMetadata).toList();

		// @formatter:off
		then(streamIdCache).should(times(streamIdsAddedToCache.size())).put(uuidCaptor.capture(), streamIdCaptor.capture());
		and.then(uuidCaptor.getAllValues())
			.as("Cached stream ID keys")
			.containsExactlyElementsOf(streamIdsAddedToCache)
			;
		and.then(streamIdCaptor.getAllValues())
			.as("Cached stream ID values")
			.containsExactlyElementsOf(idsAddedToCache)
			;
		
		and.then(results)
			.as("Stream IDs same")
			.containsOnlyKeys(streamIds)
			.values()
			.as("Metadata IDs same and ordered")
			.containsOnlyOnceElementsOf(ids)
			;
		// @formatter:on
	}
}
