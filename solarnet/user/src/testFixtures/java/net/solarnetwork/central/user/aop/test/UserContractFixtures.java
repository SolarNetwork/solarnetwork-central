/* ==================================================================
 * UserContractFixtures.java - 22/09/2026 10:45:38 am
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

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import java.util.Locale;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * User domain objects for security contract cases.
 *
 * @author matt
 * @version 1.0
 */
public final class UserContractFixtures {

	private UserContractFixtures() {
		// not available
	}

	/**
	 * Get a user entity for a tenant.
	 *
	 * @param tenant
	 *        the tenant
	 * @return the user
	 */
	public static User user(TestTenant tenant) {
		return new User(tenant.userId(), tenant.email());
	}

	/**
	 * Get a user node entity for a tenant node.
	 *
	 * @param tenant
	 *        the tenant
	 * @param nodeId
	 *        the node ID
	 * @return the user node
	 */
	public static UserNode userNode(TestTenant tenant, Long nodeId) {
		final SolarNode node = new SolarNode();
		node.setId(nodeId);
		return new UserNode(user(tenant), node);
	}

	/**
	 * Get a new node request for a tenant.
	 *
	 * @param tenant
	 *        the tenant
	 * @return the request
	 */
	public static NewNodeRequest newNodeRequest(TestTenant tenant) {
		return new NewNodeRequest(tenant.userId(), randomString(),
				TimeZone.getTimeZone(TestTenant.TIME_ZONE), Locale.ENGLISH);
	}

	/**
	 * Get a node stale data alert for a tenant.
	 *
	 * @param tenant
	 *        the tenant
	 * @param alertId
	 *        the alert ID, or {@code null} for a new alert
	 * @param nodeId
	 *        the alert node ID, or {@code null} for a user alert
	 * @return the alert
	 */
	public static UserAlert alert(TestTenant tenant, @Nullable Long alertId,
			@Nullable Long nodeId) {
		final UserAlert alert = new UserAlert();
		alert.setId(alertId);
		alert.setUserId(tenant.userId());
		alert.setNodeId(nodeId);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);
		return alert;
	}

}
