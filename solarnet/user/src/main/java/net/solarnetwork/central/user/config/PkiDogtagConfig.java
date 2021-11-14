/* ==================================================================
 * PkiDogtagConfig.java - 7/10/2021 12:43:46 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.user.pki.dogtag.DogtagPKIBiz;
import net.solarnetwork.central.user.pki.dogtag.SSLContextFactory;
import net.solarnetwork.service.CertificateService;

/**
 * Configuration for Dogtag PKI.
 * 
 * @author matt
 * @version 1.0
 */
@Profile("dogtag")
@Configuration
public class PkiDogtagConfig {

	@Autowired
	public CertificateService certificateService;

	public static final String DEFAULT_DISABLED_CIPHERS = "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384"
			+ ",TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA"
			+ ",TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"
			+ ",TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
			+ ",TLS_DHE_RSA_WITH_AES_128_CBC_SHA";

	public static class DogtagSettings {

		private String baseUrl = "https://ca.solarnetwork.net:8443";
		private String profileId = "SolarNode";
		private Resource keystoreResource = new ClassPathResource("dogtag-client.jks");
		private String keystorePassword = "changeit";
		private String disabledCiphers = DEFAULT_DISABLED_CIPHERS;

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public String getProfileId() {
			return profileId;
		}

		public void setProfileId(String profileId) {
			this.profileId = profileId;
		}

		public Resource getKeystoreResource() {
			return keystoreResource;
		}

		public void setKeystoreResource(Resource keystoreResource) {
			this.keystoreResource = keystoreResource;
		}

		public String getKeystorePassword() {
			return keystorePassword;
		}

		public void setKeystorePassword(String keystorePassword) {
			this.keystorePassword = keystorePassword;
		}

		public String getDisabledCiphers() {
			return disabledCiphers;
		}

		public void setDisabledCiphers(String disabledCiphers) {
			this.disabledCiphers = disabledCiphers;
		}

	}

	@ConfigurationProperties(prefix = "app.node.pki.dogtag")
	@Bean
	public DogtagSettings dogtagSettings() {
		return new DogtagSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public DogtagPKIBiz pkiBiz() {
		DogtagSettings settings = dogtagSettings();

		SSLContextFactory sslFactory = new SSLContextFactory();
		sslFactory.setKeystoreResource(settings.keystoreResource);
		sslFactory.setKeystorePassword(settings.keystorePassword);
		sslFactory.setDisabledCipherSuites(
				StringUtils.commaDelimitedListToStringArray(settings.disabledCiphers));

		DogtagPKIBiz biz = new DogtagPKIBiz();
		biz.setBaseUrl(settings.baseUrl);
		biz.setDogtagProfileId(settings.profileId);
		biz.setRestOps(sslFactory.createRestOps());
		biz.setCertificateService(certificateService);
		return biz;
	}

}
