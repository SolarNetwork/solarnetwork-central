/* ==================================================================
 * UserEventBizSecurityContract.java - 23/09/2026 9:26:33 am
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

import net.solarnetwork.central.biz.UserEventBiz;
import net.solarnetwork.central.common.dao.BasicUserEventFilter;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Security contract for {@link UserEventBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserEventSecurityAspect}. Events have
 * unstructured data, so a security policy cannot be applied to them: finding
 * events requires read access to every user in the filter, and an unrestricted
 * security policy.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserEventBizSecurityContract {

	private UserEventBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserEventBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(UserEventBiz.class, tenants)
				.allowing(biz -> biz.findFilteredUserEvents(filter(a.userId()), processor()),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.findFilteredUserEvents(filter(a.userId(), b.userId()), processor()))
					.as("other user")
				.allowing(biz -> biz.findFilteredUserEvents(new BasicUserEventFilter(), processor()))
					.as("no user")
				.build();
		// @formatter:on
	}

	private static FilteredResultsProcessor<UserEvent> processor() {
		return new AbstractFilteredResultsProcessor<>() {

			@Override
			public void handleResultItem(UserEvent resultItem) {
				// nothing to do
			}

		};
	}

	private static BasicUserEventFilter filter(Long... userIds) {
		final BasicUserEventFilter filter = new BasicUserEventFilter();
		filter.setUserIds(userIds);
		return filter;
	}

}
