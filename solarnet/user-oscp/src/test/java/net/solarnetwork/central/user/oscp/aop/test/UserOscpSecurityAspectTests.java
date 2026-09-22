/* ==================================================================
 * UserOscpSecurityAspectTests.java - 22/09/2026 9:12:48 pm
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

package net.solarnetwork.central.user.oscp.aop.test;

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.privateOwnershipFor;
import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.BDDMockito.given;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.oscp.aop.UserOscpSecurityAspect;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;

/**
 * Test cases for the {@link UserOscpSecurityAspect} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UserOscpSecurityAspectTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	private UserOscpSecurityAspect aspect;

	@BeforeEach
	public void setup() {
		aspect = new UserOscpSecurityAspect(nodeOwnershipDao);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	private static AssetConfigurationInput asset(Long nodeId) {
		final AssetConfigurationInput input = new AssetConfigurationInput();
		input.setNodeId(nodeId);
		return input;
	}

	@Test
	public void createAssetNodeIdCheck_ownPrivateNode() {
		// GIVEN
		final Long nodeId = randomLong();
		given(nodeOwnershipDao.ownershipForNodeId(nodeId))
				.willReturn(privateOwnershipFor(nodeId, TEST_USER_ID));
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		aspect.createAssetNodeIdCheck(TEST_USER_ID, asset(nodeId));
	}

	@Test
	public void createAssetNodeIdCheck_otherUserPrivateNode() {
		// GIVEN
		final Long nodeId = randomLong();
		given(nodeOwnershipDao.ownershipForNodeId(nodeId))
				.willReturn(privateOwnershipFor(nodeId, randomLong()));
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Asset for another user's private node denied")
			.isThrownBy(() -> aspect.createAssetNodeIdCheck(TEST_USER_ID, asset(nodeId)))
			.returns(nodeId, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void createAssetNodeIdCheck_otherUserPublicNode() {
		// GIVEN
		final Long nodeId = randomLong();
		given(nodeOwnershipDao.ownershipForNodeId(nodeId))
				.willReturn(ownershipFor(nodeId, randomLong()));
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Asset for another user's public node denied")
			.isThrownBy(() -> aspect.createAssetNodeIdCheck(TEST_USER_ID, asset(nodeId)))
			.returns(nodeId, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void updateAssetNodeIdCheck_otherUserPublicNode() {
		// GIVEN
		final Long nodeId = randomLong();
		given(nodeOwnershipDao.ownershipForNodeId(nodeId))
				.willReturn(ownershipFor(nodeId, randomLong()));
		becomeUser(randomString(), randomString(), TEST_USER_ID);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Asset for another user's public node denied")
			.isThrownBy(() -> aspect.updateAssetNodeIdCheck(TEST_USER_ID, randomLong(),
					asset(nodeId)))
			.returns(nodeId, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

}
