/* ==================================================================
 * DaoFlexibilityProviderBiz.java - 16/08/2022 5:25:42 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.biz.dao;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.oscp.dao.BasicConfigurationFilter.filterForUsers;
import static net.solarnetwork.central.oscp.domain.DatumPublishEvent.FORECAST_TYPE_PARAM;
import static net.solarnetwork.central.oscp.domain.OscpRole.CapacityOptimizer;
import static net.solarnetwork.central.oscp.domain.OscpRole.CapacityProvider;
import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.CORRELATION_ID_HEADER;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REQUEST_ID_HEADER;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_ACK_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.OwnedGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupSettingsDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.DatumPublishEvent;
import net.solarnetwork.central.oscp.domain.DatumPublishSettings;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.domain.TimeBlockAmount;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.util.DeferredSystemTask;
import net.solarnetwork.central.oscp.web.OscpWebUtils;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumSamples;
import oscp.v20.AdjustGroupCapacityForecast;
import oscp.v20.GroupCapacityComplianceError;
import oscp.v20.HandshakeAcknowledge;
import oscp.v20.Heartbeat;
import oscp.v20.Register;
import oscp.v20.UpdateGroupCapacityForecast;
import oscp.v20.VersionUrl;

/**
 * DAO based implementation of {@link FlexibilityProviderBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoFlexibilityProviderBiz implements FlexibilityProviderBiz {

	private static final Logger log = LoggerFactory.getLogger(DaoFlexibilityProviderBiz.class);

	private final Executor executor;
	private final ExternalSystemClient externalSystemClient;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final FlexibilityProviderDao flexibilityProviderDao;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final CapacityGroupSettingsDao capacityGroupSettingsDao;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private Map<String, String> versionUrlMap = defaultVersionUrlMap();
	private TransactionTemplate txTemplate;
	private TaskScheduler taskScheduler;
	private DatumEntityDao datumDao;
	private Consumer<DatumPublishEvent> fluxPublisher;
	private String sourceIdTemplate = UserSettings.DEFAULT_SOURCE_ID_TEMPLATE;
	private long taskConditionTimeout = DeferredSystemTask.DEFAULT_CONDITION_TIMEOUT;
	private long taskStartDelay = DeferredSystemTask.DEFAULT_START_DELAY;
	private long taskStartDelayRandomness = DeferredSystemTask.DEFAULT_START_DELAY_RANDOMNESS;
	private long taskRetryDelay = DeferredSystemTask.DEFAULT_RETRY_DELAY;

	/**
	 * The default version URL map supported by this service.
	 * 
	 * @return the map
	 */
	public static Map<String, String> defaultVersionUrlMap() {
		return Collections.singletonMap(V20,
				"https://oscp.solarnetwork.net" + FLEXIBILITY_PROVIDER_V20_URL_PATH);
	}

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        the executor to use
	 * @param externalSystemClient
	 *        the external system service
	 * @param userEventAppenderBiz
	 *        the user event appender
	 * @param flexibilityProviderDao
	 *        the flexibility provider DAO
	 * @param capacityProviderDao
	 *        the capacity provider configuration DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer configuration DAO
	 * @param capacityGroupDao
	 *        the capacity group configuration DAO
	 * @param userSettingsDao
	 *        the user settings DAO
	 * @param capacityGroupSettingsDao
	 *        the group settings DAo
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @capacityGroupSettingsDao the capacity group settings DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoFlexibilityProviderBiz(Executor executor, ExternalSystemClient externalSystemClient,
			UserEventAppenderBiz userEventAppenderBiz, FlexibilityProviderDao flexibilityProviderDao,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityGroupConfigurationDao capacityGroupDao,
			CapacityGroupSettingsDao capacityGroupSettingsDao, SolarNodeOwnershipDao nodeOwnershipDao) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.externalSystemClient = requireNonNullArgument(externalSystemClient, "externalSystemClient");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.capacityGroupSettingsDao = requireNonNullArgument(capacityGroupSettingsDao,
				"capacityGroupSettingsDao");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
	}

	private ExternalSystemConfigurationDao<?> configurationDaoForRole(OscpRole systemRole) {
		ExternalSystemConfigurationDao<?> dao;
		if ( systemRole == CapacityProvider ) {
			dao = capacityProviderDao;
		} else if ( systemRole == CapacityOptimizer ) {
			dao = capacityOptimizerDao;
		} else {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		return dao;
	}

	private OscpRole verifyRole(AuthRoleInfo authInfo, Set<OscpRole> allowedRoles) {
		if ( authInfo == null || authInfo.id() == null || authInfo.role() == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		OscpRole role = authInfo.role();
		if ( !allowedRoles.contains(role) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		return role;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void register(AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			Future<?> externalSystemReady) throws AuthorizationException {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider, CapacityOptimizer));
		requireNonNullArgument(externalSystemToken, "externalSystemToken");
		requireNonNullArgument(versionUrl, "versionUrl");

		log.info("Register for {} {} with version URL: {}", systemRole, authInfo.id().ident(),
				versionUrl);

		ExternalSystemConfigurationDao<?> dao = configurationDaoForRole(systemRole);
		BaseOscpExternalSystemConfiguration<?> conf = handleRegistration(authInfo, externalSystemToken,
				versionUrl, dao);

		// generate new FP token for the system to use, and return it
		var fpId = new UserLongCompositePK(authInfo.userId(), conf.getFlexibilityProviderId());
		String newToken = flexibilityProviderDao.createAuthToken(fpId);

		var task = new RegisterExternalSystemTask<>(externalSystemReady, systemRole, conf.getId(), dao,
				newToken);
		executor.execute(task);
	}

	private <C extends BaseOscpExternalSystemConfiguration<C>> C handleRegistration(
			AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			ExternalSystemConfigurationDao<C> dao) {
		BasicConfigurationFilter filter = filterForUsers(authInfo.userId());
		filter.setConfigurationId(authInfo.entityId());
		var filterResults = dao.findFiltered(filter);
		if ( filterResults.getReturnedResultCount() < 1 ) {
			// TODO UserEvent (in exception handler method)
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, authInfo);
		}
		C conf = stream(filterResults.spliterator(), false).findFirst().orElse(null);
		conf.setOscpVersion(versionUrl.getKey());
		conf.setBaseUrl(versionUrl.getValue());
		if ( conf.getRegistrationStatus() != RegistrationStatus.Pending ) {
			conf.setRegistrationStatus(RegistrationStatus.Pending);
		}

		userEventAppenderBiz.addEvent(authInfo.userId(), event(CAPACITY_PROVIDER_REGISTER_TAGS, null,
				getJSONString(Map.of(CONFIG_ID_DATA_KEY, conf.getEntityId(),
						REGISTRATION_STATUS_DATA_KEY, (char) conf.getRegistrationStatus().getCode(),
						VERSION_DATA_KEY, versionUrl.getKey(), URL_DATA_KEY, versionUrl.getValue()),
						null)));

		dao.save(conf);
		dao.saveExternalSystemAuthToken(conf.getId(), externalSystemToken);
		return conf;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void handshake(AuthRoleInfo authInfo, SystemSettings settings, String requestIdentifier,
			Future<?> externalSystemReady) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider, CapacityOptimizer));
		requireNonNullArgument(settings, "settings");
		requireNonNullArgument(requestIdentifier, "requestIdentifier");

		log.info("Handshake for {} {} with settings: {}", systemRole, authInfo.id().ident(), settings);

		ExternalSystemConfigurationDao<?> dao = configurationDaoForRole(systemRole);
		BasicConfigurationFilter filter = filterForUsers(authInfo.userId());
		filter.setConfigurationId(authInfo.entityId());
		filter.setLockResults(true);
		var filterResults = dao.findFiltered(filter);
		if ( filterResults.getReturnedResultCount() < 1 ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, authInfo);
		}
		BaseOscpExternalSystemConfiguration<?> conf = stream(filterResults.spliterator(), false)
				.findFirst().orElse(null);
		dao.saveSettings(conf.getId(), settings);

		SystemSettings ackSettings = new SystemSettings(null, null);
		var task = new HandshakeAckTask<>(externalSystemReady, systemRole, conf.getId(), dao,
				ackSettings).withParameters(singletonMap(CORRELATION_ID_HEADER, requestIdentifier));
		executor.execute(task);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void handshakeAcknowledge(AuthRoleInfo authInfo, SystemSettings settings,
			String requestIdentifier) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider, CapacityOptimizer));
		requireNonNullArgument(settings, "settings");

		log.info("Handshake acknowledge for {} {} with settings: {}", systemRole, authInfo.id().ident(),
				settings);

		ExternalSystemConfigurationDao<?> dao = configurationDaoForRole(systemRole);
		BasicConfigurationFilter filter = filterForUsers(authInfo.userId());
		filter.setConfigurationId(authInfo.entityId());
		filter.setLockResults(true);
		var filterResults = dao.findFiltered(filter);
		if ( filterResults.getReturnedResultCount() < 1 ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, authInfo);
		}
		BaseOscpExternalSystemConfiguration<?> conf = stream(filterResults.spliterator(), false)
				.findFirst().orElse(null);
		dao.saveSettings(conf.getId(), settings);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void heartbeat(AuthRoleInfo authInfo, Instant expiresDate) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider, CapacityOptimizer));
		requireNonNullArgument(expiresDate, "expiresDate");

		log.info("Heartbeat for {} {} with expiration: {}", systemRole, authInfo.id().ident(),
				expiresDate);

		ExternalSystemConfigurationDao<?> dao = configurationDaoForRole(systemRole);
		dao.updateOfflineDate(authInfo.id(), expiresDate);

		Collection<CapacityGroupConfiguration> groups = (systemRole == OscpRole.CapacityProvider
				? capacityGroupDao.findAllForCapacityProvider(authInfo.userId(), authInfo.entityId())
				: capacityGroupDao.findAllForCapacityOptimizer(authInfo.userId(), authInfo.entityId()));

		for ( CapacityGroupConfiguration group : groups ) {
			var task = new HeartbeatTask<>(authInfo, expiresDate, dao, group);
			executor.execute(task);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateGroupCapacityForecast(AuthRoleInfo authInfo, String groupIdentifier,
			String forecastIdentifier, CapacityForecast forecast) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider));
		requireNonNullArgument(groupIdentifier, "groupIdentifier");
		requireNonNullArgument(forecastIdentifier, "forecastIdentifier");
		requireNonNullArgument(forecast, "forecast");

		log.info("Update Group Capacity Forecast for {} {} to group [{}] with forecast: {}", systemRole,
				authInfo.id().ident(), groupIdentifier, forecast);

		CapacityGroupConfiguration group = capacityGroupDao.findForCapacityProvider(authInfo.userId(),
				authInfo.entityId(), groupIdentifier);
		if ( group == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, authInfo);
		}

		if ( group.getCapacityOptimizerId() == null ) {
			return;
		}

		var task = new UpdateGroupCapacityForecastTask(completedFuture(null), systemRole, group,
				forecast, forecastIdentifier);
		executor.execute(task);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void adjustGroupCapacityForecast(AuthRoleInfo authInfo, String groupIdentifier,
			String requestIdentifier, CapacityForecast forecast) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityOptimizer));
		requireNonNullArgument(groupIdentifier, "groupIdentifier");
		requireNonNullArgument(requestIdentifier, "requestIdentifier");
		requireNonNullArgument(forecast, "forecast");

		log.info("Adjust Group Capacity Forecast for {} {} to group [{}] with forecast: {}", systemRole,
				authInfo.id().ident(), groupIdentifier, forecast);

		CapacityGroupConfiguration group = capacityGroupDao.findForCapacityOptimizer(authInfo.userId(),
				authInfo.entityId(), groupIdentifier);
		if ( group == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED,
					Map.of("auth", authInfo.asIdentifier(), "group", groupIdentifier));
		}

		if ( group.getCapacityProviderId() == null ) {
			return;
		}

		var task = new AdjustGroupCapacityForecastTask(completedFuture(null), systemRole,
				new UserLongCompositePK(group.getUserId(), group.getCapacityProviderId()),
				group.getIdentifier(), forecast)
						.withParameters(singletonMap(REQUEST_ID_HEADER, requestIdentifier));
		executor.execute(task);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void groupCapacityComplianceError(AuthRoleInfo authInfo, String groupIdentifier,
			String forecastIdentifier, String message, List<TimeBlockAmount> blocks) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityOptimizer));
		requireNonNullArgument(groupIdentifier, "groupIdentifier");

		log.info(
				"Group Capacity Compliance Error for {} {} to group [{}] with message [{}] and blocks: {}",
				systemRole, authInfo.id().ident(), groupIdentifier, message, blocks);

		CapacityGroupConfiguration group = capacityGroupDao.findForCapacityOptimizer(authInfo.userId(),
				authInfo.entityId(), groupIdentifier);
		if ( group == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, authInfo);
		}

		if ( group.getCapacityProviderId() == null ) {
			return;
		}

		var task = new GroupCapacityComplianceErrorTask(CompletableFuture.completedFuture(null),
				systemRole, new UserLongCompositePK(group.getUserId(), group.getCapacityProviderId()),
				group.getIdentifier(), message, blocks);
		executor.execute(task);
	}

	private void publishDatum(String action, String sourceIdSuffix, OscpRole role, Long userId,
			BaseOscpExternalSystemConfiguration<?> src, BaseOscpExternalSystemConfiguration<?> dest,
			CapacityGroupConfiguration group, DatumPublishSettings settings,
			Supplier<Collection<OwnedGeneralNodeDatum>> datumSupplier,
			KeyValuePair... sourceIdParameters) {
		if ( !DatumPublishSettings.shouldPublish(settings)
				|| (fluxPublisher == null && datumDao == null) ) {
			return;
		}
		Long nodeId = settings.getNodeId();
		SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(nodeId);
		if ( ownership == null || !userId.equals(ownership.getUserId()) ) {
			// node ownership not known or not owned by context user: ignore
			return;
		}
		final Collection<OwnedGeneralNodeDatum> datum = datumSupplier.get();
		if ( datum == null || datum.isEmpty() ) {
			return;
		}
		DatumPublishEvent event = new DatumPublishEvent(role, action, src, dest, group, settings, datum,
				sourceIdParameters);
		String sourceId = event.sourceId();
		if ( sourceIdSuffix != null ) {
			sourceId += sourceIdSuffix;
		}
		for ( OwnedGeneralNodeDatum d : datum ) {
			d.setNodeId(nodeId);
			d.setSourceId(sourceId);
		}
		if ( datumDao != null && settings.isPublishToSolarIn() ) {
			for ( OwnedGeneralNodeDatum d : datum ) {
				datumDao.store(d);
			}
		}
		if ( fluxPublisher != null && settings.isPublishToSolarFlux() ) {
			fluxPublisher.accept(event);
		}
	}

	private abstract class BaseDeferredSystemTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends DeferredSystemTask<C> {

		private BaseDeferredSystemTask(String name, Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao) {
			super(name, externalSystemReady, role, configId, dao, externalSystemClient,
					DaoFlexibilityProviderBiz.this.userEventAppenderBiz,
					DaoFlexibilityProviderBiz.this.executor,
					DaoFlexibilityProviderBiz.this.taskScheduler,
					DaoFlexibilityProviderBiz.this.txTemplate);
			withConditionTimeout(taskConditionTimeout);
			withStartDelay(taskStartDelay);
			withStartDelayRandomness(taskStartDelayRandomness);
			withRetryDelay(taskRetryDelay);
		}

		/**
		 * Publish datum.
		 * 
		 * @param action
		 *        the OSCP action name
		 * @param sourceIdSuffix
		 *        an optional suffix to add to the resolved source ID
		 * @param src
		 *        the source system
		 * @param dest
		 *        the destination system
		 * @param group
		 *        the group
		 * @param settings
		 *        the publish settings
		 * @param datumSupplier
		 *        the datum supplier
		 * @param sourceIdParameters
		 *        additional source ID template parameters
		 */
		protected void publish(String action, String sourceIdSuffix,
				BaseOscpExternalSystemConfiguration<?> src, BaseOscpExternalSystemConfiguration<?> dest,
				CapacityGroupConfiguration group, DatumPublishSettings settings,
				Supplier<Collection<OwnedGeneralNodeDatum>> datumSupplier,
				KeyValuePair... sourceIdParameters) {
			DaoFlexibilityProviderBiz.this.publishDatum(action, sourceIdSuffix, role,
					configId.getUserId(), src, dest, group, settings, datumSupplier, sourceIdParameters);
		}
	}

	private class HeartbeatTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends BaseDeferredSystemTask<C> implements Supplier<Collection<OwnedGeneralNodeDatum>> {

		private final Instant expires;
		private final CapacityGroupConfiguration group;
		private final Instant ts = Instant.now();

		private HeartbeatTask(AuthRoleInfo authInfo, Instant expiresDate,
				ExternalSystemConfigurationDao<C> dao, CapacityGroupConfiguration group) {
			super("Heartbeat", completedFuture(null), authInfo.role(), authInfo.id(), dao);
			this.expires = requireNonNullArgument(expiresDate, "expiresDate");
			this.group = group;
			if ( role == OscpRole.CapacityProvider ) {
				withErrorEventTags(CAPACITY_PROVIDER_HEARTBEAT_ERROR_TAGS);
				withErrorEventTags(CAPACITY_PROVIDER_HEARTBEAT_TAGS);
			} else {
				withErrorEventTags(CAPACITY_OPTIMIZER_HEARTBEAT_ERROR_TAGS);
				withErrorEventTags(CAPACITY_OPTIMIZER_HEARTBEAT_TAGS);
			}
		}

		@Override
		public void doWork() {
			DatumPublishSettings settings = capacityGroupSettingsDao
					.resolveDatumPublishSettings(configId.getUserId(), group.getIdentifier());
			if ( settings == null ) {
				return;
			}
			CapacityProviderConfiguration provider = null;
			CapacityOptimizerConfiguration optimizer = null;
			BaseOscpExternalSystemConfiguration<C> src = configuration(false);
			if ( src instanceof CapacityProviderConfiguration c ) {
				provider = c;
			} else if ( src instanceof CapacityOptimizerConfiguration c ) {
				optimizer = c;
			}
			publish(Heartbeat.class.getSimpleName(), null, provider, optimizer, group, settings, this);
		}

		@Override
		public Collection<OwnedGeneralNodeDatum> get() {
			// we don't need to set node ID or source ID here, as that will be resolved in the publish() method
			OwnedGeneralNodeDatum d = new OwnedGeneralNodeDatum(configId.getUserId());
			d.setCreated(ts);

			DatumSamples s = new DatumSamples();
			s.putStatusSampleValue("expires", ISO_DATE_TIME_ALT_UTC.format(expires));
			d.setSamples(s);

			return Collections.singleton(d);
		}

	}

	private class GroupCapacityComplianceErrorTask
			extends BaseDeferredSystemTask<CapacityProviderConfiguration> {

		private final String message;
		private final List<TimeBlockAmount> blocks;

		private GroupCapacityComplianceErrorTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, String forecastIdentifier, String message,
				List<TimeBlockAmount> blocks) {
			super("GroupCapacityComplianceError", externalSystemReady, role, configId,
					capacityProviderDao);
			this.message = requireNonNullArgument(message, "message");
			this.blocks = blocks;
			withErrorEventTags(CAPACITY_OPTIMIZER_GROUP_CAPACITY_COMPLIANCE_TAGS_ERROR_TAGS);
			withSuccessEventTags(CAPACITY_OPTIMIZER_GROUP_CAPACITY_COMPLIANCE_TAGS);
			withParameters(Collections.singletonMap(OscpWebUtils.CORRELATION_ID_HEADER,
					requireNonNullArgument(forecastIdentifier, "forecastIdentifier")));
		}

		@Override
		protected void doWork() throws Exception {
			CapacityProviderConfiguration config = registeredConfiguration(false, singleton(V20));
			doWork20(config);
		}

		private void doWork20(CapacityProviderConfiguration conf) {
			GroupCapacityComplianceError msg = new GroupCapacityComplianceError(message);
			if ( blocks != null ) {
				msg.setForecastedBlocks(
						blocks.stream().map(TimeBlockAmount::toOscp20ForecastValue).toList());
			}
			post(GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH, msg);
		}

	}

	private class AdjustGroupCapacityForecastTask
			extends BaseDeferredSystemTask<CapacityProviderConfiguration> {

		private final String groupIdentifier;
		private final CapacityForecast forecast;

		private AdjustGroupCapacityForecastTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, String groupIdentifier, CapacityForecast forecast) {
			super("AdjustGroupCapacityForecast", externalSystemReady, role, configId,
					capacityProviderDao);
			this.groupIdentifier = requireNonNullArgument(groupIdentifier, "groupIdentifier");
			this.forecast = requireNonNullArgument(forecast, "forecast");
			withErrorEventTags(CAPACITY_OPTIMIZER_ADJUST_GROUP_CAPACITY_FORECAST_ERROR_TAGS);
			withSuccessEventTags(CAPACITY_OPTIMIZER_ADJUST_GROUP_CAPACITY_FORECAST_TAGS);
		}

		@Override
		protected void doWork() throws Exception {
			CapacityProviderConfiguration config = registeredConfiguration(false, singleton(V20));
			doWork20(config);
		}

		private void doWork20(CapacityProviderConfiguration conf) {
			AdjustGroupCapacityForecast msg = forecast.toOscp20AdjustGroupCapacityValue(groupIdentifier);
			post(ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH, msg);
		}

	}

	private class UpdateGroupCapacityForecastTask
			extends BaseDeferredSystemTask<CapacityOptimizerConfiguration>
			implements Supplier<Collection<OwnedGeneralNodeDatum>> {

		private final CapacityGroupConfiguration group;
		private final CapacityForecast forecast;
		private final String forecastIdentifier;

		private UpdateGroupCapacityForecastTask(Future<?> externalSystemReady, OscpRole role,
				CapacityGroupConfiguration group, CapacityForecast forecast, String forecastIdentifier) {
			super("UpdateGroupCapacityForecast", externalSystemReady, role,
					new UserLongCompositePK(group.getUserId(), group.getCapacityOptimizerId()),
					capacityOptimizerDao);
			this.group = group;
			this.forecast = requireNonNullArgument(forecast, "forecast");
			this.forecastIdentifier = requireNonNullArgument(forecastIdentifier, "forecastIdentifier");
			requireNonNullArgument(forecast.type(), "forecast.type");
			withErrorEventTags(CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_ERROR_TAGS);
			withSuccessEventTags(CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_TAGS);
			withParameters(singletonMap(REQUEST_ID_HEADER, forecastIdentifier));
		}

		@Override
		protected void doWork() throws Exception {
			CapacityOptimizerConfiguration optimizer = registeredConfiguration(false, singleton(V20));
			doWork20(optimizer);

			CapacityProviderConfiguration provider = capacityProviderDao
					.get(new UserLongCompositePK(optimizer.getUserId(), group.getCapacityProviderId()));
			if ( provider == null ) {
				// should not get here
				return;
			}
			DatumPublishSettings settings = capacityGroupSettingsDao
					.resolveDatumPublishSettings(optimizer.getUserId(), group.getIdentifier());
			String forecastTypeAlias = forecast.type().getAlias();
			String sourceIdSuffix = "/".concat(forecastTypeAlias);
			publish(UpdateGroupCapacityForecast.class.getSimpleName(), sourceIdSuffix, provider,
					optimizer, group, settings, this,
					new KeyValuePair(FORECAST_TYPE_PARAM, forecastTypeAlias));
		}

		private void doWork20(CapacityOptimizerConfiguration conf) {
			UpdateGroupCapacityForecast msg = forecast
					.toOscp20UpdateGroupCapacityValue(group.getIdentifier());
			post(UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH, msg);
		}

		@Override
		public Collection<OwnedGeneralNodeDatum> get() {
			// we don't need to set node ID or source ID here, as that will be resolved in the publish() method
			Map<Instant, OwnedGeneralNodeDatum> data = new HashMap<>();
			if ( forecast.blocks() != null && !forecast.blocks().isEmpty() ) {
				for ( TimeBlockAmount tba : forecast.blocks() ) {
					Instant start = tba.start();
					if ( start == null ) {
						continue;
					}
					Instant end = tba.end();
					if ( end == null ) {
						continue;
					}
					OwnedGeneralNodeDatum d = data.computeIfAbsent(start, k -> {
						OwnedGeneralNodeDatum newD = new OwnedGeneralNodeDatum(configId.getUserId());
						newD.setCreated(k);
						DatumSamples s = new DatumSamples();
						newD.setSamples(s);
						s.putInstantaneousSampleValue("duration", Duration.between(k, end).toSeconds());
						s.putStatusSampleValue("forecastIdentifier", forecastIdentifier);
						return newD;
					});
					BigDecimal amount = tba.amount();
					if ( amount == null ) {
						continue;
					}
					DatumSamples s = d.getSamples();
					Phase phase = tba.phase();
					s.putInstantaneousSampleValue(propName("amount", phase), tba.amount());
					s.putStatusSampleValue(propName("unit", phase), tba.unit().toString());
				}
			}
			return data.values().stream().filter(d -> d.getSamples().getInstantaneous().size() > 1)
					.sorted(Comparator.comparing(GeneralNodeDatum::getCreated)).toList();
		}

	}

	private static String propName(String base, Phase phase) {
		if ( phase == null || phase == Phase.All ) {
			return base;
		}
		return "%s_%c".formatted(base, (char) phase.getCode());
	}

	private class HandshakeAckTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends BaseDeferredSystemTask<C> {

		private final SystemSettings settings;

		private HandshakeAckTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao,
				SystemSettings settings) {
			super("HandshakeAcknowledge", externalSystemReady, role, configId, dao);
			this.settings = requireNonNullArgument(settings, "settings");
			withErrorEventTags(CAPACITY_PROVIDER_HANDSHAKE_ERROR_TAGS);
			withSuccessEventTags(CAPACITY_PROVIDER_HANDSHAKE_TAGS);
		}

		@Override
		protected void doWork() {
			C config = registeredConfiguration(false, singleton(V20));
			doWork20(config);
		}

		private void doWork20(C conf) {
			HandshakeAcknowledge ack = new HandshakeAcknowledge();
			if ( settings != null ) {
				ack.setRequiredBehaviour(settings.toOscp20Value());
			}
			post(HANDSHAKE_ACK_URL_PATH, ack);
		}

	}

	private class RegisterExternalSystemTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends BaseDeferredSystemTask<C> {

		private final String token;

		private RegisterExternalSystemTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao, String token) {
			super("Register", externalSystemReady, role, configId, dao);
			this.token = requireNonNullArgument(token, "token");
			withErrorEventTags(CAPACITY_PROVIDER_REGISTER_ERROR_TAGS);
			withSuccessEventTags(CAPACITY_PROVIDER_REGISTER_TAGS);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void doWork() {
			try {
				C config = registeredConfiguration(true, EnumSet.of(RegistrationStatus.Pending),
						singleton(V20));
				doWork20(config);
			} catch ( ExternalSystemConfigurationException confEx ) {
				if ( confEx.getConfig().getRegistrationStatus() != RegistrationStatus.Pending ) {
					log.info(
							"Unable to register with {} {} because the registration status is not Pending.",
							role, configId.ident());
				} else {
					confEx.getConfig().setRegistrationStatus(RegistrationStatus.Failed);
					dao.save((C) confEx.getConfig());
					throw confEx;
				}
			}
		}

		private void doWork20(C conf) {
			String url = versionUrlMap.get(V20);
			if ( url == null ) {
				var msg = "[%s] task with %s %s failed because the Flexibility Provider URL for version %s is not configured."
						.formatted(name, role, configId.ident(), V20);
				LogEventInfo event = eventForConfiguration(conf, CAPACITY_PROVIDER_REGISTER_ERROR_TAGS,
						"Flexibility Provider URL for version not configured");
				throw new ExternalSystemConfigurationException(role, conf, event, msg);
			}
			List<VersionUrl> versions = Collections.singletonList(new VersionUrl(V20, url));
			Register register = new Register(token, versions);
			post(REGISTER_URL_PATH, register);

			conf.setRegistrationStatus(RegistrationStatus.Registered);
			dao.save(conf);
		}

	}

	/**
	 * Get the transaction template to use.
	 * 
	 * @return the txTemplate
	 */
	public TransactionTemplate getTxTemplate() {
		return txTemplate;
	}

	/**
	 * Set the transaction template to use.
	 * 
	 * @param txTemplate
	 *        the txTemplate to set
	 */
	public void setTxTemplate(TransactionTemplate txTemplate) {
		this.txTemplate = txTemplate;
	}

	/**
	 * Get the supported version URLs to the Flexibility Provider service.
	 * 
	 * @return the version to URL mapping
	 */
	public Map<String, String> getVersionUrlMap() {
		return versionUrlMap;
	}

	/**
	 * Set the supported version URLs to the Flexibility Provider service.
	 * 
	 * @param versionUrlMap
	 *        the URL mapping to set
	 * @throws IllegalArgumentException
	 *         if the argument is {@literal null}
	 */
	public void setVersionUrlMap(Map<String, String> versionUrlMap) {
		this.versionUrlMap = requireNonNullArgument(versionUrlMap, "versionUrlMap");
	}

	/**
	 * Get the task scheduler.
	 * 
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 * 
	 * <p>
	 * If configured, a delay will be added between retry operations.
	 * </p>
	 * 
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get the task start condition timeout.
	 * 
	 * @return the timeout, in milliseconds
	 */
	public long getTaskConditionTimeout() {
		return taskConditionTimeout;
	}

	/**
	 * Set the task start condition timeout.
	 * 
	 * @param taskConditionTimeout
	 *        the timeout to set, in milliseconds
	 */
	public void setTaskConditionTimeout(long taskConditionTimeout) {
		this.taskConditionTimeout = taskConditionTimeout;
	}

	/**
	 * Get the task start delay.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getTaskStartDelay() {
		return taskStartDelay;
	}

	/**
	 * Set the task start delay.
	 * 
	 * @param taskStartDelay
	 *        the delay to set, in milliseconds
	 */
	public void setTaskStartDelay(long taskStartDelay) {
		this.taskStartDelay = taskStartDelay;
	}

	/**
	 * Get the task start delay randomness.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getTaskStartDelayRandomness() {
		return taskStartDelayRandomness;
	}

	/**
	 * Set the task start delay randomness.
	 * 
	 * @param taskStartDelayRandomness
	 *        the delay to set, in milliseconds
	 */
	public void setTaskStartDelayRandomness(long taskStartDelayRandomness) {
		this.taskStartDelayRandomness = taskStartDelayRandomness;
	}

	/**
	 * Get the task retry delay.
	 * 
	 * @return the delay, in milliseconds
	 */
	public long getTaskRetryDelay() {
		return taskRetryDelay;
	}

	/**
	 * Set the task retry delay.
	 * 
	 * @param taskRetryDelay
	 *        the delay to set, in milliseconds
	 */
	public void setTaskRetryDelay(long taskRetryDelay) {
		this.taskRetryDelay = taskRetryDelay;
	}

	/**
	 * Get the datum DAO.
	 * 
	 * @return the DAO
	 */
	public DatumEntityDao getDatumDao() {
		return datumDao;
	}

	/**
	 * Set the datum DAO.
	 * 
	 * @param datumDao
	 *        the DAO to set
	 */
	public void setDatumDao(DatumEntityDao datumDao) {
		this.datumDao = datumDao;
	}

	/**
	 * Get the SolarFlux publisher.
	 * 
	 * @return the publisher, or {@literal null}
	 */
	public Consumer<DatumPublishEvent> getFluxPublisher() {
		return fluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 * 
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public void setFluxPublisher(Consumer<DatumPublishEvent> fluxPublisher) {
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @return the template; defaults to
	 *         {@link UserSettings#DEFAULT_SOURCE_ID_TEMPLATE}
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * <p>
	 * This template string allows for these parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><code>{chargePointId}</code> - the Charge Point ID (number)</li>
	 * <li><code>{chargerIdentifier}</code> - the Charge Point info identifier
	 * (string)</li>
	 * <li><code>{connectorId}</code> - the connector ID (integer)</li>
	 * <li><code>{location}</code> - the location (string)</li>
	 * </ol>
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

}
