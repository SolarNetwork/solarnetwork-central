/* ==================================================================
 * BaseServerDatumStreamConfiguration.java - 12/08/2023 7:04:24 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.CodedValue;

/**
 * Base entity for datum stream related configuration.
 * 
 * @param <C>
 *        the configuration type
 * @param <T>
 *        the type type
 * @author matt
 * @version 1.0
 */
public abstract class BaseServerDatumStreamConfiguration<C extends BaseServerDatumStreamConfiguration<C, T>, T extends CodedValue>
		extends BaseUserModifiableEntity<C, UserLongIntegerCompositePK> {

	private static final long serialVersionUID = 659642643824344175L;

	private Long nodeId;
	private String sourceId;
	private String property;
	private T type;
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
	public BaseServerDatumStreamConfiguration(UserLongIntegerCompositePK id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setNodeId(nodeId);
		entity.setSourceId(sourceId);
		entity.setProperty(property);
		entity.setType(type);
		entity.setMultiplier(multiplier);
		entity.setOffset(offset);
		entity.setScale(scale);
	}

	/**
	 * Test if this entity has the same property values as another.
	 * 
	 * <p>
	 * The {@code id}, {@code created}, and {@code modified} properties are not
	 * compared.
	 * </p>
	 * 
	 * @param other
	 *        the entity to compare to
	 * @return {@literal true} if the properties of this entity are equal to the
	 *         other's
	 */
	@Override
	public boolean isSameAs(C other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.nodeId, other.getNodeId())
				&& Objects.equals(this.sourceId, other.getSourceId())
				&& Objects.equals(this.property, other.getProperty())
				&& Objects.equals(this.type, other.getType())
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
		final T type = getType();
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
		if ( type != null ) {
			builder.append("measurementType=");
			builder.append(type);
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
	 * Set the type.
	 * 
	 * @return the type
	 */
	public T getType() {
		return type;
	}

	/**
	 * Get the type.
	 * 
	 * @param type
	 *        the type to set
	 */
	public void setType(T type) {
		this.type = type;
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
