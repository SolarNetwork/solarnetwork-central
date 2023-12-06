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
public class CapacityGroupConfigurationInput
		extends BaseOscpConfigurationInput<CapacityGroupConfiguration> {

	@NotNull
	@NotBlank
	@Size(max = 128)
	private String identifier;

	@NotNull
	private MeasurementPeriod capacityProviderMeasurementPeriod;

	@NotNull
	private MeasurementPeriod capacityOptimizerMeasurementPeriod;

	@NotNull
	private Long capacityProviderId;

	@NotNull
	private Long capacityOptimizerId;

	@Override
	public CapacityGroupConfiguration toEntity(UserLongCompositePK id) {
		CapacityGroupConfiguration conf = new CapacityGroupConfiguration(id, Instant.now());
		populateConfiguration(conf);
		return conf;
	}

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
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Set the group identifier.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Get the Capacity Provider measurement period.
	 * 
	 * @return the period
	 */
	public MeasurementPeriod getCapacityProviderMeasurementPeriod() {
		return capacityProviderMeasurementPeriod;
	}

	/**
	 * Set the Capacity Provider measurement period.
	 * 
	 * @param capacityProviderMeasurementPeriod
	 *        the period to set
	 */
	public void setCapacityProviderMeasurementPeriod(MeasurementPeriod measurementPeriod) {
		this.capacityProviderMeasurementPeriod = measurementPeriod;
	}

	/**
	 * Get the Capacity Optimizer measurement period.
	 * 
	 * @return the period
	 */
	public MeasurementPeriod getCapacityOptimizerMeasurementPeriod() {
		return capacityOptimizerMeasurementPeriod;
	}

	/**
	 * Set the Capacity Optimizer measurement period.
	 * 
	 * @param capacityOptimizerMeasurementPeriod
	 *        the period to set
	 */
	public void setCapacityOptimizerMeasurementPeriod(MeasurementPeriod measurementPeriod) {
		this.capacityOptimizerMeasurementPeriod = measurementPeriod;
	}

	/**
	 * Get the capacity provider ID.
	 * 
	 * <p>
	 * The provider's {@code userId} is assumed to be the same as
	 * {@link #getUserId()}.
	 * </p>
	 * 
	 * @return the ID of the associated {@link CapacityProviderConfiguration}
	 */
	public Long getCapacityProviderId() {
		return capacityProviderId;
	}

	/**
	 * Set the capacity provider ID.
	 * 
	 * @param capacityProviderId
	 *        the ID of the capacity provider to set
	 */
	public void setCapacityProviderId(Long capacityProviderId) {
		this.capacityProviderId = capacityProviderId;
	}

	/**
	 * Get the capacity optimizer ID.
	 * <p>
	 * The optimizer's {@code userId} is assumed to be the same as
	 * {@link #getUserId()}.
	 * </p>
	 * 
	 * @return the ID of the associated {@link CapacityOptimizerConfiguration}
	 */
	public Long getCapacityOptimizerId() {
		return capacityOptimizerId;
	}

	/**
	 * Set the capacity optimizer ID.
	 * 
	 * @param capacityOptimizerId
	 *        the ID of the capacity optimizer to set
	 */
	public void setCapacityOptimizerId(Long capacityOptimizerId) {
		this.capacityOptimizerId = capacityOptimizerId;
	}

}
