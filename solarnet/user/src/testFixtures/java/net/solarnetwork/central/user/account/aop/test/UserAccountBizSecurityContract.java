/* ==================================================================
 * UserAccountBizSecurityContract.java - 27 Sept 2026 9:03:57 am
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

package net.solarnetwork.central.user.account.aop.test;

import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;

/**
 * Security contract for {@link UserAccountBiz}.
 *
 * <p>
 * The contract is enforced by {@code AccountSecurityAspect}.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class UserAccountBizSecurityContract {

	private UserAccountBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserAccountBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final SnAccountCreationInput creationInput = new SnAccountCreationInput();

		// @formatter:off
		return SecurityContract.forApi(UserAccountBiz.class, tenants)
				.userRead(biz -> biz.getAccountForUser(a.userId()))
				.userWrite(biz -> biz.createAccount(a.userId(), creationInput))
				.userWrite(biz -> biz.updateAccount(a.userId(), creationInput))
				.build();
		// @formatter:on
	}

}
