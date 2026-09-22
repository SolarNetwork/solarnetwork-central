/* ==================================================================
 * DatumMaintenanceBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Security contract for {@link DatumMaintenanceBiz}.
 *
 * <p>
 * The contract is enforced by {@code DatumMaintenanceSecurityAspect}. Both
 * marking and finding stale aggregates require write access to every node in
 * the filter.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumMaintenanceBizSecurityContract {

	private DatumMaintenanceBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<DatumMaintenanceBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(DatumMaintenanceBiz.class, tenants)
				.nodeWrite(biz -> biz.markDatumAggregatesStale(filter(a.privateNodeId())))
				.allowing(biz -> biz.markDatumAggregatesStale(filter(a.privateNodeId(), b.privateNodeId())))
					.as("other user node")
				.allowing(biz -> biz.markDatumAggregatesStale(new DatumFilterCommand()))
					.as("no node")
				.nodeWrite(biz -> biz.findStaleAggregateDatum(filter(a.privateNodeId()), null, null, null))
				.allowing(biz -> biz.findStaleAggregateDatum(filter(a.privateNodeId(), b.privateNodeId()),
						null, null, null))
					.as("other user node")
				.build();
		// @formatter:on
	}

	private static DatumFilterCommand filter(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

}
