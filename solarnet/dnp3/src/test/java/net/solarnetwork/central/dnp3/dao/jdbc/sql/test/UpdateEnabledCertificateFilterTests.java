/* ==================================================================
 * UpdateEnabledCertificateFilterTests.java - 8/08/2023 8:46:45 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.dnp3.dao.BasicFilter;
import net.solarnetwork.central.dnp3.dao.jdbc.sql.UpdateEnabledCertificateFilter;

/**
 * Test cases for the {@link UpdateEnabledCertificateFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class UpdateEnabledCertificateFilterTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Mock
	private Array subjectDnsArray;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any())).willReturn(stmt);
	}

	private void givenSetSubjectDnsArrayParameter(String[] value) throws SQLException {
		given(con.createArrayOf(eq("text"), aryEq(value))).willReturn(subjectDnsArray);
	}

	private void verifyPrepStatement(PreparedStatement result, Long userId, BasicFilter filter,
			boolean enabled) throws SQLException {
		int p = 0;
		verify(result).setBoolean(++p, enabled);
		verify(result).setObject(++p, userId);
		if ( filter.hasCertificateCriteria() ) {
			if ( filter.getSubjectDns().length == 1 ) {
				verify(result).setObject(++p, filter.getSubjectDn());
			} else {
				verify(result).setArray(++p, subjectDnsArray);
			}
		}
		if ( filter.hasEnabledCriteria() ) {
			verify(result).setBoolean(++p, filter.getEnabled().booleanValue());
		}
	}

	@Test
	public void multi_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final boolean enabled = true;
		BasicFilter filter = new BasicFilter();
		filter.setSubjectDns(new String[] { "3", "4" });
		filter.setEnabled(false);

		givenPrepStatement();
		givenSetSubjectDnsArrayParameter(filter.getSubjectDns());

		// WHEN
		PreparedStatement result = new UpdateEnabledCertificateFilter("solardnp3.dnp3_ca_cert", userId,
				filter, enabled).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated")
				.is(matching(equalToTextResource("update-trusted-issuer-certificate-enabled.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, filter, enabled);
	}

	@Test
	public void single_prep() throws SQLException {
		// GIVEN
		final Long userId = UUID.randomUUID().getMostSignificantBits();
		final boolean enabled = true;
		BasicFilter filter = new BasicFilter();
		filter.setSubjectDns(new String[] { "3" });
		filter.setEnabled(false);

		givenPrepStatement();

		// WHEN
		PreparedStatement result = new UpdateEnabledCertificateFilter("solardnp3.dnp3_ca_cert", userId,
				filter, enabled).createPreparedStatement(con);

		// THEN
		verify(con).prepareStatement(sqlCaptor.capture());
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		then(sqlCaptor.getValue()).as("SQL generated")
				.is(matching(equalToTextResource("update-trusted-issuer-certificate-enabled-one.sql",
						TestSqlResources.class, SQL_COMMENT)));
		then(result).as("Connection statement returned").isSameAs(stmt);
		verifyPrepStatement(result, userId, filter, enabled);
	}

}
