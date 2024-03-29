/* ==================================================================
 * JdbcCredentialAuthorizationDaoTests.java - 23/02/2024 3:01:12 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.security.jdbc.test;

import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.newEndpointAuthConfiguration;
import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.newEndpointConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.din.dao.jdbc.JdbcCredentialConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.security.EndpointUserDetails;
import net.solarnetwork.central.din.security.jdbc.JdbcCredentialAuthorizationDao;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcCredentialAuthorizationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCredentialAuthorizationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private Long userId;
	private JdbcCredentialConfigurationDao credentialDao;
	private JdbcEndpointConfigurationDao endpointDao;
	private JdbcEndpointAuthConfigurationDao endpointAuthDao;

	private JdbcCredentialAuthorizationDao dao;

	@BeforeEach
	public void setup() {
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		credentialDao = new JdbcCredentialConfigurationDao(jdbcTemplate);
		endpointDao = new JdbcEndpointConfigurationDao(jdbcTemplate);
		endpointAuthDao = new JdbcEndpointAuthConfigurationDao(jdbcTemplate);

		dao = new JdbcCredentialAuthorizationDao(jdbcTemplate);
	}

	@Test
	public void found() {
		// GIVEN
		CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
				randomString(), randomString());
		cred = credentialDao.get(credentialDao.save(cred));
		EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
				randomString(), randomLong(), randomString(), null);
		endpoint = endpointDao.get(endpointDao.save(endpoint));

		EndpointAuthConfiguration auth = newEndpointAuthConfiguration(userId, endpoint.getEndpointId(),
				cred.getCredentialId());
		auth = endpointAuthDao.get(endpointAuthDao.create(userId, endpoint.getEndpointId(), auth));

		// WHEN
		EndpointUserDetails result = dao.credentialsForEndpoint(endpoint.getEndpointId(),
				cred.getUsername());

		// THEN

		// @formatter:off
		then(result).as("User found")
			.isNotNull()
			.as("User ID provided")
			.returns(userId, EndpointUserDetails::getUserId)
			.as("Endpoint ID provided")
			.returns(endpoint.getEndpointId(), EndpointUserDetails::getEndpointId)
			.as("Username provided")
			.returns(cred.getUsername(), EndpointUserDetails::getUsername)
			.as("User enabled")
			.returns(true, EndpointUserDetails::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void disabled_cred() {
		// GIVEN
		CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
				randomString(), randomString());
		cred.setEnabled(false);
		cred = credentialDao.get(credentialDao.save(cred));
		EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
				randomString(), randomLong(), randomString(), null);
		endpoint = endpointDao.get(endpointDao.save(endpoint));

		EndpointAuthConfiguration auth = newEndpointAuthConfiguration(userId, endpoint.getEndpointId(),
				cred.getCredentialId());
		auth = endpointAuthDao.get(endpointAuthDao.create(userId, endpoint.getEndpointId(), auth));

		// WHEN
		EndpointUserDetails result = dao.credentialsForEndpoint(endpoint.getEndpointId(),
				cred.getUsername());

		// THEN

		// @formatter:off
		then(result).as("User found")
			.isNotNull()
			.as("User ID provided")
			.returns(userId, EndpointUserDetails::getUserId)
			.as("Endpoint ID provided")
			.returns(endpoint.getEndpointId(), EndpointUserDetails::getEndpointId)
			.as("Username provided")
			.returns(cred.getUsername(), EndpointUserDetails::getUsername)
			.as("User not enabled because credential disabled")
			.returns(false, EndpointUserDetails::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void disabled_endpoint() {
		// GIVEN
		CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
				randomString(), randomString());
		cred = credentialDao.get(credentialDao.save(cred));
		EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
				randomString(), randomLong(), randomString(), null);
		endpoint.setEnabled(false);
		endpoint = endpointDao.get(endpointDao.save(endpoint));

		EndpointAuthConfiguration auth = newEndpointAuthConfiguration(userId, endpoint.getEndpointId(),
				cred.getCredentialId());
		auth = endpointAuthDao.get(endpointAuthDao.create(userId, endpoint.getEndpointId(), auth));

		// WHEN
		EndpointUserDetails result = dao.credentialsForEndpoint(endpoint.getEndpointId(),
				cred.getUsername());

		// THEN

		// @formatter:off
		then(result).as("User found")
			.isNotNull()
			.as("User ID provided")
			.returns(userId, EndpointUserDetails::getUserId)
			.as("Endpoint ID provided")
			.returns(endpoint.getEndpointId(), EndpointUserDetails::getEndpointId)
			.as("Username provided")
			.returns(cred.getUsername(), EndpointUserDetails::getUsername)
			.as("User not enabled because endpoint disabled")
			.returns(false, EndpointUserDetails::isEnabled)
			;
		// @formatter:on
	}

	@Test
	public void disabled_auth() {
		// GIVEN
		CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
				randomString(), randomString());
		cred = credentialDao.get(credentialDao.save(cred));
		EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
				randomString(), randomLong(), randomString(), null);
		endpoint = endpointDao.get(endpointDao.save(endpoint));

		EndpointAuthConfiguration auth = newEndpointAuthConfiguration(userId, endpoint.getEndpointId(),
				cred.getCredentialId());
		auth.setEnabled(false);
		auth = endpointAuthDao.get(endpointAuthDao.create(userId, endpoint.getEndpointId(), auth));

		// WHEN
		EndpointUserDetails result = dao.credentialsForEndpoint(endpoint.getEndpointId(),
				cred.getUsername());

		// THEN

		// @formatter:off
		then(result).as("User found")
			.isNotNull()
			.as("User ID provided")
			.returns(userId, EndpointUserDetails::getUserId)
			.as("Endpoint ID provided")
			.returns(endpoint.getEndpointId(), EndpointUserDetails::getEndpointId)
			.as("Username provided")
			.returns(cred.getUsername(), EndpointUserDetails::getUsername)
			.as("User not enabled because endpoint auth disabled")
			.returns(false, EndpointUserDetails::isEnabled)
			;
		// @formatter:on
	}

}
