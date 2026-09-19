/* ==================================================================
 * CloudDatumStreamPropertyConfigurationInput.java - 4/10/2024 2:03:41 pm
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

package net.solarnetwork.central.user.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingRelated;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.dao.BaseUserRelatedStdInput;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * DTO for cloud datum stream property configuration.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class CloudDatumStreamPropertyConfigurationInput extends
		BaseUserRelatedStdInput<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>
		implements
		CloudIntegrationsConfigurationInput<CloudDatumStreamPropertyConfiguration, UserLongIntegerCompositePK>,
		CloudDatumStreamMappingRelated {

	@NotNull
	private @Nullable DatumSamplesType propertyType;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String propertyName;

	@NotNull
	private @Nullable CloudDatumStreamValueType valueType = CloudDatumStreamValueType.Reference;

	@NotNull
	@NotBlank
	@Size(max = 4096)
	private @Nullable String valueReference;

	private @Nullable BigDecimal multiplier;

	private @Nullable Integer scale;

	/**
	 * Constructor.
	 */
	public CloudDatumStreamPropertyConfigurationInput() {
		super();
	}

	@SuppressWarnings("NullAway")
	@Override
	public CloudDatumStreamPropertyConfiguration toEntity(UserLongIntegerCompositePK id, Instant date) {
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(
				requireNonNullArgument(id, "id"), date, propertyType, propertyName, valueType,
				valueReference);
		populateConfiguration(conf);
		return conf;
	}

	@SuppressWarnings("NullAway")
	@Override
	protected void populateConfiguration(CloudDatumStreamPropertyConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setPropertyType(propertyType);
		conf.setPropertyName(propertyName);
		conf.setValueType(valueType);
		conf.setValueReference(valueReference);
		conf.setMultiplier(multiplier);
		conf.setScale(scale);
	}

	/**
	 * Get the datum stream property type to populate.
	 *
	 * @return the property type
	 */
	public final @Nullable DatumSamplesType getPropertyType() {
		return propertyType;
	}

	/**
	 * Set the datum stream property type to populate.
	 *
	 * @param propertyType
	 *        the property type to set
	 */
	public final void setPropertyType(@Nullable DatumSamplesType propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * Get the datum stream property name to populate.
	 *
	 * @return the property name
	 */
	public final @Nullable String getPropertyName() {
		return propertyName;
	}

	/**
	 * Set the datum stream property name to populate.
	 *
	 * @param propertyName
	 *        the property name to set
	 */
	public final void setPropertyName(@Nullable String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * Get the value type.
	 *
	 * @return the value type
	 */
	public final @Nullable CloudDatumStreamValueType getValueType() {
		return valueType;
	}

	/**
	 * Set the value type.
	 *
	 * @param valueType
	 *        the value type to set
	 */
	public final void setValueType(@Nullable CloudDatumStreamValueType valueType) {
		this.valueType = valueType;
	}

	/**
	 * Get the integration-specific source data value reference.
	 *
	 * @return a reference to the source data value to populate on the datum
	 *         stream
	 */
	public final @Nullable String getValueReference() {
		return valueReference;
	}

	/**
	 * Set the integration-specific source data value reference.
	 *
	 * @param valueReference
	 *        a reference to the source data value to populate on the datum
	 *        stream
	 */
	public final void setValueReference(@Nullable String valueReference) {
		this.valueReference = valueReference;
	}

	/**
	 * Get the value multiplier.
	 *
	 * @return a number to multiply source data values by, or {@code null} for
	 *         no change
	 */
	public final @Nullable BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the value multiplier.
	 *
	 * @param multiplier
	 *        a number to multiply source data values by, or {@code null} for no
	 *        change
	 */
	public final void setMultiplier(@Nullable BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the value decimal scale.
	 *
	 * @return the scale to round the property value to, or {@code null} or less
	 *         than 0 for no rounding
	 */
	public final @Nullable Integer getScale() {
		return scale;
	}

	/**
	 * Set the value decimal scale.
	 *
	 * @param scale
	 *        the scale to round the property value to, or {@code null} or less
	 *        than 0 for no rounding
	 */
	public final void setScale(@Nullable Integer scale) {
		this.scale = scale;
	}

}
