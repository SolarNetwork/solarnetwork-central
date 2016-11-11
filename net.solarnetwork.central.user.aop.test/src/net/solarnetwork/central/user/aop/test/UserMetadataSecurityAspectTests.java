/* ==================================================================
 * UserMetadataSecurityAspectTests.java - 11/11/2016 5:16:20 PM
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

package net.solarnetwork.central.user.aop.test;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.aop.UserMetadataSecurityAspect;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserFilterCommand;

/**
 * Test cases for the {@link UserMetadataSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class UserMetadataSecurityAspectTests extends AbstractCentralTransactionalTest {

	private static final Long TEST_USER_ID = -11L;

	private UserNodeDao userNodeDao;
	private UserMetadataSecurityAspect aspect;

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
		aspect = new UserMetadataSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataNoAuth() {
		replayAll();
		aspect.updateMetadataCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(-2L);
		verifyAll();
	}

	@Test
	public void updateMetadataAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataWrongUser() {
		UserFilterCommand filter = new UserFilterCommand();
		filter.setUserId(-2L);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(filter);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataNoUser() {
		UserFilterCommand filter = new UserFilterCommand();

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(filter);
		verifyAll();
	}

	@Test
	public void findMetadataAllowed() {
		UserFilterCommand filter = new UserFilterCommand();
		filter.setUserId(TEST_USER_ID);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(filter);
		verifyAll();
	}

}
