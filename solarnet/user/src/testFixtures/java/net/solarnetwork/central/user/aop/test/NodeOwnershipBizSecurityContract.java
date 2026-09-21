/* ==================================================================
 * NodeOwnershipBizSecurityContract.java - 22/09/2026 10:45:38 am
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

import static net.solarnetwork.central.user.aop.test.UserContractFixtures.user;
import static org.mockito.BDDMockito.given;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * Security contract for {@link NodeOwnershipBiz}.
 *
 * <p>
 * The contract is enforced by {@code NodeOwnershipSecurityAspect}, which needs
 * {@link UserDao} and {@link UserNodeDao} mocks available from
 * {@link SecuredProxy#mock(Class)}. Only the node owner can request, view, or
 * cancel a transfer, and only the transfer recipient can confirm it.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class NodeOwnershipBizSecurityContract {

	private NodeOwnershipBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<NodeOwnershipBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(NodeOwnershipBiz.class, tenants)
				.allowing(biz -> biz.getNodeOwnershipTransfer(a.userId(), a.privateNodeId()),
						a.userActor(), a.tokenActor())
				.userRead(biz -> biz.pendingNodeOwnershipTransfersForEmail(a.email()))
					.given(p -> given(p.mock(UserDao.class).getUserByEmail(a.email()))
							.willReturn(user(a)))
				.allowing(biz -> biz.requestNodeOwnershipTransfer(a.userId(), a.privateNodeId(),
						b.email()),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.cancelNodeOwnershipTransfer(a.userId(), a.privateNodeId()),
						a.userActor(), a.tokenActor())
				.userWrite(biz -> biz.confirmNodeOwnershipTransfer(b.userId(), b.privateNodeId(), true))
					.as("transfer of B node to A")
					.given(p -> {
						given(p.mock(UserNodeDao.class).getUserNodeTransfer(
								new UserNodePK(b.userId(), b.privateNodeId())))
								.willReturn(new UserNodeTransfer(b.userId(), b.privateNodeId(),
										a.email()));
						given(p.mock(UserDao.class).getUserByEmail(a.email())).willReturn(user(a));
					})
				.build();
		// @formatter:on
	}

}
