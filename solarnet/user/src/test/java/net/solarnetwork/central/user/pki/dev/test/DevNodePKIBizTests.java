/* ==================================================================
 * DevNodePKIBizTests.java - Jan 25, 2015 8:36:15 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.pki.dev.test;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.user.pki.dev.DevNodePKIBiz;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DevNodePKIBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DevNodePKIBizTests {

	private BCCertificateService certificateService;
	private DevNodePKIBiz biz;

	@Before
	public void setup() {
		biz = new DevNodePKIBiz();

		certificateService = new BCCertificateService();
		certificateService.setCertificateExpireDays(365);
		certificateService.setSignatureAlgorithm("SHA256WithRSA");
		biz.setCertificateService(certificateService);
		biz.setCaService(certificateService);

		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, -1L, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", "ROLE_USER");
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private X509Certificate createSelfSignedCertificate(String dn, KeyPair keypair)
			throws GeneralSecurityException {
		PublicKey publicKey = keypair.getPublic();
		PrivateKey privateKey = keypair.getPrivate();
		Certificate cert = certificateService.generateCertificate(dn, publicKey, privateKey);
		return (X509Certificate) cert;
	}

	@Test
	public void submitCSR() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		X509Certificate certificate = createSelfSignedCertificate("UID=1111,O=SolarNetworkDev", keypair);
		String reqID = biz.submitCSR(certificate, keypair.getPrivate());
		Assert.assertNotNull("CSR request ID", reqID);
	}

	@Test
	public void approveCSR() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		X509Certificate certificate = createSelfSignedCertificate("UID=1111,O=SolarNetworkDev", keypair);
		String reqID = biz.submitCSR(certificate, keypair.getPrivate());
		Assert.assertNotNull("CSR request ID", reqID);
		X509Certificate[] result = biz.approveCSR(reqID);
		Assert.assertNotNull("X.509 certificate", result);
	}

	@Test
	public void initCA() throws Exception {
		biz.serviceDidStartup();

		File webserverKeyStoreFile = new File(biz.getBaseDir(), "central.jks");
		Assert.assertTrue("Webserver KeyStore exists", webserverKeyStoreFile.canRead());

		File nodeTrustKeyStoreFile = new File(biz.getBaseDir(), "central-trust.jks");
		Assert.assertTrue("Node trust  KeyStore exists", nodeTrustKeyStoreFile.canRead());
	}

}
