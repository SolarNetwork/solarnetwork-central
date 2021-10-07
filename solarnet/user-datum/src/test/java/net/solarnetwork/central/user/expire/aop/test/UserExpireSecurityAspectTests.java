/* ==================================================================
 * UserExpireSecurityAspectTests.java - 9/07/2018 11:05:40 AM
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

package net.solarnetwork.central.user.expire.aop.test;

import static org.easymock.EasyMock.expect;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.central.user.expire.aop.UserExpireSecurityAspect;
import net.solarnetwork.central.user.expire.domain.UserDataConfiguration;

/**
 * Test cases for the {@link UserExpireSecurityAspect} class.
 * 
 * @author matt
 * @version 2.0
 */
public class UserExpireSecurityAspectTests extends AbstractCentralTest {

	private static final Long TEST_USER_ID = -11L;

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private UserExpireSecurityAspect aspect;

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
		aspect = new UserExpireSecurityAspect(nodeOwnershipDao);
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
		UserDataConfiguration config = new UserDataConfiguration();
		config.setUserId(TEST_USER_ID);
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void saveConfigWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDataConfiguration config = new UserDataConfiguration();
		config.setUserId(-2L);
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void saveConfigMissingUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDataConfiguration config = new UserDataConfiguration();
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test
	public void saveConfigAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		UserDataConfiguration config = new UserDataConfiguration();
		config.setUserId(TEST_USER_ID);
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

	@Test
	public void datumFilterAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		aspect.datumFilterCheck(filter);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void datumFilterMissingUser() {
		becomeUser("ROLE_USER");
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		aspect.datumFilterCheck(filter);
	}

	@Test
	public void datumFilterWithNode() {
		becomeUser("ROLE_USER");
		final Long nodeId = 2L;
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId))
				.andReturn(BasicSolarNodeOwnership.ownershipFor(nodeId, TEST_USER_ID));
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setNodeId(2L);
		aspect.datumFilterCheck(filter);
	}

	@Test(expected = AuthorizationException.class)
	public void datumFilterNodeNotAllowed() {
		becomeUser("ROLE_USER");
		final Long nodeId = 2L;
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(null);
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(TEST_USER_ID);
		filter.setNodeId(2L);
		aspect.datumFilterCheck(filter);
	}

}
