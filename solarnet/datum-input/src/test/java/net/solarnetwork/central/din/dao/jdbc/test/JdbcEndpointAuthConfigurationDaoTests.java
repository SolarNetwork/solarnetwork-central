/* ==================================================================
 * JdbcEndpointAuthConfigurationDaoTests.java - 21/02/2024 4:23:06 pm
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

package net.solarnetwork.central.din.dao.jdbc.test;

import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.allEndpointAuthConfigurationData;
import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.newEndpointAuthConfiguration;
import static net.solarnetwork.central.din.dao.jdbc.test.DinJdbcTestUtils.newEndpointConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.din.dao.BasicFilter;
import net.solarnetwork.central.din.dao.jdbc.JdbcCredentialConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointAuthConfigurationDao;
import net.solarnetwork.central.din.dao.jdbc.JdbcEndpointConfigurationDao;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcEndpointConfigurationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcEndpointAuthConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcEndpointAuthConfigurationDao dao;
	private Long userId;
	private JdbcCredentialConfigurationDao credentialDao;
	private JdbcEndpointConfigurationDao endpointDao;

	private EndpointAuthConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcEndpointAuthConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		credentialDao = new JdbcCredentialConfigurationDao(jdbcTemplate);
		endpointDao = new JdbcEndpointConfigurationDao(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
				randomString(), randomString());
		cred = credentialDao.get(credentialDao.save(cred));
		EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
				randomString(), randomLong(), randomString(), null);
		endpoint = endpointDao.get(endpointDao.save(endpoint));

		EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId, endpoint.getEndpointId(),
				cred.getCredentialId());

		// WHEN
		UserUuidLongCompositePK result = dao.create(userId, endpoint.getEndpointId(), conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserUuidLongCompositePK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserUuidLongCompositePK::getGroupId)
			;

		List<Map<String, Object>> data = allEndpointAuthConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row endpoint ID")
			.containsEntry("endpoint_id", endpoint.getEndpointId())
			.as("Row credential ID")
			.containsEntry("cred_id", cred.getCredentialId())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		EndpointAuthConfiguration result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Retrieved entity matches source")
			.isEqualTo(last)
			.as("Entity values retrieved")
			.matches(c -> c.isSameAs(last))
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		EndpointAuthConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));

		UserUuidLongCompositePK result = dao.save(conf);
		EndpointAuthConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allEndpointAuthConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allEndpointAuthConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointAuthConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
						randomString(), randomString());
				cred = credentialDao.get(credentialDao.save(cred));
				EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
						randomString(), randomLong(), randomString(), null);
				endpoint = endpointDao.get(endpointDao.save(endpoint));

				EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId,
						endpoint.getEndpointId(), cred.getCredentialId());
				UserUuidLongCompositePK id = dao.create(userId, endpoint.getEndpointId(), conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final EndpointAuthConfiguration group = confs.get(5);
		Collection<EndpointAuthConfiguration> results = dao.findAll(group.getUserId(),
				group.getEndpointId(), null);

		// THEN
		EndpointAuthConfiguration[] expected = confs.stream()
				.filter(e -> group.getUserId().equals(e.getUserId())
						&& group.getEndpointId().equals(e.getEndpointId()))
				.toArray(EndpointAuthConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointAuthConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
						randomString(), randomString());
				cred = credentialDao.get(credentialDao.save(cred));
				EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
						randomString(), randomLong(), randomString(), null);
				endpoint = endpointDao.get(endpointDao.save(endpoint));

				EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId,
						endpoint.getEndpointId(), cred.getCredentialId());
				UserUuidLongCompositePK id = dao.create(userId, endpoint.getEndpointId(), conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		FilterResults<EndpointAuthConfiguration, UserUuidLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		EndpointAuthConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(EndpointAuthConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void updateEnabledStatus() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointAuthConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
						randomString(), randomString());
				cred = credentialDao.get(credentialDao.save(cred));
				EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
						randomString(), randomLong(), randomString(), null);
				endpoint = endpointDao.get(endpointDao.save(endpoint));

				EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId,
						endpoint.getEndpointId(), cred.getCredentialId());
				UserUuidLongCompositePK id = dao.create(userId, endpoint.getEndpointId(), conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final EndpointAuthConfiguration conf = confs.get(3);
		final BasicFilter filter = new BasicFilter();
		filter.setEndpointId(conf.getEndpointId());
		filter.setCredentialId(conf.getCredentialId());
		int result = dao.updateEnabledStatus(conf.getUserId(), filter, false);

		// THEN
		then(result).as("One row updated").isEqualTo(1);

		EndpointAuthConfiguration updated = dao.get(conf.getId());
		then(updated).as("Enabled status updated").returns(false, EndpointAuthConfiguration::isEnabled);
	}

	@Test
	public void updateEnabledStatus_forUser() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointAuthConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
						randomString(), randomString());
				cred = credentialDao.get(credentialDao.save(cred));
				EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
						randomString(), randomLong(), randomString(), null);
				endpoint = endpointDao.get(endpointDao.save(endpoint));

				EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId,
						endpoint.getEndpointId(), cred.getCredentialId());
				UserUuidLongCompositePK id = dao.create(userId, endpoint.getEndpointId(), conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		int result = dao.updateEnabledStatus(userId, null, false);

		// THEN
		then(result).as("All rows for user updated").isEqualTo(count);

		List<Map<String, Object>> data = allEndpointAuthConfigurationData(jdbcTemplate);
		for ( var row : data ) {
			Long rowUserId = (Long) row.get("user_id");
			if ( rowUserId.equals(userId) ) {
				then(row).as("Updated enabled for user").containsEntry("enabled", false);
			} else {
				then(row).as("Did not update enabled for other user").containsEntry("enabled", true);

			}
		}
	}

	@Test
	public void updateEnabledStatus_forUserEndpoint() throws Exception {
		// GIVEN
		final int count = 2;
		final int endpointCount = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<EndpointAuthConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId;
			userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int e = 0; e < endpointCount; e++ ) {
				EndpointConfiguration endpoint = newEndpointConfiguration(userId, UUID.randomUUID(),
						randomString(), randomLong(), randomString(), null);
				endpoint = endpointDao.get(endpointDao.save(endpoint));

				for ( int a = 0; a < count; a++ ) {
					CredentialConfiguration cred = DinJdbcTestUtils.newCredentialConfiguration(userId,
							randomString(), randomString());
					cred = credentialDao.get(credentialDao.save(cred));

					EndpointAuthConfiguration conf = newEndpointAuthConfiguration(userId,
							endpoint.getEndpointId(), cred.getCredentialId());
					UserUuidLongCompositePK id = dao.create(userId, endpoint.getEndpointId(), conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final EndpointAuthConfiguration conf = confs.get(3);
		final BasicFilter filter = new BasicFilter();
		filter.setEndpointId(conf.getEndpointId());
		int result = dao.updateEnabledStatus(conf.getUserId(), filter, false);

		// THEN
		then(result).as("All rows for user endpoint updated").isEqualTo(count);

		List<Map<String, Object>> data = allEndpointAuthConfigurationData(jdbcTemplate);
		for ( var row : data ) {
			Long rowUserId = (Long) row.get("user_id");
			UUID rowEndpointId = (UUID) row.get("endpoint_id");
			if ( rowUserId.equals(conf.getUserId()) && rowEndpointId.equals(conf.getEndpointId()) ) {
				then(row).as("Updated enabled for user endpoint").containsEntry("enabled", false);
			} else {
				then(row).as("Did not update enabled for other user endpoint").containsEntry("enabled",
						true);

			}
		}
	}

}
