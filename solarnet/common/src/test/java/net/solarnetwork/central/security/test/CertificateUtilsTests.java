/* ==================================================================
 * CertificateUtilsTests.java - 3/08/2023 11:51:54 am
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

package net.solarnetwork.central.security.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.BDDAssertions.as;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link CertificateUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CertificateUtilsTests {

	private static final Logger log = LoggerFactory.getLogger(CertificateUtilsTests.class);

	@Test
	public void extractRfc822San() throws Exception {
		// GIVEN
		final String pem = FileCopyUtils.copyToString(
				new InputStreamReader(getClass().getResourceAsStream("test-client-01.pem"), UTF_8));
		X509Certificate[] certs = new BCCertificateService().parsePKCS7CertificateChainString(pem);

		// WHEN
		String email = CertificateUtils.emailSubjectAlternativeName(certs[0]);

		log.debug("Got [{}] email from cert [{}]", email,
				certs[0].getSubjectX500Principal().getName(X500Principal.CANONICAL));

		// THEN
		then(email).as("Email SAN extracted from cert").isEqualTo("matt+proxytester@solarnetwork.net");
	}

	@Test
	public void canonicalSubjectDn() throws Exception {
		// GIVEN
		final String pem = FileCopyUtils.copyToString(
				new InputStreamReader(getClass().getResourceAsStream("test-client-01.pem"), UTF_8));
		X509Certificate[] certs = new BCCertificateService().parsePKCS7CertificateChainString(pem);

		// WHEN
		String dn = CertificateUtils.canonicalSubjectDn(certs[0]);

		// THEN
		then(dn).as("Canonical DN extracted from cert").isEqualTo(
				"emailAddress=matt\\+proxytester@solarnetwork.net,C=NZ,O=SolarNetwork,CN=SN Proxy Tester");
	}

	@Test
	public void serverKeyStore() throws Exception {
		// GIVEN
		Path certPath = Path.of(getClass().getResource("test-server-01.fullchain").toURI());
		Path keyPath = Path.of(getClass().getResource("test-server-01.key").toURI());

		// WHEN
		KeyStore result = CertificateUtils.serverKeyStore(certPath, keyPath, "server");

		// THEN
		then(result).as("Key store created").isNotNull();
		// @formatter:off
		then(asList(result.getCertificateChain("server")))
			.as("Server chain is available").asList()
			.element(0).as("First certificate is server")
			.extracting(cert -> {
				return ((X509Certificate) cert).getSubjectX500Principal();
			}, as(type(X500Principal.class)))
			.isEqualTo(new X500Principal("C=NZ,O=SolarNetwork,CN=der.solarnetworkdev.net"))
			;
		// @formatter:on
	}

}
