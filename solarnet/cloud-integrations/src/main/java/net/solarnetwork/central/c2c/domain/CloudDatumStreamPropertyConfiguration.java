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

import java.io.Serial;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * A cloud datum stream property configuration entity.
 *
 * <p>
 * The purpose of this entity is to define the mapping from a cloud data
 * reference to a datum stream property.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
@JsonIgnoreProperties({ "id", "fullyConfigured" })
@JsonPropertyOrder({ "userId", "datumStreamMappingId", "index", "created", "modified", "enabled",
		"propertyType", "propertyName", "valueType", "valueReference", "multiplier", "scale" })
public final class CloudDatumStreamPropertyConfiguration extends
		BaseUserModifiableEntity<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>
		implements
		CloudIntegrationsConfigurationEntity<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK> {

	@Serial
	private static final long serialVersionUID = -3814015511662489974L;

	private DatumSamplesType propertyType;
	private String propertyName;
	private CloudDatumStreamValueType valueType;
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
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudDatumStreamPropertyConfiguration(Long userId, Long datumStreamMappingId, Integer index,
			Instant created) {
		this(new UserLongIntegerCompositePK(userId, datumStreamMappingId, index), created);
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
		entity.setValueType(valueType);
		entity.setValueReference(valueReference);
		entity.setMultiplier(multiplier);
		entity.setScale(scale);
	}

	@SuppressWarnings("ReferenceEquality")
	@Override
	public boolean isSameAs(CloudDatumStreamPropertyConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.propertyType, other.propertyType)
				&& Objects.equals(this.propertyName, other.propertyName)
				&& Objects.equals(this.valueType, other.valueType)
				&& Objects.equals(this.valueReference, other.valueReference)
				&& ObjectUtils.comparativelyEqual(this.multiplier, other.multiplier)
				&& Objects.equals(this.scale, other.scale)
				;
		// @formatter:on
	}

	@Override
	public boolean isFullyConfigured() {
		return propertyType != null && propertyName != null && !propertyName.isEmpty()
				&& valueType != null && valueReference != null && !valueReference.isEmpty();
	}

	/**
	 * Apply the configured unit multiplier and decimal scale, if appropriate.
	 *
	 * @param propVal
	 *        the property value to transform; only {@link Number} values will
	 *        be transformed
	 * @return the transformed property value to use
	 */
	public Object applyValueTransforms(Object propVal) {
		if ( propVal instanceof Number n ) {
			if ( multiplier != null ) {
				n = applyUnitMultiplier(n, multiplier);
			}
			if ( scale != null ) {
				n = applyDecimalScale(n, scale);
			}
			if ( n instanceof BigDecimal d ) {
				n = d.stripTrailingZeros();
			}
			propVal = NumberUtils.narrow(n, 2);
		}
		return propVal;
	}

	private static Number applyDecimalScale(Number value, int decimalScale) {
		if ( decimalScale < 0 ) {
			return value;
		}
		if ( value instanceof Byte || value instanceof Short || value instanceof Integer
				|| value instanceof Long || value instanceof BigInteger ) {
			// no decimal here
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		if ( v.scale() > decimalScale ) {
			v = v.setScale(decimalScale, RoundingMode.HALF_UP);
		}
		return v;
	}

	private static Number applyUnitMultiplier(Number value, BigDecimal multiplier) {
		if ( BigDecimal.ONE.compareTo(multiplier) == 0 ) {
			return value;
		}
		BigDecimal v = NumberUtils.bigDecimalForNumber(value);
		return v.multiply(multiplier);
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
		if ( getDatumStreamMappingId() != null ) {
			builder.append("datumStreamMappingId=");
			builder.append(getDatumStreamMappingId());
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
		if ( valueType != null ) {
			builder.append("valueType=");
			builder.append(valueType);
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
	 * Get the cloud datum stream mapping ID.
	 *
	 * @return the cloud datum stream mapping ID
	 */
	public Long getDatumStreamMappingId() {
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
	public DatumSamplesType getPropertyType() {
		return propertyType;
	}

	/**
	 * Set the datum stream property type to populate.
	 *
	 * @param propertyType
	 *        the property type to set
	 */
	public void setPropertyType(DatumSamplesType propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * Get the datum stream property name to populate.
	 *
	 * @return the property name
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Set the datum stream property name to populate.
	 *
	 * @param propertyName
	 *        the property name to set
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * Get the value type.
	 *
	 * @return the value type
	 */
	public CloudDatumStreamValueType getValueType() {
		return valueType;
	}

	/**
	 * Set the value type.
	 *
	 * @param valueType
	 *        the value type to set
	 */
	public void setValueType(CloudDatumStreamValueType valueType) {
		this.valueType = valueType;
	}

	/**
	 * Get the integration-specific source data value reference.
	 *
	 * @return a reference to the source data value to populate on the datum
	 *         stream
	 */
	public String getValueReference() {
		return valueReference;
	}

	/**
	 * Set the integration-specific source data value reference.
	 *
	 * @param valueReference
	 *        a reference to the source data value to populate on the datum
	 *        stream
	 */
	public void setValueReference(String valueReference) {
		this.valueReference = valueReference;
	}

	/**
	 * Get the value multiplier.
	 *
	 * @return a number to multiply source data values by, or {@literal null}
	 *         for no change
	 */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the value multiplier.
	 *
	 * @param multiplier
	 *        a number to multiply source data values by, or {@literal null} for
	 *        no change
	 */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the value decimal scale.
	 *
	 * @return the scale to round the property value to, or {@literal null} or
	 *         less than 0 for no rounding
	 */
	public Integer getScale() {
		return scale;
	}

	/**
	 * Set the value decimal scale.
	 *
	 * @param scale
	 *        the scale to round the property value to, or {@literal null} or
	 *        less than 0 for no rounding
	 */
	public void setScale(Integer scale) {
		this.scale = scale;
	}

}
