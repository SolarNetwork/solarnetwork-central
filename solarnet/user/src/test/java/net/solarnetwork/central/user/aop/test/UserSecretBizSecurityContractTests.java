/* ==================================================================
 * UserSecretBizSecurityContractTests.java - 22/09/2026 10:45:38 am
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
import net.solarnetwork.central.user.aop.UserSecretsSecurityAspect;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.biz.dao.DaoUserSecretBiz;

/**
 * Verify the {@link UserSecretBizSecurityContract} for {@link UserSecretBiz}
 * with the {@code UserSecretsSecurityAspect} aspect applied.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class UserSecretBizSecurityContractTests {

	private final TestTenants tenants = new TestTenants();

	private SecuredProxy<UserSecretBiz> proxy() {
		return securedProxy((UserSecretBiz) mock(DaoUserSecretBiz.class),
				new UserSecretsSecurityAspect(tenants.ownershipDao()));
	}

	@TestFactory
	public Stream<DynamicNode> securityContract() {
		return UserSecretBizSecurityContract.contract(tenants).dynamicTests(this::proxy);
	}

	@Test
	public void pointcutsMatch() {
		// @formatter:off
		then(unmatchedPointcuts(UserSecretsSecurityAspect.class, DaoUserSecretBiz.class))
			.as("Every UserSecretsSecurityAspect pointcut matches a UserSecretBiz method")
			.isEmpty()
			;
		// @formatter:on
	}

}
