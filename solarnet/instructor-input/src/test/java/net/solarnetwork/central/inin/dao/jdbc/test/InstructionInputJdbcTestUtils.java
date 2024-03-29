/* ==================================================================
 * InstructionInputJdbcTestUtils.java - 28/03/2024 3:54:16 pm
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

package net.solarnetwork.central.inin.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.domain.UserUuidLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;
import net.solarnetwork.central.inin.domain.EndpointAuthConfiguration;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformPhase;

/**
 * Helper methods for instruction input endpoint JDBC tests.
 *
 * @author matt
 * @version 1.0
 */
public final class InstructionInputJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(InstructionInputJdbcTestUtils.class);

	private InstructionInputJdbcTestUtils() {
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
				.queryForList("select * from solardin.inin_credential ORDER BY user_id, id");
		log.debug("solardin.inin_credential table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new request transform configuration instance.
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
	public static TransformConfiguration newRequestTransformConfiguration(Long userId, String name,
			String serviceId, Map<String, Object> serviceProps) {
		return newTransformConfiguration(TransformPhase.Request, userId, name, serviceId, serviceProps);
	}

	/**
	 * Create a new response transform configuration instance.
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
	public static TransformConfiguration newResponseTransformConfiguration(Long userId, String name,
			String serviceId, Map<String, Object> serviceProps) {
		return newTransformConfiguration(TransformPhase.Response, userId, name, serviceId, serviceProps);
	}

	/**
	 * Create a new transform configuration instance.
	 *
	 * @param phase
	 *        the phase
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
	public static TransformConfiguration newTransformConfiguration(TransformPhase phase, Long userId,
			String name, String serviceId, Map<String, Object> serviceProps) {
		TransformConfiguration conf = (phase == TransformPhase.Request
				? new RequestTransformConfiguration(unassignedEntityIdKey(userId), Instant.now())
				: new ResponseTransformConfiguration(unassignedEntityIdKey(userId), Instant.now()));
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setServiceIdentifier(serviceId);
		conf.setServiceProps(serviceProps);
		return conf;
	}

	/**
	 * List request transform configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allRequestTransformConfigurationData(
			JdbcOperations jdbcOps) {
		return allTransformConfigurationData(jdbcOps, TransformPhase.Request);
	}

	/**
	 * List response transform configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allResponseTransformConfigurationData(
			JdbcOperations jdbcOps) {
		return allTransformConfigurationData(jdbcOps, TransformPhase.Response);
	}

	/**
	 * List transform configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allTransformConfigurationData(JdbcOperations jdbcOps,
			TransformPhase phase) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.inin_%s_xform ORDER BY user_id, id"
						.formatted(phase == TransformPhase.Request ? "req" : "res"));
		log.debug("solardin.inin_xform table has {} items: [{}]", data.size(),
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
	 * @param nodeIds
	 *        the node IDs
	 * @param reqTransformId
	 *        the request transform ID
	 * @param resTransformId
	 *        the response transform ID
	 * @return the entity
	 */
	public static EndpointConfiguration newEndpointConfiguration(Long userId, UUID endpointId,
			String name, Long[] nodeIds, Long reqTransformId, Long resTransformId) {
		EndpointConfiguration conf = new EndpointConfiguration(new UserUuidPK(userId, endpointId),
				Instant.now());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setName(name);
		conf.setNodeIds(new LinkedHashSet<>(Arrays.asList(nodeIds)));
		conf.setRequestTransformId(reqTransformId);
		conf.setResponseTransformId(resTransformId);
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
				.queryForList("select * from solardin.inin_endpoint ORDER BY user_id, id");
		log.debug("solardin.inin_endpoint table has {} items: [{}]", data.size(),
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
	 * List endpoint auth configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allEndpointAuthConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.inin_endpoint_auth_cred ORDER BY user_id, endpoint_id, cred_id");
		log.debug("solardin.inin_endpoint_auth_cred table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * List input data entity rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allInputDataEntityData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.inin_input_data ORDER BY user_id, node_id, source_id");
		log.debug("solardin.inin_input_data table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
