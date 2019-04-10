/* ==================================================================
 * DatumMaintenanceBiz.java - 10/04/2019 8:56:08 am
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

package net.solarnetwork.central.datum.biz;

import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;

/**
 * API for datum maintenance tasks.
 * 
 * @author matt
 * @version 1.0
 * @since 1.38
 */
public interface DatumMaintenanceBiz {

	/**
	 * Mark a set of datum aggregate data as "stale" so the aggregates are
	 * re-processed.
	 * 
	 * <p>
	 * The given criteria defines which data should be marked as stale.
	 * </p>
	 * 
	 * @param criteria
	 *        the criteria of datum to mark stale
	 */
	void markDatumAggregatesStale(GeneralNodeDatumFilter criteria);

}
