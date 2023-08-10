/* ==================================================================
 * Dnp3ProxyConfigurationProvider.java - 8/08/2023 3:50:33 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.service;

import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.automatak.dnp3.DNP3Manager;
import net.solarnetwork.central.biz.NodeEventObservationRegistrar;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.datum.v2.support.BasicStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.Dnp3UserEvents;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.service.ProxyConfigurationProvider;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * DNP3 proxy configuration provider.
 * 
 * @author matt
 * @version 1.0
 */
public class Dnp3ProxyConfigurationProvider implements ProxyConfigurationProvider, Dnp3UserEvents {

	/** The qualifier name for the user trust store cache. */
	public static final String USER_TRUST_STORE_CACHE_QUALIFIER = "user-trust-store-cache";

	/** User event tags for authorization events. */
	public static final String[] AUTHORIZATION_TAGS = new String[] { DNP3_TAG, AUTHORIZATION_TAG };

	/** User event tags for session events. */
	public static final String[] SESSION_TAGS = new String[] { DNP3_TAG, SESSION_TAG };

	/** User event tags for datum events. */
	public static final String[] DATUM_TAGS = new String[] { DNP3_TAG, DATUM_TAG };

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DNP3Manager manager;
	private final InstructorBiz instructorBiz;
	private final DynamicPortRegistrar portRegistrar;
	private final TrustedIssuerCertificateDao trustedCertDao;
	private final ServerAuthConfigurationDao serverAuthDao;
	private final ServerMeasurementConfigurationDao serverMeasurementDao;
	private final ServerControlConfigurationDao serverControlDao;
	private final NodeEventObservationRegistrar<ObjectDatum> datumObserver;
	private final DatumEntityDao datumDao;
	private final UserEventAppenderBiz userEventAppenderBiz;

	private Executor taskExecutor;
	private Cache<Long, KeyStore> userTrustStoreCache;

