/* ==================================================================
 * NodeOwnershipCriteria.java - 10/08/2023 1:27:29 pm
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

package net.solarnetwork.central.common.dao;

/**
 * Search criteria for node ownership.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeOwnershipCriteria {

	/**
	 * Get the valid node ownership flag.
	 * 
	 * <p>
	 * This flag, when {@literal true}, indicates a desire to filter the results
	 * to only the set of nodes actually owned by the users specified with the
	 * data. For example, a table might have a node ID column that is <b>not</b>
	 * a foreign key to the user_node mapping table, so there is no guarantee
	 * the record's node ID is actually owned by that record's account owner.
	 * </p>
	 * 
	 * @return the {@literal true} or {@literal false} to filter by that state,
	 *         or {@literal null} to not filter
	 */
	Boolean getValidNodeOwnership();

	/**
	 * Test if this filter has any node ownership criteria.
	 * 
	 * @return {@literal true} if the node ownership is non-null
	 */
	default boolean hasNodeOwnershipCriteria() {
		return getValidNodeOwnership() != null;
	}

}
