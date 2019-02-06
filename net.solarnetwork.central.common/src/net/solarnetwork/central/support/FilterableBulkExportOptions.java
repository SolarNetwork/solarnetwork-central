/* ==================================================================
 * FilterableBulkExportOptions.java - 31/01/2019 2:35:31 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * Convenience extension of {@link BasicBulkExportOptions} for filterable export
 * support.
 * 
 * @author matt
 * @version 1.0
 * @since 1.45
 */
public class FilterableBulkExportOptions extends BasicBulkExportOptions {

	/** The parameter key for a {@link Filter} object. */
	public static final String FILTER_PARAM = "filter";

	/** The parameter key for a {@link SortDescriptor} {@link List}. */
	public static final String SORT_DESC_LIST_PARAM = "sorts";

	/**
	 * Get a parameters object suitable for filterable exports.
	 * 
	 * @param filter
	 *        the filter
	 * @param sorts
	 *        the sorts
	 * @return the parameters
	 */
	public static final Map<String, Object> filterableParameters(Filter filter,
			List<SortDescriptor> sorts) {
		Map<String, Object> parameters = new LinkedHashMap<>(4);
		if ( filter != null ) {
			parameters.put(FILTER_PARAM, filter);
		}
		if ( sorts != null ) {
			parameters.put(SORT_DESC_LIST_PARAM, sorts);
		}
		return parameters;
	}

	/**
	 * @param name
	 * @param parameters
	 */
	public FilterableBulkExportOptions(String name, Filter filter, List<SortDescriptor> sorts) {
		super(name, filterableParameters(filter, sorts));
	}

}
