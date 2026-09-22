/* ==================================================================
 * AuditDatumBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.datum.aop.test;

import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Security contract for {@link AuditDatumBiz}.
 *
 * <p>
 * The contract is enforced by {@code AuditDatumSecurityAspect}. The actor must
 * be a user or a user token, and the filter must specify exactly the actor's
 * user ID.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class AuditDatumBizSecurityContract {

	private AuditDatumBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<AuditDatumBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(AuditDatumBiz.class, tenants)
				.allowing(biz -> biz.findAuditDatumFiltered(filter(a.userId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
				.allowing(biz -> biz.findAuditDatumFiltered(filter(a.userId(), b.userId())))
					.as("other user")
				.allowing(biz -> biz.findAuditDatumFiltered(new BasicDatumCriteria()))
					.as("no user")
				.allowing(biz -> biz.findAccumulativeAuditDatumFiltered(filter(a.userId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
				.allowing(biz -> biz.findAccumulativeAuditDatumFiltered(filter(a.userId(), b.userId())))
					.as("other user")
				.build();
		// @formatter:on
	}

	private static BasicDatumCriteria filter(Long... userIds) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserIds(userIds);
		return filter;
	}

}
