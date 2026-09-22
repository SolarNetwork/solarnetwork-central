/* ==================================================================
 * UserDatumInputBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.din.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.util.MimeTypeUtils;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.user.din.domain.EndpointConfigurationInput;
import net.solarnetwork.central.user.din.domain.TransformConfigurationInput;

/**
 * Security contract for {@link UserDatumInputBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserDatumInputSecurityAspect}. The node
 * ID of an endpoint is not checked, because datum input only accepts datum for
 * nodes owned by the endpoint's user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserDatumInputBizSecurityContract {

	private UserDatumInputBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserDatumInputBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final UserLongCompositePK transformId = new UserLongCompositePK(a.userId(), randomLong());
		final UserUuidPK endpointId = new UserUuidPK(a.userId(), UUID.randomUUID());

		// @formatter:off
		return SecurityContract.forApi(UserDatumInputBiz.class, tenants)
				.exempt("availableTransformServices", "global service listing")
				.userRead(biz -> biz.configurationsForUser(a.userId(), null,
						TransformConfiguration.class))
				.userRead(biz -> biz.configurationForId(transformId, TransformConfiguration.class))
				.userWrite(biz -> biz.saveConfiguration(transformId, new TransformConfigurationInput()))
				.userWrite(biz -> biz.saveConfiguration(endpointId, endpoint(a.privateNodeId())))
					.as("endpoint")
				.userWrite(biz -> biz.enableConfiguration(endpointId, true,
						EndpointConfiguration.class))
				.userWrite(biz -> biz.deleteConfiguration(endpointId, EndpointConfiguration.class))
				.userRead(biz -> biz.previewTransform(transformId, UUID.randomUUID(),
						MimeTypeUtils.APPLICATION_JSON, InputStream.nullInputStream(), null))
				.build();
		// @formatter:on
	}

	private static EndpointConfigurationInput endpoint(Long nodeId) {
		final EndpointConfigurationInput input = new EndpointConfigurationInput();
		input.setNodeId(nodeId);
		return input;
	}

}
