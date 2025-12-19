/* ==================================================================
 * ObjectDatumIdRelated.java - 17/12/2025 10:05:02 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * API for objects related to an {@link ObjectDatumKind} entity by way of an
 * object ID.
 * 
 * @author matt
 * @version 1.0
 */
public interface ObjectDatumIdRelated {

	/**
	 * Get the related entity kind.
	 * 
	 * @return the kind
	 */
	ObjectDatumKind getKind();

	/**
	 * Get the related entity's ID.
	 * 
	 * @return the object ID
	 */
	Long getObjectId();

	/**
	 * Test if the related ID is a node ID.
	 * 
	 * @return {@code true} if {@code kind} is {@code Node} or {@code null}
	 */
	@JsonIgnore
	default boolean isNodeId() {
		final var kind = getKind();
		return switch (kind) {
			case null -> true;
			case Node -> true;
			default -> false;
		};
	}

	/**
	 * Get the object ID, if the receiver is node ID related.
	 * 
	 * @return the node ID, or {@code null} if not node ID related or the ID is
	 *         {@code null}
	 */
	default Long nodeId() {
		return (isNodeId() ? getObjectId() : null);
	}

	/**
	 * Test if a node ID is available.
	 * 
	 * @return {@code true} if a node ID is available
	 */
	default boolean hasNodeId() {
		return (nodeId() != null);
	}

	/**
	 * Test if the related ID is a location ID.
	 * 
	 * @return {@code true} if {@code kind} is {@code Location}
	 */
	@JsonIgnore
	default boolean isLocationId() {
		final var kind = getKind();
		return switch (kind) {
			case Location -> true;
			case null, default -> false;
		};
	}

	/**
	 * Get the object ID, if the receiver is location ID related.
	 * 
	 * @return the location ID, or {@code null} if not location ID related or
	 *         the ID is {@code null}
	 */
	default Long locationId() {
		return (isLocationId() ? getObjectId() : null);
	}

	/**
	 * Test if a location ID is available.
	 * 
	 * @return {@code true} if a location ID is available
	 */
	default boolean hasLocationId() {
		return (locationId() != null);
	}

}
