/* ==================================================================
 * ServerMeasurementConfiguration.java - 6/08/2023 12:20:33 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.domain;

import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DNP3 server control configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerControlConfiguration
		extends BaseDnp3ConfigurationEntity<ServerControlConfiguration, UserLongIntegerCompositePK> {

	private static final long serialVersionUID = 1252781573079023190L;

	private Long nodeId;
	private String controlId;
	private ControlType controlType;

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
	public ServerControlConfiguration(UserLongIntegerCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID * @param entityId the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerControlConfiguration(Long userId, Long serverId, Integer entityId, Instant created) {
		this(new UserLongIntegerCompositePK(userId, serverId, entityId), created);
	}

	@Override
	public ServerControlConfiguration copyWithId(UserLongIntegerCompositePK id) {
		var copy = new ServerControlConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ServerControlConfiguration entity) {
		super.copyTo(entity);
		entity.setNodeId(nodeId);
		entity.setControlId(controlId);
		entity.setControlType(controlType);
	}

	@Override
	public boolean isSameAs(ServerControlConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.nodeId, other.getNodeId())
				&& Objects.equals(this.controlId, other.getControlId())
				&& Objects.equals(this.controlType, other.getControlType())
				;
		// @formatter:on
	}

	/**
	 * Get the server ID.
	 * 
	 * @return the server ID
	 */
	public Long getServerId() {
		UserLongIntegerCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	/**
	 * Get the index.
	 * 
	 * @return the index
	 */
	public Integer getIndex() {
		UserLongIntegerCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the datum node ID.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum node ID.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the datum source ID.
	 * 
	 * @return the controlId
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the datum source ID.
	 * 
	 * @param controlId
	 *        the controlId to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Set the measurement type.
	 * 
	 * @return the controlType
	 */
	public ControlType getControlType() {
		return controlType;
	}

	/**
	 * Get the measurement type.
	 * 
	 * @param controlType
	 *        the measurement type to set
	 */
	public void setControlType(ControlType measurementType) {
		this.controlType = measurementType;
	}

}
