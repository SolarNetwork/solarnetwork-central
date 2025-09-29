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

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.aop.DatumMetadataSecurityAspect;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.CentralTestConstants;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Test cases for the {@link DatumMetadataSecurityAspect} class.
 *
 * @author matt
 * @version 1.1
 */
public class DatumMetadataSecurityAspectTests implements CentralTestConstants {

	private SolarNodeOwnershipDao userNodeDao;

	private DatumMetadataSecurityAspect getTestInstance(Set<String> locMetaAdminRoles) {
		DatumMetadataSecurityAspect aspect = new DatumMetadataSecurityAspect(userNodeDao);
		if ( locMetaAdminRoles != null ) {
			aspect.setLocationMetadataAdminRoles(locMetaAdminRoles);
		}
		return aspect;
	}

	private void replayAll() {
		EasyMock.replay(userNodeDao);
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao);
	}

	private void setUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void becomeUser(String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, -1L, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		setUser(auth);
	}

	private SecurityToken setAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.ReadNodeData, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setUser(auth);
		return token;
	}

	@BeforeEach
	public void setup() {
		userNodeDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
	}

	@AfterEach
	public void teardown() {
		verifyAll();
	}

	@Test
	public void updateMetadataNoAuth() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_foo"));
		replayAll();

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.updateLocationMetadataCheck(TEST_LOC_ID));
	}

	@Test
	public void updateMetadataMissingRole() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_foo"));
		becomeUser("ROLE_USER");
		replayAll();

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.updateLocationMetadataCheck(TEST_LOC_ID));
	}

	@Test
	public void updateMetadataAllowed() {
		DatumMetadataSecurityAspect aspect = getTestInstance(Collections.singleton("role_user"));
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateLocationMetadataCheck(TEST_LOC_ID);
	}

	@Test
	public void availableSourceIdsFilteredFromPattern() throws Throwable {
		final Long nodeId = -1L;
		final Long userId = -100L;
		final String[] policySourceIds = new String[] { "/A/**/watts" };
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds)))
				.withNodeIds(Collections.singleton(nodeId)).build();
		final ProceedingJoinPoint pjp = EasyMock.createMock(org.aspectj.lang.ProceedingJoinPoint.class);
		final Set<NodeSourcePK> availableSourceIds = new LinkedHashSet<NodeSourcePK>(Arrays.asList(
				new NodeSourcePK(nodeId, "/A/B/watts"), new NodeSourcePK(nodeId, "/A/C/watts"),
				new NodeSourcePK(nodeId, "/B/B/watts"), new NodeSourcePK(nodeId, "Foo bar")));
		setAuthenticatedReadNodeDataToken(userId, policy);
		SolarNodeOwnership ownership = BasicSolarNodeOwnership.privateOwnershipFor(nodeId, userId);

		EasyMock.expect(userNodeDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		EasyMock.expect(pjp.proceed()).andReturn(availableSourceIds);

		EasyMock.replay(pjp);
		EasyMock.replay(userNodeDao);

		DatumMetadataSecurityAspect service = getTestInstance(null);

		@SuppressWarnings("unchecked")
		Set<NodeSourcePK> result = (Set<NodeSourcePK>) service.filteredMetadataSourcesAccessCheck(pjp,
				new Long[] { nodeId });
		then(result).as("Filtered source IDs").containsExactly(new NodeSourcePK(nodeId, "/A/B/watts"),
				new NodeSourcePK(nodeId, "/A/C/watts"));
	}

}
