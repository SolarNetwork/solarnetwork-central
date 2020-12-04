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

import java.lang.reflect.Array;
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
	private final T[][] idSets;
	private final T[] virtualIds;

	public CombiningIdsConfig(String name, Map<T, Set<T>> mapping, Class<T> clazz) {
		super();
		this.name = name;
		this.idSets = combiningNormalizedArray(mapping, clazz);

		@SuppressWarnings("unchecked")
		final T[] vids = (T[]) Array.newInstance(clazz, mapping.size());
		mapping.keySet().toArray(vids);
		this.virtualIds = vids;
	}

	/**
	 * Create a two-dimensional array with equal column counts in all rows from
	 * a mapping.
	 * 
	 * @param mapping
	 *        the mapping
	 * @param clazz
	 *        the element class
	 * @return the new array
	 */
	private T[][] combiningNormalizedArray(Map<T, Set<T>> mapping, Class<T> clazz) {
		final int rows = mapping.size();

		// all rows must have same column count, so figure out max
		int cols = 0;
		for ( Set<T> set : mapping.values() ) {
			if ( set.size() > cols ) {
				cols = set.size();
			}
		}

		@SuppressWarnings("unchecked")
		final T[][] result = (T[][]) Array.newInstance(clazz, rows, cols);

		int j = 0;
		for ( Map.Entry<T, Set<T>> me : mapping.entrySet() ) {
			T[] row = result[j];
			int i = 0;
			for ( T v : me.getValue() ) {
				row[i] = v;
				i++;
			}
			result[j++] = row;
		}

		return result;
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
	public T[][] getIdSets() {
		return idSets;
	}

	/**
	 * Get the virtual ID list.
	 * 
	 * @return the virtual IDs
	 */
	public T[] getVirtualIds() {
		return virtualIds;
	}

}
