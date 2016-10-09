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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.util.HashSet;
import java.util.Set;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Test cases for the {@link DaoUserBiz} class.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoUserBizTest {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_ENC_PASSWORD = "encrypted.password";
	private static final String TEST_NAME = "Test User";
	private static final String TEST_ROLE = "ROLE_TEST";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_AUTH_SECRET = "123";

	private SolarNode testNode;
	private User testUser;
	private Set<String> testUserRoles;

	private SolarLocationDao solarLocationDao;
	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserAuthTokenDao userAuthTokenDao;
	private UserNodeDao userNodeDao;

	private DaoUserBiz userBiz;

	@Before
	public void setup() {
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setName(TEST_NAME);
		testUser.setPassword(TEST_ENC_PASSWORD);

		testNode = new SolarNode();
		testNode.setId(-1L);
		testNode.setLocationId(-2L);

		testUserRoles = new HashSet<String>();
		testUserRoles.add(TEST_ROLE);

		solarLocationDao = EasyMock.createMock(SolarLocationDao.class);
		solarNodeDao = EasyMock.createMock(SolarNodeDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		userAuthTokenDao = EasyMock.createMock(UserAuthTokenDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);

		userBiz = new DaoUserBiz();
		userBiz.setSolarLocationDao(solarLocationDao);
		userBiz.setSolarNodeDao(solarNodeDao);
		userBiz.setUserDao(userDao);
		userBiz.setUserAuthTokenDao(userAuthTokenDao);
		userBiz.setUserNodeDao(userNodeDao);
	}

	private void replayProperties() {
		replay(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao);
	}

	private void verifyProperties() {
		verify(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao);
	}

	@Test
	public void generateUserAuthToken() {
		expect(userAuthTokenDao.get(anyObject(String.class))).andReturn(null);
		expect(userAuthTokenDao.store(anyObject(UserAuthToken.class))).andReturn(TEST_AUTH_TOKEN);
		replayProperties();
		UserAuthToken generated = userBiz.generateUserAuthToken(TEST_USER_ID, UserAuthTokenType.User,
				(SecurityPolicy) null);
		assertNotNull(generated);
		assertNotNull(generated.getAuthToken());
		assertEquals("Auth token should be exactly 20 characters", 20,
				generated.getAuthToken().length());
		assertNotNull(generated.getAuthSecret());
		assertEquals(TEST_USER_ID, generated.getUserId());
		assertEquals(UserAuthTokenStatus.Active, generated.getStatus());
		verifyProperties();
	}

	@Test
	public void deleteUserAuthToken() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				UserAuthTokenType.User);
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
				UserAuthTokenType.User);
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
	public void saveUserNodeNoLocationChange() {
		final UserNode userNode = new UserNode();
		userNode.setCreated(new DateTime());
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
		userNode.setCreated(new DateTime());
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
		userNode.setCreated(new DateTime());
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

}
