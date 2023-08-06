/* ==================================================================
 * ServerCriteria.java - 06/08/2023 12:52:35 pm
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

package net.solarnetwork.central.dnp3.dao;

/**
 * Search criteria for server related data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ServerCriteria {

	/**
	 * Get the first server ID.
	 * 
	 * <p>
	 * This returns the first available server ID from the
	 * {@link #getServerIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first server ID, or {@literal null} if not available
	 */
	default Long getServerId() {
		final Long[] array = getServerIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of server IDs.
	 * 
	 * @return array of server IDs (may be {@literal null})
	 */
	Long[] getServerIds();

	/**
	 * Test if this filter has any server criteria.
	 * 
	 * @return {@literal true} if the server ID is non-null
	 */
	default boolean hasServerCriteria() {
		return getServerId() != null;
	}

}
