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

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.security.SecurityUtils.becomeNode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * Test cases for the {@link SecurityUtils} class.
 * 
 * @author matt
 * @version 2.0
 */
@ExtendWith(MockitoExtension.class)
public class SecurityUtilsTest {

	private static final Long TEST_USER_ID = -1L;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	private void becomeUser(String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, TEST_USER_ID, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void becomeToken(SecurityTokenType type, Set<Long> nodeIds, String... roles) {
		User userDetails = new User("abc123", "def456", AuthorityUtils.NO_AUTHORITIES);
		SecurityPolicy policy = null;
		if ( nodeIds != null ) {
			policy = BasicSecurityPolicy.builder().withNodeIds(nodeIds).build();
		}
		AuthenticatedToken token = new AuthenticatedToken(userDetails, type, TEST_USER_ID, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	public void cleanup() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void requireAnyRoleNoAuthentication() {
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_USER"));
		});
	}

	@Test
	public void requireAnyRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
		});
	}

	@Test
	public void requireAnyRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAnyRole(Collections.singleton("ROLE_FOO"));
		});
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

	@Test
	public void requireAllRoleNoAuthentication() {
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(Collections.singleton("ROLE_USER"));
		});
	}

	@Test
	public void requireAllRoleNoMatchSingle() {
		becomeUser("ROLE_USER");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(Collections.singleton("ROLE_FOO"));
		});
	}

	@Test
	public void requireAllRoleNoMatchMultiple() {
		becomeUser("ROLE_USER", "ROLE_DUDE");
		Assertions.assertThrows(AuthorizationException.class, () -> {
			SecurityUtils.requireAllRoles(new HashSet<String>(Arrays.asList("ROLE_USER", "ROLE_FOO")));
		});
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

	@Test
	public void authorizedNodeIds_node() {
		// GIVEN
		final Long nodeId = 123L;
		becomeNode(nodeId);

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		assertThat("Node actor gets own node ID without checking DAO", nodeIds,
				is(arrayContaining(nodeId)));
	}

	@Test
	public void authorizedNodeIds_user() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeUser("ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = ownerships.stream().map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
		assertThat("User actor gets node IDs returned from DAO", nodeIds, is(arrayContaining(expected)));

	}

	@Test
	public void authorizedNodeIds_token_user() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeToken(SecurityTokenType.User, null, "ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = ownerships.stream().map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
		assertThat("User token actor without policy gets node IDs returned from DAO", nodeIds,
				is(arrayContaining(expected)));
	}

	@Test
	public void authorizedNodeIds_token_user_policy() {
		List<SolarNodeOwnership> ownerships = Arrays.asList(ownershipFor(123L, TEST_USER_ID),
				ownershipFor(234L, TEST_USER_ID));
		becomeToken(SecurityTokenType.User, Collections.singleton(ownerships.get(0).getNodeId()),
				"ROLE_USER");
		given(nodeOwnershipDao.ownershipsForUserId(TEST_USER_ID))
				.willReturn(ownerships.toArray(SolarNodeOwnership[]::new));

		// WHEN
		Long[] nodeIds = SecurityUtils.authorizedNodeIdsForCurrentActor(nodeOwnershipDao);

		// THEN
		Long[] expected = new Long[] { ownerships.get(0).getNodeId() };
		assertThat("User token actor with policy gets node IDs returned from policy, restricted by DAO",
				nodeIds, is(arrayContaining(expected)));
	}
}
