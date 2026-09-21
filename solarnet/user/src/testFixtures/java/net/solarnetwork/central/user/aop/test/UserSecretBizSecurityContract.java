/* ==================================================================
 * UserSecretBizSecurityContract.java - 22/09/2026 10:45:38 am
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

package net.solarnetwork.central.user.aop.test;

import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.domain.UserKeyPairInput;
import net.solarnetwork.central.user.domain.UserSecretInput;

/**
 * Security contract for {@link UserSecretBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserSecretsSecurityAspect}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserSecretBizSecurityContract {

	private UserSecretBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserSecretBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();

		// @formatter:off
		return SecurityContract.forApi(UserSecretBiz.class, tenants)
				.userWrite(biz -> biz.saveUserKeyPair(a.userId(), new UserKeyPairInput()))
				.userWrite(biz -> biz.deleteUserKeyPair(a.userId(), "key"))
				.userRead(biz -> biz.listKeyPairsForUser(a.userId(), null))
				.userWrite(biz -> biz.saveUserSecret(a.userId(), new UserSecretInput()))
				.userWrite(biz -> biz.deleteUserSecret(a.userId(), "topic", "key"))
				.userRead(biz -> biz.listSecretsForUser(a.userId(), null))
				.build();
		// @formatter:on
	}

}
