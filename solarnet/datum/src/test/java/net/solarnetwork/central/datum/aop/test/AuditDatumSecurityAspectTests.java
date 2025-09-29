/* ==================================================================
 * AuditDatumSecurityAspectTests.java - 12/07/2018 4:41:35 PM
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

package net.solarnetwork.central.datum.aop.test;

import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.aop.AuditDatumSecurityAspect;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * Test cases for the {@link AuditDatumSecurityAspect} class.
 *
 * @author matt
 * @version 1.1
 */
public class AuditDatumSecurityAspectTests {

	private SolarNodeOwnershipDao nodeOwnershipDao;

	@BeforeEach
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
	}

	private AuditDatumSecurityAspect getTestInstance() {
		AuditDatumSecurityAspect aspect = new AuditDatumSecurityAspect(nodeOwnershipDao);
		return aspect;
	}

	private void replayAll() {
		EasyMock.replay(nodeOwnershipDao);
	}

	private void verifyAll() {
		EasyMock.verify(nodeOwnershipDao);
	}

	private void setActor(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void becomeUser(Long userId, String... roles) {
		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		setActor(auth);
	}

	private SecurityToken setAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.ReadNodeData, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setActor(auth);
		return token;
	}

	@Test
	public void findForFilterCheckNoAuth() {
		// given
		AuditDatumSecurityAspect aspect = getTestInstance();

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.findForFilterCheck(filter));
	}

	@Test
	public void findForFilterCheckDataToken() {
		// given
		AuditDatumSecurityAspect aspect = getTestInstance();

		setAuthenticatedReadNodeDataToken(1L, null);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.findForFilterCheck(filter));
	}

	@Test
	public void findForFilterCheckWrongUserId() {
		// given
		AuditDatumSecurityAspect aspect = getTestInstance();

		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(userId + 1L);

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.findForFilterCheck(filter));
	}

	@Test
	public void findForFilterCheckMultipleUserIds() {
		// given
		AuditDatumSecurityAspect aspect = getTestInstance();

		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserIds(new Long[] { userId, userId });

		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.findForFilterCheck(filter));
	}

	@Test
	public void findForFilterCheckPass() {
		// given
		AuditDatumSecurityAspect aspect = getTestInstance();

		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(userId);
		aspect.findForFilterCheck(filter);
		verifyAll();
	}
}
