/* ==================================================================
 * OscpJdbcTestUtils.java - 22/08/2022 3:03:15 pm
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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetEnergyDatumConfiguration;
import net.solarnetwork.central.oscp.domain.AssetInstantaneousDatumConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyDirection;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.StatisticType;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * Test utilities for OSCP.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(OscpJdbcTestUtils.class);

	private OscpJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static CapacityProviderConfiguration newCapacityProviderConf(Long userId,
			Long flexibilityProviderId, Instant created) {
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), created);
		conf.setModified(created);
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(flexibilityProviderId);
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		conf.setToken(randomUUID().toString());
		return conf;
	}

	/**
	 * Create a new capacity optimizer configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static CapacityOptimizerConfiguration newCapacityOptimizerConf(Long userId,
			Long flexibilityProviderId, Instant created) {
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), created);
		conf.setModified(created);
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(flexibilityProviderId);
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		conf.setToken(randomUUID().toString());
		return conf;
	}

	/**
	 * Create a new asset configuration instance.
	 * 
	 * <p>
	 * The audience will be {@link OscpRole#CapacityProvider}.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param capacityGroupId
	 *        the capacity group configuration ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 * @see #newAssetConfiguration(Long, Long, OscpRole, Instant)
	 */
	public static AssetConfiguration newAssetConfiguration(Long userId, Long capacityGroupId,
			Instant created) {
		return newAssetConfiguration(userId, capacityGroupId, OscpRole.CapacityProvider, created);
	}

	/**
	 * Create a new asset configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param capacityGroupId
	 *        the capacity group configuration ID
	 * @param audience
	 *        the audience
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static AssetConfiguration newAssetConfiguration(Long userId, Long capacityGroupId,
			OscpRole audience, Instant created) {
		AssetConfiguration conf = new AssetConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), created);
		conf.setEnabled(true);
		conf.setName(randomUUID().toString());
		conf.setCapacityGroupId(capacityGroupId);
		conf.setIdentifier(randomUUID().toString());
		conf.setAudience(audience);
		conf.setNodeId(randomUUID().getMostSignificantBits());
		conf.setSourceId(randomUUID().toString());
		conf.setCategory(AssetCategory.Charging);
		conf.setPhase(Phase.All);

		var inst = new AssetInstantaneousDatumConfiguration();
		inst.setPropertyNames(new String[] { "watts" });
		inst.setStatisticType(StatisticType.Maximum);
		inst.setUnit(MeasurementUnit.kW);
		inst.setMultiplier(KILO_MULTIPLIER);
		conf.setInstantaneous(inst);

		var energy = new AssetEnergyDatumConfiguration();
		energy.setPropertyNames(new String[] { "wattHours" });
		energy.setStatisticType(StatisticType.Difference);
		energy.setUnit(MeasurementUnit.kWh);
		energy.setMultiplier(KILO_MULTIPLIER);
		energy.setType(EnergyType.Total);
		energy.setDirection(EnergyDirection.Import);
		conf.setEnergy(energy);

		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		return conf;
	}

	/**
	 * Create a new capacity group configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param capacityProviderId
	 *        the provider ID
	 * @param capacityOptimizerId
	 *        the optimizer ID
	 * @param created
	 *        the creation date
	 * @return the instance
	 */
	public static CapacityGroupConfiguration newCapacityGroupConfiguration(Long userId,
			Long capacityProviderId, Long capacityOptimizerId, Instant created) {
		CapacityGroupConfiguration conf = new CapacityGroupConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), created);
		conf.setModified(created);
		conf.setEnabled(true);
		conf.setName(randomUUID().toString());
		conf.setIdentifier(randomUUID().toString());
		conf.setCapacityProviderMeasurementPeriod(MeasurementPeriod.TenMinute);
		conf.setCapacityProviderId(capacityProviderId);
		conf.setCapacityOptimizerId(capacityOptimizerId);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		return conf;
	}

	/**
	 * Create a new user settings instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @return the instance
	 */
	public static UserSettings newUserSettings(Long userId, Instant created) {
		UserSettings conf = new UserSettings(userId, created);
		conf.setModified(created);
		conf.setPublishToSolarIn(true);
		conf.setPublishToSolarFlux(true);
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setSourceIdTemplate("foo/bar");
		return conf;
	}

	/**
	 * Create a new user settings instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param groupId
	 *        the group ID
	 * @param created
	 *        the creation date
	 * @return the instance
	 */
	public static CapacityGroupSettings newCapacityGroupSettings(Long userId, Long groupId,
			Instant created) {
		CapacityGroupSettings conf = new CapacityGroupSettings(userId, groupId, created);
		conf.setModified(created);
		conf.setPublishToSolarIn(true);
		conf.setPublishToSolarFlux(true);
		conf.setNodeId(UUID.randomUUID().getMostSignificantBits());
		conf.setSourceIdTemplate("group/foo/bar");
		return conf;
	}

	/**
	 * List all configuration table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param role
	 *        the role
	 * @return the rows
	 */
	public static List<Map<String, Object>> allConfigurationData(JdbcOperations jdbcOps, OscpRole role) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solaroscp.oscp_%s_conf ORDER BY user_id, id".formatted(role.getAlias()));
		log.debug("solaroscp.oscp_{}_conf table has {} items: [{}]", role.getAlias(), data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all heartbeat table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param role
	 *        the role
	 * @return the rows
	 */
	public static List<Map<String, Object>> allHeartbeatData(JdbcOperations jdbcOps, OscpRole role) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_%s_heartbeat ORDER BY user_id, id"
						.formatted(role.getAlias()));
		log.debug("solaroscp.oscp_{}_heartbeat table has {} items: [{}]", role.getAlias(), data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all capacity group measurement table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param role
	 *        the role
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCapacityGroupMeasurementData(JdbcOperations jdbcOps,
			OscpRole role) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_cg_%s_meas ORDER BY user_id, cg_id"
						.formatted(role.getAlias()));
		log.debug("solaroscp.oscp_cg_{}_meas table has {} items: [{}]", role.getAlias(), data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all token table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param role
	 *        the role
	 * @return the rows
	 */
	public static List<Map<String, Object>> allTokenData(JdbcOperations jdbcOps, OscpRole role) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solaroscp.oscp_%s_token ORDER BY user_id, id".formatted(role.getAlias()));
		log.debug("solaroscp.oscp_{}_token table has {} items: [{}]", role.getAlias(), data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all capacity group table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCapacityGroupConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_cg_conf ORDER BY user_id, id");
		log.debug("solaroscp.oscp_cg_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all asset configuration table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allAssetConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_asset_conf ORDER BY user_id, id");
		log.debug("solaroscp.oscp_asset_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all user setting table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allUserSettingsData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_user_settings ORDER BY user_id");
		log.debug("solaroscp.oscp_user_settings table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List all capacity group setting table rows.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCapacityGroupSettingsData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solaroscp.oscp_cg_settings ORDER BY user_id, cg_id");
		log.debug("solaroscp.oscp_cg_settings table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
