/* ==================================================================
 * UserNodeEventHookConfiguration.java - 3/06/2020 2:50:40 pm
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

package net.solarnetwork.central.user.domain;

import java.time.Instant;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.IdentifiableConfiguration;

/**
 * User and node specific event configuration entity.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public class UserNodeEventHookConfiguration extends BasicEntity<UserLongPK>
		implements IdentifiableConfiguration, UserRelatedEntity<UserLongPK> {

	private Long[] nodeIds;
	private String[] sourceIds;
	private String topic;
	private String name;
	private String serviceIdentifier;
	private Map<String, Object> serviceProps;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 */
	public UserNodeEventHookConfiguration(UserLongPK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 */
	public UserNodeEventHookConfiguration(Long userId, Instant created) {
		this(new UserLongPK(userId, null), created);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the long ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 */
	public UserNodeEventHookConfiguration(Long id, Long userId, Instant created) {
		this(new UserLongPK(userId, id), created);
	}

	@Override
	public boolean hasId() {
		UserLongPK id = getId();
		return (id != null && id.getId() != null && id.getUserId() != null);
	}

	@Override
	public Long getUserId() {
		final UserLongPK id = getId();
		return id != null ? id.getUserId() : null;
	}

	/**
	 * Get the node IDs.
	 * 
	 * @return the node IDs, or {@literal null} for any node
	 */
	public Long[] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 * 
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the source IDs.
	 * 
	 * <p>
	 * Source ID Ant-style patterns are allowed.
	 * </p>
	 * 
	 * @return the source IDs
	 */
	public String[] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 * 
	 * @param sourceIds
	 *        the source IDs or source ID Ant-style patterns to set
	 */
	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Get the event topic.
	 * 
	 * @return the topic the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Set the event topic.
	 * 
	 * @param topic
	 *        the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Set the configuration name.
	 * 
	 * @param name
	 *        the name to use
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the unique identifier for the service this configuration is
	 * associated with.
	 * 
	 * @param serviceIdentifier
	 *        the identifier of the service to use
	 */
	public void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
	}

	@Override
	@JsonIgnore
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

	@JsonGetter("serviceProperties")
	public Map<String, Object> getServiceProps() {
		return serviceProps;
	}

	@JsonSetter("serviceProperties")
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
	}

}
