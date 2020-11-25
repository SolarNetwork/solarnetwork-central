/* ==================================================================
 * DatumMaintenanceDao.java - 25/11/2020 2:39:08 pm
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

import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO API for datum maintenance tasks.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumMaintenanceDao {

	/**
	 * Mark a set of datum aggregate data as "stale" so the aggregates are
	 * re-processed.
	 * 
	 * <p>
	 * The given criteria defines which data should be marked as stale. The
	 * following criteria are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>stream ID(s)</li>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 * 
	 * @param criteria
	 *        the criteria of datum to mark stale
	 * @return the number of rows inserted
	 */
	int markDatumAggregatesStale(DatumStreamCriteria criteria);

	/**
	 * Find stale aggregate datum records for a given search criteria.
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the matching records
	 */
	FilterResults<StaleAggregateDatum, StreamKindPK> findStaleAggregateDatum(DatumStreamCriteria filter);

}
