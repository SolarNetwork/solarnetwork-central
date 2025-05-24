/* ==================================================================
 * DogtagPKIBizTests.java - Oct 14, 2014 7:35:10 AM
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

package net.solarnetwork.central.user.pki.dogtag.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.user.pki.dogtag.DogtagPKIBiz;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@Link DogtagPKIBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration
@SpringBootTest
@EnabledIfSystemProperty(named = "test.dogtag", matches = ".*")
public class DogtagPKIBizTests extends AbstractJUnit4SpringContextTests {

	@Value("${dogtag.baseUrl}")
	private String baseUrl;

	@Autowired
	private RestOperations restOperations;

	private BCCertificateService certificateService;
	private DogtagPKIBiz biz;

	@BeforeEach
	public void setup() throws Exception {
		biz = new DogtagPKIBiz();
		biz.setBaseUrl(baseUrl);

		certificateService = new BCCertificateService();
		certificateService.setCertificateExpireDays(365);
		certificateService.setSignatureAlgorithm("SHA256WithRSA");
		biz.setCertificateService(certificateService);

		biz.setRestOps(restOperations);

		User userDetails = new User("test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, -1L, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", "ROLE_USER");
		SecurityContextHolder.getContext().setAuthentication(auth);

		// detect version
		biz.performPingTest();
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
		then(reqID).as("CSR request ID").isNotNull();
	}

	@Test
	public void approveCSR() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		X509Certificate certificate = createSelfSignedCertificate("UID=1111,O=SolarNetworkDev", keypair);
		String reqID = biz.submitCSR(certificate, keypair.getPrivate());
		then(reqID).as("CSR request ID").isNotNull();
		X509Certificate[] result = biz.approveCSR(reqID);
		then(result).as("X.509 certificate").isNotNull();
	}

	@Test
	public void submitRenewal() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		X509Certificate certificate = createSelfSignedCertificate("UID=1111,O=SolarNetworkDev", keypair);
		String reqID = biz.submitCSR(certificate, keypair.getPrivate());
		then(reqID).as("CSR request ID").isNotNull();
		X509Certificate[] result = biz.approveCSR(reqID);
		then(result).as("X.509 certificate").isNotNull();

		// request renew
		String renewRequestID = biz.submitRenewalRequest(result[0]);
		then(renewRequestID).isNotNull();
	}

	@Test
	public void approveRenewal() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		X509Certificate certificate = createSelfSignedCertificate("UID=1111,O=SolarNetworkDev", keypair);
		String reqID = biz.submitCSR(certificate, keypair.getPrivate());
		then(reqID).as("CSR request ID").isNotNull();
		X509Certificate[] result = biz.approveCSR(reqID);
		then(result).as("X.509 certificate").isNotNull();

		// request renew
		String renewRequestID = biz.submitRenewalRequest(result[0]);
		then(renewRequestID).isNotNull();

		// approve renew
		X509Certificate[] renewed = biz.approveCSR(renewRequestID);
		then(renewed).as("X.509 certificate").isNotNull();

		// validate renewed subject the same, and has larger serial numberd
		then(renewed[0].getSubjectX500Principal()).isEqualTo(result[0].getSubjectX500Principal());
		then(renewed[0].getSerialNumber()).as("Renewed serial number larger than original")
				.isGreaterThan(result[0].getSerialNumber());
	}

}
