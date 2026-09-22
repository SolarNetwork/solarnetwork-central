/* ==================================================================
 * UserFluxBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.datum.flux.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.flux.biz.UserFluxBiz;
import net.solarnetwork.central.user.datum.flux.dao.BasicFluxConfigurationFilter;
import net.solarnetwork.central.user.datum.flux.domain.UserFluxAggregatePublishConfigurationInput;
import net.solarnetwork.central.user.datum.flux.domain.UserFluxDefaultAggregatePublishConfigurationInput;

/**
 * Security contract for {@link UserFluxBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserFluxSecurityAspect}. Publish settings
 * only apply to nodes their user owns, so the node IDs of a configuration are
 * not checked.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserFluxBizSecurityContract {

	private UserFluxBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserFluxBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final UserLongCompositePK configId = new UserLongCompositePK(a.userId(), randomLong());

		// @formatter:off
		return SecurityContract.forApi(UserFluxBiz.class, tenants)
				.userWrite(biz -> biz.saveDefaultAggregatePublishConfiguration(a.userId(),
						new UserFluxDefaultAggregatePublishConfigurationInput()))
				.userWrite(biz -> biz.deleteDefaultAggregatePublishConfiguration(a.userId()))
				.userRead(biz -> biz.defaultAggregatePublishConfigurationForUser(a.userId()))
				.userWrite(biz -> biz.saveAggregatePublishConfiguration(configId,
						new UserFluxAggregatePublishConfigurationInput()))
				.userWrite(biz -> biz.deleteAggregatePublishConfiguration(configId))
				.userRead(biz -> biz.aggregatePublishConfigurationForUser(a.userId(),
						configId.getEntityId()))
				.userRead(biz -> biz.aggregatePublishConfigurationsForUser(a.userId(),
						new BasicFluxConfigurationFilter()))
				.build();
		// @formatter:on
	}

}
