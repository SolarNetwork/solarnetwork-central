/* ==================================================================
 * AssetPropertyConfiguration.java - 6/09/2022 3:00:16 pm
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

package net.solarnetwork.central.oscp.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.util.ObjectUtils;

/**
 * Base datum configuration for assets.
 *
 * @author matt
 * @version 1.2
 */
public abstract sealed class BaseAssetDatumConfiguration implements Cloneable
		permits AssetEnergyDatumConfiguration, AssetInstantaneousDatumConfiguration {

	private String[] propertyNames;
	private MeasurementUnit unit;
	private @Nullable BigDecimal multiplier;
	private StatisticType statisticType;

	/**
	 * Constructor.
	 *
	 * @param propertyNames
	 *        the property names
	 * @param unit
	 *        the unit
	 * @param statisticType
	 *        the statistics type
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	protected BaseAssetDatumConfiguration(String[] propertyNames, MeasurementUnit unit,
			StatisticType statisticType) {
		super();
		this.propertyNames = requireNonEmptyArgument(propertyNames, "propertyNames");
		this.unit = requireNonNullArgument(unit, "unit");
		this.statisticType = requireNonNullArgument(statisticType, "statisticType");
	}

	/**
	 * Copy the properties of this instance into another.
	 *
	 * @param copy
	 *        the instance to copy into
	 */
	public void copyTo(BaseAssetDatumConfiguration copy) {
		if ( propertyNames != null ) {
			copy.propertyNames = Arrays.copyOf(propertyNames, propertyNames.length);
		}
		copy.unit = unit;
		copy.multiplier = multiplier;
		copy.statisticType = statisticType;
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
	@SuppressWarnings("ReferenceEquality")
	public boolean isSameAs(@Nullable BaseAssetDatumConfiguration other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return (Arrays.equals(propertyNames, other.propertyNames)
				&& Objects.equals(unit, other.unit)
				&& ObjectUtils.comparativelyEqual(multiplier, other.multiplier)
				&& Objects.equals(statisticType, other.statisticType));
		// @formatter:on
	}

	@Override
	protected BaseAssetDatumConfiguration clone() {
		try {
			return (BaseAssetDatumConfiguration) super.clone();
		} catch ( CloneNotSupportedException e ) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Get the property names.
	 *
	 * @return the property names.
	 */
	public final String[] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names.
	 *
	 * @param propertyNames
	 *        the property names to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null} or empty
	 */
	public final void setPropertyNames(String[] propertyNames) {
		this.propertyNames = requireNonEmptyArgument(propertyNames, "propertyNames");
	}

	/**
	 * Get the measurement unit.
	 *
	 * @return the unit
	 */
	public final MeasurementUnit getUnit() {
		return unit;
	}

	/**
	 * Set the measurement unit.
	 *
	 * @param unit
	 *        the unit to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setUnit(MeasurementUnit unit) {
		this.unit = requireNonNullArgument(unit, "unit");
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
	public final StatisticType getStatisticType() {
		return statisticType;
	}

	/**
	 * Set the statistic type.
	 *
	 * @param statisticType
	 *        the statistic type to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setStatisticType(StatisticType statisticType) {
		this.statisticType = requireNonNullArgument(statisticType, "statisticType");
	}

}
