/* ==================================================================
 * DatumAuxiliaryBizSecurityContractTests.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.datum.aop.test;

import static net.solarnetwork.central.test.aop.AspectPointcuts.unmatchedPointcuts;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.datum.aop.DatumAuxiliarySecurityAspect;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumAuxiliaryBiz;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Verify the {@link DatumAuxiliaryBizSecurityContract} for
 * {@link DatumAuxiliaryBiz} with the {@code DatumAuxiliarySecurityAspect} aspect
 * applied.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class DatumAuxiliaryBizSecurityContractTests {

	private final TestTenants tenants = new TestTenants();

	private SecuredProxy<DatumAuxiliaryBiz> proxy() {
		final DatumStreamMetadataDao metaDao = mock(DatumStreamMetadataDao.class);
		return securedProxy((DatumAuxiliaryBiz) mock(DaoDatumAuxiliaryBiz.class),
				new DatumAuxiliarySecurityAspect(tenants.ownershipDao(), metaDao))
				.withMock(DatumStreamMetadataDao.class, metaDao);
	}

	@TestFactory
	public Stream<DynamicNode> securityContract() {
		return DatumAuxiliaryBizSecurityContract.contract(tenants).dynamicTests(this::proxy);
	}

	@Test
	public void pointcutsMatch() {
		// @formatter:off
		then(unmatchedPointcuts(DatumAuxiliarySecurityAspect.class, DaoDatumAuxiliaryBiz.class))
			.as("Every DatumAuxiliarySecurityAspect pointcut matches a DatumAuxiliaryBiz method")
			.isEmpty()
			;
		// @formatter:on
	}

}
