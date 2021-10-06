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

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenStatus.Disabled;
import static net.solarnetwork.central.security.SecurityTokenType.ReadNodeData;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link JdbcSolarNodeOwnershipDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class JdbcSolarNodeOwnershipDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final Long TEST_USER_ID = Long.valueOf(-9999);
	private static final String TEST_USERNAME = "unittest@localhost";
	private static final Long TEST_USER_ID_2 = Long.valueOf(-9998);
	private static final String TEST_USERNAME_2 = "unittest2@localhost";

	private JdbcSolarNodeOwnershipDao dao;

	@Mock
	private Cache<Long, SolarNodeOwnership> cache;

	@Captor
	private ArgumentCaptor<SolarNodeOwnership> ownershipCaptor;

	private void setupTestUser(Long id, String username) {
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
		assertThat("Null returned when no match", ownership, nullValue());
	}

	@Test
	public void ownershipForNodeId_noMatch_cache() {
		// GIVEN
		dao.setUserNodeCache(cache);
		given(cache.get(TEST_NODE_ID)).willReturn(null);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		assertThat("Null returned when no match", ownership, nullValue());
	}

	@Test
	public void ownershipForNodeId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		assertThat("Ownership found", expected.isSameAs(ownership), equalTo(true));
	}

	@Test
	public void ownershipForNodeId_match_cacheMiss() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		dao.setUserNodeCache(cache);

		// test cache first
		given(cache.get(TEST_NODE_ID)).willReturn(null);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		assertThat("Ownership found", expected.isSameAs(ownership), is(true));

		// DB result was added to cache
		verify(cache).put(eq(TEST_NODE_ID), ownershipCaptor.capture());
		assertThat("Ownership info cached", expected.isSameAs(ownershipCaptor.getValue()), is(true));
	}

	@Test
	public void ownershipForNodeId_match_cacheHit() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		dao.setUserNodeCache(cache);

		// test cache first
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		given(cache.get(TEST_NODE_ID)).willReturn(expected);

		// WHEN
		SolarNodeOwnership ownership = dao.ownershipForNodeId(TEST_NODE_ID);

		// THEN
		assertThat("Ownership found in cache", ownership, sameInstance(expected));
	}

	@Test
	public void ownershipsForUserId_noMatch() {
		// GIVEN

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("Null returned when no match", ownerships, nullValue());
	}

	@Test
	public void ownershipsForUserId_match() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "Test Node");

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("One match returned", ownerships, is(arrayWithSize(1)));
		BasicSolarNodeOwnership expected = ownershipFor(TEST_NODE_ID, TEST_USER_ID, TEST_LOC_COUNTRY,
				"Pacific/Auckland");
		assertThat("Ownership returned when single match", expected.isSameAs(ownerships[0]), is(true));
	}

	@Test
	public void ownershipsForUserId_matchMulti() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
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
		setupTestUser(TEST_USER_ID_2, TEST_USERNAME_2);
		for ( int i = 0; i < 5; i++ ) {
			Long nodeId = r.nextLong();
			setupTestNode(nodeId, TEST_LOC_ID);
			setupTestUserNode(TEST_USER_ID_2, nodeId, "User 2 Test Node " + i);
		}
		Collections.sort(expected, Comparator.comparing(BasicSolarNodeOwnership::getNodeId));

		// WHEN
		SolarNodeOwnership[] ownerships = dao.ownershipsForUserId(TEST_USER_ID);

		// THEN
		assertThat("One match returned", ownerships, is(arrayWithSize(expected.size())));
		for ( int i = 0; i < expected.size(); i++ ) {
			assertThat("Ownership returned in node order for multi match",
					expected.get(i).isSameAs(ownerships[i]), is(true));
		}
	}

	private static String randomTokenId() {
		return java.util.UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
	}

	private void setupTestUserNode() {
		setupTestLocation(TEST_LOC_ID, "Pacific/Auckland");
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
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
		setupTestUser(TEST_USER_ID_2, TEST_USERNAME_2);
		final String tokenId = setupTestToken(TEST_USER_ID_2, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);
		assertThat("No nodes returned for user with no nodes", nodeIds, arrayWithSize(0));
	}

	@Test
	public void findNodeIdsForUserTokenSingleNode() {
		setupTestUserNode();
		final String tokenId = setupTestToken(TEST_USER_ID, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);
		assertThat(nodeIds, arrayContaining(TEST_NODE_ID));
	}

	@Test
	public void findNodeIdsForUserTokenSingleNodeTokenDisabled() {
		setupTestUserNode();
		final String tokenId = setupTestToken(TEST_USER_ID, User, Disabled);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);
		assertThat("No nodes returned because token disabled", nodeIds, arrayWithSize(0));
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodes() {
		SortedSet<Long> expectedNodeIds = new TreeSet<>();
		setupTestLocation();
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_NODE_ID - i;
			setupTestNode(nodeId);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
			expectedNodeIds.add(nodeId);
		}

		final String tokenId = setupTestToken(TEST_USER_ID, User);

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);
		assertThat("All nodes for user returned in node ID order", nodeIds,
				arrayContaining(expectedNodeIds.toArray(Long[]::new)));
	}

	@Test
	public void findNodeIdsForUserTokenMultipleNodesFilteredByPolicy() {
		setupTestLocation();
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = TEST_NODE_ID - i;
			setupTestNode(nodeId);
			setupTestUserNode(TEST_USER_ID, nodeId, "Test Node " + i);
		}

		final String tokenId = setupTestToken(TEST_USER_ID, User, Active, BasicSecurityPolicy.builder()
				.withNodeIds(new HashSet<>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID - 1))).build());

		Long[] nodeIds = dao.nonArchivedNodeIdsForToken(tokenId);
		assertThat("Policy filtered user nodes, in node ID order", nodeIds,
				arrayContaining(TEST_NODE_ID - 1, TEST_NODE_ID));
	}

	@Test
	public void findNodeIdsForReadNodeDataTokenMultipleNodesFilteredByPolicy() {
		setupTestLocation();
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
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
		assertThat("Policy filtered user nodes, in node ID order", nodeIds,
				arrayContaining(TEST_NODE_ID - 1, TEST_NODE_ID));
	}

}
