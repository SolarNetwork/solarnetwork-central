/* ==================================================================
 * StaleAggregateDatumProcessor.java - 4/11/2019 4:27:47 pm
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

package net.solarnetwork.central.datum.agg;

import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.Identity;

/**
 * API for handling stale aggregate datum.
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
public interface AggregateDatumProcessor {

	/**
	 * Process an aggregate datum.
	 * 
	 * @param userId
	 *        the ID of the owner of the node that produced {@code datum}
	 * @param aggregation
	 *        the datum aggregation
	 * @param datum
	 *        the datum
	 * @return {@literal true} if the processing was handled successfully
	 */
	boolean processStaleAggregateDatum(Long userId, Aggregation aggregation,
			Identity<GeneralNodeDatumPK> datum);

}
