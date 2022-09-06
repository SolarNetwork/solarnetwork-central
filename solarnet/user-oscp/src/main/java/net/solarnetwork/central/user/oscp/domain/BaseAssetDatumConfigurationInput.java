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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import net.solarnetwork.central.oscp.domain.BaseAssetDatumConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.StatisticType;

/**
 * Base asset datum configuration input.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseAssetDatumConfigurationInput {

	@NotNull
	@NotEmpty
	private String[] propertyNames;

	@NotNull
	private MeasurementUnit unit;

	private BigDecimal multiplier;

	@NotNull
	private StatisticType statisticType;

	/**
	 * Populate an entity configuration with values from this input.
	 * 
	 * @param conf
	 *        the configuration to populate
	 */
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
	public String[] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names.
	 * 
	 * @param propertyNames
	 *        the property names to set
	 */
	public void setPropertyNames(String[] propertyNames) {
		this.propertyNames = propertyNames;
	}

	/**
	 * Get the measurement unit.
	 * 
	 * @return the unit
	 */
	public MeasurementUnit getUnit() {
		return unit;
	}

	/**
	 * Set the measurement unit.
	 * 
	 * @param unit
	 *        the unit to set
	 */
	public void setUnit(MeasurementUnit unit) {
		this.unit = unit;
	}

	/**
	 * Get the datum stream property value multiplier.
	 * 
	 * @return the multiplier to convert property values into {@code unit}, or
	 *         {@literal null} for no conversion
	 */
	public BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the datum stream property value multiplier.
	 * 
	 * @param instantaneousMultiplier
	 *        the multiplier to convert property values into
	 *        {@code instantaneousUnit}, or {@literal null} for no conversion
	 */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the statistic type.
	 * 
	 * @return the statistic type
	 */
	public StatisticType getStatisticType() {
		return statisticType;
	}

	/**
	 * Set the statistic type.
	 * 
	 * @param statisticType
	 *        the statistic type to set
	 */
	public void setStatisticType(StatisticType statisticType) {
		this.statisticType = statisticType;
	}

}
