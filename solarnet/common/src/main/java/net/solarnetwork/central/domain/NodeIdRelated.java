/* ==================================================================
 * NodeIdRelated.java - 27/09/2024 6:29:29 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import org.jspecify.annotations.Nullable;

/**
 * API for objects related to a node entity by way of a node ID.
 * 
 * @author matt
 * @version 1.1
 */
public interface NodeIdRelated {

	/**
	 * A special "not a value" instance to be used for node ID values yet to be
	 * assigned.
	 * 
	 * @since 1.1
	 */
	Long UNASSIGNED_NODE_ID = EntityConstants.UNASSIGNED_LONG_ID;

	/**
	 * Get node ID this entity relates to.
	 * 
	 * @return the node ID
	 */
	@Nullable
	Long getNodeId();

	/**
	 * Test if a node ID is available.
	 * 
	 * @return {@code true} if a node ID is available (and not
	 *         {@link #UNASSIGNED_NODE_ID})
	 * @since 1.1
	 */
	default boolean nodeIdIsAssigned() {
		try {
			return EntityConstants.isAssigned(getNodeId());
		} catch ( IllegalStateException e ) {
			return false;
		}
	}

}
