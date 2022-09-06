/* ==================================================================
 * AssetEnergyConfiguration.java - 6/09/2022 2:58:28 pm
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

import java.util.Objects;
import net.solarnetwork.domain.Differentiable;

/**
 * Asset energy datum configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class AssetEnergyDatumConfiguration extends BaseAssetDatumConfiguration
		implements Differentiable<AssetEnergyDatumConfiguration> {

	private EnergyType type;
	private EnergyDirection direction;

	/**
	 * Copy the properties of this instance into another.
	 * 
	 * @param copy
	 *        the instance to copy into
	 */
	public void copyTo(AssetEnergyDatumConfiguration copy) {
		super.copyTo(copy);
		copy.type = type;
		copy.direction = direction;
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
	public boolean isSameAs(AssetEnergyDatumConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return (Objects.equals(this.type, other.type)
				&& Objects.equals(this.direction, other.direction));
		// @formatter:on
	}

	@Override
	public boolean differsFrom(AssetEnergyDatumConfiguration other) {
		return !isSameAs(other);
	}

	/**
	 * Get the statistics type, defaulting to {@code Difference} if not
	 * configured.
	 * 
	 * @return the statistics type, never {@literal null}.
	 */
	public StatisticType statisticType() {
		StatisticType type = getStatisticType();
		return (type != null ? type : StatisticType.Difference);
	}

	/**
	 * Get the energy type.
	 * 
	 * @return the energy type.
	 */
	public EnergyType getType() {
		return type;
	}

	/**
	 * Set the energy type.
	 * 
	 * @param type
	 *        the type to set
	 */
	public void setType(EnergyType type) {
		this.type = type;
	}

	/**
	 * Get the energy direction.
	 * 
	 * @return the energy direction
	 */
	public EnergyDirection getDirection() {
		return direction;
	}

	/**
	 * Set the energy direction.
	 * 
	 * @param direction
	 *        the direction to set
	 */
	public void setDirection(EnergyDirection direction) {
		this.direction = direction;
	}

}
