/* ==================================================================
 * DinJdbcTestUtils.java - 21/02/2024 7:44:58 am
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

package net.solarnetwork.central.din.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.din.domain.CredentialConfiguration;
import net.solarnetwork.central.din.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Helper methods for datum input endpoint JDBC tests.
 *
 * @author matt
 * @version 1.0
 */
public final class DinJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(DinJdbcTestUtils.class);

	private DinJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new credential configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @return the entity
	 */
	public static CredentialConfiguration newCredentialConfiguration(Long userId, String username,
			String password) {
		CredentialConfiguration conf = new CredentialConfiguration(unassignedEntityIdKey(userId),
				Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setUsername(username);
		conf.setPassword(password);
		return conf;
	}

	/**
	 * List credential configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCredentialConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.din_credential ORDER BY user_id, id");
		log.debug("solardin.din_credential table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
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
	public static TransformConfiguration newTransformConfiguration(Long userId, String name,
			String serviceId, Map<String, Object> serviceProps) {
		TransformConfiguration conf = new TransformConfiguration(unassignedEntityIdKey(userId),
				Instant.now());
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
	public static List<Map<String, Object>> allTransformConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.din_xform ORDER BY user_id, id");
		log.debug("solardin.din_xform table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new endpoint configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param name
	 *        the name
	 * @param nodeId
	 *        the node ID
	 * @param source
	 *        ID the source ID
	 * @param transformId
	 *        ID the transform ID
	 * @return the entity
	 */
	public static EndpointConfiguration newEndpointConfiguration(Long userId, UUID endpointId,
			String name, Long nodeId, String sourceId, Long transformId) {
		EndpointConfiguration conf = new EndpointConfiguration(new UserUuidPK(userId, endpointId),
				Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setName(name);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setTransformId(transformId);
		return conf;
	}

	/**
	 * List endpoint configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allEndpointConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.din_endpoint ORDER BY user_id, id");
		log.debug("solardin.din_endpoint table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new endpoint auth configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param name
	 *        the name
	 * @param credentialId
	 *        ID the transform ID
	 * @return the entity
	 */
	public static EndpointAuthConfiguration newEndpointAuthConfiguration(Long userId, UUID endpointId,
			Long credentialId) {
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(
				new UserUuidLongCompositePK(userId, endpointId, credentialId), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List endpoint autn configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allEndpointAuthConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.din_endpoint_auth_cred ORDER BY user_id, endpoint_id, cred_id");
		log.debug("solardin.din_endpoint_auth_cred table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
