/* ==================================================================
 * SecurityUtilsTest.java - Oct 20, 2014 9:18:50 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Test cases for the {@link SecurityUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SecurityUtilsTest {

	private void becomeUser(String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, -1L, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@After
	public void cleanup() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test(expected = AuthorizationException.class)
	public void requireAnyRoleNoAuthentication() {
		SecurityUtils.requireAnyRole(Collections.singleton("ROLE_USER"));
	}

	@Test(expected = AuthorizationException.class)
	public void requireAnyRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
	}

	@Test(expected = AuthorizationException.class)
	public void requireAnyRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
	}

	@Test
	public void requireAnyRoleMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAnyRole(Collections.singleton("ROLE_USER"));
	}

	public void requireAnyRoleMatchMulitple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		SecurityUtils.requireAnyRole(new HashSet<String>(Arrays.asList("ROLE_FOO", "ROLE_DUDE")));
	}

	@Test(expected = AuthorizationException.class)
	public void requireAllRoleNoAuthentication() {
		SecurityUtils.requireAllRoles(Collections.singleton("ROLE_USER"));
	}

	@Test(expected = AuthorizationException.class)
	public void requireAllRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAllRoles(Collections.singleton("ROLE_FOO"));
	}

	@Test(expected = AuthorizationException.class)
	public void requireAllRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		SecurityUtils.requireAllRoles(new HashSet<String>(Arrays.asList("ROLE_USER", "ROLE_FOO")));
	}

	@Test
	public void requireAllRoleMatchSingle() {
		becomeUser("ROLE_USER");
		SecurityUtils.requireAllRoles(Collections.singleton("ROLE_USER"));
	}

	@Test
	public void requireAllRoleMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE", "ROLE_DUDETTE");
		SecurityUtils.requireAllRoles(new HashSet<String>(Arrays.asList("ROLE_USER", "ROLE_DUDE")));
	}

}
