/* ==================================================================
 * UserExpireBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.datum.expire.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import java.time.Instant;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.datum.expire.domain.ExpireUserDataConfiguration;

/**
 * Security contract for {@link UserExpireBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserExpireSecurityAspect}. Expiring datum
 * only applies to nodes the configuration's user owns, so the node IDs of a
 * configuration are not checked.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserExpireBizSecurityContract {

	private UserExpireBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserExpireBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();

		// @formatter:off
		return SecurityContract.forApi(UserExpireBiz.class, tenants)
				.exempt("availableAggregationTypes", "global service listing")
				.userRead(biz -> biz.configurationForUser(a.userId(),
						ExpireUserDataConfiguration.class, randomLong()))
				.userRead(biz -> biz.configurationsForUser(a.userId(),
						ExpireUserDataConfiguration.class))
				.userWrite(biz -> biz.saveConfiguration(config(a.userId())))
				.userWrite(biz -> biz.deleteConfiguration(config(a.userId())))
				.userWrite(biz -> biz.countExpiredDataForConfiguration(config(a.userId())))
				.build();
		// @formatter:on
	}

	private static ExpireUserDataConfiguration config(Long userId) {
		return new ExpireUserDataConfiguration(randomLong(), userId, Instant.now(), "Test",
				"test.service");
	}

}
