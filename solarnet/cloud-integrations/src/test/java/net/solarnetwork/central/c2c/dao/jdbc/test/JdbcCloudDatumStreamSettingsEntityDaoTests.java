/* ==================================================================
 * JdbcCloudDatumStreamSettingsEntityDaoTests.java - 28/10/2024 10:40:14 am
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

import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamSettingsEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamSettingsEntity;
import static net.solarnetwork.central.test.CommonTestUtils.randomBoolean;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcUserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcCloudDatumStreamSettingsEntityDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamSettingsEntityDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcUserSettingsEntityDao userSettingsDao;
	private JdbcCloudDatumStreamSettingsEntityDao dao;
	private Long userId;

	private CloudDatumStreamSettingsEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamSettingsEntityDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		userSettingsDao = new JdbcUserSettingsEntityDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), null);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
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

	private UserSettingsEntity createUserSettings(Long userId) {
		UserSettingsEntity conf = CinJdbcTestUtils.newUserSettingsEntity(userId, randomBoolean(),
				randomBoolean());
		UserSettingsEntity entity = userSettingsDao.get(userSettingsDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudDatumStreamSettingsEntity result = dao.entityKey(id);

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
		final CloudIntegrationConfiguration integration = createIntegration(userId);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId());
		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
				mapping.getConfigId());

		// @formatter:off
		CloudDatumStreamSettingsEntity conf = newCloudDatumStreamSettingsEntity(userId,
				datumStream.getConfigId(),
				randomBoolean(),
				randomBoolean()
				);
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

		List<Map<String, Object>> data = allCloudDatumStreamSettingsEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row datum stream ID")
			.containsEntry("ds_id", datumStream.getConfigId())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date (populated with creation date)")
			.containsEntry("modified", Timestamp.from(conf.getCreated()))
			.as("Row publish SolarIn")
			.containsEntry("pub_in", conf.isPublishToSolarIn())
			.as("Row publish SolarFlux")
			.containsEntry("pub_flux", conf.isPublishToSolarFlux())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamSettingsEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamSettingsEntity conf = last.copyWithId(last.getId());
		conf.setModified(Instant.now().plusMillis(474));
		conf.setPublishToSolarIn(!conf.isPublishToSolarIn());
		conf.setPublishToSolarFlux(!conf.isPublishToSolarFlux());

		UserLongCompositePK result = dao.save(conf);
		CloudDatumStreamSettingsEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamSettingsEntityData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudDatumStreamSettingsEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int count = 3;
		final List<CloudDatumStreamSettingsEntity> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			Long integrationId = createIntegration(userId).getConfigId();
			Long mappingId = createDatumStreamMapping(userId, integrationId).getConfigId();
			for ( int i = 0; i < count; i++ ) {
				Long datumStreamId = createDatumStream(userId, mappingId).getConfigId();
				// @formatter:off
				CloudDatumStreamSettingsEntity conf = newCloudDatumStreamSettingsEntity(userId,
						datumStreamId,
						randomBoolean(),
						randomBoolean()
						);
				// @formatter:on
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final CloudDatumStreamSettingsEntity randomEntity = confs
				.get(CommonTestUtils.RNG.nextInt(confs.size()));
		Collection<CloudDatumStreamSettingsEntity> results = dao.findAll(randomEntity.getUserId(), null);

		// THEN
		CloudDatumStreamSettingsEntity[] expected = confs.stream()
				.filter(e -> randomEntity.getUserId().equals(e.getUserId()))
				.toArray(CloudDatumStreamSettingsEntity[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void resolve_none() {
		// GIVEN
		UserSettingsEntity defaultSettings = new UserSettingsEntity(userId, Instant.now());

		// WHEN
		CloudDatumStreamSettings result = dao.resolveSettings(userId, randomLong(), defaultSettings);

		// THEN
		// @formatter:off
		then(result)
			.as("Default settings instance returned when no rows in database")
			.isSameAs(defaultSettings)
			;
		// @formatter:on
	}

	@Test
	public void resolve_user() {
		// GIVEN
		UserSettingsEntity defaultSettings = new UserSettingsEntity(userId, Instant.now());
		UserSettingsEntity userSettings = createUserSettings(userId);

		// WHEN
		CloudDatumStreamSettings result = dao.resolveSettings(userId, randomLong(), defaultSettings);

		// THEN
		// @formatter:off
		then(result)
			.as("Default settings instance NOT returned when user settings row exists in database")
			.isNotSameAs(defaultSettings)
			.as("Pub SolarIn resolved from user settings when no datum stream settings available")
			.returns(userSettings.isPublishToSolarIn(), from(CloudDatumStreamSettings::isPublishToSolarIn))
			.as("Pub SolarFlux resolved from user settings when no datum stream settings available")
			.returns(userSettings.isPublishToSolarFlux(), from(CloudDatumStreamSettings::isPublishToSolarFlux))
			.asInstanceOf(InstanceOfAssertFactories.type(CloudDatumStreamSettingsEntity.class))
			.as("Unassigned datum stream ID returned for user-level settings")
			.returns(UserLongCompositePK.UNASSIGNED_ENTITY_ID, from(CloudDatumStreamSettingsEntity::getDatumStreamId))
			;
		// @formatter:on
	}

	@Test
	public void resolve_datumStream() {
		// GIVEN
		UserSettingsEntity defaultSettings = new UserSettingsEntity(userId, Instant.now());
		createUserSettings(userId);
		insert();

		// WHEN
		CloudDatumStreamSettings result = dao.resolveSettings(userId, last.getDatumStreamId(),
				defaultSettings);

		// THEN
		// @formatter:off
		then(result)
			.as("Default settings instance NOT returned when user settings row exists in database")
			.isNotSameAs(defaultSettings)
			.as("Pub SolarIn resolved from datum stream settings when no datum stream settings available")
			.returns(last.isPublishToSolarIn(), from(CloudDatumStreamSettings::isPublishToSolarIn))
			.as("Pub SolarFlux resolved from datum stream settings when no datum stream settings available")
			.returns(last.isPublishToSolarFlux(), from(CloudDatumStreamSettings::isPublishToSolarFlux))
			;
		// @formatter:on
	}

}
