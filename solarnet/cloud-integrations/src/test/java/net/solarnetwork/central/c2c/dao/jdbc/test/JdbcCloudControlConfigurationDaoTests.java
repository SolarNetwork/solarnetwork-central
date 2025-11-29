/* ==================================================================
 * JdbcCloudControlConfigurationDaoTests.java - 3/11/2025 8:11:27 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudControlConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudControlConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
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
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcCloudControlConfigurationDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudControlConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudControlConfigurationDao dao;
	private Long userId;

	private CloudControlConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudControlConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudControlConfiguration result = dao.entityKey(id);

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
		final CloudIntegrationConfiguration integration = createIntegration(userId,
				singletonMap("bim", "bam"));

		Map<String, Object> props = singletonMap("foo", "bar");
		// @formatter:off
		CloudControlConfiguration conf = newCloudControlConfiguration(userId,
				integration.getConfigId(),
				randomLong(),
				randomString(),
				randomString(),
				randomString(),
				randomString(),
				props)
				;
		// @formatter:on

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

		List<Map<String, Object>> data = allCloudControlConfigurationData(jdbcTemplate);
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
			.as("Row integration ID")
			.containsEntry("int_id", conf.getIntegrationId())
			.as("Row node ID")
			.containsEntry("node_id", conf.getNodeId())
			.as("Row control ID")
			.containsEntry("control_id", conf.getControlId())
			.as("Row control reference")
			.containsEntry("cref", conf.getControlReference())
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
		CloudControlConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudControlConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());
		conf.setNodeId(randomLong());
		conf.setControlId(randomString());
		conf.setControlReference(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudControlConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudControlConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudControlConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudControlConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int ctrl = 0; ctrl < count; ctrl++ ) {
					// @formatter:off
					CloudControlConfiguration conf = newCloudControlConfiguration(userId,
							integrationId,
							randomLong(),
							randomString(),
							randomString(),
							randomString(),
							randomString(),
							props)
							;
					// @formatter:on
					UserLongCompositePK id = dao.create(userId, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final Long randomUserId = userIds.get(RNG.nextInt(userIds.size()));
		Collection<CloudControlConfiguration> results = dao.findAll(randomUserId, null);

		// THEN
		CloudControlConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId()))
				.toArray(CloudControlConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudControlConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int ctrl = 0; ctrl < count; ctrl++ ) {
					// @formatter:off
					CloudControlConfiguration conf = newCloudControlConfiguration(
							userId,
							integrationId,
							randomLong(),
							randomString(),
							randomString(),
							randomString(),
							randomString(),
							props)
							;
					// @formatter:on
					UserLongCompositePK id = dao.create(userId, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final Long randomUserId = userIds.get(RNG.nextInt(userIds.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		FilterResults<CloudControlConfiguration, UserLongCompositePK> results = dao.findFiltered(filter);

		// THEN
		CloudControlConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId()))
				.toArray(CloudControlConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUserNodeControl() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudControlConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int ctrl = 0; ctrl < count; ctrl++ ) {
					// @formatter:off
					CloudControlConfiguration conf = newCloudControlConfiguration(
							userId,
							integrationId,
							randomLong(),
							randomString(),
							randomString(),
							randomString(),
							randomString(),
							props)
							;
					// @formatter:on
					UserLongCompositePK id = dao.create(userId, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final CloudControlConfiguration randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setNodeId(randomConf.getNodeId());
		filter.setControlId(randomConf.getControlId());
		FilterResults<CloudControlConfiguration, UserLongCompositePK> results = dao.findFiltered(filter);

		// THEN
		then(results).as("Result for user+node+control returned").containsExactly(randomConf);
	}

}
