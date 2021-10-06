/* ==================================================================
 * SolarNodeOwnershipDao.java - 6/10/2021 7:21:00 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * DAO for helping with node ownership information.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNodeOwnershipDao {

	/**
	 * Get the ownership information of a node.
	 * 
	 * @param nodeId
	 *        the ID of the node to find the ownership of
	 * @return the ownership, or {@literal null} if not available
	 */
	SolarNodeOwnership ownershipForNodeId(Long nodeId);

	/**
	 * Get all available node ownership for a user ID.
	 * 
	 * @param userId
	 *        the ID of the owner to find node ownership for
	 * @return the ownerships, or {@literal null} if none available
	 */
	SolarNodeOwnership[] ownershipsForUserId(Long userId);

	/**
	 * Get the available non-archived node IDs associated with a security token
	 * ID.
	 * 
	 * @param tokenId
	 *        the security token ID
	 * @return the node IDs; never {@literal null}
	 */
	Long[] nonArchivedNodeIdsForToken(String tokenId);

}
