/* ==================================================================
 * JdbcCloudIntegrationConfigurationDaoTests.java - 2/10/2024 8:55:51 am
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

package net.solarnetwork.central.c2c.dao.jdbc.test;

import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudIntegrationConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudIntegrationConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcCloudIntegrationConfigurationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudIntegrationConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao dao;
	private Long userId;

	private CloudIntegrationConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudIntegrationConfiguration result = dao.entityKey(id);

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
		Map<String, Object> props = Collections.singletonMap("foo", "bar");
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), props);

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

		List<Map<String, Object>> data = allCloudIntegrationConfigurationData(jdbcTemplate);
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
			.as("Row name")
			.containsEntry("cname", conf.getName())
			.as("Row service ID")
			.containsEntry("sident", conf.getServiceIdentifier())
			.as("Row service properties")
			.hasEntrySatisfying("sprops", o -> {
				then(JsonUtils.getStringMap(o.toString()))
					.as("Row service props")
					.isEqualTo(props);
			})
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CloudIntegrationConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudIntegrationConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudIntegrationConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudIntegrationConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudIntegrationConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudIntegrationConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId,
						randomString(), randomString(), props);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CloudIntegrationConfiguration> results = dao.findAll(userId, null);

		// THEN
		CloudIntegrationConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId())).toArray(CloudIntegrationConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudIntegrationConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId,
						randomString(), randomString(), props);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		CloudIntegrationConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId())).toArray(CloudIntegrationConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

}
