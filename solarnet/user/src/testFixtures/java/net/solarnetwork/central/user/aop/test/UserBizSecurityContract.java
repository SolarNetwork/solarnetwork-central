/* ==================================================================
 * UserBizSecurityContract.java - 22/09/2026 10:45:38 am
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
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.user;
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.userNode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.List;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.dao.BasicUserAuthTokenFilter;
import net.solarnetwork.central.user.dao.BasicUserNodeFilter;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.BasicSecurityPolicy;

/**
 * Security contract for {@link UserBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserSecurityAspect} and
 * {@code UserAuthTokenSecurityAspect}. Methods that take a user ID and a node
 * ID require access to both, and security token management requires an
 * unrestricted security policy.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserBizSecurityContract {

	private UserBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final Long userId = a.userId();
		final Long nodeId = a.privateNodeId();
		final String tokenId = a.userToken().tokenId();

		final Long confirmationId = randomLong();
		final UserNodeConfirmation confirmation = new UserNodeConfirmation(user(a));
		confirmation.setId(confirmationId);
		confirmation.setNodeId(nodeId);

		// @formatter:off
		return SecurityContract.forApi(UserBiz.class, tenants)
				.userRead(biz -> biz.getUser(userId))
				.userRead(biz -> biz.getUserNodes(userId))
				.userRead(biz -> biz.findUserNodeInfos(userId, new BasicUserNodeFilter()))
				.allowing(biz -> biz.getUserNode(userId, nodeId),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.nodeWrite(biz -> biz.saveUserNode(userNode(a, nodeId)))
				.allowing(biz -> biz.updateUserNodeArchivedStatus(userId, new Long[] { nodeId }, true),
						a.userActor(), a.tokenActor())
				.userRead(biz -> biz.getArchivedUserNodes(userId))
				.userRead(biz -> biz.getPendingUserNodeConfirmations(userId))
				.nodeRead(biz -> biz.getPendingUserNodeConfirmation(confirmationId))
					.given(p -> given(p.target().getPendingUserNodeConfirmation(confirmationId))
							.willReturn(confirmation))
					.targetInvokedOnDeny()
				.allowing(biz -> biz.getUserNodeCertificate(userId, nodeId),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.generateUserAuthToken(userId, SecurityTokenType.User,
						BasicSecurityPolicy.builder().build()),
						a.userActor(), a.tokenActor())
				.userRead(biz -> biz.getAllUserAuthTokens(userId))
					.given(p -> given(p.target().listUserAuthTokensForUser(eq(userId), any()))
							.willReturn(new BasicFilterResults<>(List.of())))
				.userRead(biz -> biz.listUserAuthTokensForUser(userId, new BasicUserAuthTokenFilter()))
				.allowing(biz -> biz.deleteUserAuthToken(userId, tokenId),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.updateUserAuthTokenStatus(userId, tokenId,
						SecurityTokenStatus.Disabled),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.updateUserAuthTokenPolicy(userId, tokenId,
						BasicSecurityPolicy.builder().build(), true),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.updateUserAuthTokenInfo(userId, tokenId,
						new UserAuthToken(tokenId, userId, randomString(), SecurityTokenType.User)),
						a.userActor(), a.tokenActor())
				.userWrite(biz -> biz.createSnws2AuthorizationBuilder(userId, tokenId, Instant.now()))
				.build();
		// @formatter:on
	}

}
