/* ==================================================================
 * UserMetadataBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Security contract for {@link UserMetadataBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserMetadataSecurityAspect}. Finding
 * metadata requires read access to every user in the filter.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserMetadataBizSecurityContract {

	private UserMetadataBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserMetadataBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(UserMetadataBiz.class, tenants)
				.userWrite(biz -> biz.addUserMetadata(a.userId(), new GeneralDatumMetadata()))
				.userWrite(biz -> biz.storeUserMetadata(a.userId(), new GeneralDatumMetadata()))
				.userWrite(biz -> biz.removeUserMetadata(a.userId()))
				.userRead(biz -> biz.findUserMetadata(filter(a.userId()), null, null, null))
				.allowing(biz -> biz.findUserMetadata(filter(a.userId(), b.userId()), null, null, null))
					.as("other user")
				.allowing(biz -> biz.findUserMetadata(new BasicUserMetadataFilter(), null, null, null))
					.as("no user")
				.build();
		// @formatter:on
	}

	private static BasicUserMetadataFilter filter(Long... userIds) {
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserIds(userIds);
		return filter;
	}

}