	/**
	 * Constructor.
	 * 
	 * @param manager
	 *        the manager to use
	 * @param instructorBiz
	 *        the instructor service to use
	 * @param portRegistrar
	 *        the port registrar to use
	 * @param trustedCertDao
	 *        the trusted certificate DAO
	 * @param serverAuthDao
	 *        the server auth DAO
	 * @param serverMeasurementDao
	 *        the server measurement DAO
	 * @param serverControlDao
	 *        the server control DAO
	 * @param datumObserver
	 *        the node event observer for datum
	 * @param datumDao
	 *        the datum DAO
	 * @param userEventAppenderBiz
	 *        the user event appender
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public Dnp3ProxyConfigurationProvider(DNP3Manager manager, InstructorBiz instructorBiz,
			DynamicPortRegistrar portRegistrar, TrustedIssuerCertificateDao trustedCertDao,
			ServerAuthConfigurationDao serverAuthDao,
			ServerMeasurementConfigurationDao serverMeasurementDao,
			ServerControlConfigurationDao serverControlDao,
			NodeEventObservationRegistrar<ObjectDatum> datumObserver, DatumEntityDao datumDao,
			UserEventAppenderBiz userEventAppenderBiz) {
		super();
		this.manager = requireNonNullArgument(manager, "manager");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.portRegistrar = requireNonNullArgument(portRegistrar, "portRegistrar");
		this.trustedCertDao = requireNonNullArgument(trustedCertDao, "trustedCertDao");
		this.serverAuthDao = requireNonNullArgument(serverAuthDao, "serverAuthDao");
		this.serverMeasurementDao = requireNonNullArgument(serverMeasurementDao, "serverMeasurementDao");
		this.serverControlDao = requireNonNullArgument(serverControlDao, "serverControlDao");
		this.datumObserver = requireNonNullArgument(datumObserver, "datumObserver");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
	}

	@Override
	public Iterable<X509Certificate> acceptedIdentityIssuers() {
		// in future if needed this iterator could do dynamic, paginated iteration over live DAO results;
		// for now just return nothing
		return Collections.emptyList();
	}

	@Override
	public ProxyConnectionSettings authorize(ProxyConnectionRequest request)
			throws AuthorizationException {
		final X509Certificate[] clientIdentity = requireNonNullArgument(request, "request")
				.principalIdentity();
		// assume client certificate is first
		if ( clientIdentity.length < 1 ) {
			return null;
		}

		final String clientSubjectDn = CertificateUtils.canonicalSubjectDn(clientIdentity[0]);
		final ServerAuthConfiguration auth = requireNonNullObject(
				serverAuthDao.findForIdentifier(clientSubjectDn), clientSubjectDn);

		// load trust store
		KeyStore trustStore = userTrustStore(auth.getUserId());

		// validate certificate
		try {
			PKIXCertPathValidatorResult vr = CertificateUtils.validateCertificateChain(trustStore,
					clientIdentity);
			if ( log.isInfoEnabled() ) {
				TrustAnchor ta = vr.getTrustAnchor();
				log.info("Validated connection authorization request identity [{}], trusted by [{}]",
						clientSubjectDn, ta.getTrustedCert().getSubjectX500Principal());
			}

			return new DynamicConnectionSettings(request, auth, trustStore);
		} catch ( Exception e ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, clientSubjectDn, e);
		}
	}

	private KeyStore userTrustStore(Long userId) {
		final Cache<Long, KeyStore> cache = getUserTrustStoreCache();
		if ( cache != null ) {
			KeyStore ks = cache.get(userId);
			if ( ks != null ) {
				return ks;
			}
		}
		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null);

			final var certs = trustedCertDao.findAll(userId, null).stream()
					.map(TrustedIssuerCertificate::getCertificate).toArray(X509Certificate[]::new);
			for ( X509Certificate cert : certs ) {
				String alias = sha1Hex(canonicalSubjectDn(cert));
				ks.setCertificateEntry(alias, cert);
			}

			if ( cache != null ) {
				cache.put(userId, ks);
			}
			return ks;
		} catch ( Exception e ) {
			throw new CertificateException("Error initializing trust store for user " + userId, e);
		}
	}

	/**
	 * {@link ProxyConnectionSettings} that also implements
	 * {@link ServiceLifecycleObserver} and resolves a dynamic unused port when
	 * {@link #serviceDidStartup()} is invoked.
	 * 
	 * <p>
	 * This class extends {@link BasicStreamDatumFilteredResultsProcessor} for
	 * convenience, to support the initial data load of the DNP3 server.
	 * </p>
	 */
	private final class DynamicConnectionSettings extends BasicStreamDatumFilteredResultsProcessor
			implements ProxyConnectionSettings, ServiceLifecycleObserver {

		private final ProxyConnectionRequest request;
		private final ServerAuthConfiguration auth;
		private final KeyStore trustStore;
		private int port = 0;
		private int datumLoadCount = 0;

		private OutstationService server;

		private DynamicConnectionSettings(ProxyConnectionRequest request, ServerAuthConfiguration auth,
				KeyStore trustStore) {
			super();
			this.request = requireNonNullArgument(request, "request");
			this.auth = requireNonNullArgument(auth, "auth");
			this.trustStore = requireNonNullArgument(trustStore, "trustStore");
		}

		@Override
		public ProxyConnectionRequest connectionRequest() {
			return request;
		}

		@Override
		public KeyStore clientTrustStore() {
			return trustStore;
		}

		@Override
		public String destinationHost() {
			return "127.0.0.1";
		}

		@Override
		public int destinationPort() {
			return port;
		}

		@Override
		public synchronized void serviceDidStartup() {
			if ( port > 0 ) {
				return;
			}
			final int newPort = portRegistrar.reserveNewPort();

			final BasicFilter filter = new BasicFilter();
			filter.setUserId(auth.getUserId());
			filter.setServerId(auth.getServerId());
			filter.setEnabled(true);
			filter.setValidNodeOwnership(true);
			final List<ServerMeasurementConfiguration> mConfigs = stream(
					serverMeasurementDao.findFiltered(filter).spliterator(), false)
							.filter(ServerMeasurementConfiguration::isValid).toList();
			final List<ServerControlConfiguration> cConfigs = stream(
					serverControlDao.findFiltered(filter).spliterator(), false)
							.filter(ServerControlConfiguration::isValid).toList();
			if ( mConfigs.isEmpty() && cConfigs.isEmpty() ) {
				log.warn("""
						User {} DNP3 server {} has no valid measurement or control configurations: \
						authorized connection for [{}] denied.""", auth.getUserId(), auth.getServerId(),
						auth.getIdentifier());
				userEventAppenderBiz
						.addEvent(auth.getUserId(),
								event(AUTHORIZATION_TAGS,
										"No valid measurement or control configurations.",
										getJSONString(
												Map.of(SERVER_ID_DATA_KEY, auth.getServerId(),
														IDENTIFIER_DATA_KEY, auth.getIdentifier()),
												null),
										ERROR_TAG));
				requireNonNullObject(null, "DNP3 Configuration"); // generate exception
			}

			log.info("""
					User {} DNP3 server {} starting with {} measurement and {} control configurations: \
					authorized connection for [{}] on port {}.""", auth.getUserId(), auth.getServerId(),
					mConfigs.size(), cConfigs.size(), auth.getIdentifier(), newPort);
			userEventAppenderBiz.addEvent(auth.getUserId(),
					event(SESSION_TAGS,
							"Server starting with %d and %d control configurations."
									.formatted(mConfigs.size(), cConfigs.size()),
							getJSONString(Map.of(SERVER_ID_DATA_KEY, auth.getServerId(),
									IDENTIFIER_DATA_KEY, auth.getIdentifier()), null),
							START_TAG));

			server = new OutstationService(manager, instructorBiz, auth, destinationHost(), newPort,
					mConfigs, cConfigs);
			server.setTaskExecutor(taskExecutor);
			server.serviceDidStartup();

			final Runnable reg = () -> {
				// get list of unique node and source IDs referenced by configurations
				final Set<Long> nodeIds = new HashSet<>(mConfigs.size() + cConfigs.size());
				final Set<String> sourceIds = new HashSet<>(mConfigs.size() + cConfigs.size());
				for ( var c : mConfigs ) {
					nodeIds.add(c.getNodeId());
					sourceIds.add(c.getSourceId());
				}
				for ( var c : cConfigs ) {
					nodeIds.add(c.getNodeId());
					sourceIds.add(c.getControlId());
				}
				final Long[] nodeIdsArray = nodeIds.toArray(Long[]::new);
				final String[] sourceIdsArray = sourceIds.toArray(String[]::new);

				// load initial data from database for datum streams referenced by configurations
				BasicDatumCriteria datumFilter = new BasicDatumCriteria();
				datumFilter.setNodeIds(nodeIdsArray);
				datumFilter.setSourceIds(sourceIdsArray);
				datumFilter.setMostRecent(true);
				try {
					datumDao.findFilteredStream(datumFilter, this);
					log.info("User {} DNP3 server {} loaded {} initial datum.", auth.getUserId(),
							auth.getServerId(), datumLoadCount);
					userEventAppenderBiz.addEvent(auth.getUserId(),
							event(DATUM_TAGS, "Loaded initial data.",
									getJSONString(Map.of(SERVER_ID_DATA_KEY, auth.getServerId(),
											COUNT_DATA_KEY, datumLoadCount), null)));
				} catch ( Exception e ) {
					userEventAppenderBiz
							.addEvent(auth.getUserId(),
									event(DATUM_TAGS, "Error loading initial data.",
											getJSONString(Map.of(SERVER_ID_DATA_KEY, auth.getServerId(),
													MESSAGE_DATA_KEY, e.getMessage()), null),
											ERROR_TAG));
				}

				// add observers for node datum streams referenced by configurations
				datumObserver.registerNodeObserver(server, nodeIdsArray);
				userEventAppenderBiz.addEvent(auth.getUserId(),
						event(DATUM_TAGS, "Subscribed to datum stream updates.",
								getJSONString(Map.of(SERVER_ID_DATA_KEY, auth.getServerId()), null)));
			};
			final Executor taskExecutor = getTaskExecutor();
			if ( taskExecutor != null ) {
				taskExecutor.execute(reg);
			} else {
				reg.run();
			}

			port = newPort;
		}

		@Override
		public synchronized void serviceDidShutdown() {
			final int port = destinationPort();
			if ( server != null ) {
				log.info("Stopping DNP3 outstation [{}] on port {}", server.getUid(), port);
				userEventAppenderBiz.addEvent(auth.getUserId(),
						event(SESSION_TAGS, "Server stopped.", getJSONString(Map.of(SERVER_ID_DATA_KEY,
								auth.getServerId(), IDENTIFIER_DATA_KEY, auth.getIdentifier()), null),
								END_TAG));
				datumObserver.unregisterNodeObserver(server);
				try {
					server.serviceDidShutdown();
				} catch ( Exception e ) {
					// ignore this
				}
				server = null;
			}
			if ( port > 0 ) {
				portRegistrar.releasePort(port);
				this.port = 0;
			}
		}

		@Override
		public void handleResultItem(StreamDatum resultItem) throws IOException {
			final OutstationService server = this.server;
			if ( server == null ) {
				return;
			}
			ObjectDatumStreamMetadata meta = getMetadataProvider()
					.metadataForStreamId(resultItem.getStreamId());
			ObjectDatum d = ObjectDatum.forStreamDatum(resultItem, auth.getUserId(),
					DatumId.nodeId(meta.getObjectId(), meta.getSourceId(), resultItem.getTimestamp()),
					meta);
			server.accept(d);
			datumLoadCount++;
		}

	}

	/**
	 * Get the task executor.
	 * 
	 * @return the taskExecutor
	 */
	public Executor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Set the task executor.
	 * 
	 * @param taskExecutor
	 *        the taskExecutor to set
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Get the user trust store cache.
	 * 
	 * @return the cache
	 */
	public Cache<Long, KeyStore> getUserTrustStoreCache() {
		return userTrustStoreCache;
	}

	/**
	 * Set the user trust store cache.
	 * 
	 * @param userTrustStoreCache
	 *        the userTrustStoreCache to set
	 */
	public void setUserTrustStoreCache(Cache<Long, KeyStore> userTrustStoreCache) {
		this.userTrustStoreCache = userTrustStoreCache;
	}

}
