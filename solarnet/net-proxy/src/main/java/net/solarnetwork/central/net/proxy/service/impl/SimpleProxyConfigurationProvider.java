/* ==================================================================
 * SimpleProxyConfigurationProvider.java - 3/08/2023 6:13:18 am
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

package net.solarnetwork.central.net.proxy.service.impl;

import static net.solarnetwork.central.net.proxy.util.CertificateUtils.canonicalSubjectDn;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.domain.SimpleProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.service.ProxyConfigurationProvider;
import net.solarnetwork.central.net.proxy.util.CertificateUtils;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;

/**
 * Simple proxy configuration provider designed for testing.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleProxyConfigurationProvider implements ProxyConfigurationProvider {

	private static final Logger log = LoggerFactory.getLogger(SimpleProxyConfigurationProvider.class);

	private final DynamicPortRegistrar portRegistrar;
	private final List<SimplePrincipalMapping> userMappings;

	/**
	 * Constructor.
	 * 
	 * @param portRegistrar
	 *        the port registrar to use
	 * @param userMappings
	 *        the user mappings to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleProxyConfigurationProvider(DynamicPortRegistrar portRegistrar,
			List<SimplePrincipalMapping> userMappings) {
		super();
		this.portRegistrar = requireNonNullArgument(portRegistrar, "portRegistrar");
		this.userMappings = requireNonNullArgument(userMappings, "userMappings");
	}

	@Override
	public Boolean authorize(ProxyConnectionRequest request) throws AuthorizationException {
		final String ident = requestIdentity(request);
		if ( log.isDebugEnabled() ) {
			log.debug("Connection authorization request received for: [{}]", request.principal(), ident);
		}
		final X509Certificate[] clientChain = request.principalIdentity();

		for ( SimplePrincipalMapping mapping : userMappings ) {
			if ( mapping.subjectUserMapping().containsKey(ident) ) {
				KeyStore trustStore = mapping.trustStore();
				try {
					PKIXCertPathValidatorResult vr = CertificateUtils
							.validateCertificateChain(trustStore, clientChain);
					if ( log.isInfoEnabled() ) {
						TrustAnchor ta = vr.getTrustAnchor();
						log.info(
								"Validated connection authorization request identity [{}], trusted by [{}]",
								ident, ta.getTrustedCert().getSubjectX500Principal());
					}
					return Boolean.TRUE;
				} catch ( Exception e ) {
					throw new AuthorizationException(Reason.ACCESS_DENIED, ident, e);
				}
			}
		}
		return null;
	}

	@Override
	public ProxyConnectionSettings settingsForRequest(ProxyConnectionRequest request)
			throws AuthorizationException {
		final String ident = requestIdentity(request);
		if ( log.isDebugEnabled() ) {
			log.debug("Connection settings request received for [{}]", request.principal(), ident);
		}
		for ( SimplePrincipalMapping mapping : userMappings ) {
			if ( mapping.subjectUserMapping().containsKey(ident) ) {
				return new SimpleProxyConnectionSettings(mapping.trustStore());
			}
		}
		return null;
	}

	private String requestIdentity(ProxyConnectionRequest request) {
		final String ident = request.principalIdentity() != null
				&& request.principalIdentity().length > 0
						? canonicalSubjectDn(request.principalIdentity()[0])
						: request.principal();
		return ident;
	}

	@Override
	public Iterable<X509Certificate> acceptedIdentityIssuers() {
		List<X509Certificate> result = new ArrayList<>(8);
		for ( SimplePrincipalMapping mapping : userMappings ) {
			KeyStore keyStore = mapping.trustStore();
			try {
				Enumeration<String> aliases = keyStore.aliases();
				while ( aliases.hasMoreElements() ) {
					String alias = aliases.nextElement();
					Certificate cert = keyStore.getCertificate(alias);
					if ( cert instanceof X509Certificate x509 ) {
						result.add(x509);
					}
				}
			} catch ( KeyStoreException e ) {
				log.error("Error reading key store aliases from principal mapping: {}", e.toString());
			}
		}
		return result;
	}

}
