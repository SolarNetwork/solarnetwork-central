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

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.SOURCE_ID_MAP_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.VIRTUAL_SOURCE_IDS_SETTING;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamConfiguration;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcCloudDatumStreamConfigurationDao} class.
 *
 * @author matt
 * @version 1.3
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
				Map.of("bim", "bam"));
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId(), Map.of("bif", "bop"));

		Map<String, Object> props = Map.of("foo", "bar");
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
			// the name citext column returned as PGObject
			.hasEntrySatisfying("cname", n -> then(n.toString()).isEqualTo(conf.getName()))
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
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());
		conf.setKind(ObjectDatumKind.Location);
		conf.setObjectId(randomLong());
		conf.setSourceId(randomString());

		Map<String, Object> props = Map.of("bar", "foo");
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
		List<Map<String, Object>> data = allCloudDatumStreamConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@FunctionalInterface
	public static interface DatumStreamPopulatorCallback {

		void populate(Long integrationId, CloudDatumStreamConfiguration conf);

	}

	private List<CloudDatumStreamConfiguration> populateCloudDatumStreams(final int userCount,
			final int integrationCount, final int mappingCount, final int datumSourceCount,
			DatumStreamPopulatorCallback callback) {
		final List<CloudDatumStreamConfiguration> confs = new ArrayList<>(
				userCount * integrationCount * mappingCount * datumSourceCount);
		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < integrationCount; i++ ) {
				Long integrationId = createIntegration(userId, Map.of("bim", "bam")).getConfigId();
				for ( int m = 0; m < mappingCount; m++ ) {
					Long mappingId = createDatumStreamMapping(userId, integrationId, null).getConfigId();
					for ( int ds = 0; ds < datumSourceCount; ds++ ) {
						// @formatter:off
						CloudDatumStreamConfiguration conf = newCloudDatumStreamConfiguration(
								userId,
								mappingId,
								randomString(),
								ObjectDatumKind.Node,
								randomLong(),
								randomSourceId(),
								randomString(),
								randomString(),
								null
							);
						// @formatter:on
						if ( callback != null ) {
							callback.populate(integrationId, conf);
						}
						UserLongCompositePK id = dao.create(userId, conf);
						conf = conf.copyWithId(id);
						confs.add(conf);
					}
				}
			}
		}
		return confs;
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, null);

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
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

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, null);

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
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

	@Test
	public void findFiltered_forMapping() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, null);

		// WHEN
		final CloudDatumStreamConfiguration randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setDatumStreamMappingId(randomConf.getDatumStreamMappingId());
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& randomConf.getDatumStreamMappingId().equals(e.getDatumStreamMappingId()))
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for single mapping returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forNode() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, null);

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final List<CloudDatumStreamConfiguration> userRandomConfs = List
				.of(userConfs.get(RNG.nextInt(userConfs.size())),
						userConfs.get(RNG.nextInt(userConfs.size())))
				.stream().distinct().sorted().toList();
		final Set<Long> randomNodeIds = userRandomConfs.stream()
				.map(CloudDatumStreamConfiguration::getObjectId).collect(toSet());

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setNodeIds(randomNodeIds.toArray(Long[]::new));
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		then(results).as("Results for given node IDs returned")
				.containsExactlyElementsOf(userRandomConfs);
	}

	@Test
	public void findFiltered_forSourceIds() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, null);

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final Map<String, CloudDatumStreamConfiguration> randomSourceIdConfs = new HashMap<>(2);
		while ( randomSourceIdConfs.size() < 2 ) {
			final CloudDatumStreamConfiguration randomConf = userConfs
					.get(RNG.nextInt(userConfs.size()));
			randomSourceIdConfs.put(randomConf.getSourceId(), randomConf);
		}

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceIds(randomSourceIdConfs.keySet().toArray(String[]::new));
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final List<CloudDatumStreamConfiguration> expectedConfs = randomSourceIdConfs.values().stream()
				.sorted().toList();
		then(results).as("Results for given source IDs returned")
				.containsExactlyElementsOf(expectedConfs);
	}

	@Test
	public void findFiltered_forVirtualSourceIds() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					conf.setSourceId("unused");
					conf.setServiceProps(Map.of(VIRTUAL_SOURCE_IDS_SETTING,
							List.of(randomSourceId(), randomSourceId())));
				});

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final Map<String, CloudDatumStreamConfiguration> randomSourceIdConfs = new HashMap<>(2);
		while ( randomSourceIdConfs.size() < 2 ) {
			final CloudDatumStreamConfiguration randomConf = userConfs
					.get(RNG.nextInt(userConfs.size()));
			List<String> virtualSourceIds = randomConf
					.servicePropertyStringList(VIRTUAL_SOURCE_IDS_SETTING);
			randomSourceIdConfs.put(virtualSourceIds.get(RNG.nextInt(virtualSourceIds.size())),
					randomConf);
		}

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceIds(randomSourceIdConfs.keySet().toArray(String[]::new));
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final List<CloudDatumStreamConfiguration> expectedConfs = randomSourceIdConfs.values().stream()
				.sorted().toList();
		then(results).as("Results for given virtual source IDs returned")
				.containsExactlyElementsOf(expectedConfs);
	}

	@Test
	public void findFiltered_forMappedSourceIds() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					conf.setSourceId("unused");
					conf.setServiceProps(Map.of(SOURCE_ID_MAP_SETTING,
							Map.of(randomString(), randomSourceId(), randomString(), randomSourceId())));
				});

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final Map<String, CloudDatumStreamConfiguration> randomSourceIdConfs = new HashMap<>(2);
		while ( randomSourceIdConfs.size() < 2 ) {
			final CloudDatumStreamConfiguration randomConf = userConfs
					.get(RNG.nextInt(userConfs.size()));
			Map<String, String> mappedSourceIds = randomConf
					.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
			List<String> keyList = List.copyOf(mappedSourceIds.keySet());
			randomSourceIdConfs.put(mappedSourceIds.get(keyList.get(RNG.nextInt(keyList.size()))),
					randomConf);
		}

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceIds(randomSourceIdConfs.keySet().toArray(String[]::new));
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final List<CloudDatumStreamConfiguration> expectedConfs = randomSourceIdConfs.values().stream()
				.sorted().toList();
		then(results).as("Results for given mapped source IDs returned")
				.containsExactlyElementsOf(expectedConfs);
	}

	@Test
	public void findFiltered_forSourceIds_pattern() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;
		final List<String> sourceIdPrefixes = List.of("/AAA", "/BBB", "/CCC");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					String randomPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));
					conf.setSourceId(randomPrefix + conf.getSourceId());
				});

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();

		final String randomSourceIdPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceId(randomSourceIdPrefix + "/**");
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		// @formatter:off
		final List<CloudDatumStreamConfiguration> expectedConfs = userConfs.stream()
			.filter(conf -> conf.getSourceId().startsWith(randomSourceIdPrefix))
			.sorted()
			.toList()
			;
		then(results)
			.as("Results for given source IDs returned")
			.containsExactlyElementsOf(expectedConfs)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forVirtualSourceIds_pattern() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;
		final List<String> sourceIdPrefixes = List.of("/AAA", "/BBB", "/CCC");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					String randomPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));
					conf.setSourceId("unused");
					conf.setServiceProps(Map.of(VIRTUAL_SOURCE_IDS_SETTING,
							List.of(randomPrefix + randomSourceId(), randomPrefix + randomSourceId())));
				});

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final Map<String, CloudDatumStreamConfiguration> randomSourceIdConfs = new HashMap<>(2);
		while ( randomSourceIdConfs.size() < 2 ) {
			final CloudDatumStreamConfiguration randomConf = userConfs
					.get(RNG.nextInt(userConfs.size()));
			List<String> virtualSourceIds = randomConf
					.servicePropertyStringList(VIRTUAL_SOURCE_IDS_SETTING);
			randomSourceIdConfs.put(virtualSourceIds.get(RNG.nextInt(virtualSourceIds.size())),
					randomConf);
		}

		final String randomSourceIdPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceId(randomSourceIdPrefix + "/**");
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		// @formatter:off
		final List<CloudDatumStreamConfiguration> expectedConfs = userConfs.stream()
			.filter(conf ->  {
				final List<String> virtuals = conf
				.servicePropertyStringList(VIRTUAL_SOURCE_IDS_SETTING);
				for ( String virtual : virtuals ) {
					if (virtual.startsWith(randomSourceIdPrefix)) {
						return true;
					}
				}
				return false;
			})
			.sorted()
			.toList()
			;
		then(results)
			.as("Results for given virtual source IDs returned")
			.containsExactlyElementsOf(expectedConfs)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forMappedSourceIds_pattern() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final int integrationCount = 3;
		final int mappingCount = 3;
		final List<String> sourceIdPrefixes = List.of("/AAA", "/BBB", "/CCC");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					String randomPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));
					conf.setSourceId("unused");
					conf.setServiceProps(Map.of(SOURCE_ID_MAP_SETTING,
							Map.of(randomString(), randomPrefix + randomSourceId(), randomString(),
									randomPrefix + randomSourceId())));
				});

		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final List<CloudDatumStreamConfiguration> userConfs = confs.stream()
				.filter(c -> randomUserId.equals(c.getUserId())).toList();
		final Map<String, CloudDatumStreamConfiguration> randomSourceIdConfs = new HashMap<>(2);
		while ( randomSourceIdConfs.size() < 2 ) {
			final CloudDatumStreamConfiguration randomConf = userConfs
					.get(RNG.nextInt(userConfs.size()));
			Map<String, String> mappedSourceIds = randomConf
					.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
			List<String> keyList = List.copyOf(mappedSourceIds.keySet());
			randomSourceIdConfs.put(mappedSourceIds.get(keyList.get(RNG.nextInt(keyList.size()))),
					randomConf);
		}

		final String randomSourceIdPrefix = sourceIdPrefixes.get(RNG.nextInt(sourceIdPrefixes.size()));

		// WHEN
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setSourceId(randomSourceIdPrefix + "/**");
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		// @formatter:off
		final List<CloudDatumStreamConfiguration> expectedConfs = userConfs.stream()
			.filter(conf ->  {
				final Map<String, String> mappings = conf
				.servicePropertyStringMap(SOURCE_ID_MAP_SETTING);
				for ( String mapped : mappings.values() ) {
					if (mapped.startsWith(randomSourceIdPrefix)) {
						return true;
					}
				}
				return false;
			})
			.sorted()
			.toList()
			;
		then(results)
			.as("Results for given mapped source IDs returned")
			.containsExactlyElementsOf(expectedConfs)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forEnabled() throws Exception {
		// GIVEN
		final int count = 5;
		final int userCount = 2;
		final int integrationCount = 3;
		final int mappingCount = 3;

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> conf.setEnabled(RNG.nextBoolean()));

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final boolean randomEnabled = RNG.nextBoolean();

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setEnabled(randomEnabled);
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId()) && randomEnabled == e.isEnabled())
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for user + enabled flag returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forName() throws Exception {
		// GIVEN
		final int count = 5;
		final int userCount = 2;
		final int integrationCount = 3;
		final int mappingCount = 3;

		// a limited set of key substrings we'll search on
		final List<String> keySubstrings = List.of("AbCdEfG", "HiJkLmN", "OpQrStU");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					// put one of the key substrings in the middle of our random names
					final String name = "%s %s %s".formatted(randomString(),
							keySubstrings.get(RNG.nextInt(keySubstrings.size())), randomString());
					conf.setName(name);
				});

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();
		final String randomSubstring = keySubstrings.get(RNG.nextInt(keySubstrings.size()))
				.toLowerCase(Locale.ENGLISH);

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setName(randomSubstring);
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId())
						&& e.getName().toLowerCase().contains(randomSubstring))
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for user + name substring returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forName_reservedCharacter() throws Exception {
		// GIVEN
		final int count = 5;
		final int userCount = 2;
		final int integrationCount = 3;
		final int mappingCount = 3;

		// a limited set of key substrings we'll search on
		final List<String> keySubstrings = List.of("100% Fresh", "100 Percent Fresh", "OpQrStU");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					// put one of the key substrings in the middle of our random names
					final String name = "%s %s %s".formatted(randomString(),
							keySubstrings.get(RNG.nextInt(keySubstrings.size())), randomString());
					conf.setName(name);
				});

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setName(keySubstrings.getFirst().toLowerCase(Locale.ENGLISH));
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final CloudDatumStreamConfiguration[] expected = confs.stream()
				.filter(e -> randomUserId.equals(e.getUserId())
						&& e.getName().toLowerCase().contains(filter.getName()))
				.toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for user + name substring with reserved characters returned")
				.containsExactly(expected);
	}

	@Test
	public void findFiltered_forNames() throws Exception {
		// GIVEN
		final int count = 5;
		final int userCount = 2;
		final int integrationCount = 3;
		final int mappingCount = 3;

		// a limited set of key substrings we'll search on
		final List<String> keySubstrings = List.of("AbCdEfG", "HiJkLmN", "OpQrStU");

		final List<CloudDatumStreamConfiguration> confs = populateCloudDatumStreams(userCount,
				integrationCount, mappingCount, count, (_, conf) -> {
					// put one of the key substrings in the middle of our random names
					final String name = "%s %s %s".formatted(randomString(),
							keySubstrings.get(RNG.nextInt(keySubstrings.size())), randomString());
					conf.setName(name);
				});

		// WHEN
		final Long randomUserId = confs.get(RNG.nextInt(confs.size())).getUserId();

		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomUserId);
		filter.setNames(new String[] { keySubstrings.getFirst().toLowerCase(Locale.ENGLISH),
				keySubstrings.getLast().toLowerCase(Locale.ENGLISH) });
		FilterResults<CloudDatumStreamConfiguration, UserLongCompositePK> results = dao
				.findFiltered(filter);

		// THEN
		final CloudDatumStreamConfiguration[] expected = confs.stream().filter(e -> {
			if ( !randomUserId.equals(e.getUserId()) ) {
				return false;
			}
			final String lcName = e.getName().toLowerCase(Locale.ENGLISH);
			for ( String name : filter.getNames() ) {
				if ( lcName.contains(name) ) {
					return true;
				}
			}
			return false;
		}).toArray(CloudDatumStreamConfiguration[]::new);
		then(results).as("Results for user + name substring returned").containsExactly(expected);
	}

}
