/* ==================================================================
 * BaseServerDatumStreamConfigurationInput.java - 13/08/2023 6:22:19 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.domain;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.BaseServerDatumStreamConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.domain.CodedValue;

/**
 * Base configuration input for datum stream related server configuration
 * entities.
 *
 * @param <C>
 *        the configuration type
 * @param <T>
 *        the enum type
 * @author matt
 * @version 1.0
 */
public abstract class BaseServerDatumStreamConfigurationInput<C extends BaseServerDatumStreamConfiguration<C, T>, T extends Enum<? extends CodedValue>>
		extends BaseDnp3ConfigurationInput<C, UserLongIntegerCompositePK> {

	@NotNull
	private @Nullable Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String sourceId;

	@Size(max = 255)
	private @Nullable String property;

	@NotNull
	private @Nullable T type;

	private @Nullable BigDecimal multiplier;

	private @Nullable BigDecimal offset;

	private @Nullable Integer scale;

	@Override
	protected void populateConfiguration(C conf) {
		super.populateConfiguration(conf);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setProperty(property);
		conf.setType(type);
		conf.setMultiplier(multiplier);
		conf.setOffset(offset);
		conf.setScale(scale);
	}

	/**
	 * Get the datum node ID.
	 *
	 * @return the nodeId
	 */
	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum node ID.
	 *
	 * @param nodeId
	 *        the nodeId to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the datum source ID.
	 *
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum source ID.
	 *
	 * @param sourceId
	 *        the sourceId to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the datum property name.
	 *
	 * @return the property
	 */
	public @Nullable String getProperty() {
		return property;
	}

	/**
	 * Set the datum property name.
	 *
	 * @param property
	 *        the property to set
	 */
	public void setProperty(@Nullable String property) {
		this.property = property;
	}

	/**
	 * Set the measurement type.
	 *
	 * @return the type
	 */
	public final @Nullable T getType() {
		return type;
	}

	/**
	 * Get the measurement type.
	 *
	 * @param type
	 *        the measurement type to set
	 */
	public final void setType(@Nullable T type) {
		this.type = type;
	}

	/**
	 * Get the decimal multiplier.
	 *
	 * @return the multiplier
	 */
	public final @Nullable BigDecimal getMultiplier() {
		return multiplier;
	}

	/**
	 * Set the decimal multiplier.
	 *
	 * @param multiplier
	 *        the multiplier to set
	 */
	public final void setMultiplier(@Nullable BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/**
	 * Get the decimal offset.
	 *
	 * @return the offset
	 */
	public final @Nullable BigDecimal getOffset() {
		return offset;
	}

	/**
	 * Set the decimal offset.
	 *
	 * @param offset
	 *        the offset to set
	 */
	public final void setOffset(@Nullable BigDecimal offset) {
		this.offset = offset;
	}

	/**
	 * Get the decimal scale
	 *
	 * @return the scale
	 */
	public final @Nullable Integer getScale() {
		return scale;
	}

	/**
	 * Set the decimal scale.
	 *
	 * @param scale
	 *        the scale to set
	 */
	public final void setScale(@Nullable Integer scale) {
		this.scale = scale;
	}

}
