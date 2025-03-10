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

import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_STATE_SETTING;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamMappingConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudIntegrationConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudIntegrationConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * Test cases for the {@link JdbcCloudIntegrationConfigurationDao} class.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcCloudIntegrationConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudIntegrationConfigurationDao dao;
	private Long userId;

	private CloudIntegrationConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId) {
		CloudDatumStreamMappingConfiguration conf = CinJdbcTestUtils
				.newCloudDatumStreamMappingConfiguration(userId, integrationId, randomString(), null);
		CloudDatumStreamMappingConfiguration entity = datumStreamMappingDao
				.get(datumStreamMappingDao.save(conf));
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long datumStreamMappingId) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				datumStreamMappingId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), null);
		conf.setEnabled(true);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		return entity;
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

	@Test
	public void findFiltered_forDatumStream() {
		// GIVEN
		final int userCount = 2;
		final int integrationCount = 2;
		final int datumStreamCount = 2;

		final List<CloudIntegrationConfiguration> integrations = new ArrayList<>(
				userCount * integrationCount);
		final Map<UserLongCompositePK, List<CloudDatumStreamConfiguration>> datumStreamsByIntegrationIds = new LinkedHashMap<>(
				userCount * integrationCount);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < integrationCount; i++ ) {
				CloudIntegrationConfiguration integration = newCloudIntegrationConfiguration(userId,
						randomString(), randomString(), null);
				UserLongCompositePK integrationId = dao.create(userId, integration);
				integration = integration.copyWithId(integrationId);
				integrations.add(integration);
				for ( int d = 0; d < datumStreamCount; d++ ) {
					CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
							integration.getConfigId());
					CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
							mapping.getConfigId());
					datumStreamsByIntegrationIds.computeIfAbsent(integration.getId(),
							id -> new ArrayList<>(datumStreamCount)).add(datumStream);
				}
			}
		}

		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamMappingConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);

		// WHEN
		var randomIntegration = integrations.get(RNG.nextInt(integrations.size()));
		var randomDatumStream = datumStreamsByIntegrationIds.get(randomIntegration.getId())
				.get(RNG.nextInt(datumStreamsByIntegrationIds.get(randomIntegration.getId()).size()));

		log.info("Querying for datum stream {}", randomDatumStream.getConfigId());

		BasicFilter filter = new BasicFilter();
		filter.setUserId(randomDatumStream.getUserId());
		filter.setDatumStreamId(randomDatumStream.getConfigId());
		FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		then(results).as("Result for integration for datum stream returned")
				.containsExactly(new CloudIntegrationConfiguration[] { randomIntegration });
	}

	private String[] randomServiceIdentifiers(List<CloudIntegrationConfiguration> confs) {
		String[] randomServiceIdents = confs.stream().filter(c -> RNG.nextBoolean())
				.map(c -> c.getServiceIdentifier()).toArray(String[]::new);
		if ( randomServiceIdents.length < 1 ) {
			randomServiceIdents = new String[] {
					confs.get(RNG.nextInt(confs.size())).getServiceIdentifier() };
		}
		return randomServiceIdents;
	}

	@Test
	public void findFiltered_forServiceIdentifiers() throws Exception {
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

		final String[] randomServiceIdents = randomServiceIdentifiers(confs);
		Arrays.sort(randomServiceIdents);

		// WHEN
		final Long userId = userIds.get(1);
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(userId);
		filter.setServiceIdentifiers(randomServiceIdents);
		FilterResults<CloudIntegrationConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		CloudIntegrationConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId())
						&& Arrays.binarySearch(randomServiceIdents, e.getServiceIdentifier()) >= 0)
				.toArray(CloudIntegrationConfiguration[]::new);
		then(results).as("Results for single user returned").containsExactlyInAnyOrder(expected);
	}

	@Test
	public void saveOAuthAuthorizationState_notFound() {
		// GIVEN
		final UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, null);

		// THEN
		then(result).as("No record updated").isFalse();
	}

	@Test
	public void saveOAuthAuthorizationState_noProps() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), null);
		UserLongCompositePK id = dao.create(userId, conf);

		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, null);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property created")
					.hasSize(1)
					.as("OAuth state property saved")
					.containsEntry(OAUTH_STATE_SETTING, state)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_addProp() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of("foo", randomString()));
		UserLongCompositePK id = dao.create(userId, conf);

		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, null);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property created")
					.hasSize(2)
					.as("Existing property preserved")
					.containsEntry("foo", conf.getServiceProps().get("foo"))
					.as("OAuth state property saved")
					.containsEntry(OAUTH_STATE_SETTING, state)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_replaceProp() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, randomString()));
		UserLongCompositePK id = dao.create(userId, conf);

		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, null);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property created")
					.hasSize(1)
					.as("OAuth state property saved")
					.containsEntry(OAUTH_STATE_SETTING, state)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_removeProp() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, randomString()));
		UserLongCompositePK id = dao.create(userId, conf);

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, null, null);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property removed")
					.isEmpty()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_replaceProp_whenExpected() {
		// GIVEN
		final String initialState = randomString();
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, initialState));
		UserLongCompositePK id = dao.create(userId, conf);

		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, initialState);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property created")
					.hasSize(1)
					.as("OAuth state property saved")
					.containsEntry(OAUTH_STATE_SETTING, state)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_replaceProp_whenNotExpected() {
		// GIVEN
		final String initialState = randomString();
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, initialState));
		UserLongCompositePK id = dao.create(userId, conf);

		final String state = randomString();

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, state, randomString());

		// THEN
		then(result).as("Record NOT updated with expectedState does not match").isFalse();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property preserved")
					.hasSize(1)
					.as("OAuth state property unchanged")
					.containsEntry(OAUTH_STATE_SETTING, initialState)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_removeProp_whenExpected() {
		// GIVEN
		final String initialState = randomString();
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, initialState));
		UserLongCompositePK id = dao.create(userId, conf);

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, null, initialState);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property removed")
					.isEmpty()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void saveOAuthAuthorizationState_removeProp_whenNotExpected() {
		// GIVEN
		final String initialState = randomString();
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of(OAUTH_STATE_SETTING, initialState));
		UserLongCompositePK id = dao.create(userId, conf);

		// WHEN
		boolean result = dao.saveOAuthAuthorizationState(id, null, randomString());

		// THEN
		then(result).as("Record NOT updated with expectedState does not match").isFalse();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property preserved")
					.hasSize(1)
					.as("OAuth state property unchanged")
					.containsEntry(OAUTH_STATE_SETTING, initialState)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mergeServiceProperties_merge() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(), Map.of("foo", randomString()));
		UserLongCompositePK id = dao.create(userId, conf);

		final Map<String, Object> newProps = Map.of("bim", randomString(), "baz", randomString());

		// WHEN
		boolean result = dao.mergeServiceProperties(id, newProps);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property is merged old with new")
					.isEqualTo(Map.of(
						"foo", conf.getServiceProperties().get("foo"),
						"bim", newProps.get("bim"),
						"baz", newProps.get("baz")
					))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mergeServiceProperties_replace() {
		// GIVEN
		CloudIntegrationConfiguration conf = newCloudIntegrationConfiguration(userId, randomString(),
				randomString(),
				Map.of("foo", randomString(), "baz", randomString(), "other", randomString()));
		UserLongCompositePK id = dao.create(userId, conf);

		final Map<String, Object> newProps = Map.of("foo", randomString(), "baz", randomString());

		// WHEN
		boolean result = dao.mergeServiceProperties(id, newProps);

		// THEN
		then(result).as("Record updated").isTrue();

		var rows = CinJdbcTestUtils.allCloudIntegrationConfigurationData(jdbcTemplate);

		// @formatter:off
		then(rows)
			.as("Integration row in database")
			.hasSize(1)
			.element(0)
			.satisfies(r -> {
				Map<String, Object> props = JsonUtils.getStringMap(r.get("sprops").toString());
				then(props)
					.as("OAuth state property is merged old with new")
					.isEqualTo(Map.of(
						"other", conf.getServiceProperties().get("other"),
						"foo", newProps.get("foo"),
						"baz", newProps.get("baz")
					))
					;
			})
			;
		// @formatter:on
	}

}
