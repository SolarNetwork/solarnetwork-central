/* ==================================================================
 * AuthorizationSupportTests.java - 2/11/2023 4:38:12 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.AuthorizationException.Reason.ACCESS_DENIED;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityUser;

/**
 * Test cases for the {@link AuthorizationSupport} class using auth
 * authorization.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class AuthorizationSupport_userTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String GB = "GB";
	private static final boolean REQUIRES_AUTH = true;
	private static final boolean NOT_REQUIRES_AUTH = false;
	private static final boolean ARCHIVED = true;
	private static final boolean NOT_ARCHIVED = false;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	private AuthorizationSupport support;

	@BeforeEach
	public void setup() {
		support = new AuthorizationSupport(nodeOwnershipDao);
	}

	@AfterEach
	public void cleanup() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private AuthenticatedUser becomeUser(String email, Long userId) {
		return becomeUser(email, userId, "ROLE_USER");
	}

	private AuthenticatedUser becomeUser(String email, Long userId, String... roles) {
		User userDetails = new User(email, "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, "Test randomString()",
				false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
		return user;
	}

	@Test
	public void nodeReadAccess_user_public_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_archived() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_public_archived() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_public_archived_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Non-owner cannot access archived public node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Reason")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_public_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(nodeId, log);
		})
				.as("Non-owner cannot write to public node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_archived() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_public_archived() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_public_archived_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(nodeId, log);
		})
				.as("Non-owner cannot access archived public node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, auth.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(nodeId, log);
		})
				.as("Reason")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void userReadAccess() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());

		// WHEN
		support.requireUserReadAccess(auth.getUserId(), log);
	}

	@Test
	public void userReadAccess_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());

		// WHEN
		// @formatter:off
		final Long readUserId = randomLong();
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserReadAccess(readUserId, log);
		})
				.as("Cannot read other user")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is user ID")
				.returns(readUserId, AuthorizationException::getId)
				;
		// @formatter:on
	}

	@Test
	public void userWriteAccess() {
		// GIVEN
		final SecurityUser auth = becomeUser(randomString(), randomLong());

		// WHEN
		support.requireUserWriteAccess(auth.getUserId(), log);
	}

	@Test
	public void userWriteAccess_notOwner() {
		// GIVEN
		becomeUser(randomString(), randomLong());

		// WHEN
		// @formatter:off
		final Long readUserId = randomLong();
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserWriteAccess(readUserId, log);
		})
				.as("Cannot write other user")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is user ID")
				.returns(readUserId, AuthorizationException::getId)
				;
		// @formatter:on
	}

}
