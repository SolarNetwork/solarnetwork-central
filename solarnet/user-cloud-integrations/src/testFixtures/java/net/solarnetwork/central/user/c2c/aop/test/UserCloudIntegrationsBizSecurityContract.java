/* ==================================================================
 * UserCloudIntegrationsBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

package net.solarnetwork.central.user.c2c.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.ModifiableServicePropertiesDao.MergeMode;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamRakeTaskEntityBaseInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamRakeTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.UserSettingsEntityInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link UserCloudIntegrationsBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserCloudIntegrationsSecurityAspect}.
 * Datum streams require access to their node, which the aspect looks up for
 * entities related to a datum stream, and integrations and user settings
 * require an unrestricted security policy. Listings are narrowed to the
 * security policy's nodes, and denied when only other nodes are requested. The
 * proxy must include a {@link CloudDatumStreamConfigurationDao} mock.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class UserCloudIntegrationsBizSecurityContract {

	private UserCloudIntegrationsBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserCloudIntegrationsBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final Long userId = a.userId();
		final UserLongCompositePK integrationId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK datumStreamId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK mappingId = new UserLongCompositePK(userId, randomLong());
		final UserLongCompositePK taskId = new UserLongCompositePK(userId, randomLong());
		final CloudDatumStreamConfiguration stream = datumStream(datumStreamId, a.privateNodeId());
		final Consumer<SecuredProxy<UserCloudIntegrationsBiz>> streamExists = p -> {
			final CloudDatumStreamConfigurationDao dao = p.mock(CloudDatumStreamConfigurationDao.class);
			given(dao.get(any())).willReturn(stream);
			given(dao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(stream)));
		};
		final TestActor[] unrestricted = new TestActor[] { a.userActor(), a.tokenActor() };

		// @formatter:off
		return SecurityContract.forApi(UserCloudIntegrationsBiz.class, tenants)
				.exempt("availableIntegrationServices", "global service listing")
				.exempt("integrationService", "global service lookup")
				.exempt("datumStreamService", "global service lookup")
				.exempt("controlService", "global service lookup")
				.exempt("defaultDatumStreamSettings", "global default settings")
				.userRead(biz -> biz.settingsForUser(userId))
				.allowing(biz -> biz.saveSettings(userId, new UserSettingsEntityInput()), unrestricted)
				.allowing(biz -> biz.deleteSettings(userId), unrestricted)
				.userRead(biz -> biz.listConfigurationsForUser(userId, null,
						CloudIntegrationConfiguration.class))
				.userRead(biz -> biz.listConfigurationsForUser(userId, nodeFilter(a.privateNodeId()),
						CloudDatumStreamConfiguration.class))
					.alsoDeny(a.restrictedTokenActor())
					.as("datum streams of non-policy node")
				.userRead(biz -> biz.listConfigurationsForUser(userId, nodeFilter(a.privateNodeId()),
						CloudControlConfiguration.class))
					.alsoDeny(a.restrictedTokenActor())
					.as("controls of non-policy node")
				.userRead(biz -> biz.listConfigurationsForUser(userId,
						nodeFilter(a.otherPrivateNodeId()), CloudDatumStreamConfiguration.class))
					.as("datum streams of policy node")
				.userRead(biz -> biz.configurationForId(integrationId,
						CloudIntegrationConfiguration.class))
				.allowing(biz -> biz.configurationForId(datumStreamId,
						CloudDatumStreamConfiguration.class),
						a.userActor(), a.tokenActor(), a.nodeActor())
					.given(streamExists)
					.as("datum stream")
				.userWrite(biz -> biz.saveConfiguration(integrationId,
						new CloudIntegrationConfigurationInput()))
				.allowing(biz -> biz.saveConfiguration(datumStreamId,
						datumStreamInput(a.privateNodeId())),
						unrestricted)
					.as("datum stream")
				.allowing(biz -> biz.saveConfiguration(datumStreamId,
						datumStreamInput(b.privateNodeId())))
					.as("datum stream, other user node")
				.allowing(biz -> biz.replaceDatumStreamPropertyConfiguration(mappingId,
						List.of(new CloudDatumStreamPropertyConfigurationInput())),
						unrestricted)
					.given(streamExists)
				.allowing(biz -> biz.mergeConfigurationServiceProperties(integrationId, Map.of(),
						CloudIntegrationConfiguration.class),
						unrestricted)
				.allowing(biz -> biz.mergeConfigurationServiceProperties(integrationId,
						MergeMode.Simple, new HashMap<>(), CloudIntegrationConfiguration.class),
						unrestricted)
				.userWrite(biz -> biz.updateConfigurationEnabled(integrationId, true,
						CloudIntegrationConfiguration.class))
				.allowing(biz -> biz.deleteConfiguration(integrationId,
						CloudIntegrationConfiguration.class),
						unrestricted)
				.allowing(biz -> biz.deleteConfiguration(datumStreamId,
						CloudDatumStreamConfiguration.class),
						unrestricted)
					.given(streamExists)
					.as("datum stream")
				.userRead(biz -> biz.validateIntegrationConfigurationForId(integrationId,
						Locale.ENGLISH))
				.userRead(biz -> biz.listDatumStreamDataValues(integrationId, "test.service", null))
				.userRead(biz -> biz.listControlDataValues(integrationId, "test.service", null))
				.userRead(biz -> biz.latestDatumStreamDatumForId(datumStreamId))
				.userRead(biz -> biz.listDatumStreamDatum(datumStreamId, new BasicQueryFilter()))
				.userRead(biz -> biz.listDatumStreamPollTasksForUser(userId, null))
				.userWrite(biz -> biz.updateDatumStreamPollTaskState(datumStreamId,
						BasicClaimableJobState.Queued))
				.userWrite(biz -> biz.saveDatumStreamPollTask(datumStreamId,
						new CloudDatumStreamPollTaskEntityInput()))
				.userWrite(biz -> biz.deleteDatumStreamPollTask(datumStreamId))
				.userRead(biz -> biz.listDatumStreamRakeTasksForUser(userId, null))
				.userWrite(biz -> biz.updateDatumStreamRakeTaskState(taskId,
						BasicClaimableJobState.Queued))
				.allowing(biz -> biz.saveDatumStreamRakeTask(taskId,
						rakeTaskInput(datumStreamId.getEntityId())),
						unrestricted)
					.given(streamExists)
				.userWrite(biz -> biz.deleteDatumStreamRakeTask(taskId))
				.allowing(biz -> biz.replaceDatumStreamRakeTasks(datumStreamId,
						List.of(new CloudDatumStreamRakeTaskEntityBaseInput())),
						unrestricted)
					.given(streamExists)
				.build();
		// @formatter:on
	}

	private static BasicFilter nodeFilter(Long nodeId) {
		final BasicFilter filter = new BasicFilter();
		filter.setNodeIds(new Long[] { nodeId });
		return filter;
	}

	private static CloudDatumStreamConfiguration datumStream(UserLongCompositePK id, Long nodeId) {
		final CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(id,
				Instant.now(), "Test", "test.service", ObjectDatumKind.Node);
		conf.setObjectId(nodeId);
		return conf;
	}

	private static CloudDatumStreamConfigurationInput datumStreamInput(Long nodeId) {
		final CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(nodeId);
		return input;
	}

	private static CloudDatumStreamRakeTaskEntityInput rakeTaskInput(Long datumStreamId) {
		final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
		input.setDatumStreamId(datumStreamId);
		return input;
	}

}
