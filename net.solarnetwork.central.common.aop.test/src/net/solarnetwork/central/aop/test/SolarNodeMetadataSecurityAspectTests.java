/* ==================================================================
 * SolarNodeMetadataSecurityAspectTests.java - 11/11/2016 4:41:23 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.aop.test;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.aop.SolarNodeMetadataSecurityAspect;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Test cases for the {@link SolarNodeMetadataSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeMetadataSecurityAspectTests extends AbstractCentralTransactionalTest {

	private static final Long TEST_USER_ID = -11L;

	private UserNodeDao userNodeDao;
	private SolarNodeMetadataSecurityAspect aspect;

	private void replayAll() {
		EasyMock.replay(userNodeDao);
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao);
	}

	private void becomeUser(String... roles) {
		org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
				"test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, TEST_USER_ID, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		aspect = new SolarNodeMetadataSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataNoAuth() {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataWrongNode() {
		UserNode userNode = new UserNode(new User(-2L, "someoneelse@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test
	public void updateMetadataAllowed() {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataWrongNode() {
		UserNode userNode = new UserNode(new User(-2L, "someoneelse@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		userNode.setRequiresAuthorization(true);
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		FilterSupport filter = new FilterSupport();
		filter.setNodeId(TEST_NODE_ID);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(filter);
		verifyAll();
	}

	@Test
	public void findMetadataAllowed() {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		userNode.setRequiresAuthorization(true);
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		FilterSupport filter = new FilterSupport();
		filter.setNodeId(TEST_NODE_ID);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(filter);
		verifyAll();
	}

}
