/* ==================================================================
 * EndpointConfiguration.java - 19/02/2024 5:03:49 pm
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

package net.solarnetwork.central.din.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Datum input endpoint configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "endpointId", "created", "modified", "enabled", "name", "nodeId",
		"sourceId", "xformId" })
public class EndpointConfiguration extends BaseUserModifiableEntity<EndpointConfiguration, UserUuidPK> {

	private static final long serialVersionUID = 6089703923235658246L;

	private String name;
	private Long nodeId;
	private String sourceId;
	private Long xformId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointConfiguration(UserUuidPK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointConfiguration(Long userId, UUID entityId, Instant created) {
		this(new UserUuidPK(userId, entityId), created);
	}

	@Override
	public EndpointConfiguration copyWithId(UserUuidPK id) {
		var copy = new EndpointConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(EndpointConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setNodeId(nodeId);
		entity.setSourceId(sourceId);
		entity.setXformId(xformId);
	}

	@Override
	public boolean isSameAs(EndpointConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.nodeId, other.nodeId)
				&& Objects.equals(this.sourceId, other.sourceId)
				&& Objects.equals(this.xformId, other.xformId)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Endpoint{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getEndpointId() != null ) {
			builder.append("endpointId=");
			builder.append(getEndpointId());
			builder.append(", ");
		}
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if ( nodeId != null ) {
			builder.append("nodeId=");
			builder.append(nodeId);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		if ( xformId != null ) {
			builder.append("xformId=");
			builder.append(xformId);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	public UUID getEndpointId() {
		UserUuidPK id = getId();
		return (id != null ? id.getUuid() : null);
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
	 * Get the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @return the ID
	 */
	public Long getXformId() {
		return xformId;
	}

	/**
	 * Set the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @param xformId
	 *        the ID to set
	 */
	public void setXformId(Long xformId) {
		this.xformId = xformId;
	}

}
