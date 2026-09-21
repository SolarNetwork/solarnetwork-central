/* ==================================================================
 * UserNodeInstructionBizSecurityContract.java - 22/09/2026 10:45:38 am
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;

/**
 * Security contract for {@link UserNodeInstructionBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserNodeInstructionSecurityAspect}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserNodeInstructionBizSecurityContract {

	private UserNodeInstructionBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserNodeInstructionBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final UserLongCompositePK taskId = new UserLongCompositePK(a.userId(), randomLong());

		// @formatter:off
		return SecurityContract.forApi(UserNodeInstructionBiz.class, tenants)
				.userRead(biz -> biz.listControlInstructionTasksForUser(a.userId(), null))
				.userWrite(biz -> biz.updateControlInstructionTaskState(taskId,
						BasicClaimableJobState.Queued))
				.userWrite(biz -> biz.updateControlInstructionTaskEnabled(taskId, true))
				.userWrite(biz -> biz.saveControlInstructionTask(taskId,
						new UserNodeInstructionTaskEntityInput()))
				.userWrite(biz -> biz.deleteControlInstructionTask(taskId))
				.userRead(biz -> biz.simulateControlInstructionTaskForUser(a.userId(),
						new UserNodeInstructionTaskEntityInput()))
				.build();
		// @formatter:on
	}

}
