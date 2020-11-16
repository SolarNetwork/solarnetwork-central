/* ==================================================================
 * DaoUtils.java - 17/11/2020 8:21:30 am
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

import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.Identity;

/**
 * Utilities for DAOs.
 * 
 * @author matt
 * @version 1.0
 * @since 2.7
 */
public final class DaoUtils {

	private DaoUtils() {
		super();
	}

	/**
	 * Create a {@link FilterResults} instance.
	 * 
	 * @param <M>
	 *        the result type
	 * @param <K>
	 *        the result key type
	 * @param data
	 *        the result data
	 * @param criteria
	 *        the criteria used to produce the results
	 * @param totalResults
	 *        the total result count, if available
	 * @param the
	 *        returned results count
	 * @return the results instance
	 */
	public static <M extends Identity<K>, K> FilterResults<M, K> filterResults(Iterable<M> data,
			PaginationCriteria criteria, Long totalResults, int returnedResults) {
		if ( criteria != null && criteria.getMax() != null ) {
			int offset = criteria.getOffset() != null ? criteria.getOffset() : 0;
			return new BasicFilterResults<>(data, totalResults, offset, returnedResults);
		}
		return new BasicFilterResults<>(data);
	}

}
