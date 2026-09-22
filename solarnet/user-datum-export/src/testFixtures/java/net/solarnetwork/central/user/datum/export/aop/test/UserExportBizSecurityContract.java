/* ==================================================================
 * UserExportBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.datum.export.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import java.time.Instant;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.export.biz.UserExportBiz;
import net.solarnetwork.central.user.datum.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.datum.export.domain.UserDatumExportConfiguration;

/**
 * Security contract for {@link UserExportBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserExportSecurityAspect}. Saving an
 * export requires read access to the nodes of its data configuration, and
 * exports only include datum of the export's user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserExportBizSecurityContract {

	private UserExportBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserExportBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();

		// @formatter:off
		return SecurityContract.forApi(UserExportBiz.class, tenants)
				.exempt("availableOutputFormatServices", "global service listing")
				.exempt("availableDestinationServices", "global service listing")
				.exempt("availableOutputCompressionTypes", "global service listing")
				.exempt("availableScheduleTypes", "global service listing")
				.exempt("availableAggregationTypes", "global service listing")
				.userRead(biz -> biz.datumExportConfigurationForUser(a.userId(), randomLong()))
				.userWrite(biz -> biz.saveDatumExportConfiguration(export(a.userId())))
				.allowing(biz -> biz.saveDatumExportConfiguration(export(a.userId(), a.privateNodeId())),
						a.userActor(), a.tokenActor())
					.as("node")
				.allowing(biz -> biz.saveDatumExportConfiguration(export(a.userId(), b.privateNodeId())))
					.as("other user node")
				.userWrite(biz -> biz.deleteDatumExportConfiguration(export(a.userId())))
				.userRead(biz -> biz.datumExportsForUser(a.userId()))
				.userRead(biz -> biz.configurationForUser(a.userId(), UserDataConfiguration.class,
						randomLong()))
				.userWrite(biz -> biz.saveConfiguration(dataConfig(a.userId())))
				.userWrite(biz -> biz.deleteConfiguration(dataConfig(a.userId())))
				.userRead(biz -> biz.configurationsForUser(a.userId(), UserDataConfiguration.class))
				.userWrite(biz -> biz.saveDatumExportTaskForConfiguration(export(a.userId()),
						Instant.now()))
				.userWrite(biz -> biz.saveAdhocDatumExportTaskForConfiguration(export(a.userId())))
				.userRead(biz -> biz.adhocExportTasksForUser(a.userId(), null, null))
				.build();
		// @formatter:on
	}

	private static UserDatumExportConfiguration export(Long userId, Long... nodeIds) {
		final UserDatumExportConfiguration config = new UserDatumExportConfiguration(userId,
				randomLong(), Instant.now(), "Test", ScheduleType.Daily, 0, Instant.now());
		if ( nodeIds.length > 0 ) {
			final DatumFilterCommand filter = new DatumFilterCommand();
			filter.setNodeIds(nodeIds);
			final UserDataConfiguration dataConfig = dataConfig(userId);
			dataConfig.setFilter(filter);
			config.setUserDataConfiguration(dataConfig);
		}
		return config;
	}

	private static UserDataConfiguration dataConfig(Long userId) {
		return new UserDataConfiguration(userId, randomLong(), Instant.now(), "Test",
				"test.service");
	}

}
