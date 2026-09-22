/* ==================================================================
 * UserCloudIntegrationsBizSecurityPolicyTests.java - 22/09/2026 7:12:33 pm
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

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.c2c.aop.UserCloudIntegrationsSecurityAspect;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Verify security policies are enforced on {@link UserCloudIntegrationsBiz}
 * with the {@code UserCloudIntegrationsSecurityAspect} aspect applied, using
 * the database.
 *
 * <p>
 * Each tenant has one integration and datum stream mapping, plus a datum stream
 * and a control for its private node. Tenant A also has a datum stream and a
 * control for its other private node, which is the only node the restricted
 * token's policy allows.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
@ExtendWith(SecurityContextExtension.class)
public class UserCloudIntegrationsBizSecurityPolicyTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final String INTEGRATION_SERVICE_ID = "test.i";
	private static final String DATUM_STREAM_SERVICE_ID = "test.ds";
	private static final String CONTROL_SERVICE_ID = "test.c";

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamMappingConfigurationDao mappingDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudControlConfigurationDao controlDao;
	private UserCloudIntegrationsBiz biz;

	private UserLongCompositePK aStream;
	private UserLongCompositePK aPolicyStream;
	private UserLongCompositePK bStream;
	private UserLongCompositePK aControl;
	private UserLongCompositePK aPolicyControl;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);

		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		mappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		controlDao = new JdbcCloudControlConfigurationDao(jdbcTemplate);

		final Long aIntegrationId = integration(a);
		final Long aMappingId = mapping(a, aIntegrationId);
		aStream = datumStream(a, aMappingId, a.privateNodeId());
		aPolicyStream = datumStream(a, aMappingId, a.otherPrivateNodeId());
		aControl = control(a, aIntegrationId, a.privateNodeId());
		aPolicyControl = control(a, aIntegrationId, a.otherPrivateNodeId());

		final Long bIntegrationId = integration(b);
		bStream = datumStream(b, mapping(b, bIntegrationId), b.privateNodeId());
		control(b, bIntegrationId, b.privateNodeId());

		final CloudDatumStreamService datumStreamService = mock(CloudDatumStreamService.class);
		given(datumStreamService.getId()).willReturn(DATUM_STREAM_SERVICE_ID);
		given(datumStreamService.getSettingUid()).willReturn(DATUM_STREAM_SERVICE_ID);
		given(datumStreamService.latestDatum(any())).willReturn(List.of());
		final CloudIntegrationService integrationService = mock(CloudIntegrationService.class);
		given(integrationService.getId()).willReturn(INTEGRATION_SERVICE_ID);
		given(integrationService.getSettingUid()).willReturn(INTEGRATION_SERVICE_ID);
		given(integrationService.datumStreamServices()).willReturn(List.of(datumStreamService));
		given(integrationService.controlServices()).willReturn(List.of());

		biz = securedProxy(
				(UserCloudIntegrationsBiz) new DaoUserCloudIntegrationsBiz(Clock.systemUTC(),
						mock(UserSettingsEntityDao.class), integrationDao, datumStreamDao,
						mock(CloudDatumStreamSettingsEntityDao.class), mappingDao,
						mock(CloudDatumStreamPropertyConfigurationDao.class), controlDao,
						mock(CloudDatumStreamPollTaskDao.class), mock(CloudDatumStreamRakeTaskDao.class),
						mock(ClientAccessTokenDao.class), mock(TextEncryptor.class),
						List.of(integrationService)),
				new UserCloudIntegrationsSecurityAspect(new JdbcSolarNodeOwnershipDao(jdbcTemplate),
						datumStreamDao))
				.proxy();
	}

	private Long integration(TestTenant tenant) {
		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(
				unassignedEntityIdKey(tenant.userId()), Instant.now(), "Test", INTEGRATION_SERVICE_ID);
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		return integrationDao.save(conf).getEntityId();
	}

	private Long mapping(TestTenant tenant, Long integrationId) {
		final CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(
				unassignedEntityIdKey(tenant.userId()), Instant.now(), "Test", integrationId);
		conf.setModified(conf.getCreated());
		return mappingDao.save(conf).getEntityId();
	}

	private UserLongCompositePK datumStream(TestTenant tenant, Long mappingId, Long nodeId) {
		final CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(
				unassignedEntityIdKey(tenant.userId()), Instant.now(), "Test", DATUM_STREAM_SERVICE_ID,
				ObjectDatumKind.Node);
		conf.setModified(conf.getCreated());
		conf.setDatumStreamMappingId(mappingId);
		conf.setObjectId(nodeId);
		conf.setSourceId(tenant.sourceIds().getFirst());
		conf.setEnabled(true);
		return datumStreamDao.save(conf);
	}

	private UserLongCompositePK control(TestTenant tenant, Long integrationId, Long nodeId) {
		final CloudControlConfiguration conf = new CloudControlConfiguration(
				unassignedEntityIdKey(tenant.userId()), Instant.now(), "Test", CONTROL_SERVICE_ID,
				integrationId, nodeId, "/control/1");
		conf.setModified(conf.getCreated());
		conf.setControlReference("ref");
		conf.setEnabled(true);
		return controlDao.save(conf);
	}

	private static BasicFilter nodes(Long... nodeIds) {
		final BasicFilter filter = new BasicFilter();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static Set<UserLongCompositePK> ids(
			Iterable<? extends Entity<UserLongCompositePK>> results) {
		return StreamSupport.stream(results.spliterator(), false).map(Entity::getId)
				.collect(toSet());
	}

	@Test
	public void listDatumStreams_user_ownStreamsOnly() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(), null,
				CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the user's datum streams returned")
			.containsExactlyInAnyOrder(aStream, aPolicyStream)
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_user_otherUserNode_notFound() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(), nodes(b.privateNodeId()),
				CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Other user's datum streams not found")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_restrictedToken_policyNodeOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(), new BasicFilter(),
				CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the policy node datum stream returned")
			.containsExactly(aPolicyStream)
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_restrictedToken_someNonPolicyNodes_narrowed() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(),
				nodes(a.otherPrivateNodeId(), a.privateNodeId()), CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Non-policy node removed from the query")
			.containsExactly(aPolicyStream)
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_restrictedToken_repeatedPolicyNode_allowed() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(),
				nodes(a.otherPrivateNodeId(), a.otherPrivateNodeId()),
				CloudDatumStreamConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Repeated node ID does not prevent the policy node datum stream from being returned")
			.containsExactly(aPolicyStream)
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_restrictedToken_repeatedNonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Repeated node ID outside the policy is still denied")
			.isThrownBy(() -> biz.listConfigurationsForUser(a.userId(),
					nodes(a.privateNodeId(), a.privateNodeId()),
					CloudDatumStreamConfiguration.class))
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreams_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Listing the datum streams of a non-policy node denied")
			.isThrownBy(() -> biz.listConfigurationsForUser(a.userId(), nodes(a.privateNodeId()),
					CloudDatumStreamConfiguration.class))
			;
		// @formatter:on
	}

	@Test
	public void listControls_restrictedToken_policyNodeOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.listConfigurationsForUser(a.userId(), new BasicFilter(),
				CloudControlConfiguration.class);

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the policy node control returned")
			.containsExactly(aPolicyControl)
			;
		// @formatter:on
	}

	@Test
	public void listControls_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Listing the controls of a non-policy node denied")
			.isThrownBy(() -> biz.listConfigurationsForUser(a.userId(), nodes(a.privateNodeId()),
					CloudControlConfiguration.class))
			;
		// @formatter:on
	}

	@Test
	public void datumStreamForId_otherUserEntity_notFound() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Another user's datum stream is not found under the user's ID")
			.isThrownBy(() -> biz.configurationForId(
					new UserLongCompositePK(a.userId(), bStream.getEntityId()),
					CloudDatumStreamConfiguration.class))
			.returns(AuthorizationException.Reason.UNKNOWN_OBJECT, AuthorizationException::getReason)
			;
		// @formatter:on
	}

	@Test
	public void datumStreamForId_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Viewing a non-policy node datum stream denied")
			.isThrownBy(() -> biz.configurationForId(aStream, CloudDatumStreamConfiguration.class))
			;
		// @formatter:on
	}

	@Test
	public void controlForId_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Viewing a non-policy node control denied")
			.isThrownBy(() -> biz.configurationForId(aControl, CloudControlConfiguration.class))
			;
		// @formatter:on
	}

	@Test
	public void deleteDatumStream_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Deleting a non-policy node datum stream denied")
			.isThrownBy(() -> biz.deleteConfiguration(aStream, CloudDatumStreamConfiguration.class))
			;
		then(datumStreamDao.get(aStream))
			.as("Datum stream not deleted")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void deleteControl_restrictedToken_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Deleting a control, which is related to an integration, requires no node policy")
			.isThrownBy(() -> biz.deleteConfiguration(aPolicyControl, CloudControlConfiguration.class))
			;
		then(controlDao.get(aPolicyControl))
			.as("Control not deleted")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void updateDatumStreamEnabled_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Disabling a non-policy node datum stream denied")
			.isThrownBy(() -> biz.updateConfigurationEnabled(aStream, false,
					CloudDatumStreamConfiguration.class))
			;
		then(datumStreamDao.get(aStream))
			.as("Datum stream still enabled")
			.isNotNull()
			.returns(true, CloudDatumStreamConfiguration::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void latestDatumStreamDatum_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Reading the latest datum of a non-policy node datum stream denied")
			.isThrownBy(() -> biz.latestDatumStreamDatumForId(aStream))
			;
		// @formatter:on
	}

	@Test
	public void listDatumStreamDatum_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Reading the datum of a non-policy node datum stream denied")
			.isThrownBy(() -> biz.listDatumStreamDatum(aStream, new BasicQueryFilter()))
			;
		// @formatter:on
	}

}
