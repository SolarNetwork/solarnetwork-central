/* ==================================================================
 * SolarNodeMetadataBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.aop.test;

import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Security contract for {@link SolarNodeMetadataBiz}.
 *
 * <p>
 * The contract is enforced by {@code NodeMetadataSecurityAspect}. Modifying
 * metadata requires write access to the node. Finding metadata requires read
 * access to the filter's nodes, which the security policy enforcer applies,
 * so metadata of public nodes can be found by anyone whose policy allows the
 * node.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SolarNodeMetadataBizSecurityContract {

	private SolarNodeMetadataBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<SolarNodeMetadataBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestActor[] everyone = tenants.actors().toArray(TestActor[]::new);

		// @formatter:off
		return SecurityContract.forApi(SolarNodeMetadataBiz.class, tenants)
				.nodeWrite(biz -> biz.addSolarNodeMetadata(a.privateNodeId(), new GeneralDatumMetadata()))
				.allowing(biz -> biz.addSolarNodeMetadata(a.otherPrivateNodeId(), new GeneralDatumMetadata()),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.nodeWrite(biz -> biz.storeSolarNodeMetadata(a.privateNodeId(), new GeneralDatumMetadata()))
				.nodeWrite(biz -> biz.removeSolarNodeMetadata(a.privateNodeId()))
				.nodeRead(biz -> biz.findSolarNodeMetadata(filter(a.privateNodeId()), null, null, null))
				.allowing(biz -> biz.findSolarNodeMetadata(filter(a.otherPrivateNodeId()), null, null, null),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node")
				.allowing(biz -> biz.findSolarNodeMetadata(filter(a.publicNodeId()), null, null, null),
						everyone)
					.alsoDeny(a.restrictedTokenActor())
					.as("public node")
				.allowing(biz -> biz.findSolarNodeMetadata(new FilterSupport(), null, null, null))
					.as("no node")
				.build();
		// @formatter:on
	}

	private static FilterSupport filter(Long nodeId) {
		final FilterSupport filter = new FilterSupport();
		filter.setNodeId(nodeId);
		return filter;
	}

}
