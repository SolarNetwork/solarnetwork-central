/* ==================================================================
 * UserEventHookBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.datum.event.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import java.time.Instant;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.datum.event.domain.UserNodeEventHookConfiguration;

/**
 * Security contract for {@link UserEventHookBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserEventHookSecurityAspect}. Hooks only
 * fire for nodes their user owns, so the node IDs of a hook are not checked.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class UserEventHookBizSecurityContract {

	private UserEventHookBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserEventHookBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();

		// @formatter:off
		return SecurityContract.forApi(UserEventHookBiz.class, tenants)
				.exempt("availableDatumEventProducers", "global service listing")
				.exempt("availableNodeEventHookServices", "global service listing")
				.exempt("availableDatumEventTopics", "global service listing")
				.userRead(biz -> biz.configurationForUser(a.userId(),
						UserNodeEventHookConfiguration.class, randomLong()))
				.userRead(biz -> biz.configurationsForUser(a.userId(),
						UserNodeEventHookConfiguration.class))
				.userWrite(biz -> biz.saveConfiguration(config(a.userId())))
				.userWrite(biz -> biz.deleteConfiguration(config(a.userId())))
				.build();
		// @formatter:on
	}

	private static UserNodeEventHookConfiguration config(Long userId) {
		return new UserNodeEventHookConfiguration(randomLong(), userId, Instant.now(), "Test",
				"test.service");
	}

}
