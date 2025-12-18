/* ==================================================================
 * JdbcCloudDatumStreamMappingConfigurationDaoTests.java - 16/10/2024 7:38:50 am
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
import static java.util.stream.Collectors.toCollection;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamMappingConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamMappingConfiguration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
 * Test cases for the {@link JdbcCloudDatumStreamMappingConfigurationDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudDatumStreamMappingConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudDatumStreamMappingConfigurationDao dao;
	private Long userId;

	private CloudDatumStreamMappingConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long datumStreamMappingId,
			Map<String, Object> props) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				datumStreamMappingId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), props);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudDatumStreamMappingConfiguration result = dao.entityKey(id);

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
		CloudDatumStreamMappingConfiguration conf = newCloudDatumStreamMappingConfiguration(userId,
				integration.getConfigId(),
				randomString(),
				props);
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

		List<Map<String, Object>> data = allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
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
			.as("Row integration ID")
			.containsEntry("int_id", conf.getIntegrationId())
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
		CloudDatumStreamMappingConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamMappingConfiguration conf = last.copyWithId(last.getId());
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudDatumStreamMappingConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<Long> integrationIds = new ArrayList<>(userCount);
		final List<CloudDatumStreamMappingConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				Long integrationId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);

					integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
					integrationIds.add(integrationId);
				} else {
					userId = userIds.get(u);
					integrationId = integrationIds.get(u);
				}

				// @formatter:off
				CloudDatumStreamMappingConfiguration conf = newCloudDatumStreamMappingConfiguration(userId,
						integrationId,
						randomString(),
						props
						);
				// @formatter:on
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CloudDatumStreamMappingConfiguration> results = dao.findAll(userId, null);

		// THEN
		CloudDatumStreamMappingConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId()))
				.toArray(CloudDatumStreamMappingConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<Long> integrationIds = new ArrayList<>(userCount);
		final List<CloudDatumStreamMappingConfiguration> confs = new ArrayList<>(count);

		final Map<String, Object> props = Collections.singletonMap("foo", "bar");

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				Long integrationId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);

					integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
					integrationIds.add(integrationId);
				} else {
					userId = userIds.get(u);
					integrationId = integrationIds.get(u);
				}

				// @formatter:off
				CloudDatumStreamMappingConfiguration conf = newCloudDatumStreamMappingConfiguration(userId,
						integrationId,
						randomString(),
						props
						);
				// @formatter:on
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		FilterResults<CloudDatumStreamMappingConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		CloudDatumStreamMappingConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId()))
				.toArray(CloudDatumStreamMappingConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	/*-
	 * This test finds mappings that either
	 *
	 * 1) have no reference by a cloud datum stream, or
	 * 2) have a reference to a cloud datum stream of Node type with one of a given set of node ID criteria
	 */
	@Test
	public void findFiltered_forNodeIds() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 6;

		final List<CloudDatumStreamMappingConfiguration> confs = new ArrayList<>();
		final Map<Long, CloudDatumStreamConfiguration> mappingToDatumStreams = new HashMap<>();

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, singletonMap("bim", "bam")).getConfigId();
				for ( int m = 0; m < mappingCount; m++ ) {
					// @formatter:off
					CloudDatumStreamMappingConfiguration conf = newCloudDatumStreamMappingConfiguration(
							userId,
							integrationId,
							randomString(),
							null
							);
					// @formatter:on

					UserLongCompositePK id = dao.create(userId, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);

					// create a datum stream, every other one associated with this mapping
					CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
							i % 2 == 0 ? conf.getConfigId() : null, null);
					if ( datumStream.getDatumStreamMappingId() != null ) {
						mappingToDatumStreams.put(conf.getConfigId(), datumStream);
					}
				}
			}
		}

		allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userDatumStreams = mappingToDatumStreams.values()
				.stream().filter(c -> randomUserId.equals(c.getUserId())).toList();
		final SortedSet<Long> userNodeIds = userDatumStreams.stream().map(c -> c.getObjectId())
				.collect(toCollection(TreeSet::new));
		final Set<Long> randomNodeIds = Set.of(userNodeIds.first(), userNodeIds.last());

		final List<CloudDatumStreamMappingConfiguration> expected = confs.stream().filter(c -> {
			if ( !randomUserId.equals(c.getUserId()) ) {
				return false;
			}
			var datumStream = mappingToDatumStreams.get(c.getConfigId());
			return (datumStream == null || randomNodeIds.contains(datumStream.getObjectId()));
		}).sorted().toList();

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setNodeIds(randomNodeIds.toArray(Long[]::new));

		FilterResults<CloudDatumStreamMappingConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		then(results).as("Results for node IDs returned").containsExactlyElementsOf(expected);
	}

}
