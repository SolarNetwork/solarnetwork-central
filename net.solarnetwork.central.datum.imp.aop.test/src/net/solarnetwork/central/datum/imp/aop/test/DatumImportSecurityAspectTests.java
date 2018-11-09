/* ==================================================================
 * DatumImportSecurityAspectTests.java - 9/11/2018 4:58:20 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.aop.test;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.datum.imp.aop.DatumImportSecurityAspect;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportRequest;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;;

/**
 * Test cases for the {@link DatumImportSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportSecurityAspectTests {

	private static final Long TEST_USER_ID = -11L;

	private UserNodeDao userNodeDao;
	private DatumImportSecurityAspect aspect;

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
		aspect = new DatumImportSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void actionForUserNoAuth() {
		replayAll();
		aspect.actionForUserCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void actionForUserWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.actionForUserCheck(-2L);
		verifyAll();
	}

	@Test
	public void actionForUserAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.actionForUserCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void makeRequestNoAuth() {
		replayAll();
		BasicDatumImportRequest request = new BasicDatumImportRequest(null, TEST_USER_ID);
		aspect.requestCheck(request);
	}

	@Test(expected = AuthorizationException.class)
	public void makeRequestWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		BasicDatumImportRequest request = new BasicDatumImportRequest(null, -2L);
		aspect.requestCheck(request);
	}

	@Test(expected = AuthorizationException.class)
	public void makeRequestMissingUser() {
		becomeUser("ROLE_USER");
		replayAll();
		BasicDatumImportRequest request = new BasicDatumImportRequest(null, null);
		aspect.requestCheck(request);
	}

	@Test
	public void makeRequestAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		BasicDatumImportRequest request = new BasicDatumImportRequest(null, TEST_USER_ID);
		aspect.requestCheck(request);
		verifyAll();
	}

}
