/* ==================================================================
 * UserOscpBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupSettingsInput;
import net.solarnetwork.central.user.oscp.domain.CapacityOptimizerConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityProviderConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.UserSettingsInput;

/**
 * Security contract for {@link UserOscpBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserOscpSecurityAspect}. Saving an asset
 * also requires read access to its node. The node IDs of the user and
 * capacity group settings are not checked, because OSCP only publishes datum
 * to nodes owned by the settings' user.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class UserOscpBizSecurityContract {

	private UserOscpBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserOscpBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final Long userId = a.userId();
		final Long id = randomLong();

		// @formatter:off
		return SecurityContract.forApi(UserOscpBiz.class, tenants)
				.userRead(biz -> biz.settingsForUser(userId))
				.userRead(biz -> biz.capacityProviderForUser(userId, id))
				.userRead(biz -> biz.capacityOptimizerForUser(userId, id))
				.userRead(biz -> biz.capacityGroupForUser(userId, id))
				.userRead(biz -> biz.capacityGroupSettingsForUser(userId, id))
				.userRead(biz -> biz.assetForUser(userId, id))
				.userWrite(biz -> biz.deleteUserSettings(userId))
				.userWrite(biz -> biz.deleteCapacityProvider(userId, id))
				.userWrite(biz -> biz.deleteCapacityOptimizer(userId, id))
				.userWrite(biz -> biz.deleteCapacityGroup(userId, id))
				.userWrite(biz -> biz.deleteCapacityGroupSettings(userId, id))
				.userWrite(biz -> biz.deleteAsset(userId, id))
				.userRead(biz -> biz.capacityProvidersForUser(userId))
				.userRead(biz -> biz.capacityOptimizersForUser(userId))
				.userRead(biz -> biz.capacityGroupsForUser(userId))
				.userRead(biz -> biz.capacityGroupSettingsForUser(userId))
				.userRead(biz -> biz.assetsForUser(userId))
				.userRead(biz -> biz.assetsForUserCapacityGroup(userId, id))
				.userWrite(biz -> biz.createCapacityProvider(userId,
						new CapacityProviderConfigurationInput()))
				.userWrite(biz -> biz.createCapacityOptimizer(userId,
						new CapacityOptimizerConfigurationInput()))
				.userWrite(biz -> biz.createCapacityGroup(userId, new CapacityGroupConfigurationInput()))
				.allowing(biz -> biz.createAsset(userId, asset(a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.createAsset(userId, asset(a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.allowing(biz -> biz.createAsset(userId, asset(b.privateNodeId())))
					.as("other user node")
				.allowing(biz -> biz.createAsset(userId, asset(b.publicNodeId())))
					.as("other user public node")
				.userWrite(biz -> biz.createAsset(userId, new AssetConfigurationInput()))
					.as("no node")
				.userWrite(biz -> biz.updateUserSettings(userId, new UserSettingsInput()))
				.userWrite(biz -> biz.updateCapacityProvider(userId, id,
						new CapacityProviderConfigurationInput()))
				.userWrite(biz -> biz.updateCapacityOptimizer(userId, id,
						new CapacityOptimizerConfigurationInput()))
				.userWrite(biz -> biz.updateCapacityGroup(userId, id,
						new CapacityGroupConfigurationInput()))
				.userWrite(biz -> biz.updateCapacityGroupSettings(userId, id,
						new CapacityGroupSettingsInput()))
				.allowing(biz -> biz.updateAsset(userId, id, asset(a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.updateAsset(userId, id, asset(b.privateNodeId())))
					.as("other user node")
				.allowing(biz -> biz.updateAsset(userId, id, asset(b.publicNodeId())))
					.as("other user public node")
				.build();
		// @formatter:on
	}

	private static AssetConfigurationInput asset(Long nodeId) {
		final AssetConfigurationInput input = new AssetConfigurationInput();
		input.setNodeId(nodeId);
		return input;
	}

}
