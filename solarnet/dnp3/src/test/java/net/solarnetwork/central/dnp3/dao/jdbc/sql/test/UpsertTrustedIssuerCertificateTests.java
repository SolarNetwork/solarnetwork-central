/* ==================================================================
 * UpsertTrustedIssuerCertificateTests.java - 5/08/2023 2:53:42 pm
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

package net.solarnetwork.central.dnp3.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.dnp3.test.Dnp3TestUtils.certificatesFromResource;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpsertTrustedIssuerCertificate;
import net.solarnetwork.central.dnp3.domain.TrustedIssuerCertificate;

/**
 * Test cases for the {@link UpsertTrustedIssuerCertificate} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpsertTrustedIssuerCertificateTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(Statement.NO_GENERATED_KEYS))).willReturn(stmt);
	}

	private void verifyPrepStatement(PreparedStatement result, Long userId,
			TrustedIssuerCertificate conf) throws SQLException {
		Timestamp ts = Timestamp.from(conf.getCreated());
		Timestamp mod = Timestamp.from(conf.getModified());
		int p = 0;
		verify(result).setTimestamp(++p, ts);
		verify(result).setTimestamp(++p, mod);
		verify(result).setObject(++p, userId);
		verify(result).setString(++p, conf.getSubjectDn());
		verify(result).setTimestamp(++p, Timestamp.from(conf.getExpires()));
		verify(result).setBoolean(++p, conf.isEnabled());
		verify(result).setBytes(++p, conf.certificateData());
	}

	@Test
	public void sql() {
		// GIVEN
		TrustedIssuerCertificate conf = new TrustedIssuerCertificate(
				randomUUID().getMostSignificantBits(), randomUUID().toString(), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setCertificate(certificatesFromResource("test-ca-01.pem")[0]);
		conf.setEnabled(true);

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		String sql = new UpsertTrustedIssuerCertificate(userId, conf).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		then(sql).as("SQL generated")
				.is(matching(equalToTextResource("upsert-trusted-issuer-certificate.sql",
						TestSqlResources.class, SQL_COMMENT)));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		TrustedIssuerCertificate conf = new TrustedIssuerCertificate(
				randomUUID().getMostSignificantBits(), randomUUID().toString(), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setCertificate(certificatesFromResource("test-ca-01.pem")[0]);
		conf.setEnabled(true);

		// GIVEN
		givenPrepStatement();

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		PreparedStatement result = new UpsertTrustedIssuerCertificate(userId, conf)
				.createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture(), eq(Statement.NO_GENERATED_KEYS));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated")
				.is(matching(equalToTextResource("upsert-trusted-issuer-certificate.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, conf);
	}

}
