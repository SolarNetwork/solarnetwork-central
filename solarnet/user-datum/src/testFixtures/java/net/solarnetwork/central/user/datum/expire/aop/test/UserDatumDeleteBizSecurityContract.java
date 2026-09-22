/* ==================================================================
 * UserDatumDeleteBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link UserDatumDeleteBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserExpireSecurityAspect}. Deleting datum
 * by filter requires write access to the filter's user and to every node in
 * the filter; without nodes the delete applies to all of the user's nodes.
 * Deleting datum by ID only deletes datum of the user's nodes.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class UserDatumDeleteBizSecurityContract {

	private UserDatumDeleteBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserDatumDeleteBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final String jobId = UUID.randomUUID().toString();
		final ObjectDatumId datumId = datumId(a, a.privateNodeId());
		final ObjectDatumId policyDatumId = datumId(a, a.otherPrivateNodeId());

		// @formatter:off
		return SecurityContract.forApi(UserDatumDeleteBiz.class, tenants)
				.allowing(biz -> biz.countDatumRecords(filter(a.userId(), a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.countDatumRecords(filter(a.userId(), a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.allowing(biz -> biz.countDatumRecords(filter(a.userId(), b.privateNodeId())))
					.as("other user node")
				.userWrite(biz -> biz.countDatumRecords(filter(a.userId())))
					.as("no node")
				.allowing(biz -> biz.submitDatumDeleteRequest(filter(a.userId(), a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.submitDatumDeleteRequest(filter(a.userId(), b.privateNodeId())))
					.as("other user node")
				.userWrite(biz -> biz.submitDatumDeleteRequest(filter(a.userId())))
					.as("no node")
				.userRead(biz -> biz.datumDeleteJobForUser(a.userId(), jobId))
				.userRead(biz -> biz.datumDeleteJobsForUser(a.userId(), null))
				.allowing(biz -> biz.deleteDatum(a.userId(), Set.of(datumId)),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.deleteDatum(a.userId(), Set.of(policyDatumId)),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.build();
		// @formatter:on
	}

	private static ObjectDatumId datumId(TestTenant tenant, Long nodeId) {
		final String sourceId = tenant.sourceIds().getFirst();
		return ObjectDatumId.datumId(ObjectDatumKind.Node,
				tenant.stream(nodeId, sourceId).getStreamId(), nodeId, sourceId, Instant.now(), null);
	}

	private static DatumFilterCommand filter(Long userId, Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setUserId(userId);
		if ( nodeIds.length > 0 ) {
			filter.setNodeIds(nodeIds);
		}
		return filter;
	}

}
