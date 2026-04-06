/* ==================================================================
 * UserNodeEventTask.java - 3/06/2020 3:37:11 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.event.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.dao.UserNodeRelatedEntity;
import net.solarnetwork.dao.BasicUuidEntity;

/**
 * Entity for a user node event task.
 *
 * @author matt
 * @version 1.3
 */
public class UserNodeEventTask extends BasicUuidEntity implements UserNodeRelatedEntity<UUID> {

	@Serial
	private static final long serialVersionUID = -71612276091455732L;

	private @Nullable Long userId;
	private Long nodeId;
	private Long hookId;
	private @Nullable String sourceId;
	private @Nullable Map<String, Object> taskProperties;
	private @Nullable UserNodeEventTaskState status;
	private @Nullable Boolean success;
	private @Nullable String message;
	private @Nullable Instant completed;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param nodeId
	 *        the node ID
	 * @param hookId
	 *        the hook ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeEventTask(UUID id, Instant created, Long nodeId, Long hookId) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		this.hookId = requireNonNullArgument(hookId, "hookId");
	}

	/**
	 * Get this event data as a map, suitable for posting as a message body.
	 *
	 * @param topic
	 *        the topic to include, or {@code null} to omit
	 * @return the message data, never {@code null}
	 * @since 1.1
	 */
	public Map<String, Object> asMessageData(String topic) {
		Map<String, Object> msg = new LinkedHashMap<>(8);
		if ( topic != null ) {
			msg.put("topic", topic);
		}
		msg.put("userId", userId);
		msg.put("hookId", hookId);
		msg.put("nodeId", nodeId);
		msg.put("sourceId", sourceId);

		if ( taskProperties != null ) {
			// add task properties, but don't allow overriding the hard-coded props already there
			for ( Entry<String, Object> me : taskProperties.entrySet() ) {
				String key = me.getKey();
				if ( !msg.containsKey(key) ) {
					msg.put(key, me.getValue());
				}
			}
		}
		return msg;
	}

	@SuppressWarnings("NullAway")
	@Override
	public final @Nullable Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 *
	 * @param userId
	 *        the user ID to set
	 */
	public final void setUserId(@Nullable Long userId) {
		this.userId = userId;
	}

	@Override
	public final Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public final void setNodeId(Long nodeId) {
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
	}

	/**
	 * Get the source ID.
	 *
	 * <p>
	 * A single source ID or source ID Ant-style pattern is allowed.
	 * </p>
	 *
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 *
	 * @param sourceId
	 *        the source ID or source ID Ant-style pattern to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the ID of the related {@link UserNodeEventHookConfiguration} entity.
	 *
	 * @return the hook ID
	 */
	public final Long getHookId() {
		return hookId;
	}

	/**
	 * Set the ID of the related {@link UserNodeEventHookConfiguration} entity.
	 *
	 * @param hookId
	 *        the hook ID to set
	 */
	public final void setHookId(Long hookId) {
		this.hookId = requireNonNullArgument(hookId, "hookId");
	}

	/**
	 * Get the task properties.
	 *
	 * @return the taskProperties
	 */
	public final @Nullable Map<String, Object> getTaskProperties() {
		return taskProperties;
	}

	/**
	 * Set the task properties.
	 *
	 * @param taskProperties
	 *        the taskProperties to set
	 */
	public final void setTaskProperties(@Nullable Map<String, Object> taskProperties) {
		this.taskProperties = taskProperties;
	}

	/**
	 * Get the task status.
	 *
	 * @return the status
	 */
	public final @Nullable UserNodeEventTaskState getStatus() {
		return status;
	}

	/**
	 * Set the task status.
	 *
	 * @param status
	 *        the status to set
	 */
	public final void setStatus(@Nullable UserNodeEventTaskState status) {
		this.status = status;
	}

	/**
	 * Get the task status as a key value.
	 *
	 * @return the task status key
	 */
	public final char getStatusKey() {
		UserNodeEventTaskState status = getStatus();
		return (status != null ? status.getKey() : UserNodeEventTaskState.Unknown.getKey());
	}

	/**
	 * Set the task status as a key value.
	 *
	 * @param key
	 *        the task status key to set
	 */
	public final void setStatusKey(char key) {
		UserNodeEventTaskState status;
		try {
			status = UserNodeEventTaskState.forKey(key);
		} catch ( IllegalArgumentException e ) {
			status = UserNodeEventTaskState.Unknown;
		}
		setStatus(status);
	}

	/**
	 * Get the success flag.
	 *
	 * @return the success
	 */
	public final @Nullable Boolean getSuccess() {
		return success;
	}

	/**
	 * Set the success flag.
	 *
	 * @param success
	 *        the success to set
	 */
	public final void setSuccess(@Nullable Boolean success) {
		this.success = success;
	}

	/**
	 * Get the task message.
	 *
	 * @return the message
	 */
	public final @Nullable String getMessage() {
		return message;
	}

	/**
	 * Set the task message.
	 *
	 * @param message
	 *        the message to set
	 */
	public final void setMessage(@Nullable String message) {
		this.message = message;
	}

	/**
	 * Get the completed flag.
	 *
	 * @return the completed
	 */
	public final @Nullable Instant getCompleted() {
		return completed;
	}

	/**
	 * Set the completed flag.
	 *
	 * @param completed
	 *        the completed to set
	 */
	public final void setCompleted(@Nullable Instant completed) {
		this.completed = completed;
	}

}
