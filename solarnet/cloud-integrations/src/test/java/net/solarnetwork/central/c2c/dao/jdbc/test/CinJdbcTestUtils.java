/* ==================================================================
 * CinJdbcTestUtils.java - 2/10/2024 9:43:35 am
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

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;

/**
 * Helper methods for cloud integrations JDBC tests.
 *
 * @author matt
 * @version 1.0
 */
public class CinJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(CinJdbcTestUtils.class);

	private CinJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new credential configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the name
	 * @param serviceId
	 *        the service ID
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 */
	public static CloudIntegrationConfiguration newCloudIntegrationConfiguration(Long userId,
			String name, String serviceId, Map<String, Object> serviceProps) {
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(
				unassignedEntityIdKey(userId), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setServiceIdentifier(serviceId);
		conf.setServiceProps(serviceProps);
		return conf;
	}

	/**
	 * List transform configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudIntegrationConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solarcin.cin_integration ORDER BY user_id, id");
		log.debug("solarcin.cin_integration table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
