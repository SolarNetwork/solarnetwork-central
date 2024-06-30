/* ==================================================================
 * ScaleFactorCriteria.java - 1/07/2024 6:41:17 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Search criteria for source ID to scale factor mapping.
 *
 * @author matt
 * @version 1.0
 */
public interface SourceScaleFactorMappingCriteria {

	/**
	 * Get a map whose keys represent source ID values and values an associated
	 * scale factor to apply.
	 *
	 * @return the mapping of source IDs to a scale factor
	 */
	Map<String, BigDecimal> getSourceIdScaleFactorMappings();

	/**
	 * Create a source ID scale factor mapping from a list of string encoded
	 * mappings.
	 *
	 * <p>
	 * The mappings in {@code mappings} must be encoded as
	 * {@literal SOURCE_ID:SCALE_FACTOR,...}. That is, a source ID followed by a
	 * colon followed by a decimal number scale factor.
	 * </p>
	 *
	 * @param mappings
	 *        the mappings to decode
	 * @return the mappings, or {@literal null} if {@code mappings} is empty
	 */
	static Map<String, BigDecimal> sourceIdScaleFactorMappingsFrom(String[] mappings) {
		Map<String, BigDecimal> result;
		if ( mappings == null || mappings.length < 1 ) {
			result = null;
		} else {
			result = new LinkedHashMap<>(mappings.length);
			for ( String map : mappings ) {
				int delimIdx = map.indexOf(':');
				if ( delimIdx < 1 || delimIdx + 1 >= map.length() ) {
					continue;
				}
				try {
					String sourceId = map.substring(0, delimIdx);
					BigDecimal n = NumberUtils
							.bigDecimalForNumber(StringUtils.numberValue(map.substring(delimIdx + 1)));
					if ( n != null ) {
						result.put(sourceId, n);
					}
				} catch ( NumberFormatException e ) {
					// ignore and continue
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

}
