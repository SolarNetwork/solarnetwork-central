/* ==================================================================
 * UserDatumDeleteBiz.java - 24/11/2018 9:22:39 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.biz;

import java.util.concurrent.Future;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;

/**
 * API that provides a way for users to delete datum associated with their
 * account.
 * 
 * <p>
 * This API can be though of as an "expire right now" API.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDatumDeleteBiz {

	/**
	 * Get a count of datum records that match a search criteria.
	 * 
	 * <p>
	 * At a minimum, the following criteria are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>user ID - required</li>
	 * <li>node IDs</li>
	 * <li>source IDs</li>
	 * <li>date range (start/end dates)</li>
	 * </ul>
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the count of matching records
	 * @since 1.8
	 */
	DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter);

	/**
	 * Delete datum matching a search criteria.
	 * 
	 * <p>
	 * At a minimum, the following criteria are supported:
	 * </p>
	 * 
	 * <ul>
	 * <li>user ID - required</li>
	 * <li>node IDs</li>
	 * <li>source IDs</li>
	 * <li>date range (start/end dates)</li>
	 * </ul>
	 * 
	 * @param userId
	 *        the account owner ID
	 * @param filter
	 *        the search criteria
	 * @return the number of datum deleted
	 * @since 1.8
	 */
	Future<Long> deleteFiltered(GeneralNodeDatumFilter filter);

}
