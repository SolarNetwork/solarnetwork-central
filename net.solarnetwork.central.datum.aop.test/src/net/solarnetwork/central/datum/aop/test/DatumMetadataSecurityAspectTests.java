/* ==================================================================
 * DatumMetadataSecurityAspectTests.java - Oct 20, 2014 9:52:18 AM
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

package net.solarnetwork.central.datum.aop.test;

import java.util.Collections;
import java.util.Set;
import net.solarnetwork.central.datum.aop.DatumMetadataSecurityAspect;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.dao.UserNodeDao;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Test cases for the {@link DatumMetadataSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMetadataSecurityAspectTests extends AbstractCentralTransactionalTest {

	private UserNodeDao userNodeDao;

	private DatumMetadataSecurityAspect getTestInstance(Set<String> locMetaAdminRoles) {
		DatumMetadataSecurityAspect aspect = new DatumMetadataSecurityAspect(userNodeDao);
		if ( locMetaAdminRoles != null ) {
			aspect.setLocaitonMetadataAdminRoles(locMetaAdminRoles);
		}
		return aspect;
	}

	private void replayAll() {
		EasyMock.replay(userNodeDao);
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao);
	}

	private void becomeUser(String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, -1L, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataNoAuth() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_foo"));
		replayAll();
		aspect.updateLocationMetadataCheck(TEST_LOC_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataMissingRole() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_foo"));
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateLocationMetadataCheck(TEST_LOC_ID);
		verifyAll();
	}

	@Test
	public void updateMetadataAllowed() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_user"));
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateLocationMetadataCheck(TEST_LOC_ID);
		verifyAll();
	}

}
