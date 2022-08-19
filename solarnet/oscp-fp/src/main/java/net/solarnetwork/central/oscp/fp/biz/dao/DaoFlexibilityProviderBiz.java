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
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizationHeader;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.FLEXIBILITY_PROVIDER_V20_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.BasicConfigurationFilter;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.fp.biz.FlexibilityProviderBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.KeyValuePair;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void register(AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			Future<?> externalSystemReady) throws AuthorizationException {
		if ( authInfo == null || authInfo.id() == null || authInfo.role() == null
				|| externalSystemToken == null ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}

		OscpRole systemRole = authInfo.role();
		ExternalSystemConfigurationDao<?> dao;
		if ( systemRole == OscpRole.CapacityProvider ) {
			dao = capacityProviderDao;
		} else if ( systemRole == OscpRole.CapacityOptimizer ) {
			dao = capacityOptimizerDao;
		} else {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, null);
		}
		BaseOscpExternalSystemConfiguration<?> conf = handleRegistration(authInfo, externalSystemToken,
				versionUrl, dao);

		// generate new FP token for the system to use, and return it
		var fpId = new UserLongCompositePK(authInfo.userId(), conf.getFlexibilityProviderId());
		String newToken = flexibilityProviderDao.createAuthToken(fpId);

		executor.execute(new RegisterExternalSystemTask<>(systemRole, conf.getId(), newToken,
				externalSystemReady, dao));
	}

	private <C extends BaseOscpExternalSystemConfiguration<C>> C handleRegistration(
			AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl,
			ExternalSystemConfigurationDao<C> dao) {
		BasicConfigurationFilter filter = BasicConfigurationFilter.filterForUsers(authInfo.userId());
		filter.setConfigurationId(authInfo.entityId());
		var filterResults = dao.findFiltered(filter);
		if ( filterResults.getReturnedResultCount() > 0 ) {
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
		} else {
			// TODO UserEvent (in exception handler method)
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, authInfo);
		}
	}

	private static LogEventInfo eventForConfiguration(BaseOscpExternalSystemConfiguration<?> conf,
			String[] baseTags, String message) {
		return event(baseTags, message,
				getJSONString(Map.of(CONFIG_ID_DATA_KEY, conf.getEntityId(),
						REGISTRATION_STATUS_DATA_KEY, (char) conf.getRegistrationStatus().getCode(),
						VERSION_DATA_KEY, conf.getOscpVersion(), URL_DATA_KEY, conf.getBaseUrl()),
						null));
	}

	private class RegisterExternalSystemTask<C extends BaseOscpExternalSystemConfiguration<C>>
			implements Runnable {

		private final OscpRole role;
		private final UserLongCompositePK configId;
		private final String token;
		private final Future<?> externalSystemReady;
		private final ExternalSystemConfigurationDao<C> dao;

		private int tries = 0;
		private int remainingTries = 3; // TODO: make configurable

		private RegisterExternalSystemTask(OscpRole role, UserLongCompositePK configId, String token,
				Future<?> externalSystemReady, ExternalSystemConfigurationDao<C> dao) {
			super();
			this.role = requireNonNullArgument(role, "role");
			this.configId = requireNonNullArgument(configId, "configId");
			this.token = requireNonNullArgument(token, "token");
			this.externalSystemReady = requireNonNullArgument(externalSystemReady,
					"externalSystemReady");
			this.dao = requireNonNullArgument(dao, "dao");
		}

		@Override
		public void run() {
			final TransactionTemplate tt = getTxTemplate();
			try {
				log.debug("Waiting for external system ready signal");
				externalSystemReady.get(1, TimeUnit.MINUTES); // TODO make configurable
				log.info("Registering with {} {}", role, configId.ident());
				if ( tt != null ) {
					tt.executeWithoutResult((t) -> {
						doWork();
					});
				} else {
					doWork();
				}
			} catch ( Exception e ) {
				if ( --remainingTries > 0 ) {
					log.warn("Error registering with {} {}; will re-try up to {} more times: {}", role,
							configId.ident(), remainingTries, e.getMessage());
					final TaskScheduler scheduler = getTaskScheduler();
					if ( scheduler != null ) {
						scheduler.schedule(() -> {
							executor.execute(RegisterExternalSystemTask.this);
						}, Instant.now().plusSeconds(tries * 5));
					} else {
						executor.execute(this);
					}
				} else {
					log.error("Error registering with {} {}; tried {} times: {}", role, configId.ident(),
							tries, e.getMessage(), e);
				}
			}
		}

		private void doWork() {
			tries++;
			C conf = dao.getForUpdate(configId);
			if ( conf == null ) {
				log.warn(
						"Unable to register with {} {} because the configuration does not exist; perhaps it was deleted.",
						role, configId.ident());
				return;
			}

			if ( conf.getRegistrationStatus() != RegistrationStatus.Pending ) {
				log.info("Unable to register with {} {} because the registration status is not Pending.",
						role, configId.ident());
				return;
			}

			if ( !V20.equals(conf.getOscpVersion()) ) {
				log.error("Unable to register with {} {} because the OSCP version {} is not supported.",
						role, configId.ident(), conf.getOscpVersion());

				conf.setRegistrationStatus(RegistrationStatus.Failed);

				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_REGISTER_ERROR_TAGS, "Unsupported OSCP version"));

				dao.save(conf);
				return;
			}

			URI uri;
			try {
				uri = URI.create(conf.getBaseUrl());
			} catch ( IllegalArgumentException | NullPointerException e ) {
				log.error("Unable to register with {} {} because the OSCP URL [{}] is not valid: {}",
						role, configId.ident(), conf.getBaseUrl(), e.getMessage());

				conf.setRegistrationStatus(RegistrationStatus.Failed);

				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_REGISTER_ERROR_TAGS, "Invalid URL"));

				dao.save(conf);
				return;
			}

			String authToken = dao.getExternalSystemAuthToken(configId);
			if ( authToken == null ) {
				log.error(
						"Unable to register with {} {} because the authorization token is not available.",
						role, configId.ident());
				conf.setRegistrationStatus(RegistrationStatus.Failed);

				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_REGISTER_ERROR_TAGS, "Missing authorization token"));

				dao.save(conf);
				return;
			}

			try {
				doWork20(conf, uri, authToken);
			} catch ( RuntimeException e ) {
				userEventAppenderBiz.addEvent(conf.getUserId(), eventForConfiguration(conf,
						CAPACITY_PROVIDER_REGISTER_ERROR_TAGS, e.getMessage()));
				throw e;
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

}
