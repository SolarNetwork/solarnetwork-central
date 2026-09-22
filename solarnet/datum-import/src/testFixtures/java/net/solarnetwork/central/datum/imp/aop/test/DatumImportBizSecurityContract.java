/* ==================================================================
 * DatumImportBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.datum.imp.aop.test;

import java.util.Set;
import java.util.UUID;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Security contract for {@link DatumImportBiz}.
 *
 * <p>
 * The contract is enforced by {@code DatumImportSecurityAspect}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumImportBizSecurityContract {

	private DatumImportBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<DatumImportBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final String jobId = UUID.randomUUID().toString();

		// @formatter:off
		return SecurityContract.forApi(DatumImportBiz.class, tenants)
				.exempt("availableInputFormatServices", "global service listing")
				.userWrite(biz -> biz.submitDatumImportRequest(
						new BasicDatumImportRequest(new BasicConfiguration("Test", true), a.userId()),
						null))
				.userWrite(biz -> biz.previewStagedImportRequest(
						new BasicDatumImportPreviewRequest(a.userId(), jobId, 1)))
				.userRead(biz -> biz.datumImportJobStatusForUser(a.userId(), jobId))
				.userRead(biz -> biz.datumImportJobStatusesForUser(a.userId(), null))
				.userWrite(biz -> biz.updateDatumImportJobConfigurationForUser(a.userId(), jobId,
						new BasicConfiguration("Test", true)))
				.userWrite(biz -> biz.updateDatumImportJobStateForUser(a.userId(), jobId,
						DatumImportState.Queued, null))
				.userWrite(biz -> biz.deleteDatumImportJobsForUser(a.userId(), Set.of(jobId)))
				.userWrite(biz -> biz.deleteDatumImportJobsForUser(a.userId(), Set.of(jobId), true))
				.build();
		// @formatter:on
	}

}
