/* ==================================================================
 * SolarNodeOwnership.java - 6/10/2021 8:48:02 AM
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

package net.solarnetwork.central.domain;

/**
 * API for node ownership details.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNodeOwnership {

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Get the user ID of the owner of a node.
	 * 
	 * @return the owner user ID
	 */
	Long getUserId();

	/**
	 * Flag indicating if a node's data is "public" vs "private".
	 * 
	 * @return {@literal true} if the node requires authorization for viewing
	 *         its data
	 */
	boolean isRequiresAuthorization();

	/**
	 * Flag indicating if a node has been "archived".
	 * 
	 * @return {@literal true} if the node has been archived
	 */
	boolean isArchived();

}
