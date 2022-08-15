/* ==================================================================
 * CapacityGroupConfiguration.java - 14/08/2022 11:20:31 am
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

import java.time.Instant;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Configuration for capacity groups.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityGroupConfiguration extends BaseOscpConfigurationEntity<CapacityGroupConfiguration> {

	private static final long serialVersionUID = 2455264341587923374L;

	private String identifier;
	private MeasurementPeriod measurementPeriod;
	private Long capacityProviderId;
	private Long capacityOptimizerId;

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
	public CapacityGroupConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CapacityGroupConfiguration(Long userId, Long entityId, Instant created) {
		super(userId, entityId, created);
	}

	@Override
	public CapacityGroupConfiguration clone() {
		return (CapacityGroupConfiguration) super.clone();
	}

	@Override
	public CapacityGroupConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CapacityGroupConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CapacityGroupConfiguration entity) {
		super.copyTo(entity);
		entity.setIdentifier(identifier);
		entity.setMeasurementPeriod(measurementPeriod);
		entity.setCapacityProviderId(capacityProviderId);
		entity.setCapacityOptimizerId(capacityOptimizerId);
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
	 * Get the measurement period.
	 * 
	 * @return the period
	 */
	public MeasurementPeriod getMeasurementPeriod() {
		return measurementPeriod;
	}

	/**
	 * Set the measurement period.
	 * 
	 * @param measurementPeriod
	 *        the period to set
	 */
	public void setMeasurementPeriod(MeasurementPeriod measurementPeriod) {
		this.measurementPeriod = measurementPeriod;
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
