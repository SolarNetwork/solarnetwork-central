/* ==================================================================
 * UserExportSecurityAspectTests.java - 28/03/2018 4:14:42 PM
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

package net.solarnetwork.central.user.export.aop.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.domain.UserLongCompositePK.UNASSIGNED_USER_ID;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.central.user.export.aop.UserExportSecurityAspect;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * Test cases for the {@link UserExportSecurityAspect} class.
 * 
 * @author matt
 * @version 2.0
 */
public class UserExportSecurityAspectTests extends AbstractCentralTest {

	private static final Long TEST_USER_ID = -11L;

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private UserExportSecurityAspect aspect;

	private void replayAll() {
		EasyMock.replay(nodeOwnershipDao);
	}

	private void verifyAll() {
		EasyMock.verify(nodeOwnershipDao);
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
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		aspect = new UserExportSecurityAspect(nodeOwnershipDao);
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
	public void saveConfigNoAuth() {
		replayAll();
		UserDatumExportConfiguration config = new UserDatumExportConfiguration(
				unassignedEntityIdKey(TEST_USER_ID), now());
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void saveConfigWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDatumExportConfiguration config = new UserDatumExportConfiguration(
				unassignedEntityIdKey(-2L), now());
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void saveConfigMissingUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDatumExportConfiguration config = new UserDatumExportConfiguration(
				unassignedEntityIdKey(UNASSIGNED_USER_ID), now());
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test
	public void saveConfigAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDatumExportConfiguration config = new UserDatumExportConfiguration(
				unassignedEntityIdKey(TEST_USER_ID), now());
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}
}
