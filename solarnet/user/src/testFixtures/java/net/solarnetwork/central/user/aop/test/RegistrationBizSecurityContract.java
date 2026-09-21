/* ==================================================================
 * RegistrationBizSecurityContract.java - 22/09/2026 10:45:38 am
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
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.newNodeRequest;
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.user;
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.userNode;
import static org.mockito.BDDMockito.given;
import java.io.InputStream;
import java.util.function.Consumer;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * Security contract for {@link RegistrationBiz}.
 *
 * <p>
 * The contract is enforced by {@code RegistrationSecurityAspect}, which needs a
 * {@link UserNodeConfirmationDao} mock available from
 * {@link SecuredProxy#mock(Class)}. User registration, password reset, and node
 * association are anonymous flows authorized by confirmation codes, and are
 * exempt.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class RegistrationBizSecurityContract {

	private RegistrationBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<RegistrationBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final Long confirmationId = randomLong();
		final Consumer<SecuredProxy<RegistrationBiz>> confirmationSetup = p -> given(
				p.mock(UserNodeConfirmationDao.class).get(confirmationId))
				.willReturn(new UserNodeConfirmation(user(a)));

		// @formatter:off
		return SecurityContract.forApi(RegistrationBiz.class, tenants)
				.exempt("registerUser", "anonymous user registration")
				.exempt("createReceipt", "anonymous user registration")
				.exempt("confirmRegisteredUser", "anonymous user registration")
				.userWrite(biz -> biz.createNodeAssociation(newNodeRequest(a)))
				.allowing(biz -> biz.createNodeManually(newNodeRequest(a)),
						a.userActor(), a.tokenActor())
				.userRead(biz -> biz.getNodeAssociation(confirmationId))
					.given(confirmationSetup)
				.userWrite(biz -> biz.cancelNodeAssociation(confirmationId))
					.given(confirmationSetup)
				.exempt("confirmNodeAssociation",
						"anonymous node association, authorized by the invitation confirmation key")
				.exempt("getNodeCertificate",
						"anonymous node association, authorized by the invitation confirmation key")
				.exempt(biz -> biz.renewNodeCertificate(InputStream.nullInputStream(), "password"),
						"renews the certificate of the active node, verified in the service")
				.exempt("getNodeCertificateRenewalPeriod", "global configuration")
				.nodeWrite(biz -> biz.renewNodeCertificate(userNode(a, a.privateNodeId()), "password"))
				.nodeWrite(biz -> biz.getPendingNodeCertificateRenewal(
						userNode(a, a.privateNodeId()), "key"))
				.userWrite(biz -> biz.updateUser(user(a)))
				.exempt("generateResetPasswordReceipt", "anonymous password reset")
				.exempt("resetPassword", "anonymous password reset")
				.build();
		// @formatter:on
	}

}
