/* ==================================================================
 * UserNodeEventHookConfigurationInput.java - 2 Oct 2026 9:26:18 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.domain;

import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.user.datum.event.domain.UserNodeEventHookConfiguration;

/**
 * Input DTO for {@link UserNodeEventHookConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public final class UserNodeEventHookConfigurationInput {

	private @Nullable Long id;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String name;

	@NotNull
	@NotBlank
	private @Nullable String topic;

	@NotNull
	@NotBlank
	@Size(max = 128)
	private @Nullable String serviceIdentifier;

	private Long @Nullable [] nodeIds;

	private String @Nullable [] sourceIds;

	private @Nullable Map<String, Object> serviceProperties;

	/**
	 * Constructor.
	 */
	public UserNodeEventHookConfigurationInput() {
		super();
	}

	/**
	 * Create an entity from the input properties and a given user and date.
	 *
	 * <p>
	 * If no ID is available then the entity will not have a configuration ID
	 * assigned.
	 * </p>
	 *
	 * @param userId
	 *        the ID of the user to assign to the entity
	 * @param date
	 *        the creation date to use
	 * @return the new entity
	 */
	@SuppressWarnings("NullAway")
	public UserNodeEventHookConfiguration toEntity(Long userId, Instant date) {
		UserNodeEventHookConfiguration entity = new UserNodeEventHookConfiguration(id, userId, date,
				name, serviceIdentifier);
		entity.setTopic(topic);
		entity.setNodeIds(nodeIds);
		entity.setSourceIds(sourceIds);
		entity.setServiceProps(serviceProperties);
		return entity;
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public final void setId(@Nullable Long id) {
		this.id = id;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		this.name = name;
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

	/**
	 * Get the identifier of the service to use.
	 *
	 * @return the identifier
	 */
	public final @Nullable String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the identifier of the service to use.
	 *
	 * @param serviceIdentifier
	 *        the identifier to use
	 */
	public final void setServiceIdentifier(@Nullable String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
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
	 *        the node IDs to set, or {@code null} for any node
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
	 * @return the source IDs, or {@code null} for any source
	 */
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 *
	 * @param sourceIds
	 *        the source IDs or source ID Ant-style patterns to set, or
	 *        {@code null} for any source
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public final @Nullable Map<String, Object> getServiceProperties() {
		return serviceProperties;
	}

	/**
	 * Set the service properties to use.
	 *
	 * @param serviceProperties
	 *        the service properties to set
	 */
	public final void setServiceProperties(@Nullable Map<String, Object> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

}
