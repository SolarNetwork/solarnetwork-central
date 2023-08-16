/* ==================================================================
 * JdbcTrustedIssuerCertificateDaoTests.java - 5/08/2023 6:09:58 pm
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

package net.solarnetwork.central.dnp3.dao.jdbc.test;

import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.allTrustedIssuerCertificateData;
import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.newTrustedIssuerCertificate;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcTrustedIssuerCertificateDao;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;
import net.solarnetwork.central.dnp3.test.Dnp3TestUtils;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link JdbcTrustedIssuerCertificateDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcTrustedIssuerCertificateDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcTrustedIssuerCertificateDao dao;
	private Long userId;

	private TrustedIssuerCertificate last;

	@BeforeEach
	public void setup() {
		dao = new JdbcTrustedIssuerCertificateDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		TrustedIssuerCertificate conf = newTrustedIssuerCertificate(userId, "test-ca-01.pem");

		// WHEN
		UserStringCompositePK result = dao.create(userId, conf);

		// THEN
		List<Map<String, Object>> data = allTrustedIssuerCertificateData(jdbcTemplate);
		// @formatter:off
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row subject DN")
			.containsEntry("subject_dn", conf.getSubjectDn())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row expires")
			.containsEntry("expires", Timestamp.from(conf.getExpires()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row certificate data")
			.containsEntry("cert", conf.certificateData())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		TrustedIssuerCertificate result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		TrustedIssuerCertificate conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setCertificate(Dnp3TestUtils.certificatesFromResource("test-client-01.pem")[0]);

		UserStringCompositePK result = dao.save(conf);
		TrustedIssuerCertificate updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allTrustedIssuerCertificateData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allTrustedIssuerCertificateData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<TrustedIssuerCertificate> confs = new ArrayList<>(count);

		final BCCertificateService certService = new BCCertificateService();
		final KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException(e);
		}
		keyGen.initialize(2048, SecureRandom.getInstanceStrong());

		final String caDnTemplate = "CN=Test CA %d.%d, O=Solar Test CA";

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				KeyPair caKey = keyGen.generateKeyPair();
				X509Certificate caCert = certService.generateCertificationAuthorityCertificate(
						caDnTemplate.formatted(userId, i), caKey.getPublic(), caKey.getPrivate());
				TrustedIssuerCertificate conf = Dnp3JdbcTestUtils.newTrustedIssuerCertificate(userId,
						caCert);
				UserStringCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<TrustedIssuerCertificate> results = dao.findAll(userId, null);

		// THEN
		TrustedIssuerCertificate[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(TrustedIssuerCertificate[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

}
