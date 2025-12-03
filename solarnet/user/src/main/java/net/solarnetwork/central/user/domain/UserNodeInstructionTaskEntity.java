/* ==================================================================
 * UserNodeInstructionTaskEntity.java - 10/11/2025 3:23:45 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.central.common.http.HttpConstants.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.OAUTH_TOKEN_URL_SETTING;
import static net.solarnetwork.central.common.http.HttpConstants.USERNAME_SETTING;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.util.CollectionUtils.getMapString;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.common.http.OAuth2ClientIdentity;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Cloud control instruction task runtime information.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "configId", "enabled", "name", "nodeId", "topic", "schedule", "state",
		"executeAt", "serviceProperties", "lastExecuteAt", "message", "resultProperties" })
public class UserNodeInstructionTaskEntity
		extends BaseUserModifiableEntity<UserNodeInstructionTaskEntity, UserLongCompositePK> {

	/** A service property key for expression settings. */
	public static final String EXPRESSION_SETTINGS_PROP = "settings";

	/** A service property key for encrypted expression settings. */
	public static final String EXPRESSION_SECURE_SETTINGS_PROP = "secrets";

	/** A system identifier component for OAuth registration IDs. */
	public static final String OAUTH_SYSTEM_NAME = "user-instr";

	@Serial
	private static final long serialVersionUID = -8913216603960129724L;

	/** The name. */
	private String name;

	/** The node ID. */
	private Long nodeId;

	/** A cron schedule, or number or seconds. */
	private String schedule;

	/** The instruction topic. */
	private String topic;

	/** The job state. */
	private BasicClaimableJobState state;

	/** The next time the job should execute. */
	private Instant executeAt;

	/** The service properties as JSON. */
	private String servicePropsJson;

	/** The service properties. */
	private volatile transient Map<String, Object> serviceProps;

	/** The last time the job executed. */
	private Instant lastExecuteAt;

	/** A status message. */
	private String message;

	/** The last execution result properties as JSON. */
	private String resultPropsJson;

	/** The last execution result properties. */
	private volatile transient Map<String, Object> resultProps;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserNodeInstructionTaskEntity(UserLongCompositePK id) {
		super(id);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserNodeInstructionTaskEntity(Long userId, Long configId) {
		this(new UserLongCompositePK(userId, configId));
	}

	@Override
	public UserNodeInstructionTaskEntity copyWithId(UserLongCompositePK id) {
		var copy = new UserNodeInstructionTaskEntity(id);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(UserNodeInstructionTaskEntity entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setNodeId(nodeId);
		entity.setTopic(topic);
		entity.setSchedule(schedule);
		entity.setState(state);
		entity.setExecuteAt(executeAt);
		entity.setServicePropsJson(servicePropsJson);
		entity.setLastExecuteAt(lastExecuteAt);
		entity.setMessage(message);
		entity.setResultPropsJson(resultPropsJson);
	}

	@Override
	public boolean isSameAs(UserNodeInstructionTaskEntity other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.nodeId, other.nodeId)
				&& Objects.equals(this.topic, other.topic)
				&& Objects.equals(this.schedule, other.schedule)
				&& Objects.equals(this.state, other.state)
				&& Objects.equals(this.executeAt, other.executeAt)
				&& Objects.equals(this.lastExecuteAt, other.lastExecuteAt)
				&& Objects.equals(this.message, other.message)
				&& Objects.equals(getServiceProperties(), other.getServiceProperties())
				&& Objects.equals(getResultProperties(), other.getResultProperties())
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append("UserNodeInstructionTaskEntity{");
		builder.append("userId=");
		builder.append(getUserId());
		builder.append(", id=");
		builder.append(getConfigId());
		builder.append(", nodeId=");
		builder.append(nodeId);
		builder.append(", topic=");
		builder.append(topic);
		builder.append(", schedule=");
		builder.append(schedule);
		if ( state != null ) {
			builder.append(", state=");
			builder.append(state);
		}
		if ( executeAt != null ) {
			builder.append(", executeAt=");
			builder.append(executeAt);
		}
		if ( lastExecuteAt != null ) {
			builder.append(", lastExecuteAt=");
			builder.append(lastExecuteAt);
		}
		if ( message != null ) {
			builder.append(", message=");
			builder.append(message);
		}
		if ( serviceProps != null ) {
			builder.append(", serviceProps=");
			builder.append(serviceProps);
		}
		if ( resultProps != null ) {
			builder.append(", resultProps=");
			builder.append(resultProps);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the task settings.
	 * 
	 * <p>
	 * This returns the combination of settings and secure settings from the
	 * {@link #EXPRESSION_SETTINGS_PROP} and
	 * {@link #EXPRESSION_SECURE_SETTINGS_PROP} service property keys, which
	 * must have {@code Map} object values. The value of any key in the secure
	 * settings that also exists in the regular settings will override the
	 * regular settings value in the returned map.
	 * </p>
	 * 
	 * @param decryptor
	 *        a function to apply to all values in the
	 *        {@link #EXPRESSION_SECURE_SETTINGS_PROP} service property map
	 * @return a new map instance, never {@code null}
	 */
	public Map<String, Object> settings(Function<String, String> decryptor) {
		final Map<String, Object> result = new LinkedHashMap<>(4);
		final Map<String, ?> props = getServiceProperties();
		if ( props != null && props.get(EXPRESSION_SETTINGS_PROP) instanceof Map<?, ?> s ) {
			for ( Entry<?, ?> e : s.entrySet() ) {
				if ( e.getKey() == null || e.getValue() == null ) {
					continue;
				}
				result.put(e.getKey().toString(), e.getValue());
			}
		}
		if ( props != null && props.get(EXPRESSION_SECURE_SETTINGS_PROP) instanceof Map<?, ?> s ) {
			final Set<String> secureKeys = new HashSet<>(s.size());
			for ( Entry<?, ?> e : s.entrySet() ) {
				if ( e.getKey() == null || e.getValue() == null ) {
					continue;
				}
				String key = e.getKey().toString();
				secureKeys.add(key);
				result.put(key, e.getValue());
			}
			SecurityUtils.decryptedMap(result, secureKeys, decryptor);
		}
		return result;
	}

	/**
	 * Encrypt all secure settings.
	 * 
	 * <p>
	 * This will encrypt all settings found in a
	 * {@link #EXPRESSION_SECURE_SETTINGS_PROP} service property map value.
	 * </p>
	 * 
	 * @param encryptor
	 *        a function to apply to all values in the
	 *        {@link #EXPRESSION_SECURE_SETTINGS_PROP} service property map
	 */
	public void encryptSettings(Function<String, String> encryptor) {
		final Map<String, Object> props = getServiceProps();
		if ( props == null || !(props.get(EXPRESSION_SECURE_SETTINGS_PROP) instanceof Map<?, ?> s) ) {
			return;
		}

		final Map<String, String> encryptedSettings = new LinkedHashMap<>(s.size());
		for ( Entry<?, ?> e : s.entrySet() ) {
			if ( e.getKey() == null || e.getValue() == null ) {
				continue;
			}
			String key = e.getKey().toString();
			encryptedSettings.put(key, encryptor.apply(e.getValue().toString()));
		}
		putServiceProps(Map.of(EXPRESSION_SECURE_SETTINGS_PROP, encryptedSettings));
	}

	/**
	 * Cryptographically digest all secure settings.
	 * 
	 * <p>
	 * This will perform a one-way digest of all settings found in a
	 * {@link #EXPRESSION_SECURE_SETTINGS_PROP} service property map value.
	 * </p>
	 */
	public void digestSensitiveInformation() {
		final Map<String, Object> props = getServiceProps();
		if ( props == null || !(props.get(EXPRESSION_SECURE_SETTINGS_PROP) instanceof Map<?, ?> s) ) {
			return;
		}

		final Map<String, String> encryptedSettings = new LinkedHashMap<>(s.size());
		for ( Entry<?, ?> e : s.entrySet() ) {
			if ( e.getKey() == null || e.getValue() == null ) {
				continue;
			}
			encryptedSettings.put(e.getKey().toString(),
					StringUtils.sha256Base64Value(e.getValue().toString()));
		}
		putServiceProps(Map.of(EXPRESSION_SECURE_SETTINGS_PROP, encryptedSettings));
	}

	/**
	 * Get the configured OAuth client identity.
	 * 
	 * @return the identity, or {@code null} if no OAuth identity is available
	 */
	public OAuth2ClientIdentity oauthClientIdentity() {
		final Map<String, Object> props = getServiceProps();
		if ( props == null || !(props.get(EXPRESSION_SETTINGS_PROP) instanceof Map<?, ?> s) ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> settings = (Map<String, Object>) s;
		final String tokenUrl = nonEmptyString(getMapString(OAUTH_TOKEN_URL_SETTING, settings));
		if ( tokenUrl == null ) {
			return null;
		}
		final String username = nonEmptyString(getMapString(USERNAME_SETTING, settings));
		final String clientId = nonEmptyString(getMapString(OAUTH_CLIENT_ID_SETTING, settings));
		return new OAuth2ClientIdentity(getId(),
				userIdSystemIdentifier(getUserId(), OAUTH_SYSTEM_NAME, getConfigId()),
				username != null ? username
						: clientId != null ? clientId : "%s %s".formatted(getId().ident(), getName()));
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	public Long getConfigId() {
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 * 
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the topic.
	 *
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Set the topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * Get the schedule at which to execute the instruction.
	 *
	 * @return the schedule, as either a cron schedule or a number of seconds
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule at which to execute the instruction.
	 *
	 * @param schedule
	 *        the schedule to set, as either a cron schedule or a number of
	 *        seconds
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the job state.
	 *
	 * @return the state
	 */
	public BasicClaimableJobState getState() {
		return state;
	}

	/**
	 * Set the job state.
	 *
	 * @param state
	 *        the state to set
	 */
	public void setState(BasicClaimableJobState state) {
		this.state = state;
	}

	/**
	 * Get the time at which the job should next execute.
	 *
	 * @return the date
	 */
	public Instant getExecuteAt() {
		return executeAt;
	}

	/**
	 * Set the time at which the job should next execute.
	 *
	 * @param executeAt
	 *        the date to set
	 */
	public void setExecuteAt(Instant executeAt) {
		this.executeAt = executeAt;
	}

	/**
	 * Get the time at which the job last executed.
	 *
	 * @return the date
	 */
	public Instant getLastExecuteAt() {
		return lastExecuteAt;
	}

	/**
	 * Set the time at which the job last executed.
	 *
	 * @param lastExecuteAt
	 *        the date to set
	 */
	public void setLastExecuteAt(Instant lastExecuteAt) {
		this.lastExecuteAt = lastExecuteAt;
	}

	/**
	 * Get the status message.
	 *
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the status message.
	 *
	 * @param message
	 *        the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@literal null} if no service
	 *         properties available
	 */
	@JsonIgnore
	public String getServicePropsJson() {
		return servicePropsJson;
	}

	/**
	 * Set the service properties object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created service properties and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to parse as service properties
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setServicePropsJson(String json) {
		servicePropsJson = json;
		serviceProps = null;
	}

	/**
	 * Get the service properties.
	 *
	 * <p>
	 * This will decode the {@link #getServicePropsJson()} value into a map
	 * instance.
	 * </p>
	 *
	 * @return the service properties
	 */
	@JsonIgnore
	public Map<String, Object> getServiceProps() {
		if ( serviceProps == null && servicePropsJson != null ) {
			serviceProps = JsonUtils.getStringMap(servicePropsJson);
		}
		return serviceProps;
	}

	/**
	 * Set the service properties to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setServicePropsJson(String)} as well.
	 * </p>
	 *
	 * @param serviceProps
	 *        the service properties to set
	 */
	@JsonSetter("serviceProperties")
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
	}

	/**
	 * Add a collection of service properties.
	 *
	 * @param props
	 *        the properties to add
	 */
	public void putServiceProps(Map<String, Object> props) {
		Map<String, Object> serviceProps = getServiceProps();
		if ( serviceProps == null ) {
			serviceProps = props;
		} else {
			serviceProps.putAll(props);
		}
		setServiceProps(serviceProps);
	}

	/**
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

	/**
	 * Get the result properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@literal null} if no result properties
	 *         available
	 */
	@JsonIgnore
	public String getResultPropsJson() {
		return resultPropsJson;
	}

	/**
	 * Set the result properties object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created result properties and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to parse as result properties
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setResultPropsJson(String json) {
		resultPropsJson = json;
		resultProps = null;
	}

	/**
	 * Get the result properties.
	 *
	 * <p>
	 * This will decode the {@link #getResultPropsJson()} value into a map
	 * instance.
	 * </p>
	 *
	 * @return the result properties
	 */
	@JsonIgnore
	public Map<String, Object> getResultProps() {
		if ( resultProps == null && resultPropsJson != null ) {
			resultProps = JsonUtils.getStringMap(resultPropsJson);
		}
		return resultProps;
	}

	/**
	 * Set the result properties to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setResultPropsJson(String)} as well.
	 * </p>
	 *
	 * @param resultProps
	 *        the result properties to set
	 */
	@JsonSetter("resultProperties")
	public void setResultProps(Map<String, Object> resultProps) {
		this.resultProps = resultProps;
		resultPropsJson = JsonUtils.getJSONString(resultProps, null);
	}

	/**
	 * Add a collection of result properties.
	 *
	 * @param props
	 *        the properties to add
	 */
	public void putResultProps(Map<String, Object> props) {
		Map<String, Object> resultProps = getResultProps();
		if ( resultProps == null ) {
			resultProps = props;
		} else {
			resultProps.putAll(props);
		}
		setResultProps(resultProps);
	}

	/**
	 * Get the result properties.
	 *
	 * @return the result properties
	 */
	public Map<String, ?> getResultProperties() {
		return getResultProps();
	}

}
