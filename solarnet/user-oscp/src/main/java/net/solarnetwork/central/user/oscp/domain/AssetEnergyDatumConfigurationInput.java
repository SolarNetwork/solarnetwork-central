/* ==================================================================
 * AssetInstantaneousDatumConfigurationInput.java - 6/09/2022 7:40:33 pm
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

import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.oscp.domain.AssetEnergyDatumConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyDirection;
import net.solarnetwork.central.oscp.domain.EnergyType;

/**
 * Input for asset energy datum configuration.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public final class AssetEnergyDatumConfigurationInput extends BaseAssetDatumConfigurationInput {

	@NotNull
	private @Nullable EnergyType type;

	@NotNull
	private @Nullable EnergyDirection direction;

	/**
	 * Create an entity from this input.
	 *
	 * @return the entity
	 */
	@SuppressWarnings("NullAway")
	public AssetEnergyDatumConfiguration toEntity() {
		AssetEnergyDatumConfiguration conf = new AssetEnergyDatumConfiguration(getPropertyNames(),
				getUnit(), getStatisticType(), getType(), getDirection());
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Populate an entity configuration with values from this input.
	 *
	 * @param conf
	 *        the configuration to populate
	 */
	@SuppressWarnings("NullAway")
	public void populateConfiguration(AssetEnergyDatumConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setType(type);
		conf.setDirection(direction);
	}

	/**
	 * Get the energy type.
	 *
	 * @return the energy type.
	 */
	public final @Nullable EnergyType getType() {
		return type;
	}

	/**
	 * Set the energy type.
	 *
	 * @param type
	 *        the type to set
	 */
	public final void setType(@Nullable EnergyType type) {
		this.type = type;
	}

	/**
	 * Get the energy direction.
	 *
	 * @return the energy direction
	 */
	public final @Nullable EnergyDirection getDirection() {
		return direction;
	}

	/**
	 * Set the energy direction.
	 *
	 * @param direction
	 *        the direction to set
	 */
	public final void setDirection(@Nullable EnergyDirection direction) {
		this.direction = direction;
	}

}
