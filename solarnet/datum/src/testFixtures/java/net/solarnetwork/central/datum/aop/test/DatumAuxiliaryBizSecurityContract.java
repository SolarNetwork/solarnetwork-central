/* ==================================================================
 * DatumAuxiliaryBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import java.time.Instant;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Security contract for {@link DatumAuxiliaryBiz}.
 *
 * <p>
 * The contract is enforced by {@code DatumAuxiliarySecurityAspect}. Viewing and
 * finding auxiliary datum requires read access to the nodes, subject to the
 * security policy's node and source restrictions. Modifying auxiliary datum
 * requires write access to the nodes.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumAuxiliaryBizSecurityContract {

	private DatumAuxiliaryBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<DatumAuxiliaryBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final String policySourceId = a.sourceIds().getFirst();
		final String otherSourceId = a.sourceIds().get(1);

		// @formatter:off
		return SecurityContract.forApi(DatumAuxiliaryBiz.class, tenants)
				.nodeRead(biz -> biz.getGeneralNodeDatumAuxiliary(pk(a.privateNodeId(), policySourceId)))
				.allowing(biz -> biz.getGeneralNodeDatumAuxiliary(
						pk(a.otherPrivateNodeId(), policySourceId)),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node and source")
				.allowing(biz -> biz.getGeneralNodeDatumAuxiliary(
						pk(a.otherPrivateNodeId(), otherSourceId)),
						a.userActor(), a.tokenActor(), a.dataTokenActor())
					.as("policy node, other source")
				.nodeWrite(biz -> biz.storeGeneralNodeDatumAuxiliary(
						new GeneralNodeDatumAuxiliary(pk(a.privateNodeId(), policySourceId))))
				.nodeWrite(biz -> biz.moveGeneralNodeDatumAuxiliary(pk(a.privateNodeId(), policySourceId),
						new GeneralNodeDatumAuxiliary(pk(a.privateNodeId(), otherSourceId))))
				.allowing(biz -> biz.moveGeneralNodeDatumAuxiliary(pk(a.privateNodeId(), policySourceId),
						new GeneralNodeDatumAuxiliary(pk(b.privateNodeId(), policySourceId))))
					.as("to other user node")
				.nodeWrite(biz -> biz.removeGeneralNodeDatumAuxiliary(pk(a.privateNodeId(), policySourceId)))
				.nodeRead(biz -> biz.findGeneralNodeDatumAuxiliary(filter(a.privateNodeId()), null, null,
						null))
				.allowing(biz -> biz.findGeneralNodeDatumAuxiliary(filter(a.otherPrivateNodeId()), null,
						null, null),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node")
				.allowing(biz -> biz.findGeneralNodeDatumAuxiliary(
						filter(a.privateNodeId(), b.privateNodeId()), null, null, null))
					.as("other user node")
				.allowing(biz -> biz.findGeneralNodeDatumAuxiliary(new DatumFilterCommand(), null, null,
						null))
					.as("no node")
				.build();
		// @formatter:on
	}

	private static GeneralNodeDatumAuxiliaryPK pk(Long nodeId, String sourceId) {
		return new GeneralNodeDatumAuxiliaryPK(nodeId, Instant.now(), sourceId);
	}

	private static DatumFilterCommand filter(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

}
