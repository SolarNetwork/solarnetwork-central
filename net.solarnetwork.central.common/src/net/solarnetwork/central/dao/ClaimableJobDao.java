/* ==================================================================
 * ClaimableJobDao.java - 26/11/2018 9:31:48 AM
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.util.Set;
import org.joda.time.DateTime;
import net.solarnetwork.central.domain.ClaimableJob;
import net.solarnetwork.central.domain.ClaimableJobState;

/**
 * DAO API for {@link ClaimableJob} entities.
 * 
 * @param <C>
 *        the job configuration type
 * @param <R>
 *        the job result type
 * @param <S>
 *        the job state type
 * @param <T>
 *        the job entity type
 * @param <PK>
 *        the job entity primary key type
 * @author matt
 * @version 1.0
 * @since 1.44
 */
public interface ClaimableJobDao<C, R, S extends ClaimableJobState, T extends ClaimableJob<C, R, S, PK>, PK extends Serializable> {

	/**
	 * Claim a queued job.
	 * 
	 * This method will "claim" a job that is currently in a "queued" state,
	 * changing the state to "claimed".
	 * 
	 * @return a claimed job, or {@literal null} if none could be claimed
	 */
	T claimQueuedJob();

	/**
	 * Purge old jobs.
	 * 
	 * <p>
	 * This method will delete jobs that have reached a "completed" state and
	 * whose completion date is older than a given date. It might also delete
	 * jobs that are in other, implementation specific, states that meet some
	 * implementation specific criteria allowing them to be purged.
	 * </p>
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge old jobs
	 * @return the number of jobs deleted
	 */
	long purgeOldJobs(DateTime olderThanDate);

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
	boolean updateJobState(PK id, S desiredState, Set<S> expectedStates);

	/**
	 * Update the configuration for a specific job.
	 * 
	 * <p>
	 * The configuration update might be ignored if the job is not in a
	 * re-configurable state, e.g. it is "claimed" or "executing".
	 * </p>
	 * 
	 * @param id
	 *        the ID of the job to update
	 * @param configuration
	 *        the configuration to save with the job, completely replacing any
	 *        existing configuration
	 * @return {@literal true} if the job configuration was changed
	 */
	boolean updateJobConfiguration(PK id, C configuration);

	/**
	 * Update the progress of a specific job.
	 * 
	 * @param id
	 *        the ID of the job to update
	 * @param percentComplete
	 *        the percent complete, from 0 to 1
	 * @param result
	 *        the result
	 * @return {@literal true} if the job progress was updated
	 */
	boolean updateJobProgress(PK id, double percentComplete, R result);
}
