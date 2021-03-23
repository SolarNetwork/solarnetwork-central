/* ==================================================================
 * ClaimableJob.java - 26/11/2018 9:45:26 AM
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

package net.solarnetwork.central.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * A "claimable job" entity.
 * 
 * @param <C>
 *        the job configuration type
 * @param <R>
 *        the job result type
 * @param <S>
 *        the job state type
 * @param <PK>
 *        the job entity primary key type
 * @author matt
 * @version 1.1
 * @since 1.44
 */
public interface ClaimableJob<C, R, S extends ClaimableJobState, PK> extends Entity<PK> {

	/**
	 * Get the job configuration details.
	 * 
	 * @return the job configuration
	 */
	C getConfiguration();

	/**
	 * Get the job state.
	 * 
	 * @return the job state
	 */
	S getJobState();

	/**
	 * Get a job group key.
	 * 
	 * <p>
	 * A group key represents a grouping of related jobs, such that only one job
	 * within a given group should be allowed to execute at a time. This
	 * provides a way to synchronize multiple related jobs in a reliable manner.
	 * </p>
	 * 
	 * @return the group key, or {@literal null} for the "default" group
	 * @since 1.1
	 */
	String getGroupKey();

	/**
	 * Get a percentage complete for the job overall.
	 * 
	 * @return a percentage complete, or {@literal -1} if not known
	 */
	double getPercentComplete();

	/**
	 * Get the date the job started execution.
	 * 
	 * @return the started date or {@literal null} if not started
	 */
	DateTime getStarted();

	/**
	 * Get the completed date.
	 * 
	 * @return the completed date, or {@literal null} if not complete
	 */
	DateTime getCompleted();

	/**
	 * Get the job execution duration.
	 * 
	 * <p>
	 * This will return the overall job execution duration, based on the
	 * {@code started} and {@code completed} dates. If both are available, the
	 * duration returned is the difference between the two. If just
	 * {@code started} is available, the difference between now and then is
	 * returned. Otherwise a zero-duration value is returned.
	 * </p>
	 * 
	 * @return the duration, never {@literal null}
	 */
	default public Duration getJobDuration() {
		DateTime s = getStarted();
		DateTime e = getCompleted();
		if ( s != null && e != null ) {
			return new Duration(s.getMillis(), e.getMillis());
		} else if ( s != null ) {
			return new Duration(s.getMillis(), System.currentTimeMillis());
		}
		return Duration.ZERO;
	}

	/**
	 * Get a success flag.
	 * 
	 * @return the success flag, or {@literal null} if not known
	 */
	Boolean getJobSuccess();

	/**
	 * Get a message about the result.
	 * 
	 * <p>
	 * If {@link #getJobSuccess()} returns {@literal false}, this method will
	 * return a message about the error.
	 * </p>
	 * 
	 * @return a message
	 */
	String getMessage();

	/**
	 * Get the number of datum successfully loaded.
	 * 
	 * <p>
	 * Note that even if {@link #getJobSuccess()} is {@literal false} this
	 * method can return a value greater than {@literal 0}, if partial results
	 * are supported by the transaction mode of the import process.
	 * </p>
	 * 
	 * @return the number of successfully loaded datum
	 */
	R getResult();

}
