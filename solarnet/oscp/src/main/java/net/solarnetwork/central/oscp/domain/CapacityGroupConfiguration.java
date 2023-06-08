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
import java.util.Objects;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Configuration for capacity groups.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityGroupConfiguration extends BaseOscpConfigurationEntity<CapacityGroupConfiguration> {

	private static final long serialVersionUID = 2002817702265442625L;

	private String identifier;
	private MeasurementPeriod capacityProviderMeasurementPeriod;
	private MeasurementPeriod capacityOptimizerMeasurementPeriod;
	private Long capacityProviderId;
	private Long capacityOptimizerId;
	private Instant capacityProviderMeasurementDate;
	private Instant capacityOptimizerMeasurementDate;

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
		entity.setCapacityProviderMeasurementPeriod(capacityProviderMeasurementPeriod);
		entity.setCapacityOptimizerMeasurementPeriod(capacityOptimizerMeasurementPeriod);
		entity.setCapacityProviderId(capacityProviderId);
		entity.setCapacityOptimizerId(capacityOptimizerId);
		entity.setCapacityProviderMeasurementDate(capacityProviderMeasurementDate);
		entity.setCapacityOptimizerMeasurementDate(capacityOptimizerMeasurementDate);
	}

	@Override
	public boolean isSameAs(CapacityGroupConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return (Objects.equals(this.identifier, other.identifier) 
				&& Objects.equals(this.capacityProviderMeasurementPeriod, other.capacityProviderMeasurementPeriod)
				&& Objects.equals(this.capacityOptimizerMeasurementPeriod, other.capacityOptimizerMeasurementPeriod)
				&& Objects.equals(this.capacityProviderId, other.capacityProviderId)
				&& Objects.equals(this.capacityOptimizerId, other.capacityOptimizerId)
				&& Objects.equals(this.capacityProviderMeasurementDate, other.capacityProviderMeasurementDate)
				&& Objects.equals(this.capacityOptimizerMeasurementDate, other.capacityOptimizerMeasurementDate));
		// @formatter:on
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

	/**
	 * Get the capacity provider last measurement date.
	 * 
	 * @return the measurement date
	 */
	public Instant getCapacityProviderMeasurementDate() {
		return capacityProviderMeasurementDate;
	}

	/**
	 * Set the capacity provider last measurement date.
	 * 
	 * @param capacityProviderMeasurementDate
	 *        the measurement date to set
	 */
	public void setCapacityProviderMeasurementDate(Instant capacityProviderMeasurementDate) {
		this.capacityProviderMeasurementDate = capacityProviderMeasurementDate;
	}

	/**
	 * Get the capacity optimizer last measurement date.
	 * 
	 * @return the measurement date
	 */
	public Instant getCapacityOptimizerMeasurementDate() {
		return capacityOptimizerMeasurementDate;
	}

	/**
	 * Set the capacity optimizer last measurement date.
	 * 
	 * @param capacityOptimizerMeasurementDate
	 *        the measurement date to set
	 */
	public void setCapacityOptimizerMeasurementDate(Instant capacityOptimizerMeasurementDate) {
		this.capacityOptimizerMeasurementDate = capacityOptimizerMeasurementDate;
	}

}
