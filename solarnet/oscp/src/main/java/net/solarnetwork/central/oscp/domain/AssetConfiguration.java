/* ==================================================================
 * AssetConfiguration.java - 14/08/2022 5:47:15 pm
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

import static java.util.Arrays.copyOf;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Instant;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Configuration for an asset.
 * 
 * @author matt
 * @version 1.0
 */
public class AssetConfiguration extends BaseOscpConfigurationEntity<AssetConfiguration> {

	private static final long serialVersionUID = -337374989043029897L;

	private Long capacityGroupId;
	private String identifier;
	private OscpRole audience;
	private Long nodeId;
	private String sourceId;
	private AssetCategory category;
	private String[] instantaneousPropertyNames;
	private MeasurementUnit instantaneousUnit;
	private BigDecimal instantaneousMultiplier;
	private Phase instantaneousPhase;
	private String[] energyPropertyNames;
	private MeasurementUnit energyUnit;
	private BigDecimal energyMultiplier;
	private EnergyType energyType;

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
	public AssetConfiguration(UserLongCompositePK id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
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
	public AssetConfiguration(Long userId, Long entityId, Instant created) {
		super(new UserLongCompositePK(userId, entityId), created);
	}

	@Override
	public AssetConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new AssetConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public AssetConfiguration clone() {
		return (AssetConfiguration) super.clone();
	}

	@Override
	public void copyTo(AssetConfiguration entity) {
		super.copyTo(entity);
		entity.setIdentifier(identifier);
		if ( audience != null ) {
			entity.setAudience(audience);
		}
		entity.setCapacityGroupId(capacityGroupId);
		entity.setNodeId(nodeId);
		entity.setSourceId(sourceId);
		entity.setCategory(category);
		if ( instantaneousPropertyNames != null ) {
			entity.setInstantaneousPropertyNames(
					copyOf(instantaneousPropertyNames, instantaneousPropertyNames.length));
		}
		entity.setInstantaneousUnit(instantaneousUnit);
		entity.setInstantaneousMultiplier(instantaneousMultiplier);
		entity.setInstantaneousPhase(instantaneousPhase);
		if ( energyPropertyNames != null ) {
			entity.setEnergyPropertyNames(copyOf(energyPropertyNames, energyPropertyNames.length));
		}
		entity.setEnergyUnit(energyUnit);
		entity.setEnergyMultiplier(energyMultiplier);
		entity.setEnergyType(energyType);
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
	 * Get an identifier.
	 * 
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Set an identifier.
	 * 
	 * @param identifier
	 *        the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Get the audience.
	 * 
	 * @return the audience
	 */
	public OscpRole getAudience() {
		return audience;
	}

	/**
	 * Set the audience.
	 * 
	 * @param audience
	 *        the audience to set
	 * @throws IllegalArgumentException
	 *         if {@code audience} is {@literal null} or not supported
	 */
	public void setAudience(OscpRole audience) {
		switch (requireNonNullArgument(audience, "audience")) {
			case CapacityProvider:
			case CapacityOptimizer:
				this.audience = audience;
				break;

			default:
				throw new IllegalArgumentException("Audience [%s] not supported.".formatted(audience));
		}
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
