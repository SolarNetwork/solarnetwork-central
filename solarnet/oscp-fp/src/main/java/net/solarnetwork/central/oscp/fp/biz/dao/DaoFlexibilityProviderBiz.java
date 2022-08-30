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
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.oscp.dao.BasicConfigurationFilter.filterForUsers;
import static net.solarnetwork.central.oscp.domain.OscpRole.CapacityOptimizer;
import static net.solarnetwork.central.oscp.domain.OscpRole.CapacityProvider;
import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_ACK_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.util.DeferredSystemTask;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.KeyValuePair;
import oscp.v20.HandshakeAcknowledge;
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
	private Map<String, String> versionUrlMap = defaultVersionUrlMap();
	private TransactionTemplate txTemplate;
	private TaskScheduler taskScheduler;
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoFlexibilityProviderBiz(Executor executor, ExternalSystemClient externalSystemClient,
			UserEventAppenderBiz userEventAppenderBiz, FlexibilityProviderDao flexibilityProviderDao,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityGroupConfigurationDao capacityGroupDao) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.externalSystemClient = requireNonNullArgument(externalSystemClient, "externalSystemClient");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
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
	public void handshake(AuthRoleInfo authInfo, SystemSettings settings,
			Future<?> externalSystemReady) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider, CapacityOptimizer));
		requireNonNullArgument(settings, "settings");

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
				ackSettings);
		executor.execute(task);
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
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateGroupCapacityForecast(AuthRoleInfo authInfo, String groupIdentifier,
			CapacityForecast forecast) {
		OscpRole systemRole = verifyRole(authInfo, EnumSet.of(CapacityProvider));
		requireNonNullArgument(groupIdentifier, "groupIdentifier");
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

		var task = new UpdateGroupCapacityForecastTask<>(CompletableFuture.completedFuture(null),
				systemRole, new UserLongCompositePK(group.getUserId(), group.getCapacityOptimizerId()),
				capacityOptimizerDao, group.getIdentifier(), forecast);
		executor.execute(task);
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

	}

	private class UpdateGroupCapacityForecastTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends BaseDeferredSystemTask<C> {

		private final String groupIdentifier;
		private final CapacityForecast forecast;

		private UpdateGroupCapacityForecastTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao,
				String groupIdentifier, CapacityForecast forecast) {
			super("UpdateGroupCapacityForecast", externalSystemReady, role, configId, dao);
			this.groupIdentifier = requireNonNullArgument(groupIdentifier, "groupIdentifier");
			this.forecast = requireNonNullArgument(forecast, "forecast");
			switch (role) {
				case CapacityProvider:
					withErrorEventTags(CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_ERROR_TAGS);
					withSuccessEventTags(CAPACITY_PROVIDER_UPDATE_GROUP_CAPACITY_FORECAST_TAGS);
					break;
				case CapacityOptimizer:
					withErrorEventTags(CAPACITY_OPTIMIZER_UPDATE_GROUP_CAPACITY_FORECAST_ERROR_TAGS);
					withSuccessEventTags(CAPACITY_OPTIMIZER_UPDATE_GROUP_CAPACITY_FORECAST_TAGS);
					break;
				default:
					throw new IllegalArgumentException("Role [%s] not supported.".formatted(role));
			}
		}

		@Override
		protected void doWork() throws Exception {
			C config = registeredConfiguration(false, singleton(V20));
			doWork20(config);
		}

		private void doWork20(C conf) {
			UpdateGroupCapacityForecast msg = forecast.toOscp20GroupCapacityValue(groupIdentifier);
			post(UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH, msg);
		}

	}

	private class HandshakeAckTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends BaseDeferredSystemTask<C> {

		private final SystemSettings settings;

		private HandshakeAckTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao,
				SystemSettings settings) {
			super("Handshake", externalSystemReady, role, configId, dao);
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

}
