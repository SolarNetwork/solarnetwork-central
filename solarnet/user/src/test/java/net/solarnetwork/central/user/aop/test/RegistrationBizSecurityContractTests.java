/* ==================================================================
 * RegistrationBizSecurityContractTests.java - 22/09/2026 10:45:38 am
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
import net.solarnetwork.central.user.aop.RegistrationSecurityAspect;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;

/**
 * Verify the {@link RegistrationBizSecurityContract} for {@link
 * RegistrationBiz} with the {@code RegistrationSecurityAspect} aspect applied.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class RegistrationBizSecurityContractTests {

	private final TestTenants tenants = new TestTenants();

	private SecuredProxy<RegistrationBiz> proxy() {
		final UserNodeConfirmationDao confirmationDao = mock(UserNodeConfirmationDao.class);
		return securedProxy((RegistrationBiz) mock(DaoRegistrationBiz.class),
				new RegistrationSecurityAspect(tenants.ownershipDao(), confirmationDao))
				.withMock(UserNodeConfirmationDao.class, confirmationDao);
	}

	@TestFactory
	public Stream<DynamicNode> securityContract() {
		return RegistrationBizSecurityContract.contract(tenants).dynamicTests(this::proxy);
	}

	@Test
	public void pointcutsMatch() {
		// @formatter:off
		then(unmatchedPointcuts(RegistrationSecurityAspect.class, DaoRegistrationBiz.class))
			.as("Every RegistrationSecurityAspect pointcut matches a RegistrationBiz method")
			.isEmpty()
			;
		// @formatter:on
	}

}
