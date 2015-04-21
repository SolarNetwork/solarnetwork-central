/* ==================================================================
 * NodeOwnershipSecurityAspectTests.java - Apr 22, 2015 7:29:24 AM
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

package net.solarnetwork.central.user.aop.test;

import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.aop.NodeOwnershipSecurityAspect;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Test cases for the {@link NodeOwnershipSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeOwnershipSecurityAspectTests extends AbstractCentralTransactionalTest {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";

	private UserNodeDao userNodeDao;
	private UserDao userDao;

	private NodeOwnershipSecurityAspect getTestInstance() {
		NodeOwnershipSecurityAspect aspect = new NodeOwnershipSecurityAspect(userNodeDao, userDao);
		return aspect;
	}

	private void replayAll() {
		EasyMock.replay(userNodeDao, userDao);
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao, userDao);
	}

	private void becomeUser(Long userId, String... roles) {
		User userDetails = new User(TEST_EMAIL, "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void requestOrCancelTransferNoAuth() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOC_ID);
		final UserNode userNode = new UserNode(user, node);

		// get UserNode for given node ID
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		replayAll();
		aspect.userNodeRequestOrCancelTransferRequest(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void requestOrCancelTransferNotOwner() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOC_ID);
		final UserNode userNode = new UserNode(user, node);

		// get UserNode for given node ID
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser(-2L, "ROLE_USER");
		replayAll();
		aspect.userNodeRequestOrCancelTransferRequest(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

	@Test
	public void requestOrCancelTransferSuccess() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final SolarNode node = new SolarNode(TEST_NODE_ID, TEST_LOC_ID);
		final UserNode userNode = new UserNode(user, node);

		// get UserNode for given node ID
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser(TEST_USER_ID, "ROLE_USER");
		replayAll();
		aspect.userNodeRequestOrCancelTransferRequest(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void confirmTransferNoAuth() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final UserNodePK pk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		final UserNodeTransfer xfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID, TEST_EMAIL);

		EasyMock.expect(userNodeDao.getUserNodeTransfer(pk)).andReturn(xfer);
		EasyMock.expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(user);

		replayAll();
		aspect.userNodeConfirmTransferAccessCheck(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void confirmTransferNotOwner() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final UserNodePK pk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		final UserNodeTransfer xfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID, TEST_EMAIL);

		EasyMock.expect(userNodeDao.getUserNodeTransfer(pk)).andReturn(xfer);
		EasyMock.expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(user);

		becomeUser(-2L, "ROLE_USER");
		replayAll();
		aspect.userNodeConfirmTransferAccessCheck(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

	@Test
	public void confirmTransferSuccess() {
		final NodeOwnershipSecurityAspect aspect = getTestInstance();
		final net.solarnetwork.central.user.domain.User user = new net.solarnetwork.central.user.domain.User(
				TEST_USER_ID, TEST_EMAIL);
		final UserNodePK pk = new UserNodePK(TEST_USER_ID, TEST_NODE_ID);
		final UserNodeTransfer xfer = new UserNodeTransfer(TEST_USER_ID, TEST_NODE_ID, TEST_EMAIL);

		EasyMock.expect(userNodeDao.getUserNodeTransfer(pk)).andReturn(xfer);
		EasyMock.expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(user);

		becomeUser(TEST_USER_ID, "ROLE_USER");
		replayAll();
		aspect.userNodeConfirmTransferAccessCheck(TEST_USER_ID, TEST_NODE_ID);
		verifyAll();
	}

}
