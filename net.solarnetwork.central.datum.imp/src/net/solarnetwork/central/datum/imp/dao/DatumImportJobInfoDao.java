/* ==================================================================
 * DatumImportJobInfoDao.java - 7/11/2018 11:24:47 AM
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

package net.solarnetwork.central.datum.imp.dao;

import java.util.Set;
import org.joda.time.DateTime;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * DAO API for {@link DatumImportJobInfo} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportJobInfoDao extends GenericDao<DatumImportJobInfo, UserUuidPK> {

	/**
	 * Claim a queued job.
	 * 
	 * This method will "claim" a job that is currently in the
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Queued}
	 * state, changing the state to
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Claimed}.
	 * 
	 * @return a claimed job, or {@literal null} if none could be claimed
	 */
	DatumImportJobInfo claimQueuedJob();

	/**
	 * Purge jobs that have reached a
	 * {@link net.solarnetwork.central.datum.export.domain.DatumImportState#Completed}
	 * and are older than a given date.
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge completed jobs
	 * @return the number of jobs deleted
	 */
	long purgeCompletedJobs(DateTime olderThanDate);

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
	 */
	boolean updateJobState(UserUuidPK id, DatumImportState desiredState,
			Set<DatumImportState> expectedStates);

}
