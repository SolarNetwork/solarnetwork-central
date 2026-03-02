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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
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
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.reactor.ssl.SSLBufferMode;
import org.apache.hc.core5.ssl.SSLContexts;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.web.jakarta.support.LoggingHttpRequestInterceptor;

/**
 * Factory for {@link SSLContext} objects configured with an associated
 * key/trust store.
 *
 * @author matt
 * @version 2.2
 */
public class SSLContextFactory implements PingTest {

	private final Resource keystoreResource;
	private final String keystorePassword;

	private int trustedCertificateExpireWarningDays = 30;
	private String @Nullable [] enabledProtocols;
	private String @Nullable [] disabledProtocols;
	private String @Nullable [] enabledCipherSuites;
	private String @Nullable [] disabledCipherSuites;

	private @Nullable CachedResult<PingTest.Result> cachedResult;

	/**
	 * Constructor.
	 * 
	 * @param keystoreResource
	 *        the resource
	 * @param keystorePassword
	 *        the password
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SSLContextFactory(Resource keystoreResource, String keystorePassword) {
		super();
		this.keystoreResource = requireNonNullArgument(keystoreResource, "keystoreResource");
		this.keystorePassword = requireNonNullArgument(keystorePassword, "keystorePassword");
	}

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

		DefaultClientTlsStrategy sslstg = new DefaultClientTlsStrategy(createContext(), protocols,
				ciphers, SSLBufferMode.STATIC, new DefaultHostnameVerifier());

		HttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setTlsSocketStrategy(sslstg).build();

		HttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();

		ClientHttpRequestFactory reqFactory = LoggingHttpRequestInterceptor
				.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

		RestTemplate restTemplate = new RestTemplate(reqFactory);

		if ( LoggingHttpRequestInterceptor.supportsLogging(reqFactory) ) {
			restTemplate.getInterceptors().add(new LoggingHttpRequestInterceptor());
		}

		// Spring 6 no longer includes SourceHttpMessageConverter by default, but we rely on that
		if ( restTemplate.getMessageConverters().stream()
				.filter(c -> c instanceof SourceHttpMessageConverter<?>).findAny().isEmpty() ) {
			restTemplate.getMessageConverters().addFirst(new SourceHttpMessageConverter<>());
		}

		return restTemplate;
	}

	private KeyStore loadKeyStore() throws GeneralSecurityException {
		KeyStore keyStore;
		try (InputStream in = keystoreResource.getInputStream()) {
			keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(in, keystorePassword.toCharArray());
			return keyStore;
		} catch ( IOException e ) {
			String msg;
			if ( e.getCause() instanceof UnrecoverableKeyException ) {
				msg = "Invalid password loading key store ";
			} else {
				msg = "Error loading certificate key store ";
			}
			throw new CertificateException(msg + keystoreResource.getFilename(), e);
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

	@SuppressWarnings("JavaUtilDate")
	@Override
	public PingTest.Result performPingTest() throws Exception {
		final CachedResult<Result> cachedResult = this.cachedResult;
		if ( cachedResult != null && cachedResult.isValid() ) {
			return ObjectUtils.nonnull(cachedResult.getResult(), "Cached result");
		}
		if ( keystoreResource == null ) {
			return new PingTestResult(false, "No keystore configured");
		}
		if ( keystorePassword == null ) {
			return new PingTestResult(false, "No keystore password configured");
		}
		final long now = System.currentTimeMillis();
		final long warnDaysMills = TimeUnit.DAYS.toMillis(trustedCertificateExpireWarningDays);
		KeyStore keyStore = loadKeyStore();
		Enumeration<String> aliases = keyStore.aliases();
		PingTestResult result = null;
		boolean validated = false;
		StringBuilder message = new StringBuilder();
		while ( aliases.hasMoreElements() ) {
			String alias = aliases.nextElement();
			Entry entry;
			try {
				entry = keyStore.getEntry(alias, null);
			} catch ( UnrecoverableKeyException e ) {
				entry = keyStore.getEntry(alias, new KeyStore.PasswordProtection(
						keystorePassword == null ? null : keystorePassword.toCharArray()));
			}
			if ( entry instanceof PrivateKeyEntry keyEntry ) {
				Certificate[] certs = keyEntry.getCertificateChain();
				for ( Certificate cert : certs ) {
					if ( cert instanceof X509Certificate x509 ) {
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
						if ( x509.getNotAfter().getTime() - warnDaysMills < now ) {
							result = new PingTestResult(false,
									"Certificate " + x509.getSubjectX500Principal().getName()
											+ " will exipre on " + x509.getNotAfter() + ".");
							break;
						}
						if ( !message.isEmpty() ) {
							message.append(" ");
						}
						message.append("Certificate ").append(x509.getSubjectX500Principal().getName())
								.append(" valid until ").append(x509.getNotAfter()).append(".");
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
		CachedResult<PingTest.Result> cached = new CachedResult<>(result,
				(result.isSuccess() ? 1L : 30L),
				(result.isSuccess() ? TimeUnit.DAYS : TimeUnit.MINUTES));
		this.cachedResult = cached;
		return result;
	}

	public final Resource getKeystoreResource() {
		return keystoreResource;
	}

	public final int getTrustedCertificateExpireWarningDays() {
		return trustedCertificateExpireWarningDays;
	}

	public final void setTrustedCertificateExpireWarningDays(int trustedCertificateExpireWarningDays) {
		this.trustedCertificateExpireWarningDays = trustedCertificateExpireWarningDays;
	}

	/**
	 * Get the list of explicitly enabled SSL protocols.
	 *
	 * @return the enabled SSL protocols
	 * @since 1.5
	 */
	public final String @Nullable [] getEnabledProtocols() {
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
	public final void setEnabledProtocols(String @Nullable [] enabledProtocols) {
		this.enabledProtocols = enabledProtocols;
	}

	/**
	 * Get the list of disabled SSL protocols.
	 *
	 * @return the disabled SSL protocols
	 * @since 1.5
	 */
	public final String @Nullable [] getDisabledProtocols() {
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
	public final void setDisabledProtocols(String @Nullable [] disabledProtocols) {
		this.disabledProtocols = disabledProtocols;
	}

	/**
	 * Get the list of explicitly enabled SSL cipher suites.
	 *
	 * @return the enabled SSL cipher suites
	 * @since 1.5
	 */
	public final String @Nullable [] getEnabledCipherSuites() {
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
	public final void setEnabledCipherSuites(String @Nullable [] enabledCipherSuites) {
		this.enabledCipherSuites = enabledCipherSuites;
	}

	/**
	 * Get the list of disabled SSL cipher suites.
	 *
	 * @return the disabled SSL cipher suites
	 * @since 1.5
	 */
	public final String @Nullable [] getDisabledCipherSuites() {
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
	public final void setDisabledCipherSuites(String @Nullable [] disabledCipherSuites) {
		this.disabledCipherSuites = disabledCipherSuites;
	}

}
