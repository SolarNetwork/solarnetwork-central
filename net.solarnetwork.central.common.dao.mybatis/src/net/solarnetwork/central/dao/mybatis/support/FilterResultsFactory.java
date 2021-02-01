/* ==================================================================
 * FilterResultsFactory.java - 17/11/2020 9:43:10 am
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

package net.solarnetwork.central.dao.mybatis.support;

import java.util.Map;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Identity;

/**
 * Factory API for creating filter results instances.
 * 
 * @param <M>
 *        the data type
 * @param <K>
 *        the key type
 * @param <F>
 *        the filter type
 * @author matt
 * @version 1.0
 * @since 2.7
 */
@FunctionalInterface
public interface FilterResultsFactory<M extends Identity<K>, K, F> {

	/**
	 * Create a results instance from query results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sqlProps
	 *        the SQL parameters
	 * @param rows
	 *        the resulting rows
	 * @param totalCount
	 *        the total count (or {@literal null} if not known)
	 * @param offset
	 *        the offset of the first result
	 * @param returnedCount
	 *        the number of results returned
	 * @return the result instance
	 */
	FilterResults<M, K> createFilterResults(F filter, Map<String, Object> sqlProps, Iterable<M> rows,
			Long totalCount, Integer offset, Integer returnedCount);

}
