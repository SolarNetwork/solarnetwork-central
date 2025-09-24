/* ==================================================================
 * DatumImportJobBiz.java - 13/11/2018 2:19:38 PM
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

package net.solarnetwork.central.datum.imp.biz;

import java.time.Instant;
import java.util.Set;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;

/**
 * Service API for operations related to datum import jobs.
 *
 * <p>
 * This API is meant more for internal use by import scheduling jobs.
 * </p>
 *
 * @author matt
 * @version 2.1
 */
public interface DatumImportJobBiz {

	/**
	 * Perform a datum import.
	 *
	 * <p>
	 * This method can only be called after a job ID has been returned from a
	 * previous call to
	 * {@link DatumImportBiz#submitDatumImportRequest(DatumImportRequest, DatumImportResource)}.
	 * </p>
	 *
	 * @param id
	 *        the ID of the import job to perform
	 * @return the job status, or {@literal null} if the job is not available
	 */
	DatumImportStatus performImport(UserUuidPK id);

	/**
	 * Purge old jobs.
	 *
	 * <p>
	 * This method will delete the job status associated with jobs that have
	 * reached a {@link DatumImportState#Completed} state and whose completion
	 * date is older than a given date, <b>or</b> are in the
	 * {@link DatumImportState#Staged} state and whose creation date is older
	 * than a given date.
	 * </p>
	 *
	 * @param olderThanDate
	 *        the maximum date for which to purge jobs
	 * @return the number of jobs deleted
	 */
	long purgeOldJobs(Instant olderThanDate);

	/**
	 * Claim a queued job.
	 *
	 * <p>
	 * This method will "claim" a job that is currently in a "queued" state,
	 * changing the state to "claimed".
	 * </p>
	 *
	 * @return a claimed job, or {@literal null} if none could be claimed
	 */
	DatumImportJobInfo claimQueuedJob();

	/**
	 * Save job info.
	 *
	 * @param jobInfo
	 *        the job info to save
	 * @return the job primary key
	 */
	UserUuidPK saveJobInfo(DatumImportJobInfo jobInfo);

	/**
	 * Update the state of a specific job.
	 *
	 * @param id
	 *        the ID of the job to update
	 * @param desiredState
	 *        the state to change the job to
	 * @param expectedStates
	 *        a set of states that must include the job's current state in order
	 *        to change it to {@code desiredState}, or {@literal null} if the
	 *        current state of the job does not matter
	 * @return {@literal true} if the job state was changed
	 * @since 2.1
	 */
	boolean updateJobState(UserUuidPK id, DatumImportState desiredState,
			Set<DatumImportState> expectedStates);

}
