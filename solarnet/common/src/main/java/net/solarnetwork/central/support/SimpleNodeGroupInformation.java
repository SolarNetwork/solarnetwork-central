/* ==================================================================
 * SimpleNodeGroupInformation.java - Apr 30, 2011 1:18:09 PM
 *
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.BaseIdentity;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.NodeGroupInformation;
import net.solarnetwork.central.domain.SolarCapability;
import net.solarnetwork.central.domain.SolarNodeGroupCapability;

/**
 * Simple implementation of {@link NodeGroupInformation}.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleNodeGroupInformation extends BaseIdentity implements NodeGroupInformation {

	@Serial
	private static final long serialVersionUID = -1983417976743765775L;

	private @Nullable String name;
	private @Nullable Location location;
	private @Nullable SolarCapability capability;

	/**
	 * Default constructor.
	 */
	public SimpleNodeGroupInformation() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param name
	 *        the name
	 * @param capability
	 *        the capability
	 * @param location
	 *        the location
	 */
	public SimpleNodeGroupInformation(@Nullable String name,
			@Nullable SolarNodeGroupCapability capability, @Nullable Location location) {
		if ( capability != null ) {
			setId(capability.getGroupId());
		}
		this.name = name;
		this.capability = capability;
		this.location = location;
	}

	@Override
	public final @Nullable Location getLocation() {
		return location;
	}

	@Override
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Increment the generation capacity.
	 *
	 * @param amount
	 *        the amount to add
	 */
	public void addGenerationCapacityWatts(final @Nullable Long amount) {
		if ( amount == null ) {
			return;
		}
		final var c = nonnull(this.capability, "capability");
		final Long curr = c.getGenerationCapacityWatts();
		c.setGenerationCapacityWatts(curr != null ? curr + amount : amount);
	}

	/**
	 * Increment the storage capacity.
	 *
	 * @param amount
	 *        the amount to add
	 */
	public void addStorageCapacityWattHours(final @Nullable Long amount) {
		if ( amount == null ) {
			return;
		}
		final var c = nonnull(this.capability, "capability");
		final Long curr = c.getGenerationCapacityWatts();
		c.setStorageCapacityWattHours(curr != null ? curr + amount : amount);
	}

	/**
	 * Get the generation capacity.
	 * 
	 * @return the generation capacity, in watts
	 */
	@Override
	public final @Nullable Long getGenerationCapacityWatts() {
		final var c = this.capability;
		return (c != null ? c.getGenerationCapacityWatts() : null);
	}

	/**
	 * Get the storage capacity.
	 * 
	 * @return the storage capacity, in watt-hours
	 */
	@Override
	public final @Nullable Long getStorageCapacityWattHours() {
		final var c = this.capability;
		return (c != null ? c.getStorageCapacityWattHours() : null);
	}

}
