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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DNP3 server measurement configuration.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "serverId", "index", "created", "modified", "enabled", "nodeId",
		"sourceId", "property", "measurementType", "multiplier", "offset", "scale" })
public class ServerMeasurementConfiguration
		extends BaseUserModifiableEntity<ServerMeasurementConfiguration, UserLongIntegerCompositePK> {

	private static final long serialVersionUID = 4025037339553169877L;

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
	 * Test if this configuration is valid.
	 * 
	 * <p>
	 * This only checks the existence and non-blankness of the fields necessary
	 * to configure in DNP3.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration is valid
	 */
	public boolean isValid() {
		final Long nodeId = getNodeId();
		final String sourceId = getSourceId();
		final String property = getProperty();
		final MeasurementType type = getMeasurementType();
		return (nodeId != null && sourceId != null && property != null && type != null
				&& !sourceId.isBlank() && !property.isBlank());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServerMeasurement{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getServerId() != null ) {
			builder.append("serverId=");
			builder.append(getServerId());
			builder.append(", ");
		}
		if ( getIndex() != null ) {
			builder.append("index=");
			builder.append(getIndex());
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
		if ( property != null ) {
			builder.append("property=");
			builder.append(property);
			builder.append(", ");
		}
		if ( measurementType != null ) {
			builder.append("measurementType=");
			builder.append(measurementType);
			builder.append(", ");
		}
		if ( multiplier != null ) {
			builder.append("multiplier=");
			builder.append(multiplier);
			builder.append(", ");
		}
		if ( offset != null ) {
			builder.append("offset=");
			builder.append(offset);
			builder.append(", ");
		}
		if ( scale != null ) {
			builder.append("scale=");
			builder.append(scale);
			builder.append(", ");
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
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
