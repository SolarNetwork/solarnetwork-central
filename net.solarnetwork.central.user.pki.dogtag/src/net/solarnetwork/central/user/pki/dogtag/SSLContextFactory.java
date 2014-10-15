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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.SSLContext;
import net.solarnetwork.support.CertificateException;
import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * Factory for {@link SSLContext} objects configured with an associated
 * key/trust store.
 * 
 * @author matt
 * @version 1.0
 */
public class SSLContextFactory {

	private Resource keystoreResource;
	private String keystorePassword;

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
			ctx = SSLContexts.custom().useTLS().loadTrustMaterial(keyStore)
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
		LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(createContext());
		org.apache.http.config.Registry<ConnectionSocketFactory> r = RegistryBuilder
				.<ConnectionSocketFactory> create().register("https", sslsf).build();

		HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(r);
		HttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();

		HttpComponentsClientHttpRequestFactory reqFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		return new RestTemplate(reqFactory);
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
			throw new CertificateException(msg
					+ (keystoreResource == null ? null : keystoreResource.getFilename()), e);
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

}
