/* ==================================================================
 * ObjectMappingCriteria.java - 4/12/2020 2:29:56 pm
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.util.StringUtils;

/**
 * Criteria for mapping object IDs into virtual IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ObjectMappingCriteria {

	/**
	 * Get a map whose keys represent virtual object ID values for the
	 * associated value's set of real object IDs.
	 * 
	 * <p>
	 * This mapping provides a way to request a set of object IDs be treated as
	 * a single logical virtual object ID. For example a set of object IDs for
	 * data collected from different buildings could be treated as a single
	 * virtual site object ID.
	 * <p>
	 * 
	 * @return the mapping of virtual object IDs to the set of real object IDs
	 *         that should be mapped to them
	 */
	Map<Long, Set<Long>> getObjectIdMappings();

	/**
	 * Create object ID mappings from a list of string encoded mappings.
	 * 
	 * <p>
	 * Each mapping in {@code mappings} must be encoded as
	 * {@literal VIRT_OBJ_ID:OBJ_ID1,OBJ_ID2,...}. That is, a virtual object ID
	 * followed by a colon followed by a comma-delimited list of real object
	 * IDs.
	 * </p>
	 * <p>
	 * A special case is handled when the mappings are such that the first
	 * includes the colon delimiter (e.g. {@literal 1:2}), and the remaining
	 * values are simple strings (e.g. {@literal 3, 4, 5}). In that case a
	 * single virtual object ID mapping is created.
	 * </p>
	 * 
	 * @param mappings
	 *        the mappings to decode
	 * @return the mappings, or {@literal null} if {@code mappings} is empty
	 */
	static Map<Long, Set<Long>> mappingsFrom(String[] mappings) {
		Map<Long, Set<Long>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<Long, Set<Long>>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like 1:2, 3, 4
					try {
						result.get(result.keySet().iterator().next()).add(Long.valueOf(map));
					} catch ( NumberFormatException e ) {
						// ignore
					}
					continue;
				} else if ( vIdDelimIdx < 1 || vIdDelimIdx + 1 >= map.length() ) {
					continue;
				}
				try {
					Long vId = Long.valueOf(map.substring(0, vIdDelimIdx));
					Set<String> rIds = StringUtils
							.commaDelimitedStringToSet(map.substring(vIdDelimIdx + 1));
					Set<Long> rNodeIds = new LinkedHashSet<Long>(rIds.size());
					for ( String rId : rIds ) {
						rNodeIds.add(Long.valueOf(rId));
					}
					result.put(vId, rNodeIds);
				} catch ( NumberFormatException e ) {
					// ignore and continue
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

}
