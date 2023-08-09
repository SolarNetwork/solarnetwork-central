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
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import java.security.KeyStore;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.automatak.dnp3.DNP3Manager;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
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
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * DNP3 proxy configuration provider.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class Dnp3ProxyConfigurationProvider implements ProxyConfigurationProvider {

	/** The qualifier name for the user trust store cache. */
	public static final String USER_TRUST_STORE_CACHE_QUALIFIER = "user-trust-store-cache";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DNP3Manager manager;
	private final InstructorBiz instructorBiz;
	private final DynamicPortRegistrar portRegistrar;
	private final TrustedIssuerCertificateDao trustedCertDao;
	private final ServerAuthConfigurationDao serverAuthDao;
	private final ServerMeasurementConfigurationDao serverMeasurementDao;
	private final ServerControlConfigurationDao serverControlDao;

	@Autowired(required = false)
	@Qualifier(USER_TRUST_STORE_CACHE_QUALIFIER)
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public Dnp3ProxyConfigurationProvider(DNP3Manager manager, InstructorBiz instructorBiz,
			DynamicPortRegistrar portRegistrar, TrustedIssuerCertificateDao trustedCertDao,
			ServerAuthConfigurationDao serverAuthDao,
			ServerMeasurementConfigurationDao serverMeasurementDao,
			ServerControlConfigurationDao serverControlDao) {
		super();
		this.manager = requireNonNullArgument(manager, "manager");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.portRegistrar = requireNonNullArgument(portRegistrar, "portRegistrar");
		this.trustedCertDao = requireNonNullArgument(trustedCertDao, "trustedCertDao");
		this.serverAuthDao = requireNonNullArgument(serverAuthDao, "serverAuthDao");
		this.serverMeasurementDao = requireNonNullArgument(serverMeasurementDao, "serverMeasurementDao");
		this.serverControlDao = requireNonNullArgument(serverControlDao, "serverControlDao");
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
	 */
	private final class DynamicConnectionSettings
			implements ProxyConnectionSettings, ServiceLifecycleObserver {

		private final ProxyConnectionRequest request;
		private final ServerAuthConfiguration auth;
		private final KeyStore trustStore;
		private int port = 0;

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

			BasicFilter filter = new BasicFilter();
			filter.setUserId(auth.getUserId());
			filter.setServerId(auth.getServerId());
			filter.setEnabled(true);
			List<ServerMeasurementConfiguration> mConfigs = stream(
					serverMeasurementDao.findFiltered(filter).spliterator(), false).toList();
			List<ServerControlConfiguration> cConfigs = stream(
					serverControlDao.findFiltered(filter).spliterator(), false).toList();
			if ( mConfigs.isEmpty() && cConfigs.isEmpty() ) {
				requireNonNullObject(null, "DNP3 Configuration");
			}
			server = new OutstationService(manager, instructorBiz, auth, destinationHost(), newPort,
					mConfigs, cConfigs);
			server.serviceDidStartup();
			port = newPort;
		}

		@Override
		public synchronized void serviceDidShutdown() {
			if ( server != null ) {
				log.info("Stopping DNP3 outstation [{}]", server.getUid());
				try {
					server.serviceDidShutdown();
				} catch ( Exception e ) {
					// ignore this
				}
				server = null;
			}
			final int port = destinationPort();
			if ( port > 0 ) {
				portRegistrar.releasePort(port);
				this.port = 0;
			}
		}

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
