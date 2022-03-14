/* ==================================================================
 * SourceMappingCriteria.java - 4/12/2020 2:39:29 pm
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
import java.util.Map;
import java.util.Set;
import net.solarnetwork.util.StringUtils;

/**
 * Criteria for mapping source IDs into virtual IDs.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface SourceMappingCriteria {

	/**
	 * Get a map whose keys represent virtual source ID values for the
	 * associated value's set of real source IDs.
	 * 
	 * <p>
	 * This mapping provides a way to request a set of source IDs be treated as
	 * a single logical virtual source ID. For example a set of source IDs for
	 * data collected from PV inverters could be treated as a single virtual
	 * source ID.
	 * <p>
	 * 
	 * @return the mapping of virtual source IDs to the set of real source IDs
	 *         that should be mapped to them
	 */
	Map<String, Set<String>> getSourceIdMappings();

	/**
	 * Create source ID mappings from a list of string encoded mappings.
	 * 
	 * <p>
	 * Each mapping in {@code mappings} must be encoded as
	 * {@literal VIRT_SRC_ID:SRC_ID1,SRC_ID2,...}. That is, a virtual source ID
	 * followed by a colon followed by a comma-delimited list of real source
	 * IDs.
	 * </p>
	 * <p>
	 * A special case is handled when the mappings are such that the first
	 * includes the colon delimiter (e.g. {@literal V:A}), and the remaining
	 * values are simple strings (e.g. {@literal B, C, D}). In that case a
	 * single virtual source ID mapping is created.
	 * </p>
	 * 
	 * @param mappings
	 *        the mappings to decode
	 * @return the mappings, or {@literal null} if {@code mappings} is empty
	 */
	static Map<String, Set<String>> mappingsFrom(String[] mappings) {
		Map<String, Set<String>> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<String, Set<String>>(mappings.length);
			for ( String map : mappings ) {
				int vIdDelimIdx = map.indexOf(':');
				if ( vIdDelimIdx < 1 && result.size() == 1 ) {
					// special case, when Spring maps single query param into 3 fields split on comma like A:B, C, D
					try {
						result.get(result.keySet().iterator().next()).add(map);
					} catch ( NumberFormatException e ) {
						// ignore
					}
					continue;
				} else if ( vIdDelimIdx < 1 || vIdDelimIdx + 1 >= map.length() ) {
					continue;
				}
				String vId = map.substring(0, vIdDelimIdx);
				Set<String> rSourceIds = StringUtils
						.commaDelimitedStringToSet(map.substring(vIdDelimIdx + 1));
				result.put(vId, rSourceIds);
			}
		}
		return (result.isEmpty() ? null : result);
	}

}
