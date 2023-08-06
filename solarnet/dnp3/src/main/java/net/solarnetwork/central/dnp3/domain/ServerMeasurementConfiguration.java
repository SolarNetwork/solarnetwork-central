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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DNP3 server measurement configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerMeasurementConfiguration
		extends BaseDnp3ConfigurationEntity<ServerMeasurementConfiguration, UserLongIntegerCompositePK> {

	private static final long serialVersionUID = -6645715641643850671L;

	private Long nodeId;
	private String sourceId;
	private String property;
	private MeasurementType measurementType;
	private BigDecimal multiplier;
	private BigDecimal offset;
	private Integer scale;

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
	public ServerMeasurementConfiguration(UserLongIntegerCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerMeasurementConfiguration(Long userId, Long serverId, Integer entityId,
			Instant created) {
		this(new UserLongIntegerCompositePK(userId, serverId, entityId), created);
	}

	@Override
	public ServerMeasurementConfiguration copyWithId(UserLongIntegerCompositePK id) {
		var copy = new ServerMeasurementConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ServerMeasurementConfiguration entity) {
		super.copyTo(entity);
		entity.setNodeId(nodeId);
		entity.setSourceId(sourceId);
		entity.setProperty(property);
		entity.setMeasurementType(measurementType);
		entity.setMultiplier(multiplier);
		entity.setOffset(offset);
		entity.setScale(scale);
	}

	@Override
	public boolean isSameAs(ServerMeasurementConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.nodeId, other.getNodeId())
				&& Objects.equals(this.sourceId, other.getSourceId())
				&& Objects.equals(this.property, other.getProperty())
				&& Objects.equals(this.measurementType, other.getMeasurementType())
				&& Objects.equals(this.multiplier, other.getMultiplier())
				&& Objects.equals(this.offset, other.getOffset())
				&& Objects.equals(this.scale, other.getScale())
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
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum source ID.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the datum property name.
	 * 
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Set the datum property name.
	 * 
	 * @param property
	 *        the property to set
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * Set the measurement type.
	 * 
	 * @return the measurementType
	 */
	public MeasurementType getMeasurementType() {
		return measurementType;
	}

	/**
	 * Get the measurement type.
	 * 
	 * @param measurementType
	 *        the measurement type to set
	 */
	public void setMeasurementType(MeasurementType measurementType) {
		this.measurementType = measurementType;
	}

	/**
	 * Get the decimal multiplier.
	 * 
	 * @return the multiplier
	 */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the decimal multiplier.
	 * 
	 * @param multiplier
	 *        the multiplier to set
	 */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the decimal offset.
	 * 
	 * @return the offset
	 */
	public BigDecimal getOffset() {
		return offset;
	}

	/**
	 * Set the decimal offset.
	 * 
	 * @param offset
	 *        the offset to set
	 */
	public void setOffset(BigDecimal offset) {
		this.offset = offset;
	}

	/**
	 * Get the decimal scale
	 * 
	 * @return the scale
	 */
	public Integer getScale() {
		return scale;
	}

	/**
	 * Set the decimal scale.
	 * 
	 * @param scale
	 *        the scale to set
	 */
	public void setScale(Integer scale) {
		this.scale = scale;
	}

}
