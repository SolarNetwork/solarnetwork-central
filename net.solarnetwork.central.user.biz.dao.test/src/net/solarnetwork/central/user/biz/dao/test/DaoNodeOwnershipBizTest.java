/* ==================================================================
 * DaoNodeOwnershipBizTest.java - Apr 20, 2015 8:33:06 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
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
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link NodeOwnershipBiz} implementation of
 * {@link DaoUserBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoNodeOwnershipBizTest {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_ENC_PASSWORD = "encrypted.password";
	private static final String TEST_NAME = "Test User";
	private static final String TEST_ROLE = "ROLE_TEST";
	private static final String TEST_NEW_EMAIL = "new@localhost";

	private SolarNode testNode;
	private User testUser;
	private Set<String> testUserRoles;

	private SolarLocationDao solarLocationDao;
	private SolarNodeDao solarNodeDao;
	private UserDao userDao;
	private UserAlertDao userAlertDao;
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
		userAlertDao = EasyMock.createMock(UserAlertDao.class);
		userAuthTokenDao = EasyMock.createMock(UserAuthTokenDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);

		userBiz = new DaoUserBiz();
		userBiz.setSolarLocationDao(solarLocationDao);
		userBiz.setSolarNodeDao(solarNodeDao);
		userBiz.setUserDao(userDao);
		userBiz.setUserAlertDao(userAlertDao);
		userBiz.setUserAuthTokenDao(userAuthTokenDao);
		userBiz.setUserNodeDao(userNodeDao);
	}

	private void replayAll() {
		replay(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao);
	}

	private void verifyAll() {
		verify(solarLocationDao, solarNodeDao, userAuthTokenDao, userDao, userNodeDao);
	}

	@Test
	public void requestOwnershipTransfer() {
		final UserNodeTransfer xfer = new UserNodeTransfer(testUser.getId(), testNode.getId(),
				TEST_NEW_EMAIL);
		userNodeDao.storeUserNodeTransfer(EasyMock.eq(xfer));
		replayAll();
		userBiz.requestNodeOwnershipTransfer(testUser.getId(), testNode.getId(), TEST_NEW_EMAIL);
		verifyAll();
	}

	@Test
	public void confirmOwnershipTransfer() {
		final UserNodePK originalUserNdoePK = new UserNodePK(testUser.getId(), testNode.getId());
		final User newOwner = new User(-3L, TEST_NEW_EMAIL);
		final UserNode originalUserNode = new UserNode(testUser, testNode);
		final UserNodeTransfer xfer = new UserNodeTransfer(testUser.getId(), testNode.getId(),
				TEST_NEW_EMAIL);
		final List<UserAuthToken> authTokens = Collections.emptyList();
		final UserNode newUserNode = new UserNode(newOwner, testNode);

		// get the xfer record
		expect(userNodeDao.getUserNodeTransfer(originalUserNdoePK)).andReturn(xfer);

		// get the actual UserNode entity
		expect(userNodeDao.get(testNode.getId())).andReturn(originalUserNode);

		// get the new owner User entity
		expect(userDao.getUserByEmail(newOwner.getEmail())).andReturn(newOwner);

		// delete the xfer record
		userNodeDao.deleteUserNodeTrasnfer(xfer);

		// remove alerts associated with node
		expect(userAlertDao.deleteAllAlertsForNode(testUser.getId(), testNode.getId())).andReturn(0);

		// delete auth tokens associated with node
		expect(userAuthTokenDao.findUserAuthTokensForUser(testUser.getId())).andReturn(authTokens);

		// and finally update the UserNode to the new owner
		expect(userNodeDao.store(newUserNode)).andReturn(testNode.getId());

		replayAll();
		UserNodeTransfer result = userBiz.confirmNodeOwnershipTransfer(testUser.getId(),
				testNode.getId(), true);
		Assert.assertSame("UserNodeTransfer from DAO returned", xfer, result);
		verifyAll();
	}
}
