/* ==================================================================
 * SSLContextFactory.java - Oct 14, 2014 2:38:08 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dogtag;

import static net.solarnetwork.util.ArrayUtils.filterByEnabledDisabled;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.web.support.LoggingHttpRequestInterceptor;

/**
 * Factory for {@link SSLContext} objects configured with an associated
 * key/trust store.
 * 
 * @author matt
 * @version 2.1
 */
public class SSLContextFactory implements PingTest {

	private Resource keystoreResource;
	private String keystorePassword;
	private int trustedCertificateExpireWarningDays = 30;
	private String[] enabledProtocols = null;
	private String[] disabledProtocols = null;
	private String[] enabledCipherSuites = null;
	private String[] disabledCipherSuites = null;

	private CachedResult<PingTest.Result> cachedResult;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get a {@link SSLContext} configured to use the
	 * {@link #getKeystoreResource()} as both a trust and key store.
	 * 
	 * @return SSLContext
	 */
	public SSLContext createContext() {
		SSLContext ctx;
		try {
			KeyStore keyStore = loadKeyStore();
			ctx = SSLContexts.custom().loadTrustMaterial(keyStore, null)
					.loadKeyMaterial(keyStore, keystorePassword.toCharArray()).build();
		} catch ( GeneralSecurityException e ) {
			throw new CertificateException("Error creating SSLContext from "
					+ (keystoreResource == null ? null : keystoreResource.getFilename()), e);
		}
		return ctx;
	}

	/**
	 * Get a {@link RestOperations} instance configured to use the
	 * {@code SSLContext} returned by {@link #createContext()}.
	 * 
	 * @return RestOperations
	 */
	public RestOperations createRestOps() {
		SSLContext ctx = createContext();
		String[] protocols = filterByEnabledDisabled(ctx.getSupportedSSLParameters().getProtocols(),
				enabledProtocols, disabledProtocols);
		String[] ciphers = filterByEnabledDisabled(ctx.getSupportedSSLParameters().getCipherSuites(),
				enabledCipherSuites, disabledCipherSuites);
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(createContext(), protocols,
				ciphers, new DefaultHostnameVerifier());
		Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("https", sslsf).build();

		HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(r);
		HttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();

		ClientHttpRequestFactory reqFactory = LoggingHttpRequestInterceptor
				.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

		RestTemplate restTemplate = new RestTemplate(reqFactory);

		if ( LoggingHttpRequestInterceptor.supportsLogging(reqFactory) ) {
			restTemplate.getInterceptors().add(new LoggingHttpRequestInterceptor());
		}

		return restTemplate;
	}

	private KeyStore loadKeyStore() throws GeneralSecurityException {
		InputStream in = null;
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			in = keystoreResource.getInputStream();
			keyStore.load(in, keystorePassword.toCharArray());
			return keyStore;
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store ";
			} else {
				msg = "Error loading certificate key store ";
			}
			throw new CertificateException(
					msg + (keystoreResource == null ? null : keystoreResource.getFilename()), e);
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					log.warn("Error closing key store file {}: {}", keystoreResource.getFilename(),
							e.getMessage());
				}
			}
		}
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "SolarNetwork CA Agent Certificate";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public PingTest.Result performPingTest() throws Exception {
		if ( cachedResult != null && cachedResult.isValid() ) {
			return cachedResult.getResult();
		}
		if ( keystoreResource == null ) {
			return new PingTestResult(false, "No keystore configured");
		}
		if ( keystorePassword == null ) {
			return new PingTestResult(false, "No keystore password configured");
		}
		final long now = System.currentTimeMillis();
		final long monthAgo = now - (1000 * 60 * 60 * 24 * trustedCertificateExpireWarningDays);
		KeyStore keyStore = loadKeyStore();
		Enumeration<String> aliases = keyStore.aliases();
		PingTestResult result = null;
		boolean validated = false;
		StringBuilder message = new StringBuilder();
		while ( aliases.hasMoreElements() ) {
			String alias = aliases.nextElement();
			Entry entry = null;
			try {
				entry = keyStore.getEntry(alias, null);
			} catch ( UnrecoverableKeyException e ) {
				entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(
						keystorePassword == null ? null : keystorePassword.toCharArray()));
			}
			if ( entry instanceof PrivateKeyEntry ) {
				PrivateKeyEntry keyEntry = (PrivateKeyEntry) entry;
				Certificate[] certs = keyEntry.getCertificateChain();
				for ( Certificate cert : certs ) {
					if ( cert instanceof X509Certificate ) {
						X509Certificate x509 = (X509Certificate) cert;
						validated = true;
						if ( x509.getNotBefore().getTime() > now ) {
							result = new PingTestResult(false,
									"Certificate " + x509.getSubjectX500Principal().getName()
											+ " not yet valid. Valid from " + x509.getNotBefore() + ".");
							break;
						}
						if ( x509.getNotAfter().getTime() < now ) {
							result = new PingTestResult(false,
									"Certificate " + x509.getSubjectX500Principal().getName()
											+ " expired on " + x509.getNotAfter() + ".");
							break;
						}
						if ( x509.getNotAfter().getTime() < monthAgo ) {
							result = new PingTestResult(false,
									"Certificate " + x509.getSubjectX500Principal().getName()
											+ " will exipre on " + x509.getNotAfter() + ".");
							break;
						}
						if ( message.length() > 0 ) {
							message.append(" ");
						}
						message.append("Certificate " + x509.getSubjectX500Principal().getName()
								+ " valid until " + x509.getNotAfter() + ".");
					}
				}
			}
		}
		if ( result == null ) {
			if ( !validated ) {
				message.append("No certificates found in keystore.");
			}
			result = new PingTestResult(validated, message.toString());
		}
		// cache the results: for success cache for longer so we don't spend a lot of time parsing the certificates
		CachedResult<PingTest.Result> cached = new CachedResult<PingTest.Result>(result,
				(result.isSuccess() ? 1L : 30L),
				(result.isSuccess() ? TimeUnit.DAYS : TimeUnit.MINUTES));
		cachedResult = cached;
		return result;
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

	public int getTrustedCertificateExpireWarningDays() {
		return trustedCertificateExpireWarningDays;
	}

	public void setTrustedCertificateExpireWarningDays(int trustedCertificateExpireWarningDays) {
		this.trustedCertificateExpireWarningDays = trustedCertificateExpireWarningDays;
	}

	/**
	 * Get the list of explicitly enabled SSL protocols.
	 * 
	 * @return the enabled SSL protocols
	 * @since 1.5
	 */
	public String[] getEnabledProtocols() {
		return enabledProtocols;
	}

	/**
	 * Set the list of explicitly enabled SSL protocols.
	 * 
	 * <p>
	 * This list is treated as regular expressions.
	 * </p>
	 * 
	 * @param enabledProtocols
	 *        a list of regular expressions for the SSL protocols to enable
	 * @since 1.5
	 */
	public void setEnabledProtocols(String[] enabledProtocols) {
		this.enabledProtocols = enabledProtocols;
	}

	/**
	 * Get the list of disabled SSL protocols.
	 * 
	 * @return the disabled SSL protocols
	 * @since 1.5
	 */
	public String[] getDisabledProtocols() {
		return disabledProtocols;
	}

	/**
	 * Set the list of disabled SSL protocols.
	 * 
	 * <p>
	 * This list is treated as regular expressions, and applied <b>after</b> any
	 * configured {@link #setEnabledProtocols(String[])} expressions.
	 * </p>
	 * 
	 * @param disabledProtocols
	 *        a list of regular expressions for the SSL protocols to disable
	 * @since 1.5
	 */
	public void setDisabledProtocols(String[] disabledProtocols) {
		this.disabledProtocols = disabledProtocols;
	}

	/**
	 * Get the list of explicitly enabled SSL cipher suites.
	 * 
	 * @return the enabled SSL cipher suites
	 * @since 1.5
	 */
	public String[] getEnabledCipherSuites() {
		return enabledCipherSuites;
	}

	/**
	 * Set the list of explicitly enabled SSL cipher suites.
	 * 
	 * <p>
	 * This list is treated as regular expressions.
	 * </p>
	 * 
	 * @param enabledCipherSuites
	 *        a list of regular expressions for the SSL cipher suites to enable
	 * @since 1.5
	 */
	public void setEnabledCipherSuites(String[] enabledCipherSuites) {
		this.enabledCipherSuites = enabledCipherSuites;
	}

	/**
	 * Get the list of disabled SSL cipher suites.
	 * 
	 * @return the disabled SSL cipher suites
	 * @since 1.5
	 */
	public String[] getDisabledCipherSuites() {
		return disabledCipherSuites;
	}

	/**
	 * Set the list of disabled SSL cipher suites.
	 * 
	 * <p>
	 * This list is treated as regular expressions, and applied <b>after</b> any
	 * configured {@link #setEnabledCipherSuites(String[])} expressions.
	 * </p>
	 * 
	 * @param disabledCipherSuites
	 *        a list of regular expressions for the SSL cipher suites to disable
	 * @since 1.5
	 */
	public void setDisabledCipherSuites(String[] disabledCipherSuites) {
		this.disabledCipherSuites = disabledCipherSuites;
	}

}
