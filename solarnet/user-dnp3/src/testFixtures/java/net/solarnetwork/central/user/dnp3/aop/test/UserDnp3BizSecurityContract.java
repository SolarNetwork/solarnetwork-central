/* ==================================================================
 * UserDnp3BizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Locale;
import org.springframework.core.io.ByteArrayResource;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.dnp3.domain.ServerAuthConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerControlConfigurationInput;
import net.solarnetwork.central.user.dnp3.domain.ServerMeasurementConfigurationInput;

/**
 * Security contract for {@link UserDnp3Biz}.
 *
 * <p>
 * The contract is enforced by {@code UserDnp3SecurityAspect}. The node IDs of
 * measurement and control configurations are not checked, because DNP3
 * servers only use configurations for nodes owned by the server's user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserDnp3BizSecurityContract {

	private UserDnp3BizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserDnp3Biz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final Long userId = a.userId();
		final Long serverId = randomLong();

		// @formatter:off
		return SecurityContract.forApi(UserDnp3Biz.class, tenants)
				.userWrite(biz -> biz.saveTrustedIssuerCertificates(userId, new X509Certificate[0]))
				.userRead(biz -> biz.trustedIssuerCertificatesForUser(userId, null))
				.userWrite(biz -> biz.updateTrustedIssuerCertificateEnabledStatus(userId, null, true))
				.userWrite(biz -> biz.deleteTrustedIssuerCertificate(userId, "CN=Test"))
				.userWrite(biz -> biz.createServer(userId, new ServerConfigurationInput()))
				.userWrite(biz -> biz.updateServer(userId, serverId, new ServerConfigurationInput()))
				.userWrite(biz -> biz.updateServerEnabledStatus(userId, null, true))
				.userWrite(biz -> biz.deleteServer(userId, serverId))
				.userRead(biz -> biz.serversForUser(userId, null))
				.userWrite(biz -> biz.saveServerAuth(userId, serverId, "Test",
						new ServerAuthConfigurationInput()))
				.userWrite(biz -> biz.updateServerAuthEnabledStatus(userId, null, true))
				.userWrite(biz -> biz.deleteServerAuth(userId, serverId, "Test"))
				.userRead(biz -> biz.serverAuthsForUser(userId, null))
				.userWrite(biz -> biz.saveServerMeasurement(userId, serverId, 0,
						new ServerMeasurementConfigurationInput()))
				.userWrite(biz -> biz.updateServerMeasurementEnabledStatus(userId, null, true))
				.userWrite(biz -> biz.deleteServerMeasurement(userId, serverId, 0))
				.userRead(biz -> biz.serverMeasurementsForUser(userId, null))
				.userWrite(biz -> biz.saveServerControl(userId, serverId, 0,
						new ServerControlConfigurationInput()))
				.userWrite(biz -> biz.updateServerControlEnabledStatus(userId, null, true))
				.userWrite(biz -> biz.deleteServerControl(userId, serverId, 0))
				.userRead(biz -> biz.serverControlsForUser(userId, null))
				.exempt("serverConfigurationCsvExample", "static example resource")
				.userWrite(biz -> biz.importServerConfigurationsCsv(userId, serverId,
						new ByteArrayResource(new byte[0]), Locale.ENGLISH))
				.userRead(biz -> biz.exportServerConfigurationsCsv(userId, null,
						OutputStream.nullOutputStream(), Locale.ENGLISH))
				.build();
		// @formatter:on
	}

}
