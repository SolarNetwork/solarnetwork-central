/* ==================================================================
 * BaseAssetDatumConfigurationInput.java - 6/09/2022 7:36:48 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.oscp.domain;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.oscp.domain.BaseAssetDatumConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.StatisticType;

/**
 * Base asset datum configuration input.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public abstract sealed class BaseAssetDatumConfigurationInput
		permits AssetEnergyDatumConfigurationInput, AssetInstantaneousDatumConfigurationInput {

	@NotNull
	@NotEmpty
	private String @Nullable [] propertyNames;

	@NotNull
	private @Nullable MeasurementUnit unit;

	private @Nullable BigDecimal multiplier;

	@NotNull
	private @Nullable StatisticType statisticType;

	/**
	 * Populate an entity configuration with values from this input.
	 *
	 * @param conf
	 *        the configuration to populate
	 */
	@SuppressWarnings("NullAway")
	public void populateConfiguration(BaseAssetDatumConfiguration conf) {
		conf.setPropertyNames(propertyNames);
		conf.setUnit(unit);
		conf.setMultiplier(multiplier);
		conf.setStatisticType(statisticType);
	}

	/**
	 * Get the property names.
	 *
	 * @return the property names.
	 */
	public final String @Nullable [] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names.
	 *
	 * @param propertyNames
	 *        the property names to set
	 */
	public final void setPropertyNames(String @Nullable [] propertyNames) {
		this.propertyNames = propertyNames;
	}

	/**
	 * Get the measurement unit.
	 *
	 * @return the unit
	 */
	public final @Nullable MeasurementUnit getUnit() {
		return unit;
	}

	/**
	 * Set the measurement unit.
	 *
	 * @param unit
	 *        the unit to set
	 */
	public final void setUnit(@Nullable MeasurementUnit unit) {
		this.unit = unit;
	}

	/**
	 * Get the datum stream property value multiplier.
	 *
	 * @return the multiplier to convert property values into {@code unit}, or
	 *         {@code null} for no conversion
	 */
	public final @Nullable BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the datum stream property value multiplier.
	 *
	 * @param multiplier
	 *        the multiplier to convert property values into
	 *        {@code instantaneousUnit}, or {@code null} for no conversion
	 */
	public final void setMultiplier(@Nullable BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the statistic type.
	 *
	 * @return the statistic type
	 */
	public final @Nullable StatisticType getStatisticType() {
		return statisticType;
	}

	/**
	 * Set the statistic type.
	 *
	 * @param statisticType
	 *        the statistic type to set
	 */
	public final void setStatisticType(@Nullable StatisticType statisticType) {
		this.statisticType = statisticType;
	}

}
