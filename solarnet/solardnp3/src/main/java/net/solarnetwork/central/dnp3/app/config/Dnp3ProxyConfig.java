/* ==================================================================
 * Dnp3ProxyConfig.java - 9/08/2023 5:05:46 pm
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

package net.solarnetwork.central.dnp3.app.config;

import static net.solarnetwork.central.dnp3.app.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import static net.solarnetwork.central.dnp3.app.service.Dnp3ProxyConfigurationProvider.USER_TRUST_STORE_CACHE_QUALIFIER;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import com.automatak.dnp3.DNP3Manager;
import net.solarnetwork.central.biz.NodeEventObservationRegistrar;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.dnp3.app.service.Dnp3ProxyConfigurationProvider;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.support.SimpleCache;

/**
 * DNP3 proxy configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class Dnp3ProxyConfig {

	@Autowired
	private TaskExecutor taskExecutor;

	@Autowired
	private DNP3Manager manager;

	@Autowired
	private InstructorBiz instructorBiz;

	@Autowired
	private DynamicPortRegistrar portRegistrar;

	@Autowired
	private TrustedIssuerCertificateDao trustedCertDao;

	@Autowired
	private ServerAuthConfigurationDao serverAuthDao;

	@Autowired
	private ServerMeasurementConfigurationDao serverMeasurementDao;

	@Autowired
	private ServerControlConfigurationDao serverControlDao;

	@Autowired
	@Qualifier(SOLARQUEUE)
	private NodeEventObservationRegistrar<ObjectDatum> datumObserver;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Cache settings for the DNP3 proxy configuration provider.
	 * 
	 * @return the settings
	 */
	@Bean
	@ConfigurationProperties(prefix = "app.dnp3.user-trust-store-cache")
	@Qualifier(USER_TRUST_STORE_CACHE_QUALIFIER)
	public CacheSettings userTrustStoreCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_TRUST_STORE_CACHE_QUALIFIER)
	public Cache<Long, KeyStore> userTrustStoreCache(
			@Qualifier(USER_TRUST_STORE_CACHE_QUALIFIER) CacheSettings settings) {
		SimpleCache<Long, KeyStore> cache = new SimpleCache<>(USER_TRUST_STORE_CACHE_QUALIFIER);
		cache.setTtl(settings.getTtl());
		cache.setTimeUnit(TimeUnit.SECONDS);
		return cache;
	}

	@Bean
	public Dnp3ProxyConfigurationProvider dnp3ProxyConfigurationProvider(
			@Autowired(required = false) @Qualifier(USER_TRUST_STORE_CACHE_QUALIFIER) Cache<Long, KeyStore> userTrustStoreCache) {
		Dnp3ProxyConfigurationProvider provider = new Dnp3ProxyConfigurationProvider(manager,
				instructorBiz, portRegistrar, trustedCertDao, serverAuthDao, serverMeasurementDao,
				serverControlDao, datumObserver, datumDao, userEventAppenderBiz);
		provider.setTaskExecutor(taskExecutor);
		provider.setUserTrustStoreCache(userTrustStoreCache);
		return provider;
	}

}
