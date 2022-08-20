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

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.oscp.dao.BasicConfigurationFilter.filterForUsers;
import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizationHeader;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.oscp.util.DeferredSystemTask;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.KeyValuePair;
import oscp.v20.HandshakeAcknowledge;
import oscp.v20.Register;
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
	private final RestOperations restOps;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final FlexibilityProviderDao flexibilityProviderDao;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private Map<String, String> versionUrlMap = defaultVersionUrlMap();
	private TransactionTemplate txTemplate;
	private TaskScheduler taskScheduler;
	private long taskConditionTimeout = 60_000L;
	private long taskStartDelay = 1_000L;
	private long taskRetryDelay = 5_000L;

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
	 * @param restOps
	 *        the REST operations to use
	 * @param userEventAppenderBiz
	 *        the user event appender
	 * @param flexibilityProviderDao
	 *        the flexibility provider DAO
	 * @param capacityProviderDao
	 *        the capacity provider configuration DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer configuration DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoFlexibilityProviderBiz(Executor executor, RestOperations restOps,
			UserEventAppenderBiz userEventAppenderBiz, FlexibilityProviderDao flexibilityProviderDao,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
	}

	private ExternalSystemConfigurationDao<?> configurationDaoForRole(OscpRole systemRole) {
		ExternalSystemConfigurationDao<?> dao;
		if ( systemRole == OscpRole.CapacityProvider ) {
			dao = capacityProviderDao;
		} else if ( systemRole == OscpRole.CapacityOptimizer ) {
			dao = capacityOptimizerDao;
		} else {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		return dao;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void register(AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			Future<?> externalSystemReady) throws AuthorizationException {
		if ( authInfo == null || authInfo.id() == null || authInfo.role() == null
				|| externalSystemToken == null ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}

		OscpRole systemRole = authInfo.role();
		ExternalSystemConfigurationDao<?> dao = configurationDaoForRole(systemRole);
		BaseOscpExternalSystemConfiguration<?> conf = handleRegistration(authInfo, externalSystemToken,
				versionUrl, dao);

		// generate new FP token for the system to use, and return it
		var fpId = new UserLongCompositePK(authInfo.userId(), conf.getFlexibilityProviderId());
		String newToken = flexibilityProviderDao.createAuthToken(fpId);

		executor.execute(new RegisterExternalSystemTask<>(externalSystemReady, systemRole, conf.getId(),
				dao, newToken).withConditionTimeout(taskConditionTimeout).withStartDelay(taskStartDelay)
						.withRetryDelay(taskRetryDelay));
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
		if ( authInfo == null || authInfo.id() == null || authInfo.role() == null || settings == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		OscpRole systemRole = authInfo.role();
		if ( systemRole != OscpRole.CapacityProvider ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}

		BasicConfigurationFilter filter = filterForUsers(authInfo.userId());
		filter.setConfigurationId(authInfo.entityId());
		filter.setLockResults(true);
		var filterResults = capacityProviderDao.findFiltered(filter);
		if ( filterResults.getReturnedResultCount() < 1 ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, authInfo);
		}
		CapacityProviderConfiguration conf = stream(filterResults.spliterator(), false).findFirst()
				.orElse(null);
		capacityProviderDao.saveSettings(conf.getId(), settings);

		SystemSettings ackSettings = new SystemSettings(null, null);
		executor.execute(new HandshakeAckTask<>(externalSystemReady, systemRole, conf.getId(),
				capacityProviderDao, ackSettings).withConditionTimeout(taskConditionTimeout)
						.withStartDelay(taskStartDelay).withRetryDelay(taskRetryDelay));
	}

	private class HandshakeAckTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends DeferredSystemTask<C> {

		private final SystemSettings settings;

		private HandshakeAckTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao,
				SystemSettings settings) {
			super("Handshake", externalSystemReady, role, configId, 3, dao,
					DaoFlexibilityProviderBiz.this.userEventAppenderBiz,
					CAPACITY_PROVIDER_HANDSHAKE_ERROR_TAGS, DaoFlexibilityProviderBiz.this.executor,
					DaoFlexibilityProviderBiz.this.taskScheduler,
					DaoFlexibilityProviderBiz.this.txTemplate);
			this.settings = requireNonNullArgument(settings, "settings");
		}

		@Override
		protected void doWork() {
			C config = configuration(true);
			if ( config.getRegistrationStatus() != RegistrationStatus.Registered ) {
				var msg = "[%s] task with {} {} failed because the registration status is not Registered."
						.formatted(name, role, configId.ident());
				log.info(msg);
				userEventAppenderBiz.addEvent(config.getUserId(), eventForConfiguration(config,
						CAPACITY_PROVIDER_HANDSHAKE_ERROR_TAGS, "Not registered"));
				return;
			}

			verifySystemOscpVersion(singleton(V20));
			URI uri = systemUri();
			String authToken = authToken();
			doWork20(config, uri, authToken);
		}

		private void doWork20(C conf, URI uri, String authToken) {
			HandshakeAcknowledge ack = new HandshakeAcknowledge();
			if ( settings != null ) {
				ack.setRequiredBehaviour(settings.toOscp20Value());
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(HttpHeaders.AUTHORIZATION, tokenAuthorizationHeader(authToken));
			HttpEntity<HandshakeAcknowledge> req = new HttpEntity<>(ack, headers);

			ResponseEntity<?> result = restOps.postForEntity(uri, req, null);
			if ( result.getStatusCode() == HttpStatus.NO_CONTENT ) {
				log.info("Successfully handshake acknowledged with {} {} at: {}", role, configId.ident(),
						uri);
				userEventAppenderBiz.addEvent(conf.getUserId(),
						eventForConfiguration(conf, CAPACITY_PROVIDER_HANDSHAKE_TAGS, "Acknoledged"));
			} else {
				log.warn(
						"Unable to handshake acknowledge with {} {} at [{}] because the HTTP status {} was returned (expected {}).",
						role, configId.ident(), uri, result.getStatusCodeValue(),
						HttpStatus.NO_CONTENT.value());
				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_HANDSHAKE_ERROR_TAGS,
						format("Invalid HTTP status returned: %d", result.getStatusCodeValue())));
			}
		}

	}

	private class RegisterExternalSystemTask<C extends BaseOscpExternalSystemConfiguration<C>>
			extends DeferredSystemTask<C> {

		private final String token;

		private RegisterExternalSystemTask(Future<?> externalSystemReady, OscpRole role,
				UserLongCompositePK configId, ExternalSystemConfigurationDao<C> dao, String token) {
			super("Register", externalSystemReady, role, configId, 3, dao,
					DaoFlexibilityProviderBiz.this.userEventAppenderBiz,
					CAPACITY_PROVIDER_REGISTER_ERROR_TAGS, DaoFlexibilityProviderBiz.this.executor,
					DaoFlexibilityProviderBiz.this.taskScheduler,
					DaoFlexibilityProviderBiz.this.txTemplate);
			this.token = requireNonNullArgument(token, "token");
		}

		@Override
		protected void doWork() {
			C config = configuration(true);
			if ( config.getRegistrationStatus() != RegistrationStatus.Pending ) {
				log.info("Unable to register with {} {} because the registration status is not Pending.",
						role, configId.ident());
				return;
			}

			try {
				verifySystemOscpVersion(singleton(V20));
				URI uri = systemUri();
				String authToken = authToken();
				doWork20(config, uri, authToken);
			} catch ( ExternalSystemConfigurationException confEx ) {
				config.setRegistrationStatus(RegistrationStatus.Failed);
				dao.save(config);
				throw confEx;
			}
		}

		private void doWork20(C conf, URI uri, String authToken) {
			String url = versionUrlMap.get(V20);
			if ( url == null ) {
				log.error(
						"Unable to register with {} {} because the Flexibility Provider URL for version {} is not configured.",
						role, configId.ident(), V20);

				conf.setRegistrationStatus(RegistrationStatus.Failed);

				userEventAppenderBiz.addEvent(conf.getUserId(),
						eventForConfiguration(conf, CAPACITY_PROVIDER_REGISTER_ERROR_TAGS,
								"Flexibility Provider URL for version not configured"));

				dao.save(conf);
				return;
			}
			List<VersionUrl> versions = Collections.singletonList(new VersionUrl(V20, url));
			Register register = new Register(token, versions);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set(HttpHeaders.AUTHORIZATION, tokenAuthorizationHeader(authToken));
			HttpEntity<Register> req = new HttpEntity<>(register, headers);

			ResponseEntity<?> result = restOps.postForEntity(uri, req, null);
			if ( result.getStatusCode() == HttpStatus.NO_CONTENT ) {
				log.info("Successfully registered with {} {} at: {}", role, configId.ident(), uri);
				conf.setRegistrationStatus(RegistrationStatus.Registered);
				userEventAppenderBiz.addEvent(conf.getUserId(),
						eventForConfiguration(conf, CAPACITY_PROVIDER_REGISTER_TAGS, null));
			} else {
				log.error(
						"Unable to register with {} {} at [{}] because the HTTP status {} was returned (expected {}).",
						role, configId.ident(), uri, result.getStatusCodeValue(),
						HttpStatus.NO_CONTENT.value());
				conf.setRegistrationStatus(RegistrationStatus.Failed);
				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_REGISTER_ERROR_TAGS,
						format("Invalid HTTP status returned: %d", result.getStatusCodeValue())));
			}
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
