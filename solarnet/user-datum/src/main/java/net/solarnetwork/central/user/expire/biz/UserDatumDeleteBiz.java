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

import java.util.Collection;
import java.util.Set;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;

/**
 * API that provides a way for users to delete datum associated with their
 * account.
 * 
 * <p>
 * This API can be though of as an "expire right now" API.
 * </p>
 * 
 * @author matt
 * @version 2.0
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
	 * Submit a delete datum request.
	 * 
	 * <p>
	 * The delete process is not expected to start after calling this method.
	 * Rather it should enter the {@link DatumDeleteJobState#Queued} state. To
	 * initiate the import process, the
	 * {@link UserDatumDeleteJobBiz#performDatumDelete(UserUuidPK)} must be
	 * called, passing in the same user ID and the returned
	 * {@link DatumDeleteJobInfo#getJobId()}.
	 * </p>
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
	 * @param request
	 *        the request
	 * @return the job info
	 */
	DatumDeleteJobInfo submitDatumDeleteRequest(GeneralNodeDatumFilter request);

	/**
	 * Get the status of a specific datum delete job.
	 * 
	 * @param userId
	 *        the user ID that owns the job
	 * @param jobId
	 *        the ID of the job to get
	 * @return the job status, or {@literal null} if not available
	 */
	DatumDeleteJobInfo datumDeleteJobForUser(Long userId, String jobId);

	/**
	 * Find all available datum delete job statuses for a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to find the job statuses for
	 * @param states
	 *        the specific states to limit the results to, or {@literal null}
	 *        for all states
	 * @return the job statuses, never {@literal null}
	 */
	Collection<DatumDeleteJobInfo> datumDeleteJobsForUser(Long userId, Set<DatumDeleteJobState> states);
}
