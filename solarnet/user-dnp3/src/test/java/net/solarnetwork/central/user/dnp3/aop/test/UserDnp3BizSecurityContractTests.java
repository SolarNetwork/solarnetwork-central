/* ==================================================================
 * UserDnp3BizSecurityContractTests.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.dnp3.aop.test;

import static net.solarnetwork.central.test.aop.AspectPointcuts.unmatchedPointcuts;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.dnp3.aop.UserDnp3SecurityAspect;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.dnp3.biz.dao.DaoUserDnp3Biz;

/**
 * Verify the {@link UserDnp3BizSecurityContract} for {@link UserDnp3Biz} with
 * the {@code UserDnp3SecurityAspect} aspect applied.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class UserDnp3BizSecurityContractTests {

	private final TestTenants tenants = new TestTenants();

	private SecuredProxy<UserDnp3Biz> proxy() {
		return securedProxy((UserDnp3Biz) mock(DaoUserDnp3Biz.class),
				new UserDnp3SecurityAspect(tenants.ownershipDao()));
	}

	@TestFactory
	public Stream<DynamicNode> securityContract() {
		return UserDnp3BizSecurityContract.contract(tenants).dynamicTests(this::proxy);
	}

	@Test
	public void pointcutsMatch() {
		// @formatter:off
		then(unmatchedPointcuts(UserDnp3SecurityAspect.class, DaoUserDnp3Biz.class))
			.as("Every UserDnp3SecurityAspect pointcut matches a UserDnp3Biz method")
			.isEmpty()
			;
		// @formatter:on
	}

}
