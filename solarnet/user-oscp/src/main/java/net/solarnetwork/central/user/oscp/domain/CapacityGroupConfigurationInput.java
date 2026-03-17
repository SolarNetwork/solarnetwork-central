/* ==================================================================
 * CapacityGroupConfigurationInput.java - 15/08/2022 1:15:01 pm
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

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;

/**
 * DTO for capacity group configuration.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class CapacityGroupConfigurationInput
		extends BaseOscpConfigurationInput<CapacityGroupConfiguration> {

	@NotNull
	@NotBlank
	@Size(max = 128)
	private @Nullable String identifier;

	@NotNull
	private @Nullable MeasurementPeriod capacityProviderMeasurementPeriod;

	@NotNull
	private @Nullable MeasurementPeriod capacityOptimizerMeasurementPeriod;

	@NotNull
	private @Nullable Long capacityProviderId;

	@NotNull
	private @Nullable Long capacityOptimizerId;

	@SuppressWarnings("NullAway")
	@Override
	public CapacityGroupConfiguration toEntity(UserLongCompositePK id) {
		CapacityGroupConfiguration conf = new CapacityGroupConfiguration(id, Instant.now(), getName(),
				identifier, capacityProviderId, capacityOptimizerId, capacityProviderMeasurementPeriod,
				capacityOptimizerMeasurementPeriod);
		populateConfiguration(conf);
		return conf;
	}

	@SuppressWarnings("NullAway")
	@Override
	protected void populateConfiguration(CapacityGroupConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setIdentifier(identifier);
		conf.setCapacityProviderMeasurementPeriod(capacityProviderMeasurementPeriod);
		conf.setCapacityOptimizerMeasurementPeriod(capacityOptimizerMeasurementPeriod);
		conf.setCapacityProviderId(capacityProviderId);
		conf.setCapacityOptimizerId(capacityOptimizerId);
	}

	/**
	 * Get the group identifier.
	 *
	 * @return the identifier
	 */
	public final @Nullable String getIdentifier() {
		return identifier;
	}

	/**
	 * Set the group identifier.
	 *
	 * @param identifier
	 *        the identifier to set
	 */
	public final void setIdentifier(@Nullable String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Get the Capacity Provider measurement period.
	 *
	 * @return the period
	 */
	public final @Nullable MeasurementPeriod getCapacityProviderMeasurementPeriod() {
		return capacityProviderMeasurementPeriod;
	}

	/**
	 * Set the Capacity Provider measurement period.
	 *
	 * @param capacityProviderMeasurementPeriod
	 *        the period to set
	 */
	public final void setCapacityProviderMeasurementPeriod(
			@Nullable MeasurementPeriod capacityProviderMeasurementPeriod) {
		this.capacityProviderMeasurementPeriod = capacityProviderMeasurementPeriod;
	}

	/**
	 * Get the Capacity Optimizer measurement period.
	 *
	 * @return the period
	 */
	public final @Nullable MeasurementPeriod getCapacityOptimizerMeasurementPeriod() {
		return capacityOptimizerMeasurementPeriod;
	}

	/**
	 * Set the Capacity Optimizer measurement period.
	 *
	 * @param capacityOptimizerMeasurementPeriod
	 *        the period to set
	 */
	public final void setCapacityOptimizerMeasurementPeriod(
			@Nullable MeasurementPeriod capacityOptimizerMeasurementPeriod) {
		this.capacityOptimizerMeasurementPeriod = capacityOptimizerMeasurementPeriod;
	}

	/**
	 * Get the capacity provider ID.
	 *
	 * @return the ID of the associated {@link CapacityProviderConfiguration}
	 */
	public final @Nullable Long getCapacityProviderId() {
		return capacityProviderId;
	}

	/**
	 * Set the capacity provider ID.
	 *
	 * @param capacityProviderId
	 *        the ID of the capacity provider to set
	 */
	public final void setCapacityProviderId(@Nullable Long capacityProviderId) {
		this.capacityProviderId = capacityProviderId;
	}

	/**
	 * Get the capacity optimizer ID.
	 *
	 * @return the ID of the associated {@link CapacityOptimizerConfiguration}
	 */
	public final @Nullable Long getCapacityOptimizerId() {
		return capacityOptimizerId;
	}

	/**
	 * Set the capacity optimizer ID.
	 *
	 * @param capacityOptimizerId
	 *        the ID of the capacity optimizer to set
	 */
	public final void setCapacityOptimizerId(@Nullable Long capacityOptimizerId) {
		this.capacityOptimizerId = capacityOptimizerId;
	}

}
