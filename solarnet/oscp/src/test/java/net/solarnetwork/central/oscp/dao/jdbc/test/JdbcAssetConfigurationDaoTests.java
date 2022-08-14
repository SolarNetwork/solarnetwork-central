/* ==================================================================
 * JdbcAssetConfigurationDaoTests.java - 12/08/2022 6:33:46 pm
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

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.oscp.domain.MeasurementUnit.KILO_MULTIPLIER;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcAssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcAssetConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAssetConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;
	private JdbcCapacityGroupConfigurationDao capacityGroupDao;

	private JdbcAssetConfigurationDao dao;
	private Long userId;

	private CapacityProviderConfiguration lastProvider;
	private CapacityOptimizerConfiguration lastOptimzer;
	private CapacityGroupConfiguration lastGroup;
	private AssetConfiguration last;

	@BeforeEach
	public void setup() {
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		capacityGroupDao = new JdbcCapacityGroupConfigurationDao(jdbcTemplate);
		dao = new JdbcAssetConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	private List<Map<String, Object>> allAssetConfigurationData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solaruser.user_oscp_asset_conf ORDER BY user_id, id");
		log.debug("solaruser.user_oscp_asset_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param capacityGroupId
	 *        the group ID
	 * @return the instance
	 */
	public static AssetConfiguration newConf(Long userId, Instant created, Long capacitGroupId) {
		AssetConfiguration conf = new AssetConfiguration(UserLongPK.unassignedEntityIdKey(userId),
				created);
		conf.setModified(created);
		conf.setEnabled(true);
		conf.setName(randomUUID().toString());
		conf.setCapacityGroupId(capacitGroupId);
		conf.setNodeId(randomUUID().getMostSignificantBits());
		conf.setSourceId(randomUUID().toString());
		conf.setCategory(AssetCategory.Charging);
		conf.setInstantaneousPropertyNames(new String[] { randomUUID().toString() });
		conf.setInstantaneousUnit(MeasurementUnit.kW);
		conf.setInstantaneousMultiplier(KILO_MULTIPLIER);
		conf.setInstantaneousPhase(Phase.All);
		conf.setEnergyPropertyNames(new String[] { randomUUID().toString() });
		conf.setEnergyUnit(MeasurementUnit.kWh);
		conf.setEnergyMultiplier(KILO_MULTIPLIER);
		conf.setEnergyType(EnergyType.Total);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		return conf;
	}

	@Test
	public void insert() {
		// GIVEN
		lastProvider = capacityProviderDao.get(capacityProviderDao.create(userId,
				JdbcCapacityProviderConfigurationDaoTests.newConf(userId, Instant.now())));
		lastOptimzer = capacityOptimizerDao.get(capacityOptimizerDao.create(userId,
				JdbcCapacityOptimizerConfigurationDaoTests.newConf(userId, Instant.now())));
		lastGroup = capacityGroupDao.get(
				capacityGroupDao.create(userId, JdbcCapacityGroupConfigurationDaoTests.newConf(userId,
						Instant.now(), lastProvider.getEntityId(), lastOptimzer.getEntityId())));
		AssetConfiguration conf = newConf(userId, Instant.now(), lastGroup.getEntityId());

		// WHEN
		UserLongPK result = dao.create(userId, conf);

		// THEN
		List<Map<String, Object>> data = allAssetConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID has been generated by DB", row,
				hasEntry(equalTo("id"), allOf(notNullValue(), not(equalTo(conf.getEntityId())))));
		assertThat("Primary key returned", result, is(notNullValue()));
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(conf.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(conf.getCreated())));
		assertThat("Row user ID", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Row enabled", row, hasEntry("enabled", conf.isEnabled()));
		assertThat("Row name", row, hasEntry("cname", conf.getName()));
		assertThat("Row group ID", row, hasEntry("cg_id", conf.getCapacityGroupId()));
		assertThat("Row node ID", row, hasEntry("node_id", conf.getNodeId()));
		assertThat("Row source ID", row, hasEntry("source_id", conf.getSourceId()));
		assertThat("Row category", row, hasEntry("category", conf.getCategory().getCode()));
		assertThat("Row iprop names", row, hasEntry(equalTo("iprops"), notNullValue()));

		assertThat("Row serviceProps", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
		assertThat("Row matches returned primary key result", result,
				is(equalTo(new UserLongPK(userId, (Long) row.get("id")))));

		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		AssetConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity matches source", result, is(equalTo(last)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		lastProvider = capacityProviderDao.get(capacityProviderDao.create(userId,
				JdbcCapacityProviderConfigurationDaoTests.newConf(userId, Instant.now())));
		lastOptimzer = capacityOptimizerDao.get(capacityOptimizerDao.create(userId,
				JdbcCapacityOptimizerConfigurationDaoTests.newConf(userId, Instant.now())));
		lastGroup = capacityGroupDao.get(
				capacityGroupDao.create(userId, JdbcCapacityGroupConfigurationDaoTests.newConf(userId,
						Instant.now(), lastProvider.getEntityId(), lastOptimzer.getEntityId())));

		// WHEN
		AssetConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomUUID().toString());
		conf.setCapacityGroupId(lastGroup.getEntityId());
		conf.setNodeId(randomUUID().getMostSignificantBits());
		conf.setSourceId(randomUUID().toString());
		conf.setCategory(AssetCategory.Charging);
		conf.setInstantaneousPropertyNames(new String[] { randomUUID().toString() });
		conf.setInstantaneousUnit(MeasurementUnit.kW);
		conf.setInstantaneousMultiplier(KILO_MULTIPLIER);
		conf.setInstantaneousPhase(Phase.All);
		conf.setEnergyPropertyNames(new String[] { randomUUID().toString() });
		conf.setEnergyUnit(MeasurementUnit.kWh);
		conf.setEnergyMultiplier(KILO_MULTIPLIER);
		conf.setEnergyType(EnergyType.Total);
		conf.setServiceProps(Collections.singletonMap("bim", randomUUID().toString()));

		UserLongPK result = dao.save(conf);
		AssetConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allAssetConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		assertThat("Retrieved entity matches updated source", updated, is(equalTo(conf)));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allAssetConfigurationData();
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, CapacityGroupConfiguration> userGroups = new LinkedHashMap<>(userCount);
		final List<AssetConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					UserLongPK providerId = capacityProviderDao.create(userId,
							JdbcCapacityProviderConfigurationDaoTests.newConf(userId, Instant.now()));
					UserLongPK optimizerId = capacityOptimizerDao.create(userId,
							JdbcCapacityOptimizerConfigurationDaoTests.newConf(userId, Instant.now()));
					userGroups.put(userId,
							capacityGroupDao.get(capacityGroupDao.create(userId,
									JdbcCapacityGroupConfigurationDaoTests.newConf(userId, Instant.now(),
											providerId.getEntityId(), optimizerId.getEntityId()))));

				} else {
					userId = userIds.get(u);
				}
				AssetConfiguration conf = newConf(userId, t, userGroups.get(userId).getEntityId());
				UserLongPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<AssetConfiguration> results = dao.findAll(userId, null);

		// THEN
		AssetConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(AssetConfiguration[]::new);
		assertThat("Results for single user returned", results, contains(expected));
	}

	@Test
	public void findForGroup() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		Map<Long, CapacityGroupConfiguration> userGroups = new LinkedHashMap<>(userCount);
		final List<AssetConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					UserLongPK providerId = capacityProviderDao.create(userId,
							JdbcCapacityProviderConfigurationDaoTests.newConf(userId, Instant.now()));
					UserLongPK optimizerId = capacityOptimizerDao.create(userId,
							JdbcCapacityOptimizerConfigurationDaoTests.newConf(userId, Instant.now()));
					userGroups.put(userId,
							capacityGroupDao.get(capacityGroupDao.create(userId,
									JdbcCapacityGroupConfigurationDaoTests.newConf(userId, Instant.now(),
											providerId.getEntityId(), optimizerId.getEntityId()))));

				} else {
					userId = userIds.get(u);
				}
				AssetConfiguration conf = newConf(userId, t, userGroups.get(userId).getEntityId());
				UserLongPK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		final Long groupId = userGroups.get(userId).getEntityId();
		Collection<AssetConfiguration> results = dao.findAllForCapacityGroup(userId, groupId, null);

		// THEN
		AssetConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId()) && groupId.equals(e.getCapacityGroupId()))
				.toArray(AssetConfiguration[]::new);
		assertThat("Results for single group returned", results, contains(expected));
	}

}
