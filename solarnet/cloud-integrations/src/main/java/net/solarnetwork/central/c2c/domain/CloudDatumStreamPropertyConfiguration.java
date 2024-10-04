/* ==================================================================
 * CloudDatumStreamPropertyPropertyConfiguration.java - 4/10/2024 6:35:16 am
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

package net.solarnetwork.central.c2c.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * A cloud datum stream property configuration entity.
 *
 * <p>
 * The purpose of this entity is to define the mapping from a cloud data
 * reference to a datum stream property.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "datumStreamId", "index", "created", "modified", "enabled",
		"propertyType", "propertyName", "valueReference", "multiplier", "scale" })
public class CloudDatumStreamPropertyConfiguration extends
		BaseUserModifiableEntity<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>
		implements
		CloudIntegrationsConfigurationEntity<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK> {

	private static final long serialVersionUID = 1997920480387562443L;

	private DatumSamplesType propertyType;
	private String propertyName;
	private String valueReference;
	private BigDecimal multiplier;
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
	public CloudDatumStreamPropertyConfiguration(UserLongIntegerCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param dataSourceId
	 *        the data source ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamPropertyConfiguration(Long userId, Long dataSourceId, Integer index,
			Instant created) {
		this(new UserLongIntegerCompositePK(userId, dataSourceId, index), created);
	}

	@Override
	public CloudDatumStreamPropertyConfiguration copyWithId(UserLongIntegerCompositePK id) {
		var copy = new CloudDatumStreamPropertyConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamPropertyConfiguration entity) {
		super.copyTo(entity);
		entity.setPropertyType(propertyType);
		entity.setPropertyName(propertyName);
		entity.setValueReference(valueReference);
		entity.setMultiplier(multiplier);
		entity.setScale(scale);
	}

	@Override
	public boolean isSameAs(CloudDatumStreamPropertyConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.propertyType, other.propertyType)
				&& Objects.equals(this.propertyName, other.propertyName)
				&& Objects.equals(this.valueReference, other.valueReference)
				&& Objects.equals(this.multiplier, other.multiplier)
				&& Objects.equals(this.scale, other.scale)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudDatumStreamProperty{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getDatumStreamId() != null ) {
			builder.append("datumStreamId=");
			builder.append(getDatumStreamId());
			builder.append(", ");
		}
		if ( propertyType != null ) {
			builder.append("propertyType=");
			builder.append(propertyType);
			builder.append(", ");
		}
		if ( propertyName != null ) {
			builder.append("propertyName=");
			builder.append(propertyName);
			builder.append(", ");
		}
		if ( valueReference != null ) {
			builder.append("valueReference=");
			builder.append(valueReference);
			builder.append(", ");
		}
		if ( multiplier != null ) {
			builder.append("multiplier=");
			builder.append(multiplier);
			builder.append(", ");
		}
		if ( scale != null ) {
			builder.append("scale=");
			builder.append(scale);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the cloud datum stream ID.
	 *
	 * @return the cloud datum stream ID
	 */
	public Long getDatumStreamId() {
		UserLongIntegerCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	/**
	 * Get the index (sort order).
	 *
	 * @return the index
	 */
	public Integer getIndex() {
		UserLongIntegerCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the datum stream property type to populate.
	 *
	 * @return the property type
	 */
	public final DatumSamplesType getPropertyType() {
		return propertyType;
	}

	/**
	 * Set the datum stream property type to populate.
	 *
	 * @param propertyType
	 *        the property type to set
	 */
	public final void setPropertyType(DatumSamplesType propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * Get the datum stream property name to populate.
	 *
	 * @return the property name
	 */
	public final String getPropertyName() {
		return propertyName;
	}

	/**
	 * Set the datum stream property name to populate.
	 *
	 * @param propertyName
	 *        the property name to set
	 */
	public final void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * Get the integration-specific source data value reference.
	 *
	 * @return a reference to the source data value to populate on the datum
	 *         stream
	 */
	public final String getValueReference() {
		return valueReference;
	}

	/**
	 * Set the integration-specific source data value reference.
	 *
	 * @param valueReference
	 *        a reference to the source data value to populate on the datum
	 *        stream
	 */
	public final void setValueReference(String valueReference) {
		this.valueReference = valueReference;
	}

	/**
	 * Get the value multiplier.
	 *
	 * @return a number to multiply source data values by, or {@literal null}
	 *         for no change
	 */
	public final BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the value multiplier.
	 *
	 * @param multiplier
	 *        a number to multiply source data values by, or {@literal null} for
	 *        no change
	 */
	public final void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the value decimal scale.
	 *
	 * @return the scale to round the property value to, or {@literal null} or
	 *         less than 0 for no rounding
	 */
	public final Integer getScale() {
		return scale;
	}

	/**
	 * Set the value decimal scale.
	 *
	 * @param scale
	 *        the scale to round the property value to, or {@literal null} or
	 *        less than 0 for no rounding
	 */
	public final void setScale(Integer scale) {
		this.scale = scale;
	}

}
