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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Helper methods for cloud integrations JDBC tests.
 *
 * @author matt
 * @version 1.3
 */
public class CinJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(CinJdbcTestUtils.class);

	private CinJdbcTestUtils() {
		// not available
	}

	/**
	 * Create a new integration configuration instance.
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
				unassignedEntityIdKey(userId), Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setServiceIdentifier(serviceId);
		conf.setServiceProps(serviceProps);
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List integration configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudIntegrationConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_integration ORDER BY user_id, id");
		log.debug("solardin.cin_integration table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param schedule
	 *        the schedule
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID
	 * @param kind
	 *        the stream kind
	 * @param objectId
	 *        the stream object ID
	 * @param sourceId
	 *        the stream source ID
	 * @param name
	 *        the name
	 * @param serviceId
	 *        the service ID
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 */
	public static CloudDatumStreamConfiguration newCloudDatumStreamConfiguration(Long userId,
			Long datumStreamMappingId, String schedule, ObjectDatumKind kind, Long objectId,
			String sourceId, String name, String serviceId, Map<String, Object> serviceProps) {
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(
				unassignedEntityIdKey(userId), Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setServiceIdentifier(serviceId);
		conf.setDatumStreamMappingId(datumStreamMappingId);
		conf.setSchedule(schedule);
		conf.setKind(kind);
		conf.setObjectId(objectId);
		conf.setSourceId(sourceId);
		conf.setServiceProps(serviceProps);
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List datum stream configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudDatumStreamConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_datum_stream ORDER BY user_id, id");
		log.debug("solardin.cin_datum_stream table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream mapping configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param integrationId
	 *        the integration ID
	 * @param name
	 *        the name
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 */
	public static CloudDatumStreamMappingConfiguration newCloudDatumStreamMappingConfiguration(
			Long userId, Long integrationId, String name, Map<String, Object> serviceProps) {
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(
				unassignedEntityIdKey(userId), Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setIntegrationId(integrationId);
		conf.setServiceProps(serviceProps);
		return conf;
	}

	/**
	 * List datum stream mapping configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudDatumStreamMappingConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_datum_stream_map ORDER BY user_id, id");
		log.debug("solardin.cin_datum_stream_map table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream property configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param schedule
	 *        the schedule
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID
	 * @param index
	 *        the index
	 * @param propertyType
	 *        the property type
	 * @param propertyName
	 *        the property name
	 * @param valueType
	 *        the value type
	 * @param valueReference
	 *        the source value reference
	 * @param multiplier
	 *        the multiplier
	 * @param scale
	 *        the scale
	 * @return the entity
	 */
	public static CloudDatumStreamPropertyConfiguration newCloudDatumStreamPropertyConfiguration(
			Long userId, Long datumStreamMappingId, Integer index, DatumSamplesType propertyType,
			String propertyName, CloudDatumStreamValueType valueType, String valueReference,
			BigDecimal multiplier, Integer scale) {
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(userId,
				datumStreamMappingId, index, Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setModified(conf.getCreated());
		conf.setPropertyType(propertyType);
		conf.setPropertyName(propertyName);
		conf.setValueType(valueType);
		conf.setValueReference(valueReference);
		conf.setMultiplier(multiplier);
		conf.setScale(scale);
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List datum stream property configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudDatumStreamPropertyConfigurationData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.cin_datum_stream_prop ORDER BY user_id, map_id, idx");
		log.debug("solardin.cin_datum_stream_prop table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream rake task instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param datumStreamId
	 *        the datum stream ID
	 * @param state
	 *        the state
	 * @param executeAt
	 *        the execution time
	 * @param startAt
	 *        the query start time
	 * @param message
	 *        a message
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 */
	public static CloudDatumStreamPollTaskEntity newCloudDatumStreamPollTaskEntity(Long userId,
			Long datumStreamId, BasicClaimableJobState state, Instant executeAt, Instant startAt,
			String message, Map<String, Object> serviceProps) {
		CloudDatumStreamPollTaskEntity conf = new CloudDatumStreamPollTaskEntity(userId, datumStreamId);
		conf.setState(state);
		conf.setExecuteAt(executeAt);
		conf.setStartAt(startAt);
		conf.setMessage(message);
		conf.setServiceProps(serviceProps);
		return conf;
	}

	/**
	 * List datum stream poll task rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudDatumStreamPollTaskEntityData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.cin_datum_stream_poll_task ORDER BY user_id, ds_id");
		log.debug("solardin.cin_datum_stream_poll_task table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream rake task instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param datumStreamId
	 *        the datum stream ID
	 * @param state
	 *        the state
	 * @param executeAt
	 *        the execution time
	 * @param offset
	 *        the offset
	 * @param message
	 *        a message
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 */
	public static CloudDatumStreamRakeTaskEntity newCloudDatumStreamRakeTaskEntity(Long userId,
			Long datumStreamId, BasicClaimableJobState state, Instant executeAt, Period offset,
			String message, Map<String, Object> serviceProps) {
		CloudDatumStreamRakeTaskEntity conf = new CloudDatumStreamRakeTaskEntity(
				unassignedEntityIdKey(userId));
		conf.setDatumStreamId(datumStreamId);
		conf.setState(state);
		conf.setExecuteAt(executeAt);
		conf.setOffset(offset);
		conf.setMessage(message);
		conf.setServiceProps(serviceProps);
		return conf;
	}

	/**
	 * List datum stream rake task rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allCloudDatumStreamRakeTaskEntityData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList(
				"select * from solardin.cin_datum_stream_rake_task ORDER BY user_id, ds_id, id");
		log.debug("solardin.cin_datum_stream_rake_task table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new user settings instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param publishToSolarIn
	 *        the SolarIn publish mode
	 * @param publishToSolarFlux
	 *        the SolarFlux publish mode
	 * @return the entity
	 * @since 1.2
	 */
	public static UserSettingsEntity newUserSettingsEntity(Long userId, boolean publishToSolarIn,
			boolean publishToSolarFlux) {
		UserSettingsEntity conf = new UserSettingsEntity(userId, Instant.now());
		conf.setPublishToSolarIn(publishToSolarIn);
		conf.setPublishToSolarFlux(publishToSolarFlux);
		return conf;
	}

	/**
	 * List user settings rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 * @since 1.2
	 */
	public static List<Map<String, Object>> allUserSettingsEntityData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_user_settings ORDER BY user_id");
		log.debug("solardin.cin_user_settings table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new datum stream settings instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param datumStreamId
	 *        the datum stream ID
	 * @param publishToSolarIn
	 *        the SolarIn publish mode
	 * @param publishToSolarFlux
	 *        the SolarFlux publish mode
	 * @return the entity
	 * @since 1.2
	 */
	public static CloudDatumStreamSettingsEntity newCloudDatumStreamSettingsEntity(Long userId,
			Long datumStreamId, boolean publishToSolarIn, boolean publishToSolarFlux) {
		CloudDatumStreamSettingsEntity conf = new CloudDatumStreamSettingsEntity(userId, datumStreamId,
				Instant.now());
		conf.setPublishToSolarIn(publishToSolarIn);
		conf.setPublishToSolarFlux(publishToSolarFlux);
		return conf;
	}

	/**
	 * List datum stream settings rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 * @since 1.2
	 */
	public static List<Map<String, Object>> allCloudDatumStreamSettingsEntityData(
			JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_datum_stream_settings ORDER BY user_id");
		log.debug("solardin.cin_datum_stream_settings table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new control configuration instance.
	 *
	 * @param userId
	 *        the user ID
	 * @param integrationId
	 *        the integration ID
	 * @param nodeId
	 *        the node ID
	 * @param controlId
	 *        the control ID
	 * @param controlReference
	 *        the control reference
	 * @param name
	 *        the name
	 * @param serviceId
	 *        the service ID
	 * @param serviceProps
	 *        the service properties
	 * @return the entity
	 * @since 1.3
	 */
	public static CloudControlConfiguration newCloudControlConfiguration(Long userId, Long integrationId,
			Long nodeId, String controlId, String controlReference, String name, String serviceId,
			Map<String, Object> serviceProps) {
		CloudControlConfiguration conf = new CloudControlConfiguration(unassignedEntityIdKey(userId),
				Instant.now().truncatedTo(ChronoUnit.MILLIS));
		conf.setModified(conf.getCreated());
		conf.setName(name);
		conf.setServiceIdentifier(serviceId);
		conf.setIntegrationId(integrationId);
		conf.setNodeId(nodeId);
		conf.setControlId(controlId);
		conf.setControlReference(controlReference);
		conf.setServiceProps(serviceProps);
		conf.setEnabled(true);
		return conf;
	}

	/**
	 * List control configuration rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 * @since 1.3
	 */
	public static List<Map<String, Object>> allCloudControlConfigurationData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps
				.queryForList("select * from solardin.cin_control ORDER BY user_id, id");
		log.debug("solardin.cin_control table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
