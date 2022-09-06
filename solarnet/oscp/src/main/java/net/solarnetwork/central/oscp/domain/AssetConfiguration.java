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

import static net.solarnetwork.domain.Differentiable.differ;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Objects;
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
	private Phase phase;
	private AssetInstantaneousDatumConfiguration instantaneous;
	private AssetEnergyDatumConfiguration energy;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if the {code id} argument is {@literal null}
	 */
	public AssetConfiguration(UserLongCompositePK id, Instant created) {
		super(requireNonNullArgument(id, "id"), created);
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
		entity.setPhase(phase);
		if ( instantaneous != null ) {
			AssetInstantaneousDatumConfiguration copy = new AssetInstantaneousDatumConfiguration();
			instantaneous.copyTo(copy);
			entity.setInstantaneous(copy);
		}
		if ( energy != null ) {
			AssetEnergyDatumConfiguration copy = new AssetEnergyDatumConfiguration();
			energy.copyTo(copy);
			entity.setEnergy(copy);
		}
	}

	@Override
	public boolean isSameAs(AssetConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return (Objects.equals(this.capacityGroupId, other.capacityGroupId) 
				&& Objects.equals(this.identifier, other.identifier)
				&& Objects.equals(this.audience, other.audience)
				&& Objects.equals(this.nodeId, other.nodeId)
				&& Objects.equals(this.sourceId, other.sourceId)
				&& Objects.equals(this.category, other.category)
				&& Objects.equals(this.phase, other.phase)
				&& !differ(this.instantaneous, other.instantaneous)
				&& !differ(this.energy, other.energy));
		// @formatter:on
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
	 * Get the phase.
	 * 
	 * @return the instantaneous phase
	 */
	public Phase getPhase() {
		return phase;
	}

	/**
	 * Set the phase.
	 * 
	 * @param phase
	 *        the phase to set
	 */
	public void setPhase(Phase instantaneousPhase) {
		this.phase = instantaneousPhase;
	}

	/**
	 * Get the instantaneous configuration.
	 * 
	 * @return the configuration
	 */
	public AssetInstantaneousDatumConfiguration getInstantaneous() {
		return instantaneous;
	}

	/**
	 * Set the instantaneous configuration
	 * 
	 * @param instantaneous
	 *        the configuration to set
	 */
	public void setInstantaneous(AssetInstantaneousDatumConfiguration instantaneous) {
		this.instantaneous = instantaneous;
	}

	/**
	 * Get the energy configuration.
	 * 
	 * @return the configuration
	 */
	public AssetEnergyDatumConfiguration getEnergy() {
		return energy;
	}

	/**
	 * Set the energy configuration.
	 * 
	 * @param energy
	 *        the configuration to set
	 */
	public void setEnergy(AssetEnergyDatumConfiguration energy) {
		this.energy = energy;
	}

}
