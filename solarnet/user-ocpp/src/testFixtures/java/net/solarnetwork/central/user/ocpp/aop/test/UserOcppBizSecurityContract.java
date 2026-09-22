/* ==================================================================
 * UserOcppBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.ocpp.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.mockito.Mockito.mock;
import java.util.UUID;
import net.solarnetwork.central.ocpp.dao.BasicOcppCriteria;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;
import net.solarnetwork.central.ocpp.domain.UserSettings;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;

/**
 * Security contract for {@link UserOcppBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserOcppSecurityAspect}. Saving a charge
 * point also requires write access to its node.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserOcppBizSecurityContract {

	private UserOcppBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	@SuppressWarnings("unchecked")
	public static SecurityContract<UserOcppBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final Long userId = a.userId();
		final Long id = randomLong();
		final ChargePointConnectorKey connectorId = new ChargePointConnectorKey(id, 1);

		// @formatter:off
		return SecurityContract.forApi(UserOcppBiz.class, tenants)
				.userRead(biz -> biz.systemUserForUser(userId, "test"))
				.userRead(biz -> biz.systemUserForUser(userId, id))
				.userWrite(biz -> biz.deleteUserSystemUser(userId, id))
				.userRead(biz -> biz.systemUsersForUser(userId))
				.userWrite(biz -> biz.saveSystemUser(new CentralSystemUser(userId)))
				.userRead(biz -> biz.authorizationForUser(userId, id))
				.userWrite(biz -> biz.deleteUserAuthorization(userId, id))
				.userRead(biz -> biz.authorizationsForUser(userId))
				.userWrite(biz -> biz.saveAuthorization(new CentralAuthorization(userId)))
				.userRead(biz -> biz.chargePointsForUser(userId))
				.userRead(biz -> biz.listChargePointsForUser(userId, null))
				.allowing(biz -> biz.saveChargePoint(new CentralChargePoint(userId, a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.saveChargePoint(new CentralChargePoint(userId,
						a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.allowing(biz -> biz.saveChargePoint(new CentralChargePoint(userId, b.privateNodeId())))
					.as("other user node")
				.userRead(biz -> biz.chargePointForUser(userId, id))
				.userWrite(biz -> biz.deleteUserChargePoint(userId, id))
				.userRead(biz -> biz.chargePointConnectorForUser(userId, connectorId))
				.userWrite(biz -> biz.deleteUserChargePointConnector(userId, connectorId))
				.userRead(biz -> biz.chargePointConnectorsForUser(userId))
				.userRead(biz -> biz.chargePointConnectorsForUser(userId, id))
				.userWrite(biz -> biz.saveChargePointConnector(
						new CentralChargePointConnector(connectorId, userId)))
				.userRead(biz -> biz.chargePointSettingsForUser(userId, id))
				.userWrite(biz -> biz.deleteUserChargePointSettings(userId, id))
				.userRead(biz -> biz.chargePointSettingsForUser(userId))
				.userWrite(biz -> biz.saveChargePointSettings(new ChargePointSettings(id, userId)))
				.userRead(biz -> biz.findFilteredChargePointStatus(filter(userId),
						mock(FilteredResultsProcessor.class), null, null, null))
				.userRead(biz -> biz.findFilteredChargePointActionStatus(filter(userId),
						mock(FilteredResultsProcessor.class), null, null, null))
				.userRead(biz -> biz.settingsForUser(userId))
				.userWrite(biz -> biz.deleteUserSettings(userId))
				.userWrite(biz -> biz.saveSettings(new UserSettings(userId)))
				.userRead(biz -> biz.chargeSessionForUser(userId, UUID.randomUUID()))
				.userRead(biz -> biz.incompleteChargeSessionsForChargePoint(userId, id))
				.userRead(biz -> biz.findFilteredChargeSessions(filter(userId)))
				.userWrite(biz -> biz.endChargeSession(userId, UUID.randomUUID(),
						ChargeSessionEndReason.Local, null))
				.build();
		// @formatter:on
	}

	private static BasicOcppCriteria filter(Long userId) {
		final BasicOcppCriteria filter = new BasicOcppCriteria();
		filter.setUserId(userId);
		return filter;
	}

}
