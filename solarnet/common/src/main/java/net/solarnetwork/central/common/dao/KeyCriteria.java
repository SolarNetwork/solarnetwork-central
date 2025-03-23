/* ==================================================================
 * KeyCriteria.java - 22/03/2025 7:36:26 am
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

package net.solarnetwork.central.common.dao;

/**
 * Criteria API for keyed entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface KeyCriteria {

	/**
	 * Get the key criteria.
	 * 
	 * @return the keys
	 */
	String[] getKeys();

	/**
	 * Get a single key criteria.
	 * 
	 * @return the first available key criteria
	 */
	default String getKey() {
		String[] keys = getKeys();
		return (keys != null && keys.length > 0 ? keys[0] : null);
	}

	/**
	 * Test if any key criteria is available.
	 * 
	 * @return {@code true} if at least one key criteria is available
	 */
	default boolean hasKeyCriteria() {
		return getKey() != null;
	}

}
