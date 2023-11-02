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

import static java.util.Collections.singleton;
import static net.solarnetwork.central.security.AuthorizationException.Reason.ACCESS_DENIED;
import static net.solarnetwork.central.security.AuthorizationException.Reason.UNKNOWN_OBJECT;
import static net.solarnetwork.central.security.BasicSecurityPolicy.builder;
import static net.solarnetwork.central.security.SecurityTokenType.ReadNodeData;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
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
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;

/**
 * Test cases for the {@link AuthorizationSupport} class using token
 * authorization.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class AuthorizationSupport_tokenTests {

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

	private AuthenticatedToken becomeToken(SecurityTokenType type, Long userId, SecurityPolicy policy) {
		return becomeToken(type, userId, policy, "ROLE_USER");
	}

	private AuthenticatedToken becomeToken(SecurityTokenType type, Long userId, SecurityPolicy policy,
			String... roles) {
		User userDetails = new User("abc123", "def456", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedToken token = new AuthenticatedToken(userDetails, type, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
		return token;
	}

	@Test
	public void nodeReadAccess_data_public() {
		// GIVEN
		becomeToken(ReadNodeData, randomLong(), null);
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
	public void nodeReadAccess_data_public_archived() {
		// GIVEN
		becomeToken(ReadNodeData, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, randomLong(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Unknown for data token and archived node")
				.returns(UNKNOWN_OBJECT, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_data_archived() {
		// GIVEN
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Reason")
				.returns(UNKNOWN_OBJECT, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_data_noPolicy() {
		// GIVEN
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_data_noPolicy_notOwner() {
		// GIVEN
		becomeToken(ReadNodeData, randomLong(), null);
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
	public void nodeReadAccess_data_policy_notOwner() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		becomeToken(ReadNodeData, randomLong(), policy);

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
	public void nodeReadAccess_data_policy_included() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_data_policy_notIncluded() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(randomLong())).build();
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Policy does not include node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_public() {
		// GIVEN
		becomeToken(User, randomLong(), null);
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
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
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
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_noPolicy() {
		// GIVEN
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_noPolicy_notOwner() {
		// GIVEN
		becomeToken(User, randomLong(), null);
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
	public void nodeReadAccess_user_policy_notOwner() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		becomeToken(User, randomLong(), policy);

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
	public void nodeReadAccess_user_policy_included() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		final SecurityToken token = becomeToken(User, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeReadAccess_user_policy_notIncluded() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(randomLong())).build();
		final SecurityToken token = becomeToken(User, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(nodeId, log);
		})
				.as("Policy does not include node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_data() {
		// GIVEN
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(nodeId, log);
		})
				.as("Data token not allowed write access")
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
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
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
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, NOT_REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_noPolicy() {
		// GIVEN
		final SecurityToken token = becomeToken(User, randomLong(), null);
		final Long nodeId = randomLong();

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_noPolicy_notOwner() {
		// GIVEN
		becomeToken(User, randomLong(), null);
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
	public void nodeWriteAccess_user_policy_notOwner() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		becomeToken(User, randomLong(), policy);

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
	public void nodeWriteAccess_user_policy_included() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(nodeId)).build();
		final SecurityToken token = becomeToken(User, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(nodeId, log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void nodeWriteAccess_user_policy_notIncluded() {
		// GIVEN
		final Long nodeId = randomLong();
		final SecurityPolicy policy = builder().withNodeIds(singleton(randomLong())).build();
		final SecurityToken token = becomeToken(User, randomLong(), policy);

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(nodeId, token.getUserId(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(nodeId, log);
		})
				.as("Policy does not include node")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(nodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(nodeId);
	}

	@Test
	public void userReadAccess_data_noPolicy() {
		// GIVEN
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), null);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserReadAccess(token.getUserId(), log);
		})
				.as("Cannot read other user")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is user ID")
				.returns(token.getUserId(), AuthorizationException::getId)
				;
		// @formatter:on
	}

	@Test
	public void userReadAccess_data_withPolicy_noUserMetadataPaths() {
		// GIVEN
		final SecurityPolicy policy = builder().withNodeIds(singleton(randomLong())).build();
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), policy);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserReadAccess(token.getUserId(), log);
		})
				.as("Cannot read user without user metadata policy")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is user ID")
				.returns(token.getUserId(), AuthorizationException::getId)
				;
		// @formatter:on
	}

	@Test
	public void userReadAccess_data_withPolicy_withUserMetadataPaths() {
		// GIVEN
		final SecurityPolicy policy = builder().withNodeIds(singleton(randomLong()))
				.withUserMetadataPaths(singleton("/pm/foobar/**")).build();
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), policy);

		// WHEN
		support.requireUserReadAccess(token.getUserId(), log);
	}

	@Test
	public void userReadAccess_user() {
		// GIVEN
		final SecurityToken token = becomeToken(User, randomLong(), null);

		// WHEN
		support.requireUserReadAccess(token.getUserId(), log);
	}

	@Test
	public void userReadAccess_user_notOwner() {
		// GIVEN
		becomeToken(User, randomLong(), null);

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
	public void userWriteAccess_data() {
		// GIVEN
		final SecurityToken token = becomeToken(ReadNodeData, randomLong(), null);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserWriteAccess(token.getUserId(), log);
		})
				.as("Data token not allowed write access")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(token.getUserId(), AuthorizationException::getId)
				;
		// @formatter:on
	}

	@Test
	public void userWriteAccess_user() {
		// GIVEN
		final SecurityToken token = becomeToken(User, randomLong(), null);

		// WHEN
		support.requireUserWriteAccess(token.getUserId(), log);
	}

	@Test
	public void userWriteAccess_user_notOwner() {
		// GIVEN
		becomeToken(User, randomLong(), null);

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
