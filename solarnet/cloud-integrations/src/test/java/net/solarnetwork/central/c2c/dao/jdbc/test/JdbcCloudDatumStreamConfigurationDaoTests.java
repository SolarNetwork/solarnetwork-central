/* ==================================================================
 * JdbcCloudDatumStreamConfigurationDaoTests.java - 3/10/2024 1:36:39 pm
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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamConfiguration;
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
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcCloudDatumStreamConfigurationDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamConfigurationDao dao;
	private Long userId;

	private CloudDatumStreamConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId, Map<String, Object> props) {
		CloudDatumStreamMappingConfiguration conf = CinJdbcTestUtils
				.newCloudDatumStreamMappingConfiguration(userId, integrationId, randomString(), props);
		CloudDatumStreamMappingConfiguration entity = datumStreamMappingDao
				.get(datumStreamMappingDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudDatumStreamConfiguration result = dao.entityKey(id);

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
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId(), Map.of("bif", "bop"));

		Map<String, Object> props = singletonMap("foo", "bar");
		// @formatter:off
		CloudDatumStreamConfiguration conf = newCloudDatumStreamConfiguration(userId,
				mapping.getConfigId(),
				randomString(),
				ObjectDatumKind.Node,
				randomLong(),
				randomString(),
				randomString(),
				randomString(), props)
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

		List<Map<String, Object>> data = allCloudDatumStreamConfigurationData(jdbcTemplate);
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
			.as("Row datum stream mapping ID")
			.containsEntry("map_id", conf.getDatumStreamMappingId())
			.as("Row schedule")
			.containsEntry("schedule", conf.getSchedule())
			.as("Row kind key")
			.containsEntry("kind", String.valueOf(conf.getKind().getKey()))
			.as("Row object ID")
			.containsEntry("obj_id", conf.getObjectId())
			.as("Row source ID")
			.containsEntry("source_id", conf.getSourceId())
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
		CloudDatumStreamConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());
		conf.setKind(ObjectDatumKind.Location);
		conf.setObjectId(randomLong());
		conf.setSourceId(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudDatumStreamConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudDatumStreamConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudDatumStreamConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int m = 0; m < mappingCount; m++ ) {
					Long mappingId = createDatumStreamMapping(userId, integrationId, null).getConfigId();
					for ( int ds = 0; ds < count; ds++ ) {
						// @formatter:off
						CloudDatumStreamConfiguration conf = newCloudDatumStreamConfiguration(userId,
								mappingId,
								randomString(),
								ObjectDatumKind.Node,
								randomLong(),
								randomString(),
								randomString(),
								randomString(), props)
								;
						// @formatter:on
						UserLongCompositePK id = dao.create(userId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}

		// WHEN
		final Long randomUserId = userIds.get(RNG.nextInt(userIds.size()));
		Collection<CloudDatumStreamConfiguration> results = dao.findAll(randomUserId, null);

		// THEN
		CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId()))
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<CloudDatumStreamConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int m = 0; m < mappingCount; m++ ) {
					Long mappingId = createDatumStreamMapping(userId, integrationId, null).getConfigId();
					for ( int ds = 0; ds < count; ds++ ) {
						// @formatter:off
						CloudDatumStreamConfiguration conf = newCloudDatumStreamConfiguration(userId,
								mappingId,
								randomString(),
								ObjectDatumKind.Node,
								randomLong(),
								randomString(),
								randomString(),
								randomString(), props)
								;
						// @formatter:on
						UserLongCompositePK id = dao.create(userId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}

		// WHEN
		final Long randomUserId = userIds.get(RNG.nextInt(userIds.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId()))
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

}