/* ==================================================================
 * DaoUserBizTest.java - Dec 12, 2012 3:46:49 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Test cases for the {@link DaoUserBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoUserBizTest {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_ENC_PASSWORD = "encrypted.password";
	private static final String TEST_NAME = "Test User";
	private static final String TEST_ROLE = "ROLE_TEST";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_AUTH_SECRET = "123";
	private static final Long TEST_LOC_ID = -2L;
	private static final Long TEST_NODE_ID = -3L;
	private static final Long TEST_NODE_ID_2 = -4L;
	private static final Long TEST_USER_ID_2 = -5L;

	private SolarNode testNode;
	private User testUser;
	private Set<String> testUserRoles;

	private SolarLocationDao solarLocationDao;
	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserAuthTokenDao userAuthTokenDao;
	private UserNodeDao userNodeDao;
	private UserAlertDao userAlertDao;

	private DaoUserBiz userBiz;

	@Before
	public void setup() {
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setName(TEST_NAME);
		testUser.setPassword(TEST_ENC_PASSWORD);

		testNode = new SolarNode();
		testNode.setId(TEST_NODE_ID);
		testNode.setLocationId(TEST_LOC_ID);

		testUserRoles = new HashSet<String>();
		testUserRoles.add(TEST_ROLE);

		solarLocationDao = EasyMock.createMock(SolarLocationDao.class);
		solarNodeDao = EasyMock.createMock(SolarNodeDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userAuthTokenDao = EasyMock.createMock(UserAuthTokenDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userAlertDao = EasyMock.createMock(UserAlertDao.class);

		userBiz = new DaoUserBiz();
		userBiz.setSolarLocationDao(solarLocationDao);
		userBiz.setSolarNodeDao(solarNodeDao);
		userBiz.setUserDao(userDao);
		userBiz.setUserAuthTokenDao(userAuthTokenDao);
		userBiz.setUserNodeDao(userNodeDao);
		userBiz.setUserAlertDao(userAlertDao);
	}

	private void replayProperties() {
		replay(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao, userAlertDao);
	}

	private void verifyProperties() {
		verify(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao, userAlertDao);
	}

	@Test
	public void generateUserAuthToken() {
		expect(userAuthTokenDao.get(anyObject(String.class))).andReturn(null);
		expect(userAuthTokenDao.store(anyObject(UserAuthToken.class))).andReturn(TEST_AUTH_TOKEN);
		replayProperties();
		UserAuthToken generated = userBiz.generateUserAuthToken(TEST_USER_ID, SecurityTokenType.User,
				(SecurityPolicy) null);
		assertNotNull(generated);
		assertNotNull(generated.getAuthToken());
		assertEquals("Auth token should be exactly 20 characters", 20,
				generated.getAuthToken().length());
		assertNotNull(generated.getAuthSecret());
		assertEquals(TEST_USER_ID, generated.getUserId());
		assertEquals(SecurityTokenStatus.Active, generated.getStatus());
		verifyProperties();
	}

	@Test
	public void deleteUserAuthToken() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);
		userAuthTokenDao.delete(same(token));
		replayProperties();
		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);
		verifyProperties();
	}

	@Test
	public void deleteUserAuthTokenNotFound() {
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(null);
		replayProperties();
		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);
		verifyProperties();
	}

	@Test
	public void deleteUserAuthTokenWrongUser() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);
		replayProperties();
		try {
			userBiz.deleteUserAuthToken(TEST_USER_ID - 1L, TEST_AUTH_TOKEN);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.ACCESS_DENIED, e.getReason());
			assertEquals(TEST_AUTH_TOKEN, e.getId());
		}
		verifyProperties();
	}

	@Test
	public void updateSecurityTokenStatus() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);

		Capture<UserAuthToken> tokenCapture = new Capture<UserAuthToken>();
		expect(userAuthTokenDao.store(EasyMock.capture(tokenCapture))).andReturn(TEST_AUTH_TOKEN);

		replayProperties();

		UserAuthToken updated = userBiz.updateUserAuthTokenStatus(TEST_USER_ID, TEST_AUTH_TOKEN,
				SecurityTokenStatus.Disabled);

		verifyProperties();

		assertNotNull("Updated token", updated);
		assertEquals("Updated token ID", TEST_AUTH_TOKEN, updated.getAuthToken());
		assertEquals("Token secret", TEST_AUTH_SECRET, updated.getAuthSecret());
		assertEquals("Token user", TEST_USER_ID, updated.getUserId());
		assertEquals("Token state", SecurityTokenStatus.Disabled, updated.getStatus());

		assertSame("Persisted token", updated, tokenCapture.getValue());
	}

	@Test
	public void replaceUserAuthTokenPolicy() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Day).withMinLocationPrecision(LocationPrecision.Block)
				.build();
		token.setPolicy(policy);

		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);

		Capture<UserAuthToken> tokenCapture = new Capture<UserAuthToken>();
		expect(userAuthTokenDao.store(EasyMock.capture(tokenCapture))).andReturn(TEST_AUTH_TOKEN);

		replayProperties();

		final BasicSecurityPolicy newPolicy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		UserAuthToken updated = userBiz.updateUserAuthTokenPolicy(TEST_USER_ID, TEST_AUTH_TOKEN,
				newPolicy, true);

		verifyProperties();

		assertNotNull("Updated token", updated);
		assertEquals("Updated token ID", TEST_AUTH_TOKEN, updated.getAuthToken());
		assertEquals("Token secret", TEST_AUTH_SECRET, updated.getAuthSecret());
		assertEquals("Token user", TEST_USER_ID, updated.getUserId());
		assertEquals("Token policy", newPolicy, updated.getPolicy());

		assertSame("Persisted token", updated, tokenCapture.getValue());
	}

	@Test
	public void mergeUserAuthTokenPolicy() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		final BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Day).withMinLocationPrecision(LocationPrecision.Block)
				.build();
		token.setPolicy(policy);

		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);

		Capture<UserAuthToken> tokenCapture = new Capture<UserAuthToken>();
		expect(userAuthTokenDao.store(EasyMock.capture(tokenCapture))).andReturn(TEST_AUTH_TOKEN);

		replayProperties();

		final BasicSecurityPolicy policyPatch = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		UserAuthToken updated = userBiz.updateUserAuthTokenPolicy(TEST_USER_ID, TEST_AUTH_TOKEN,
				policyPatch, false);

		verifyProperties();

		assertNotNull("Updated token", updated);
		assertEquals("Updated token ID", TEST_AUTH_TOKEN, updated.getAuthToken());
		assertEquals("Token secret", TEST_AUTH_SECRET, updated.getAuthSecret());
		assertEquals("Token user", TEST_USER_ID, updated.getUserId());

		BasicSecurityPolicy expectedPolicy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).withMinLocationPrecision(LocationPrecision.Block)
				.build();

		assertEquals("Token policy", expectedPolicy, updated.getPolicy());

		assertSame("Persisted token", updated, tokenCapture.getValue());
	}

	@Test
	public void saveUserNodeNoLocationChange() {
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");

		expect(userNodeDao.get(testNode.getId())).andReturn(userNode);
		expect(solarLocationDao.getSolarLocationForLocation(EasyMock.isA(loc.getClass())))
				.andReturn(loc);
		expect(userNodeDao.store(userNode)).andReturn(testNode.getId());

		replayProperties();

		UserNode entry = new UserNode(testUser, (SolarNode) testNode.clone());
		entry.getNode().setLocation(loc);

		UserNode result = userBiz.saveUserNode(entry);
		Assert.assertEquals(userNode, result);

		verifyProperties();
	}

	public static SolarNode nodeLocationMatch(final Long nodeId, final Long locId) {
		EasyMock.reportMatcher(new IArgumentMatcher() {

			private Long nid;
			private Long lid;

			@Override
			public boolean matches(Object argument) {
				SolarNode node = (SolarNode) argument;
				nid = (node == null ? null : node.getId());
				lid = (node == null ? null : node.getLocationId());
				return (nodeId.equals(nid) && locId.equals(lid));
			}

			@Override
			public void appendTo(StringBuffer buffer) {
				if ( !nodeId.equals(nid) ) {
					buffer.append("SolarNode expected (" + nodeId + ") got (" + nid + ") ");
				}
				if ( !locId.equals(lid) ) {
					buffer.append("SolarNode location expected (" + locId + ") got (" + lid + ")");
				}
			}
		});
		return null;
	}

	@Test
	public void saveUserNodeLocationChange() {
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");

		SolarLocation locMatch = new SolarLocation();
		locMatch.setId(-9L);
		locMatch.setName("bar");

		expect(userNodeDao.get(testNode.getId())).andReturn(userNode);
		expect(solarLocationDao.getSolarLocationForLocation(EasyMock.isA(loc.getClass())))
				.andReturn(locMatch);
		expect(solarNodeDao.store(nodeLocationMatch(testNode.getId(), -9L))).andReturn(testNode.getId());
		expect(userNodeDao.store(userNode)).andReturn(testNode.getId());

		replayProperties();

		UserNode entry = new UserNode(testUser, (SolarNode) testNode.clone());
		entry.getNode().setLocation(loc);

		UserNode result = userBiz.saveUserNode(entry);
		Assert.assertEquals(userNode, result);

		verifyProperties();
	}

	@Test
	public void saveUserNodeNewLocation() {
		final UserNode userNode = new UserNode();
		userNode.setCreated(Instant.now());
		userNode.setDescription("Test user node");
		userNode.setName("Test UserNode");
		userNode.setRequiresAuthorization(true);
		userNode.setUser(testUser);
		userNode.setNode(testNode);

		SolarLocation loc = new SolarLocation();
		loc.setId(testNode.getLocationId());
		loc.setName("foo");

		SolarLocation newLoc = new SolarLocation();
		newLoc.setId(-99L);

		expect(userNodeDao.get(testNode.getId())).andReturn(userNode);
		expect(solarLocationDao.getSolarLocationForLocation(EasyMock.isA(loc.getClass())))
				.andReturn(null);
		expect(solarLocationDao.store(EasyMock.isA(loc.getClass()))).andReturn(newLoc.getId());
		expect(solarLocationDao.get(newLoc.getId())).andReturn(newLoc);
		expect(solarNodeDao.store(nodeLocationMatch(testNode.getId(), newLoc.getId())))
				.andReturn(testNode.getId());
		expect(userNodeDao.store(userNode)).andReturn(testNode.getId());

		replayProperties();

		UserNode entry = new UserNode(testUser, (SolarNode) testNode.clone());
		entry.getNode().setLocation(loc);

		UserNode result = userBiz.saveUserNode(entry);
		Assert.assertEquals(userNode, result);

		verifyProperties();
	}

	@Test
	public void confirmTransferWithAuthTokenMatchingNodeId() {
		// lookup required user/node data
		UserNodeTransfer userNodeXfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID,
				"recipient@localhost");
		UserNodePK userNodePk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		expect(userNodeDao.getUserNodeTransfer(userNodePk)).andReturn(userNodeXfer);

		UserNode userNode = new UserNode(testUser, testNode);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		User recipient = new User(TEST_USER_ID_2, userNodeXfer.getEmail());
		expect(userDao.getUserByEmail(userNodeXfer.getEmail())).andReturn(recipient);

		// delete the xfer
		userNodeDao.deleteUserNodeTrasnfer(userNodeXfer);

		// delete alerts associated with node
		expect(userAlertDao.deleteAllAlertsForNode(TEST_USER_ID, TEST_NODE_ID)).andReturn(0);

		// find auth token that has multiple node IDs in policy
		UserAuthToken userAuthToken = new UserAuthToken("abc123", TEST_USER_ID, "secret",
				SecurityTokenType.ReadNodeData);
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID))).build();
		userAuthToken.setPolicy(policy);
		expect(userAuthTokenDao.findUserAuthTokensForUser(TEST_USER_ID))
				.andReturn(Arrays.asList(userAuthToken));

		// then delete the token
		userAuthTokenDao.delete(EasyMock.same(userAuthToken));

		// then store the updated UserNode
		expect(userNodeDao.store(userNode)).andReturn(TEST_NODE_ID);

		replayProperties();

		UserNodeTransfer xfer = userBiz.confirmNodeOwnershipTransfer(TEST_USER_ID, TEST_NODE_ID, true);

		verifyProperties();

		assertThat(xfer, sameInstance(userNodeXfer));
		assertThat("UserNode now owned by recipient", userNode.getUser(), sameInstance(recipient));
	}

	@Test
	public void confirmTransferWithAuthTokenContainingOtherNodeId() {
		// lookup required user/node data
		UserNodeTransfer userNodeXfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID,
				"recipient@localhost");
		UserNodePK userNodePk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		expect(userNodeDao.getUserNodeTransfer(userNodePk)).andReturn(userNodeXfer);

		UserNode userNode = new UserNode(testUser, testNode);
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		User recipient = new User(TEST_USER_ID_2, userNodeXfer.getEmail());
		expect(userDao.getUserByEmail(userNodeXfer.getEmail())).andReturn(recipient);

		// delete the xfer
		userNodeDao.deleteUserNodeTrasnfer(userNodeXfer);

		// delete alerts associated with node
		expect(userAlertDao.deleteAllAlertsForNode(TEST_USER_ID, TEST_NODE_ID)).andReturn(0);

		// find auth token that has multiple node IDs in policy
		UserAuthToken userAuthToken = new UserAuthToken("abc123", TEST_USER_ID, "secret",
				SecurityTokenType.ReadNodeData);
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(TEST_NODE_ID, TEST_NODE_ID_2)))
				.build();
		userAuthToken.setPolicy(policy);
		expect(userAuthTokenDao.findUserAuthTokensForUser(TEST_USER_ID))
				.andReturn(Arrays.asList(userAuthToken));

		// then store the updated token
		expect(userAuthTokenDao.store(EasyMock.same(userAuthToken))).andReturn("abc123");

		// then store the updated UserNode
		expect(userNodeDao.store(userNode)).andReturn(TEST_NODE_ID);

		replayProperties();

		UserNodeTransfer xfer = userBiz.confirmNodeOwnershipTransfer(TEST_USER_ID, TEST_NODE_ID, true);

		verifyProperties();

		assertThat(xfer, sameInstance(userNodeXfer));
		assertThat("UserNode now owned by recipient", userNode.getUser(), sameInstance(recipient));
		assertThat("Auth token no longer contains transferred node", userAuthToken.getNodeIds(),
				hasItems(TEST_NODE_ID_2));
	}

	@Test
	public void createAuthBuilder() {
		// given
		UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				SecurityTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);

		Instant signingDate = LocalDateTime.of(2017, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
		Snws2AuthorizationBuilder builder = new Snws2AuthorizationBuilder(TEST_AUTH_TOKEN);
		expect(userAuthTokenDao.createSnws2AuthorizationBuilder(TEST_AUTH_TOKEN, signingDate))
				.andReturn(builder);

		// when
		replayProperties();
		Snws2AuthorizationBuilder result = userBiz.createSnws2AuthorizationBuilder(TEST_USER_ID,
				TEST_AUTH_TOKEN, signingDate);

		// then
		assertThat("Builder", result, sameInstance(builder));

		verifyProperties();
	}

}
