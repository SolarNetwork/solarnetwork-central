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

package net.solarnetwork.central.user.event.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.solarnetwork.central.user.dao.UserNodeRelatedEntity;
import net.solarnetwork.dao.BasicUuidEntity;

/**
 * Entity for a user node event task.
 * 
 * @author matt
 * @version 1.2
 */
public class UserNodeEventTask extends BasicUuidEntity implements UserNodeRelatedEntity<UUID> {

	private static final long serialVersionUID = -71612276091455732L;

	private Long userId;
	private Long nodeId;
	private Long hookId;
	private String sourceId;
	private Map<String, Object> taskProperties;
	private UserNodeEventTaskState status;
	private Boolean success;
	private String message;
	private Instant completed;

	/**
	 * Constructor.
	 */
	public UserNodeEventTask() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public UserNodeEventTask(UUID id, Instant created) {
		super(id, created);
	}

	/**
	 * Get this event data as a map, suitable for posting as a message body.
	 * 
	 * @param topic
	 *        the topic to include, or {@literal null} to omit
	 * @return the message data, never {@literal null}
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

	@Override
	public Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 * 
	 * @param userId
	 *        the user ID to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
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
	 * Get the source ID.
	 * 
	 * <p>
	 * A single source ID or source ID Ant-style pattern is allowed.
	 * </p>
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 * 
	 * @param sourceId
	 *        the source ID or source ID Ant-style pattern to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the ID of the related {@link UserNodeEventHookConfiguration} entity.
	 * 
	 * @return the hook ID
	 */
	public Long getHookId() {
		return hookId;
	}

	/**
	 * Set the ID of the related {@link UserNodeEventHookConfiguration} entity.
	 * 
	 * @param hookId
	 *        the hook ID to set
	 */
	public void setHookId(Long hookId) {
		this.hookId = hookId;
	}

	/**
	 * Get the task properties.
	 * 
	 * @return the taskProperties
	 */
	public Map<String, Object> getTaskProperties() {
		return taskProperties;
	}

	/**
	 * Set the task properties.
	 * 
	 * @param taskProperties
	 *        the taskProperties to set
	 */
	public void setTaskProperties(Map<String, Object> taskProperties) {
		this.taskProperties = taskProperties;
	}

	/**
	 * Get the task status.
	 * 
	 * @return the status
	 */
	public UserNodeEventTaskState getStatus() {
		return status;
	}

	/**
	 * Set the task status.
	 * 
	 * @param status
	 *        the status to set
	 */
	public void setStatus(UserNodeEventTaskState status) {
		this.status = status;
	}

	/**
	 * Get the task status as a key value.
	 * 
	 * @return the task status key
	 */
	public char getStatusKey() {
		UserNodeEventTaskState status = getStatus();
		return (status != null ? status.getKey() : UserNodeEventTaskState.Unknown.getKey());
	}

	/**
	 * Set the task status as a key value.
	 * 
	 * @param key
	 *        the task status key to set
	 */
	public void setStatusKey(char key) {
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
	public Boolean getSuccess() {
		return success;
	}

	/**
	 * Set the success flag.
	 * 
	 * @param success
	 *        the success to set
	 */
	public void setSuccess(Boolean success) {
		this.success = success;
	}

	/**
	 * Get the task message.
	 * 
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the task message.
	 * 
	 * @param message
	 *        the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Get the completed flag.
	 * 
	 * @return the completed
	 */
	public Instant getCompleted() {
		return completed;
	}

	/**
	 * Set the completed flag.
	 * 
	 * @param completed
	 *        the completed to set
	 */
	public void setCompleted(Instant completed) {
		this.completed = completed;
	}

}
