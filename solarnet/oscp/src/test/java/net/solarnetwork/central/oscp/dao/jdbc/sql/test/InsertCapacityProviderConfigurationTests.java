/* ==================================================================
 * InsertCapacityProviderConfigurationTests.java - 12/08/2022 3:29:58 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.SQL_COMMENT;
import static net.solarnetwork.central.test.CommonTestUtils.equalToTextResource;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.sql.InsertCapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link InsertCapacityProviderConfiguration} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class InsertCapacityProviderConfigurationTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Mock
	private Connection con;

	@Mock
	private PreparedStatement stmt;

	@Captor
	private ArgumentCaptor<String> sqlCaptor;

	private void givenPrepStatement() throws SQLException {
		given(con.prepareStatement(any(), eq(Statement.RETURN_GENERATED_KEYS))).willReturn(stmt);
	}

	private CapacityProviderConfiguration createCapacityProviderConfiguration(Long userId) {
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), Instant.now());
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		return conf;
	}

	private void thenPrepStatement(PreparedStatement result, Long userId,
			CapacityProviderConfiguration conf) throws SQLException {
		Timestamp ts = Timestamp.from(conf.getCreated());
		int p = 0;
		then(result).should().setTimestamp(++p, ts);
		then(result).should().setTimestamp(++p, ts);
		then(result).should().setObject(++p, userId);
		if ( conf.getId().entityIdIsAssigned() ) {
			then(result).should().setObject(++p, conf.getId().getEntityId());
		}
		then(result).should().setBoolean(++p, conf.isEnabled());
		then(result).should().setObject(++p, conf.getFlexibilityProviderId());
		then(result).should().setInt(++p, conf.getRegistrationStatus().getCode());
		then(result).should().setString(++p, conf.getName());
		then(result).should().setString(++p, conf.getBaseUrl());
		if ( conf.getServiceProps() != null ) {
			then(result).should().setString(++p, JsonUtils.getJSONString(conf.getServiceProps(), "{}"));
		} else {
			then(result).should().setNull(++p, Types.VARCHAR);
		}
	}

	@Test
	public void sql() {
		// GIVEN
		CapacityProviderConfiguration conf = createCapacityProviderConfiguration(
				randomUUID().getMostSignificantBits());

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		String sql = new InsertCapacityProviderConfiguration(userId, conf).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("insert-capacity-provider-conf.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void prep() throws SQLException {
		// GIVEN
		CapacityProviderConfiguration conf = createCapacityProviderConfiguration(
				randomUUID().getMostSignificantBits());

		// GIVEN
		givenPrepStatement();

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		PreparedStatement result = new InsertCapacityProviderConfiguration(userId, conf)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"insert-capacity-provider-conf.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, userId, conf);
	}

	@Test
	public void assigned_sql() {
		// GIVEN
		CapacityProviderConfiguration conf = createCapacityProviderConfiguration(
				randomUUID().getMostSignificantBits())
						.copyWithId(new UserLongCompositePK(randomUUID().getMostSignificantBits(),
								randomUUID().getMostSignificantBits()));

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		String sql = new InsertCapacityProviderConfiguration(userId, conf).getSql();

		// THEN
		log.debug("Generated SQL:\n{}", sql);
		assertThat("SQL matches", sql, equalToTextResource("insert-capacity-provider-conf-assigned.sql",
				TestSqlResources.class, SQL_COMMENT));
	}

	@Test
	public void assigned_prep() throws SQLException {
		// GIVEN
		CapacityProviderConfiguration conf = createCapacityProviderConfiguration(
				randomUUID().getMostSignificantBits())
						.copyWithId(new UserLongCompositePK(randomUUID().getMostSignificantBits(),
								randomUUID().getMostSignificantBits()));

		// GIVEN
		givenPrepStatement();

		// WHEN
		Long userId = randomUUID().getMostSignificantBits();
		PreparedStatement result = new InsertCapacityProviderConfiguration(userId, conf)
				.createPreparedStatement(con);

		// THEN
		then(con).should().prepareStatement(sqlCaptor.capture(), eq(Statement.RETURN_GENERATED_KEYS));
		log.debug("Generated SQL:\n{}", sqlCaptor.getValue());
		assertThat("Generated SQL", sqlCaptor.getValue(), equalToTextResource(
				"insert-capacity-provider-conf-assigned.sql", TestSqlResources.class, SQL_COMMENT));
		assertThat("Statement returned", result, sameInstance(stmt));
		thenPrepStatement(result, userId, conf);
	}

}
