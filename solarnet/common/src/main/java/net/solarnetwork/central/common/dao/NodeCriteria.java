/* ==================================================================
 * NodeCriteria.java - 23/10/2020 9:16:44 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for node related data.
 * 
 * @author matt
 * @version 1.2
 * @since 2.8
 */
public interface NodeCriteria {

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This returns the first available node ID from the {@link #getNodeIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the node ID, or {@code null} if not available
	 */
	@Nullable
	Long getNodeId();

	/**
	 * Get an array of node IDs.
	 * 
	 * @return array of node IDs (may be {@code null})
	 */
	Long @Nullable [] getNodeIds();

	/**
	 * Test if this filter has any node criteria.
	 * 
	 * @return {@literal true} if the node ID is non-null
	 * @since 1.1
	 */
	default boolean hasNodeCriteria() {
		return getNodeId() != null;
	}

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasNodeCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first node ID (presumed non-null)
	 * @since 1.2
	 */
	@SuppressWarnings("NullAway")
	default Long nodeId() {
		return getNodeId();
	}

	/**
	 * Get an array of node IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasNodeCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of node IDs (presumed non-null)
	 * @since 1.2
	 */
	@SuppressWarnings("NullAway")
	default Long[] nodeIds() {
		return getNodeIds();
	}

}
