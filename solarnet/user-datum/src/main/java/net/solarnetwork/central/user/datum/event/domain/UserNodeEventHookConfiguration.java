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

package net.solarnetwork.central.user.datum.event.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.UserLongIdentifiableConfigurationEntity;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicEntity;

/**
 * User and node specific event configuration entity.
 *
 * @author matt
 * @version 1.3
 */
@JsonPropertyOrder({ "id", "userId", "created", "name", "topic", "serviceIdentifier", "nodeIds",
		"sourceIds", "serviceProperties" })
public class UserNodeEventHookConfiguration extends BasicEntity<UserLongPK>
		implements UserLongIdentifiableConfigurationEntity<UserLongPK> {

	@Serial
	private static final long serialVersionUID = 3878313156603958081L;

	private String name;
	private String serviceIdentifier;
	private Long @Nullable [] nodeIds;
	private String @Nullable [] sourceIds;
	private @Nullable String topic;
	private @Nullable Map<String, Object> serviceProps;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeEventHookConfiguration(UserLongPK id, Instant created, String name,
			String serviceIdentifier) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
		this.name = requireNonNullArgument(name, "name");
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserNodeEventHookConfiguration(Long userId, Instant created, String name,
			String serviceIdentifier) {
		this(null, userId, created, name, serviceIdentifier);
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
	 * @param name
	 *        the configuration name
	 * @param serviceIdentifier
	 *        the service identifier
	 * @throws IllegalArgumentException
	 *         if any argument except {@code id} is {@code null}
	 */
	public UserNodeEventHookConfiguration(@Nullable Long id, Long userId, Instant created, String name,
			String serviceIdentifier) {
		this(new UserLongPK(requireNonNullArgument(userId, "userId"), id), created, name,
				serviceIdentifier);
	}

	/**
	 * Create a copy with a given user ID.
	 *
	 * @param userId
	 *        the user ID ot assign to the copy
	 * @return the new copy
	 */
	public UserNodeEventHookConfiguration withUserId(Long userId) {
		UserNodeEventHookConfiguration copy = new UserNodeEventHookConfiguration(getConfigurationId(),
				userId, created(), name, serviceIdentifier);
		copy.setNodeIds(nodeIds);
		copy.setServiceProps(serviceProps);
		copy.setTopic(topic);
		return copy;
	}

	@Override
	public final boolean hasId() {
		UserLongPK id = getId();
		return (id != null && id.getId() != null && id.getUserId() != null);
	}

	@Override
	public final Long getUserId() {
		return id().getUserId();
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 * @since 1.1
	 */
	@JsonGetter("id")
	public final @Nullable Long getConfigurationId() {
		return id().getId();
	}

	/**
	 * Get the node IDs.
	 *
	 * @return the node IDs, or {@code null} for any node
	 */
	public final Long @Nullable [] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 *
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public final void setNodeIds(Long @Nullable [] nodeIds) {
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
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 *
	 * @param sourceIds
	 *        the source IDs or source ID Ant-style patterns to set
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Get the event topic.
	 *
	 * @return the topic
	 */
	public final @Nullable String getTopic() {
		return topic;
	}

	/**
	 * Set the event topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public final void setTopic(@Nullable String topic) {
		this.topic = topic;
	}

	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Set the configuration name.
	 *
	 * @param name
	 *        the name to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

	@Override
	public final String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the unique identifier for the service this configuration is
	 * associated with.
	 *
	 * @param serviceIdentifier
	 *        the identifier of the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	@Override
	@JsonIgnore
	public final @Nullable Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

	@JsonGetter("serviceProperties")
	public final @Nullable Map<String, Object> getServiceProps() {
		return serviceProps;
	}

	@JsonSetter("serviceProperties")
	public final void setServiceProps(@Nullable Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
	}

}
