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
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * Configuration for an asset.
 *
 * @author matt
 * @version 1.1
 */
public class AssetConfiguration extends BaseOscpConfigurationEntity<AssetConfiguration> {

	@Serial
	private static final long serialVersionUID = -337374989043029897L;

	private Long capacityGroupId;
	private String identifier;
	private OscpRole audience;
	private Long nodeId;
	private String sourceId;
	private AssetCategory category;
	private @Nullable Phase phase;
	private @Nullable AssetInstantaneousDatumConfiguration instantaneous;
	private @Nullable AssetEnergyDatumConfiguration energy;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param capacityGroupId
	 *        the capacity group ID
	 * @param identifier
	 *        the identifier
	 * @param audience
	 *        the audience
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param category
	 *        the category
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public AssetConfiguration(UserLongCompositePK id, Instant created, String name, Long capacityGroupId,
			String identifier, OscpRole audience, Long nodeId, String sourceId, AssetCategory category) {
		super(id, created, name);
		this.capacityGroupId = requireNonNullArgument(capacityGroupId, "capacityGroupId");
		this.identifier = requireNonNullArgument(identifier, "identifier");
		this.audience = requireNonNullArgument(audience, "audience");
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
		this.category = requireNonNullArgument(category, "category");
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param capacityGroupId
	 *        the capacity group ID
	 * @param identifier
	 *        the identifier
	 * @param audience
	 *        the audience
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param category
	 *        the category
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public AssetConfiguration(Long userId, Long entityId, Instant created, String name,
			Long capacityGroupId, String identifier, OscpRole audience, Long nodeId, String sourceId,
			AssetCategory category) {
		this(new UserLongCompositePK(userId, entityId), created, name, capacityGroupId, identifier,
				audience, nodeId, sourceId, category);
	}

	@Override
	public AssetConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new AssetConfiguration(id, created(), getName(), capacityGroupId, identifier,
				audience, nodeId, sourceId, category);
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
			entity.setInstantaneous(instantaneous.clone());
		}
		if ( energy != null ) {
			entity.setEnergy(energy.clone());
		}
	}

	@Override
	public boolean isSameAs(@Nullable AssetConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		final var o = ObjectUtils.nonnull(other, "other");
		// @formatter:off
		return (Objects.equals(capacityGroupId, o.capacityGroupId)
				&& Objects.equals(identifier, o.identifier)
				&& Objects.equals(audience, o.audience)
				&& Objects.equals(nodeId, o.nodeId)
				&& Objects.equals(sourceId, o.sourceId)
				&& Objects.equals(category, o.category)
				&& Objects.equals(phase, o.phase)
				&& !differ(instantaneous, o.instantaneous)
				&& !differ(energy, o.energy));
		// @formatter:on
	}

	/**
	 * Get the Capacity Group ID.
	 *
	 * @return the ID of the {@link CapacityGroupConfiguration} associated with
	 *         this entity
	 */
	public final Long getCapacityGroupId() {
		return capacityGroupId;
	}

	/**
	 * Set the Capacity Group ID.
	 *
	 * @param capacityGroupId
	 *        the ID of the capacity group to set
	 */
	public final void setCapacityGroupId(Long capacityGroupId) {
		this.capacityGroupId = capacityGroupId;
	}

	/**
	 * Get an identifier.
	 *
	 * @return the identifier
	 */
	public final String getIdentifier() {
		return identifier;
	}

	/**
	 * Set an identifier.
	 *
	 * @param identifier
	 *        the identifier to set
	 */
	public final void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Get the audience.
	 *
	 * @return the audience
	 */
	public final OscpRole getAudience() {
		return audience;
	}

	/**
	 * Set the audience.
	 *
	 * @param audience
	 *        the audience to set
	 * @throws IllegalArgumentException
	 *         if {@code audience} is {@code null} or not supported
	 */
	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	public final void setAudience(OscpRole audience) {
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
	public final Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum stream node ID.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public final void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the datum stream source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum stream source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the asset category.
	 *
	 * @return the category
	 */
	public final AssetCategory getCategory() {
		return category;
	}

	/**
	 * Set the asset category.
	 *
	 * @param category
	 *        the category to set
	 */
	public final void setCategory(AssetCategory category) {
		this.category = category;
	}

	/**
	 * Get the phase.
	 *
	 * @return the instantaneous phase
	 */
	public final @Nullable Phase getPhase() {
		return phase;
	}

	/**
	 * Set the phase.
	 *
	 * @param phase
	 *        the phase to set
	 */
	public final void setPhase(@Nullable Phase phase) {
		this.phase = phase;
	}

	/**
	 * Get the instantaneous configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable AssetInstantaneousDatumConfiguration getInstantaneous() {
		return instantaneous;
	}

	/**
	 * Set the instantaneous configuration
	 *
	 * @param instantaneous
	 *        the configuration to set
	 */
	public final void setInstantaneous(@Nullable AssetInstantaneousDatumConfiguration instantaneous) {
		this.instantaneous = instantaneous;
	}

	/**
	 * Get the energy configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable AssetEnergyDatumConfiguration getEnergy() {
		return energy;
	}

	/**
	 * Set the energy configuration.
	 *
	 * @param energy
	 *        the configuration to set
	 */
	public final void setEnergy(@Nullable AssetEnergyDatumConfiguration energy) {
		this.energy = energy;
	}

}
