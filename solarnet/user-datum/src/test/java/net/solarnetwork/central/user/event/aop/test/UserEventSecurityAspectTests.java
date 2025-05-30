/* ==================================================================
 * UserEventSecurityAspectTests.java - 11/06/2020 9:54:13 am
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

package net.solarnetwork.central.user.event.aop.test;

import static java.time.Instant.now;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.CentralTestConstants;
import net.solarnetwork.central.user.event.aop.UserEventSecurityAspect;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;

/**
 * Test cases for the {@link UserEventSecurityAspect} class.
 * 
 * @author matt
 * @version 2.0
 */
public class UserEventSecurityAspectTests implements CentralTestConstants {

	private static final Long TEST_USER_ID = -11L;

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private UserEventSecurityAspect aspect;

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

	@BeforeEach
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		aspect = new UserEventSecurityAspect(nodeOwnershipDao);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void actionForUserNoAuth() {
		replayAll();
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.actionForUserCheck(TEST_USER_ID));
		verifyAll();
	}

	@Test
	public void actionForUserWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.actionForUserCheck(-2L));
		verifyAll();
	}

	@Test
	public void actionForUserAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.actionForUserCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test
	public void saveConfigNoAuth() {
		replayAll();
		UserNodeEventHookConfiguration config = new UserNodeEventHookConfiguration(TEST_USER_ID, now());
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.saveConfigurationCheck(config));
		verifyAll();
	}

	@Test
	public void saveConfigWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserNodeEventHookConfiguration config = new UserNodeEventHookConfiguration(-99L, now());
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.saveConfigurationCheck(config));
		verifyAll();
	}

	@Test
	public void saveConfigMissingUser() {
		becomeUser("ROLE_USER");
		replayAll();
		UserNodeEventHookConfiguration config = new UserNodeEventHookConfiguration((Long) null, now());
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.saveConfigurationCheck(config));
		verifyAll();
	}

	@Test
	public void saveConfigAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		UserNodeEventHookConfiguration config = new UserNodeEventHookConfiguration(TEST_USER_ID, now());
		aspect.saveConfigurationCheck(config);
		verifyAll();
	}

}
