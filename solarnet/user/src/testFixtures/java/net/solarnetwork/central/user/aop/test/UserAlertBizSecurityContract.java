/* ==================================================================
 * UserAlertBizSecurityContract.java - 22/09/2026 10:45:38 am
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
import static net.solarnetwork.central.user.aop.test.UserContractFixtures.alert;
import static org.mockito.BDDMockito.given;
import java.util.function.Consumer;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;

/**
 * Security contract for {@link UserAlertBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserAlertSecurityAspect}, which needs a
 * {@link UserAlertDao} mock available from {@link SecuredProxy#mock(Class)}.
 * Node alerts require write access to the node, so that the alerts of public
 * nodes are not visible to other users.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserAlertBizSecurityContract {

	private UserAlertBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserAlertBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final Long alertId = randomLong();
		final Consumer<SecuredProxy<UserAlertBiz>> alertSetup = p -> given(
				p.mock(UserAlertDao.class).get(alertId))
				.willReturn(alert(a, alertId, a.privateNodeId()));

		// @formatter:off
		return SecurityContract.forApi(UserAlertBiz.class, tenants)
				.userRead(biz -> biz.userAlertsForUser(a.userId()))
				.allowing(biz -> biz.saveAlert(alert(a, null, a.privateNodeId())),
						a.userActor(), a.tokenActor())
					.as("new node alert")
				.userWrite(biz -> biz.saveAlert(alert(a, null, null)))
					.as("new user alert")
				.allowing(biz -> biz.saveAlert(alert(a, alertId, a.privateNodeId())),
						a.userActor(), a.tokenActor())
					.as("existing node alert")
					.given(alertSetup)
				.userWrite(biz -> biz.deleteAlert(alertId))
					.given(alertSetup)
				.userRead(biz -> biz.alertSituation(alertId))
					.given(alertSetup)
				.userWrite(biz -> biz.updateSituationStatus(alertId,
						UserAlertSituationStatus.Resolved))
					.given(alertSetup)
				.userRead(biz -> biz.alertSituationCountForUser(a.userId()))
				.userRead(biz -> biz.alertSituationsForUser(a.userId()))
				.nodeWrite(biz -> biz.alertSituationsForNode(a.privateNodeId()))
				.build();
		// @formatter:on
	}

}
