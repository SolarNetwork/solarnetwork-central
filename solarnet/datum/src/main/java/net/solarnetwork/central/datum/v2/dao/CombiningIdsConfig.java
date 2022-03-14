/* ==================================================================
 * CombiningIdsConfig.java - 4/12/2020 3:09:10 pm
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

package net.solarnetwork.central.datum.v2.dao;

import java.util.Map;
import java.util.Set;

/**
 * A mapping of a set of real IDs to virtual IDs, to help with combining query
 * execution.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class CombiningIdsConfig<T> {

	private final String name;
	private final Map<T, Set<T>> idSets;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the name
	 * @param idSets
	 *        the mapping
	 */
	public CombiningIdsConfig(String name, Map<T, Set<T>> idSets) {
		super();
		this.name = name;
		this.idSets = idSets;
	}

	/**
	 * Get the configuration name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the normalized ID sets.
	 * 
	 * @return the ID sets
	 */
	public Map<T, Set<T>> getIdSets() {
		return idSets;
	}

}
