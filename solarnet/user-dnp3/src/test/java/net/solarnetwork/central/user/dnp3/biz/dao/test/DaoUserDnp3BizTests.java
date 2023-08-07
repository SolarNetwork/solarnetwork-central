/* ==================================================================
 * DaoUserDnp3BizTests.java - 7/08/2023 12:44:07 pm
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

package net.solarnetwork.central.user.dnp3.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.dnp3.dao.ServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerControlConfigurationDao;
import net.solarnetwork.central.dnp3.dao.ServerMeasurementConfigurationDao;
import net.solarnetwork.central.dnp3.dao.TrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.security.CertificateUtils;
import net.solarnetwork.central.user.dnp3.biz.dao.DaoUserDnp3Biz;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DaoUserDnp3Biz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoUserDnp3BizTests {

	private static final String TEST_CA_DN = "CN=Test CA, OU=CA, O=Test Org";
	private static final String TEST_DN_TEMPLATE = "CN=%s %d, O=Test Org";

	@Mock
	private TrustedIssuerCertificateDao trustedCertDao;

	@Mock
	private ServerConfigurationDao serverDao;

	@Mock
	private ServerAuthConfigurationDao serverAuthDao;

	@Mock
	private ServerMeasurementConfigurationDao serverMeasurementDao;

	@Mock
	private ServerControlConfigurationDao serverControlDao;

	@Captor
	private ArgumentCaptor<TrustedIssuerCertificate> trustedCertCaptor;

	private KeyPairGenerator keyGen;
	private BCCertificateService certService;
	private KeyPair caKey;
	private X509Certificate caCert;

	private DaoUserDnp3Biz service;

	@BeforeEach
	public void setup() {
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException(e);
		}
		keyGen.initialize(2048, new SecureRandom());
		caKey = keyGen.generateKeyPair();
		certService = new BCCertificateService();
		caCert = certService.generateCertificationAuthorityCertificate(TEST_CA_DN, caKey.getPublic(),
				caKey.getPrivate());

		service = new DaoUserDnp3Biz(trustedCertDao, serverDao, serverAuthDao, serverMeasurementDao,
				serverControlDao);
	}

	private X509Certificate[] generateCertificates(String name, int start, int count) {
		X509Certificate[] result = new X509Certificate[count];
		for ( int i = start, max = start + count; i < max; i++ ) {
			KeyPair clientKey = keyGen.generateKeyPair();
			X509Certificate clientCert = certService.generateCertificate(
					TEST_DN_TEMPLATE.formatted(name, i), clientKey.getPublic(), clientKey.getPrivate());
			String clientCsr = certService.generatePKCS10CertificateRequestString(clientCert,
					clientKey.getPrivate());
			result[i - start] = certService.signCertificate(clientCsr, caCert, caKey.getPrivate());

		}
		return result;
	}

	@Test
	public void importCerts() {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final int count = 3;
		final X509Certificate[] certs = generateCertificates("Test Client", 1, count);

		given(trustedCertDao.save(any())).will(i -> {
			return i.getArgument(0, TrustedIssuerCertificate.class).getId();
		});

		// WHEN
		Collection<TrustedIssuerCertificate> result = service.saveTrustedIssuerCertificates(userId,
				certs);

		// THEN
		// @formatter:off
		then(result).as("Results returned").hasSize(count)
			.extracting(TrustedIssuerCertificate::getSubjectDn)
			.as("Subject DNs from import certificates")
			.contains(Arrays.stream(certs).map(CertificateUtils::canonicalSubjectDn).toArray(String[]::new))
			;

		then(result).extracting(TrustedIssuerCertificate::getUserId)
			.as("User ID set")
			.containsExactly(userId, userId, userId)
			;

		verify(trustedCertDao, times(3)).save(trustedCertCaptor.capture());

		then(trustedCertCaptor.getAllValues())
			.as("Persisted certificates same as returned")
			.containsExactlyElementsOf(result);
		// @formatter:on
	}

}
