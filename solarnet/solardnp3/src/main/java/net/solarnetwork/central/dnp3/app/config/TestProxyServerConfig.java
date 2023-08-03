/* ==================================================================
 * TestProxyServerConfig.java - 3/08/2023 6:15:08 am
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.dnp3.app.config.ConfigurationConstants.PROFILE_TEST;
import static net.solarnetwork.central.net.proxy.util.CertificateUtils.canonicalSubjectDn;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.net.proxy.config.DynamicProxyServerSettings;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.service.impl.NettyDynamicProxyServer;
import net.solarnetwork.central.net.proxy.service.impl.SimplePrincipalMapping;
import net.solarnetwork.central.net.proxy.service.impl.SimpleProxyConfigurationProvider;
import net.solarnetwork.central.net.proxy.util.CertificateUtils;

/**
 * Testing configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(PROFILE_TEST)
public class TestProxyServerConfig {

	@ConfigurationProperties("app.pki.simple-trust")
	@Bean
	@Qualifier("simple-trust")
	public Map<String, Map<String, String>> testTrustSettings() {
		return new LinkedHashMap<>();
	}

	@Bean
	public SimpleProxyConfigurationProvider testProxyConfigurationProvider(
			@Qualifier("simple-trust") Map<String, Map<String, String>> trust,
			DynamicPortRegistrar portRegistrar)
			throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

		final List<SimplePrincipalMapping> userMappings = new ArrayList<>(trust.size());
		for ( Entry<String, Map<String, String>> e : trust.entrySet() ) {
			KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
			store.load(null);

			X509Certificate[] certs = CertificateUtils
					.parsePemCertificates(Files.newBufferedReader(Path.of(e.getKey()), UTF_8));
			for ( X509Certificate cert : certs ) {
				String alias = sha1Hex(canonicalSubjectDn(cert));
				store.setCertificateEntry(alias, cert);
			}

			userMappings.add(new SimplePrincipalMapping(store, e.getValue()));
		}

		SimpleProxyConfigurationProvider provider = new SimpleProxyConfigurationProvider(portRegistrar,
				userMappings);
		return provider;
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public NettyDynamicProxyServer testProxyServer(DynamicProxyServerSettings settings) {
		NettyDynamicProxyServer server = new NettyDynamicProxyServer(settings.bindAddress(),
				settings.bindPort());
		server.setWireLogging(settings.isWireLoggingEnabled());
		if ( settings.hasTlsSettings() ) {
			KeyStore keyStore = CertificateUtils.serverKeyStore(settings.tls(),
					NettyDynamicProxyServer.DEFAULT_KEYSTORE_ALIAS);
			server.setKeyStore(keyStore);
		}
		return server;
	}

}
