/* ==================================================================
 * UserDatumDeleteJobBiz.java - 13/11/2018 2:19:38 PM
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

import java.time.Instant;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobStatus;

/**
 * Service API for operations related to datum delete jobs.
 * 
 * <p>
 * This API is meant more for internal use by datum delete scheduling jobs.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public interface UserDatumDeleteJobBiz {

	/**
	 * Perform a datum delete job.
	 * 
	 * <p>
	 * This method can only be called after a job ID has been returned from a
	 * previous call to
	 * {@link UserDatumDeleteBiz#submitDatumDeleteRequest(net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter)}.
	 * </p>
	 * 
	 * @param id
	 *        the ID of the delete job to perform
	 * @return the job status, or {@literal null} if the job is not available
	 */
	DatumDeleteJobStatus performDatumDelete(UserUuidPK id);

	/**
	 * Purge old jobs.
	 * 
	 * <p>
	 * This method will delete the job status associated with jobs that have
	 * reached a {@link DatumDeleteJobState#Completed} state and whose
	 * completion date is older than a given date.
	 * </p>
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge jobs
	 * @return the number of jobs deleted
	 */
	long purgeOldJobs(Instant olderThanDate);

}
