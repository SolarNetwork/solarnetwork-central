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
import static net.solarnetwork.central.security.AuthorizationException.Reason.UNKNOWN_OBJECT;
import static net.solarnetwork.central.security.SecurityUtils.becomeNode;
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
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;
import net.solarnetwork.central.security.SecurityNode;

/**
 * Test cases for the {@link AuthorizationSupport} class using auth
 * authorization.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class AuthorizationSupport_nodeTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String GB = "GB";
	private static final boolean REQUIRES_AUTH = true;
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

	@Test
	public void nodeReadAccess() {
		// GIVEN
		final SecurityNode auth = becomeNode(randomLong());

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(auth.getNodeId(), randomLong(),
				GB, ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(auth.getNodeId())).willReturn(ownership);

		// WHEN
		support.requireNodeReadAccess(auth.getNodeId(), log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(auth.getNodeId());
	}

	@Test
	public void nodeReadAccess_notSelf() {
		// GIVEN
		becomeNode(randomLong());

		final Long otherNodeId = randomLong();
		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(otherNodeId, randomLong(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(otherNodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeReadAccess(otherNodeId, log);
		})
				.as("Reason")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(otherNodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(otherNodeId);
	}

	@Test
	public void nodeWriteAccess() {
		// GIVEN
		final SecurityNode auth = becomeNode(randomLong());

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(auth.getNodeId(), randomLong(),
				GB, ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(auth.getNodeId())).willReturn(ownership);

		// WHEN
		support.requireNodeWriteAccess(auth.getNodeId(), log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(auth.getNodeId());
	}

	@Test
	public void nodeWriteAccess_notSelf() {
		// GIVEN
		becomeNode(randomLong());

		final Long otherNodeId = randomLong();
		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(otherNodeId, randomLong(), GB,
				ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(otherNodeId)).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(otherNodeId, log);
		})
				.as("Reason")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(otherNodeId, AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(otherNodeId);
	}

	@Test
	public void nodeWriteAccess_archived() {
		// GIVEN
		final SecurityNode auth = becomeNode(randomLong());

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(auth.getNodeId(), randomLong(),
				GB, ZoneOffset.UTC, REQUIRES_AUTH, ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(auth.getNodeId())).willReturn(ownership);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireNodeWriteAccess(auth.getNodeId(), log);
		})
				.as("Cannot write to archived node")
				.returns(UNKNOWN_OBJECT, AuthorizationException::getReason)
				.as("Reference is node ID")
				.returns(auth.getNodeId(), AuthorizationException::getId)
				;
		// @formatter:on

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(auth.getNodeId());
	}

	@Test
	public void userReadAccess() {
		// GIVEN
		final SecurityNode auth = becomeNode(randomLong());

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(auth.getNodeId(), randomLong(),
				GB, ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(auth.getNodeId())).willReturn(ownership);

		// WHEN
		support.requireUserReadAccess(ownership.getUserId(), log);

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(auth.getNodeId());
	}

	@Test
	public void userReadAccess_notOwner() {
		// GIVEN
		final SecurityNode auth = becomeNode(randomLong());

		final SolarNodeOwnership ownership = new BasicSolarNodeOwnership(auth.getNodeId(), randomLong(),
				GB, ZoneOffset.UTC, REQUIRES_AUTH, NOT_ARCHIVED);
		given(nodeOwnershipDao.ownershipForNodeId(auth.getNodeId())).willReturn(ownership);

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

		// THEN
		then(nodeOwnershipDao).should().ownershipForNodeId(auth.getNodeId());
	}

	@Test
	public void userWriteAccess() {
		// GIVEN
		becomeNode(randomLong());

		// WHEN
		// @formatter:off
		final Long writeUserId = randomLong();
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			support.requireUserWriteAccess(writeUserId, log);
		})
				.as("Cannot write user")
				.returns(ACCESS_DENIED, AuthorizationException::getReason)
				.as("Reference is user ID")
				.returns(writeUserId, AuthorizationException::getId)
				;
		// @formatter:on
	}

}
