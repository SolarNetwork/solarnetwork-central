/* ==================================================================
 * JdbcCredentialConfigurationDaoTests.java - 21/02/2024 7:43:26 am
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

package net.solarnetwork.central.inin.dao.jdbc.test;

import static net.solarnetwork.central.inin.dao.jdbc.test.InstructionInputJdbcTestUtils.allCredentialConfigurationData;
import static net.solarnetwork.central.inin.dao.jdbc.test.InstructionInputJdbcTestUtils.newCredentialConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.dao.BasicFilter;
import net.solarnetwork.central.inin.dao.jdbc.JdbcCredentialConfigurationDao;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcCredentialConfigurationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCredentialConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCredentialConfigurationDao dao;
	private Long userId;

	private CredentialConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCredentialConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CredentialConfiguration result = dao.entityKey(id);

		// @formatter:off
		then(result)
			.as("Entity for key returned")
			.isNotNull()
			.as("ID of entity from provided value")
			.returns(id, Entity::getId)
			;
		// @formatter:on
	}

	@Test
	public void insert() {
		// GIVEN
		CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
				randomString());
		conf.setExpires(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setOauth(true);

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserLongCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allCredentialConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row username")
			.containsEntry("username", conf.getUsername())
			.as("Row password")
			.containsEntry("password", conf.getPassword())
			.as("Row expires")
			.containsEntry("expires", Timestamp.from(conf.getExpires()))
			.as("OAuth flag")
			.containsEntry("oauth", true)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void insert_noPassword() {
		// GIVEN
		CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(), null);
		conf.setExpires(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setOauth(true);

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserLongCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allCredentialConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row username")
			.containsEntry("username", conf.getUsername())
			.as("Row password not present")
			.containsEntry("password", null)
			.as("Row expires")
			.containsEntry("expires", Timestamp.from(conf.getExpires()))
			.as("OAuth flag")
			.containsEntry("oauth", true)
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void insert_oauth_unique() {
		// GIVEN
		CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
				randomString());
		conf.setOauth(true);

		// another conf using same OAuth username
		Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);
		CredentialConfiguration conf2 = newCredentialConfiguration(userId2, conf.getUsername(),
				randomString());
		conf2.setOauth(true);

		// WHEN
		dao.create(userId, conf);

		// THEN
		thenThrownBy(() -> {
			dao.create(userId2, conf2);
		}).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	public void insert_notOauth_crossAccount() {
		// GIVEN
		CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
				randomString());
		conf.setOauth(false);

		// another conf using same OAuth username
		Long userId2 = CommonDbTestUtils.insertUser(jdbcTemplate);
		CredentialConfiguration conf2 = newCredentialConfiguration(userId2, conf.getUsername(),
				randomString());
		conf2.setOauth(false);

		// WHEN
		UserLongCompositePK result1 = dao.create(userId, conf);
		UserLongCompositePK result2 = dao.create(userId2, conf2);

		// THEN
		// @formatter:off
		then(result1)
			.as("First configuration created")
			.isNotNull()
			;
		then(result2)
			.as("Second configuration created")
			.isNotNull();
			;
		// @formatter:on
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CredentialConfiguration result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Retrieved entity matches source")
			.isEqualTo(last)
			.as("Same values returned as stored")
			.satisfies(c -> c.isSameAs(last))
			;
		// @formatter:on
	}

	@Test
	public void get_noPassword() {
		// GIVEN
		insert_noPassword();

		// WHEN
		CredentialConfiguration result = dao.get(last.getId());

		// THEN
		// @formatter:off
		then(result)
			.as("Retrieved entity matches source")
			.isEqualTo(last)
			.as("Same values returned as stored")
			.satisfies(c -> c.isSameAs(last))
			;
		// @formatter:on
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CredentialConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setUsername(randomString());
		conf.setPassword(randomString());
		conf.setExpires(Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setOauth(false);

		UserLongCompositePK result = dao.save(conf);
		CredentialConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCredentialConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(conf));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allCredentialConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CredentialConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
						randomString());
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CredentialConfiguration> results = dao.findAll(userId, null);

		// THEN
		CredentialConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(CredentialConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CredentialConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
						randomString());
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		FilterResults<CredentialConfiguration, UserLongCompositePK> results = dao.findFiltered(filter);

		// THEN
		CredentialConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(CredentialConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void updateEnabledStatus() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CredentialConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
						randomString());
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final CredentialConfiguration conf = confs.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setCredentialId(conf.getCredentialId());
		int result = dao.updateEnabledStatus(conf.getUserId(), filter, false);

		// THEN
		then(result).as("One row updated").isEqualTo(1);

		CredentialConfiguration updated = dao.get(conf.getId());
		then(updated).as("Enabled status updated").returns(false, CredentialConfiguration::isEnabled);
	}

	@Test
	public void updateEnabledStatus_forUser() throws Exception {
		// GIVEN
		final int count = 2;
		final int userCount = 2;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CredentialConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CredentialConfiguration conf = newCredentialConfiguration(userId, randomString(),
						randomString());
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		int result = dao.updateEnabledStatus(userId, null, false);

		// THEN
		then(result).as("All rows for user updated").isEqualTo(count);

		List<Map<String, Object>> data = allCredentialConfigurationData(jdbcTemplate);
		for ( var row : data ) {
			Long rowUserId = (Long) row.get("user_id");
			if ( rowUserId.equals(userId) ) {
				then(row).as("Updated enabled for user").containsEntry("enabled", false);
			} else {
				then(row).as("Did not update enabled for other user").containsEntry("enabled", true);

			}
		}
	}

}
