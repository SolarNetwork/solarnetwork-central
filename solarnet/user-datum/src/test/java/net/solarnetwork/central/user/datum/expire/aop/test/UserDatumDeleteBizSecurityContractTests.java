/* ==================================================================
 * UserDatumDeleteBizSecurityContractTests.java - 22/09/2026 2:19:25 pm
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

import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.mockito.Mockito.mock;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.expire.aop.UserExpireSecurityAspect;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.datum.expire.biz.dao.DaoUserDatumDeleteBiz;

/**
 * Verify the {@link UserDatumDeleteBizSecurityContract} for
 * {@link UserDatumDeleteBiz} with the {@code UserExpireSecurityAspect} aspect
 * applied.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class UserDatumDeleteBizSecurityContractTests {

	private final TestTenants tenants = new TestTenants();

	private SecuredProxy<UserDatumDeleteBiz> proxy() {
		return securedProxy((UserDatumDeleteBiz) mock(DaoUserDatumDeleteBiz.class),
				new UserExpireSecurityAspect(tenants.ownershipDao()));
	}

	@TestFactory
	public Stream<DynamicNode> securityContract() {
		return UserDatumDeleteBizSecurityContract.contract(tenants).dynamicTests(this::proxy);
	}

}
