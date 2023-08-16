/* ==================================================================
 * SimpleProxyConfigurationProviderTests.java - 3/08/2023 9:17:20 am
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

package net.solarnetwork.central.net.proxy.service.impl.test;

import static java.util.Arrays.asList;
import static net.solarnetwork.central.security.CertificateUtils.canonicalSubjectDn;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.net.proxy.domain.SimpleProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.service.DynamicPortRegistrar;
import net.solarnetwork.central.net.proxy.service.impl.SimplePrincipalMapping;
import net.solarnetwork.central.net.proxy.service.impl.SimpleProxyConfigurationProvider;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link SimpleProxyConfigurationProvider} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class SimpleProxyConfigurationProviderTests {

	@Mock
	private DynamicPortRegistrar portRegistrar;

	private static final String TEST_CA_DN = "CN=Test CA, O=Solar Test CA";
	private static final String TEST_DN = "CN=Test Client, O=Test Org";

	private KeyPairGenerator keyGen;
	private BCCertificateService certService;
	private KeyPair caKey;
	private X509Certificate caCert;
	private KeyStore trustStore;
	private Map<String, String> userMapping;

	private SimpleProxyConfigurationProvider service;

	@BeforeEach
	public void setup() throws Exception {
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException(e);
		}
		keyGen.initialize(2048, SecureRandom.getInstanceStrong());
		caKey = keyGen.generateKeyPair();
		certService = new BCCertificateService();
		caCert = certService.generateCertificationAuthorityCertificate(TEST_CA_DN, caKey.getPublic(),
				caKey.getPrivate());

		trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null);
		trustStore.setCertificateEntry("ca", caCert);

		userMapping = new LinkedHashMap<>(4);

		service = new SimpleProxyConfigurationProvider(portRegistrar,
				asList(new SimplePrincipalMapping(trustStore, userMapping)));
	}

	@Test
	public void acceptedIssuers() throws Exception {
		// WHEN
		Iterable<X509Certificate> certs = service.acceptedIdentityIssuers();

		// @formatter:off
		then(certs).as("Trust certificates returned")
			.hasSize(1)
			.asList()
			.element(0)
			.as("Trust certificate is CA certficate")
			.isEqualTo(caCert);
		// @formatter:on
	}

	@Test
	public void authorize() throws Exception {
		// GIVEN
		final String principal = UUID.randomUUID().toString();

		// generate a signed client certificate
		KeyPair clientKey = keyGen.generateKeyPair();
		X509Certificate clientCert = certService.generateCertificate(TEST_DN, clientKey.getPublic(),
				clientKey.getPrivate());
		String clientCsr = certService.generatePKCS10CertificateRequestString(clientCert,
				clientKey.getPrivate());
		X509Certificate clientSignedCert = certService.signCertificate(clientCsr, caCert,
				caKey.getPrivate());

		final String username = UUID.randomUUID().toString();
		userMapping.put(canonicalSubjectDn(clientSignedCert), username);

		// WHEN
		X509Certificate[] ident = new X509Certificate[] { clientSignedCert };
		SimpleProxyConnectionRequest req = new SimpleProxyConnectionRequest(principal, ident);
		ProxyConnectionSettings conf = service.authorize(req);

		// THEN
		// @formatter:off
		then(conf).as("Connection settings returned")
			.isNotNull()
			.as("Uses key store from supplier")
			.returns(trustStore, from(ProxyConnectionSettings::clientTrustStore))
			;
		// @formatter:on

		// just demonstrate verification of client certificate for testing purposes
		Enumeration<String> trustAliases = trustStore.aliases();
		boolean verified = false;
		while ( trustAliases.hasMoreElements() ) {
			String alias = trustAliases.nextElement();
			try {
				clientSignedCert.checkValidity();
				clientSignedCert.verify(trustStore.getCertificate(alias).getPublicKey());
				verified = true;
				break;
			} catch ( Exception e ) {
				// ignore
			}
		}
		then(verified).isTrue();
	}

	public void todo() {
		// GIVEN
		final int port = Math.abs((int) UUID.randomUUID().getMostSignificantBits());
		given(portRegistrar.reserveNewPort()).willReturn(port);

	}

}
