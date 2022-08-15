/* ==================================================================
 * AssetConfigurationInput.java - 15/08/2022 12:54:42 pm
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

import static java.time.Instant.now;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.Phase;

/**
 * DTO for asset configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class AssetConfigurationInput extends BaseOscpConfigurationInput<AssetConfiguration> {

	@NotNull
	private Long capacityGroupId;

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String sourceId;

	@NotNull
	private AssetCategory category;

	@NotNull
	@NotEmpty
	private String[] instantaneousPropertyNames;

	@NotNull
	private MeasurementUnit instantaneousUnit;

	@NotNull
	private BigDecimal instantaneousMultiplier;

	@NotNull
	private Phase instantaneousPhase;

	@NotNull
	@NotEmpty
	private String[] energyPropertyNames;

	@NotNull
	private MeasurementUnit energyUnit;

	@NotNull
	private BigDecimal energyMultiplier;

	@NotNull
	private EnergyType energyType;

	@Override
	public AssetConfiguration toEntity(UserLongPK id) {
		AssetConfiguration conf = new AssetConfiguration(requireNonNullArgument(id, "id"), now());
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(AssetConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setCapacityGroupId(capacityGroupId);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setCategory(category);
		conf.setInstantaneousPropertyNames(instantaneousPropertyNames);
		conf.setInstantaneousUnit(instantaneousUnit);
		conf.setInstantaneousMultiplier(instantaneousMultiplier);
		conf.setInstantaneousPhase(instantaneousPhase);
		conf.setEnergyPropertyNames(energyPropertyNames);
		conf.setEnergyUnit(energyUnit);
		conf.setEnergyMultiplier(energyMultiplier);
		conf.setEnergyType(energyType);
	}

	/**
	 * Get the Capacity Group ID.
	 * 
	 * @return the ID of the {@link CapacityGroupConfiguration} associated with
	 *         this entity
	 */
	public Long getCapacityGroupId() {
		return capacityGroupId;
	}

	/**
	 * Set the Capacity Group ID.
	 * 
	 * @param capacityGroupId
	 *        the ID of the capacity group to set
	 */
	public void setCapacityGroupId(Long capacityGroupId) {
		this.capacityGroupId = capacityGroupId;
	}

	/**
	 * Get the datum stream node ID.
	 * 
	 * @return the nodeId the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum stream node ID.
	 * 
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the datum stream source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum stream source ID.
	 * 
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the asset category.
	 * 
	 * @return the category
	 */
	public AssetCategory getCategory() {
		return category;
	}

	/**
	 * Set the asset category.
	 * 
	 * @param category
	 *        the category to set
	 */
	public void setCategory(AssetCategory category) {
		this.category = category;
	}

	/**
	 * Get the instantaneous datum stream property names.
	 * 
	 * @return the property names
	 */
	public String[] getInstantaneousPropertyNames() {
		return instantaneousPropertyNames;
	}

	/**
	 * Set the instantaneous datum stream property names.
	 * 
	 * @param instantaneousPropertyNames
	 *        the property names to set
	 */
	public void setInstantaneousPropertyNames(String[] instantaneousPropertyNames) {
		this.instantaneousPropertyNames = instantaneousPropertyNames;
	}

	/**
	 * Get the instantaneous unit.
	 * 
	 * @return the unit
	 */
	public MeasurementUnit getInstantaneousUnit() {
		return instantaneousUnit;
	}

	/**
	 * Set the instantaneous unit.
	 * 
	 * @param instantaneousUnit
	 *        the unit to set
	 */
	public void setInstantaneousUnit(MeasurementUnit instantaneousUnit) {
		this.instantaneousUnit = instantaneousUnit;
	}

	/**
	 * Get the instantaneous datum stream property value multiplier.
	 * 
	 * @return the multiplier to convert instantaneous property values into
	 *         {@code instantaneousUnit}, or {@literal null} for no conversion
	 */
	public BigDecimal getInstantaneousMultiplier() {
		return instantaneousMultiplier;
	}

	/**
	 * Set the instantaneous datum stream property value multiplier.
	 * 
	 * @param instantaneousMultiplier
	 *        the multiplier to convert instantaneous property values into
	 *        {@code instantaneousUnit}, or {@literal null} for no conversion
	 */
	public void setInstantaneousMultiplier(BigDecimal instantaneousMultiplier) {
		this.instantaneousMultiplier = instantaneousMultiplier;
	}

	/**
	 * Get the instantaneous phase.
	 * 
	 * @return the instantaneous phase
	 */
	public Phase getInstantaneousPhase() {
		return instantaneousPhase;
	}

	/**
	 * Set the instantaneous phase.
	 * 
	 * @param instantaneousPhase
	 *        the phase to set
	 */
	public void setInstantaneousPhase(Phase instantaneousPhase) {
		this.instantaneousPhase = instantaneousPhase;
	}

	/**
	 * Get the instantaneous datum stream property names.
	 * 
	 * @return the property names
	 */
	public String[] getEnergyPropertyNames() {
		return energyPropertyNames;
	}

	/**
	 * Set the instantaneous datum stream property names.
	 * 
	 * @param energyPropertyNames
	 *        the property names to set
	 */
	public void setEnergyPropertyNames(String[] energyPropertyNames) {
		this.energyPropertyNames = energyPropertyNames;
	}

	/**
	 * Get the energy unit.
	 * 
	 * @return the unit
	 */
	public MeasurementUnit getEnergyUnit() {
		return energyUnit;
	}

	/**
	 * Set the energy unit
	 * 
	 * @param energyUnit
	 *        the unit to set
	 */
	public void setEnergyUnit(MeasurementUnit energyUnit) {
		this.energyUnit = energyUnit;
	}

	/**
	 * Get the energy datum stream property value multiplier.
	 * 
	 * @return the multiplier to convert energy property values into
	 *         {@code energyUnit}, or {@literal null} for no conversion
	 */
	public BigDecimal getEnergyMultiplier() {
		return energyMultiplier;
	}

	/**
	 * Set the energy datum stream property value multiplier.
	 * 
	 * @param energyMultiplier
	 *        the multiplier to convert energy property values into
	 *        {@code energyUnit}, or {@literal null} for no conversion
	 */
	public void setEnergyMultiplier(BigDecimal energyMultiplier) {
		this.energyMultiplier = energyMultiplier;
	}

	/**
	 * Get the energy type.
	 * 
	 * @return the type
	 */
	public EnergyType getEnergyType() {
		return energyType;
	}

	/**
	 * Set the energy type.
	 * 
	 * @param energyType
	 *        the type to set
	 */
	public void setEnergyType(EnergyType energyType) {
		this.energyType = energyType;
	}

}
