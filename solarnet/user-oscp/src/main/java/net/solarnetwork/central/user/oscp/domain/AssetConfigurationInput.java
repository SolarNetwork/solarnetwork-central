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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
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
	@NotBlank
	private String identifier;

	@NotNull
	private OscpRole audience;

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String sourceId;

	@NotNull
	private AssetCategory category;

	@NotNull
	private Phase phase;

	@Valid
	private AssetInstantaneousDatumConfigurationInput instantaneous;

	@Valid
	private AssetEnergyDatumConfigurationInput energy;

	@Override
	public AssetConfiguration toEntity(UserLongCompositePK id) {
		AssetConfiguration conf = new AssetConfiguration(requireNonNullArgument(id, "id"), now());
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(AssetConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setCapacityGroupId(capacityGroupId);
		conf.setIdentifier(identifier);
		conf.setAudience(audience);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setCategory(category);
		conf.setPhase(phase);
		if ( instantaneous != null ) {
			conf.setInstantaneous(instantaneous.toEntity());
		}
		if ( energy != null ) {
			conf.setEnergy(energy.toEntity());
		}
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
	 * @return the phase
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
	public AssetInstantaneousDatumConfigurationInput getInstantaneous() {
		return instantaneous;
	}

	/**
	 * Set the instantaneous configuration
	 * 
	 * @param instantaneous
	 *        the configuration to set
	 */
	public void setInstantaneous(AssetInstantaneousDatumConfigurationInput instantaneous) {
		this.instantaneous = instantaneous;
	}

	/**
	 * Get the energy configuration.
	 * 
	 * @return the configuration
	 */
	public AssetEnergyDatumConfigurationInput getEnergy() {
		return energy;
	}

	/**
	 * Set the energy configuration.
	 * 
	 * @param energy
	 *        the configuration to set
	 */
	public void setEnergy(AssetEnergyDatumConfigurationInput energy) {
		this.energy = energy;
	}

}
