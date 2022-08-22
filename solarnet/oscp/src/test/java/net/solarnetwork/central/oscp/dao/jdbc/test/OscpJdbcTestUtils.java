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
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;

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
	 * Create a new instance.
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

}
