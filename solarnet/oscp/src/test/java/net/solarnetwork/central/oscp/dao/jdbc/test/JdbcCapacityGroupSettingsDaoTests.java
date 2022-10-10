/* ==================================================================
 * JdbcCapacityGroupSettingsDaoTests.java - 10/10/2022 11:14:41 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.allCapacityGroupSettingsData;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newCapacityGroupConfiguration;
import static net.solarnetwork.central.oscp.dao.jdbc.test.OscpJdbcTestUtils.newCapacityGroupSettings;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupSettingsDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcUserSettingsDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.DatumPublishSettings;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcCapacityGroupSettingsDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityGroupSettingsDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;
	private JdbcCapacityGroupConfigurationDao capacityGroupDao;
	private JdbcUserSettingsDao userSettingsDao;

	private JdbcCapacityGroupSettingsDao dao;
	private Long userId;
	private Long flexibilityProviderId;
	private CapacityProviderConfiguration provider;
	private CapacityOptimizerConfiguration optimizer;
	private CapacityGroupConfiguration group;

	private CapacityGroupSettings last;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		capacityGroupDao = new JdbcCapacityGroupConfigurationDao(jdbcTemplate);
		userSettingsDao = new JdbcUserSettingsDao(jdbcTemplate);
		dao = new JdbcCapacityGroupSettingsDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);

		flexibilityProviderId = flexibilityProviderDao
				.idForToken(flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)), false)
				.getEntityId();
		provider = capacityProviderDao.get(capacityProviderDao.create(userId, OscpJdbcTestUtils
				.newCapacityProviderConf(userId, flexibilityProviderId, Instant.now())));
		optimizer = capacityOptimizerDao.get(capacityOptimizerDao.create(userId, OscpJdbcTestUtils
				.newCapacityOptimizerConf(userId, flexibilityProviderId, Instant.now())));
		group = capacityGroupDao.get(
				capacityGroupDao.create(userId, OscpJdbcTestUtils.newCapacityGroupConfiguration(userId,
						provider.getEntityId(), optimizer.getEntityId(), Instant.now())));
	}

	@Test
	public void insert() {
		// GIVEN
		CapacityGroupSettings settings = new CapacityGroupSettings(userId, group.getEntityId(),
				Instant.now());
		settings.setModified(settings.getCreated());
		settings.setPublishToSolarIn(true);
		settings.setPublishToSolarFlux(true);
		settings.setNodeId(UUID.randomUUID().getMostSignificantBits());
		settings.setSourceIdTemplate("foo/bar");

		// WHEN
		UserLongCompositePK result = dao.save(settings);

		// THEN
		List<Map<String, Object>> data = allCapacityGroupSettingsData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", settings.getUserId()));
		assertThat("Row group ID matches", row, hasEntry("cg_id", settings.getGroupId()));
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(settings.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(settings.getCreated())));
		assertThat("Row pub_in matches", row, hasEntry("pub_in", settings.isPublishToSolarIn()));
		assertThat("Row pub_flux matches", row, hasEntry("pub_flux", settings.isPublishToSolarFlux()));
		assertThat("Row node_id matches", row, hasEntry("node_id", settings.getNodeId()));
		assertThat("Row source_id_tmpl matches", row,
				hasEntry("source_id_tmpl", settings.getSourceIdTemplate()));

		last = settings.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CapacityGroupSettings result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity matches source", result.isSameAs(last), is(equalTo(true)));
	}

	@Test
	public void resolveDatumPublishSettings_none() {
		// GIVEN

		// WHEN
		DatumPublishSettings result = dao.resolveDatumPublishSettings(userId, group.getEntityId());

		// THEN
		assertThat("No settings available", result, is(nullValue()));
	}

	@Test
	public void resolveDatumPublishSettings_user() {
		// GIVEN
		UserSettings settings = OscpJdbcTestUtils.newUserSettings(userId, Instant.now());
		settings.setPublishToSolarFlux(false);
		userSettingsDao.save(settings);

		// WHEN
		DatumPublishSettings result = dao.resolveDatumPublishSettings(userId, group.getEntityId());

		// THEN
		assertThat("Settings resolved to user values", result, is(notNullValue()));
		assertThat("Resolved as group settings", result, is(instanceOf(CapacityGroupSettings.class)));
		assertThat("Resolved user ID", ((CapacityGroupSettings) result).getUserId(),
				is(equalTo(group.getUserId())));
		assertThat("Resolved group ID", ((CapacityGroupSettings) result).getGroupId(),
				is(equalTo(group.getEntityId())));
		assertThat("Pub SolarIn setting", result.isPublishToSolarIn(),
				is(equalTo(settings.isPublishToSolarIn())));
		assertThat("Pub SolarFlux setting", result.isPublishToSolarFlux(),
				is(equalTo(settings.isPublishToSolarFlux())));
		assertThat("Node ID setting", result.getNodeId(), is(equalTo(settings.getNodeId())));
		assertThat("Source ID template setting", result.getSourceIdTemplate(),
				is(equalTo(settings.getSourceIdTemplate())));
	}

	@Test
	public void resolveDatumPublishSettings_group() {
		// GIVEN
		insert();

		UserSettings settings = OscpJdbcTestUtils.newUserSettings(userId, Instant.now());
		settings.setPublishToSolarFlux(false);
		userSettingsDao.save(settings);

		// WHEN
		DatumPublishSettings result = dao.resolveDatumPublishSettings(userId, group.getEntityId());

		// THEN
		assertThat("Settings resolved to group values", result, is(notNullValue()));
		assertThat("Resolved as group settings", result, is(instanceOf(CapacityGroupSettings.class)));
		assertThat("Resolved values from group", ((CapacityGroupSettings) result).isSameAs(last),
				is(equalTo(true)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CapacityGroupSettings settings = last.copyWithId(last.getId());
		settings.setModified(Instant.now().plusMillis(474));
		settings.setPublishToSolarIn(false);
		settings.setPublishToSolarIn(false);
		settings.setNodeId(UUID.randomUUID().getMostSignificantBits());
		settings.setSourceIdTemplate("bim/bam");

		UserLongCompositePK result = dao.save(settings);

		// THEN
		assertThat("Result is user ID", result, is(equalTo(last.getId())));
		List<Map<String, Object>> data = allCapacityGroupSettingsData(jdbcTemplate);
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", settings.getUserId()));
		assertThat("Row group ID matches", row, hasEntry("cg_id", settings.getGroupId()));
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(settings.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(settings.getCreated())));
		assertThat("Row pub_in matches", row, hasEntry("pub_in", settings.isPublishToSolarIn()));
		assertThat("Row pub_flux matches", row, hasEntry("pub_flux", settings.isPublishToSolarFlux()));
		assertThat("Row node_id matches", row, hasEntry("node_id", settings.getNodeId()));
		assertThat("Row source_id_tmpl matches", row,
				hasEntry("source_id_tmpl", settings.getSourceIdTemplate()));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allCapacityGroupSettingsData(jdbcTemplate);
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, CapacityProviderConfiguration> userProviders = new LinkedHashMap<>(userCount);
		Map<Long, CapacityOptimizerConfiguration> userOptimizers = new LinkedHashMap<>(userCount);
		final List<CapacityGroupSettings> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					Long flexibilityProviderId = flexibilityProviderDao
							.idForToken(flexibilityProviderDao
									.createAuthToken(unassignedEntityIdKey(userId)), false)
							.getEntityId();
					userProviders.put(userId,
							capacityProviderDao.get(capacityProviderDao.create(userId,
									OscpJdbcTestUtils.newCapacityProviderConf(userId,
											flexibilityProviderId, Instant.now()))));
					userOptimizers.put(userId,
							capacityOptimizerDao.get(capacityOptimizerDao.create(userId,
									OscpJdbcTestUtils.newCapacityOptimizerConf(userId,
											flexibilityProviderId, Instant.now()))));

				} else {
					userId = userIds.get(u);
				}
				CapacityGroupConfiguration conf = newCapacityGroupConfiguration(userId,
						userProviders.get(userId).getEntityId(),
						userOptimizers.get(userId).getEntityId(), t);
				UserLongCompositePK id = capacityGroupDao.create(userId, conf);

				CapacityGroupSettings settings = newCapacityGroupSettings(userId, id.getEntityId(), t);
				dao.save(settings);
				confs.add(settings);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CapacityGroupSettings> results = dao.findAll(userId, null);

		// THEN
		CapacityGroupSettings[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(CapacityGroupSettings[]::new);
		assertThat("Results for single user returned", results, contains(expected));
	}

}
