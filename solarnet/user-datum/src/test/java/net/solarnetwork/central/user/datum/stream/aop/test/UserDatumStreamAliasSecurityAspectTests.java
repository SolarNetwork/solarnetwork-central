/* ==================================================================
 * UserDatumStreamAliasSecurityAspectTests.java - 1/04/2026 6:32:30 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.stream.aop.test;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.security.SecurityUtils.becomeToken;
import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.BDDMockito.given;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.CentralTestConstants;
import net.solarnetwork.central.user.datum.stream.aop.UserDatumStreamAliasSecurityAspect;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.domain.BasicSecurityPolicy;

/**
 * Test cases for the {@link UserDatumStreamAliasSecurityAspect}.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UserDatumStreamAliasSecurityAspectTests implements CentralTestConstants {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	private UserDatumStreamAliasSecurityAspect aspect;

	@BeforeEach
	public void setup() {
		aspect = new UserDatumStreamAliasSecurityAspect(nodeOwnershipDao);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void userIdReadAccessCheck_noAuth() {
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.userIdReadAccessCheck(TEST_USER_ID));
	}

	@Test
	public void userIdReadAccessCheck_wrongAuth() {
		// GIVEN
		becomeUser(randomString(), randomString(), randomLong());

		// THEN
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.userIdReadAccessCheck(TEST_USER_ID));
	}

	@Test
	public void userIdReadAccessCheck_ok() {
		// GIVEN
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// WHEN
		aspect.userIdReadAccessCheck(TEST_USER_ID);
	}

	@Test
	public void saveAliasAccessCheck_noAuth() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();

		// THEN
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input));
	}

	@Test
	public void saveAliasAccessCheck_wrongAuth() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();

		// WHEN
		becomeUser(randomString(), randomString(), randomLong());

		// THEN
		thenExceptionOfType(AuthorizationException.class)
				.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input));
	}

	@Test
	public void saveAliasAccessCheck_noPolicy_ok() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input);
	}

	@Test
	public void saveAliasAccessCheck_noPolicy_notOriginalOwner() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				randomLong(), "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		// WHEN
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is original node ID because owned by different user")
			.returns(input.getOriginalObjectId(), from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAliasAccessCheck_noPolicy_notAliasOwner() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), randomLong(), "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is alias node ID because owned by different user")
			.returns(input.getObjectId(), from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAliasAccessCheck_withSourcePolicy_ok() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withSourceIds(Set.of(input.getOriginalSourceId(), input.getSourceId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input);
	}

	@Test
	public void saveAliasAccessCheck_withSourcePolicy_notOriginalSourceId() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withSourceIds(Set.of(input.getSourceId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is original source ID because not allowed by policy")
			.returns(new String[] { input.getOriginalSourceId() }, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAliasAccessCheck_withSourcePolicy_notAliasSourceId() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withSourceIds(Set.of(input.getSourceId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is original source ID because not allowed by policy")
			.returns(new String[] { input.getOriginalSourceId() }, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAliasAccessCheck_withNodePolicy_ok() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withNodeIds(Set.of(input.getOriginalObjectId(), input.getObjectId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input);
	}

	@Test
	public void saveAliasAccessCheck_withNodePolicy_notOriginalNodeId() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withNodeIds(Set.of(input.getObjectId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is original node ID because not allowed by policy")
			.returns(input.getOriginalObjectId(), from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void saveAliasAccessCheck_withNodePolicy_notAliasNodeId() {
		// GIVEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final var originalOwnership = new BasicSolarNodeOwnership(input.getOriginalObjectId(),
				TEST_USER_ID, "NZ", UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getOriginalObjectId()))
				.willReturn(originalOwnership);

		final var aliasOwnership = new BasicSolarNodeOwnership(input.getObjectId(), TEST_USER_ID, "NZ",
				UTC, true, false);
		given(nodeOwnershipDao.ownershipForNodeId(input.getObjectId())).willReturn(aliasOwnership);

		// WHEN
		// @formatter:off
		final var policy = BasicSecurityPolicy.builder()
				.withNodeIds(Set.of(input.getOriginalObjectId()))
				.build()
				;
		becomeToken(randomString(), SecurityTokenType.User, TEST_USER_ID, policy);
		// @formatter:on

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.isThrownBy(() -> aspect.saveAliasAccessCheck(TEST_USER_ID, randomUUID(), input))
			.as("Denied ID is alias node ID because not allowed by policy")
			.returns(input.getObjectId(), from(AuthorizationException::getId))
			;
		// @formatter:on
	}

}
