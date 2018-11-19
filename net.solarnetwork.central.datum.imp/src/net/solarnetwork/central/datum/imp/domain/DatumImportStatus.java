/* ==================================================================
 * DatumImportStatus.java - 7/11/2018 7:21:45 AM
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

package net.solarnetwork.central.datum.imp.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.osgi.service.event.Event;

/**
 * The status of a datum import job.
 * 
 * <p>
 * This API is also a {@link Future} so you can get the results of the import
 * when it finishes.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface DatumImportStatus extends DatumImportReceipt, Future<DatumImportResult> {

	/** Topic for an import job status change notification. */
	String EVENT_TOPIC_JOB_STATUS_CHANGED = "net/solarnetwork/central/datum/imp/JOB_STATUS_CHANGED";

	/** Event property for the import job ID. */
	String EVENT_PROP_JOB_ID = "jobId";

	/** Event property for the import job {@link DatumImportState} key. */
	String EVENT_PROP_JOB_STATE = "jobState";

	/**
	 * Event property for the import job percent complete (number from 0 to 1).
	 */
	String EVENT_PROP_PERCENT_COMPLETE = "percentComplete";

	/** Event property for the import job completion date (if completed). */
	String EVENT_PROP_COMPLETION_DATE = "completionDate";

	/** Event property for the import job success/failure flag. */
	String EVENT_PROP_SUCCESS = "success";

	/** Event property for the import job status or error message. */
	String EVENT_PROP_MESSAGE = "message";

	/**
	 * Get the owner ID of the import task.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

	/**
	 * Get a percentage complete for the job overall.
	 * 
	 * @return a percentage complete, or {@literal -1} if not known
	 */
	double getPercentComplete();

	/**
	 * Get the date the import job was submitted.
	 * 
	 * @return the date the job was submitted, as milliseconds since the epoch.
	 */
	long getSubmitDate();

	/**
	 * Get the configured import date.
	 * 
	 * @return the import date, as milliseconds since the epoch.
	 */
	long getImportDate();

	/**
	 * Get the date the import task started.
	 * 
	 * @return the started date, as milliseconds since the epoch, or
	 *         {@literal 0} if not started
	 */
	long getStartedDate();

	/**
	 * Get the completion date.
	 * 
	 * @return the completion date, as milliseconds since the epoch, or
	 *         {@literal 0} if not complete
	 */
	long getCompletionDate();

	/**
	 * Get a success flag.
	 * 
	 * @return the success flag
	 */
	boolean isSuccess();

	/**
	 * Get a message about the result.
	 * 
	 * <p>
	 * If {@link #isSuccess()} returns {@literal false}, this method will return
	 * a message about the error.
	 * </p>
	 * 
	 * @return a message
	 */
	String getMessage();

	/**
	 * Get the number of datum successfully loaded.
	 * 
	 * <p>
	 * Note that even if {@link #isSuccess()} is {@literal false} this method
	 * can return a value greater than {@literal 0}, if partial results are
	 * supported by the transaction mode of the import process.
	 * </p>
	 * 
	 * @return the number of successfully loaded datum
	 */
	long getLoadedCount();

	/**
	 * Get the configuration associated with this job.
	 * 
	 * @return the configuration
	 */
	Configuration getConfiguration();

	/**
	 * Create a job status changed event out of this instance.
	 * 
	 * @return the event, never {@literal null}
	 * @see #createJobStatusChagnedEvent(DatumImportStatus)
	 */
	default Event asJobStatusChagnedEvent() {
		return createJobStatusChagnedEvent(this);
	}

	/**
	 * Create a job status changed event out of this instance.
	 * 
	 * @param result
	 *        a specific result to use
	 * @return the event, never {@literal null}
	 * @see #createJobStatusChagnedEvent(DatumImportStatus, DatumImportResult)
	 */
	default Event asJobStatusChagnedEvent(DatumImportResult result) {
		return createJobStatusChagnedEvent(this, result);
	}

	/**
	 * Create an event out of a status instance.
	 * 
	 * <p>
	 * The event will be populated with the property constants defined on this
	 * interface, using values from {@code status}.
	 * </p>
	 * 
	 * @param status
	 *        the status instance to create the event for
	 * @return the event, never {@literal null}
	 */
	static Event createJobStatusChagnedEvent(DatumImportStatus status) {
		DatumImportResult result = null;
		if ( status.isDone() ) {
			try {
				result = status.get(1, TimeUnit.SECONDS);
			} catch ( Exception e ) {
				// ignore
			}
		}
		return createJobStatusChagnedEvent(status, result);
	}

	/**
	 * Create an event out of a status instance.
	 * 
	 * <p>
	 * The event will be populated with the property constants defined on this
	 * interface, using values from {@code status}.
	 * </p>
	 * 
	 * @param status
	 *        the status instance to create the event for
	 * @return the event, never {@literal null}
	 */
	static Event createJobStatusChagnedEvent(DatumImportStatus status, DatumImportResult result) {
		Map<String, Object> props = new HashMap<String, Object>(4);
		if ( status != null ) {
			props.put(EVENT_PROP_JOB_ID, status.getJobId());
			props.put(EVENT_PROP_JOB_STATE, status.getJobState() != null ? status.getJobState().getKey()
					: DatumImportState.Unknown.getKey());
			props.put(EVENT_PROP_PERCENT_COMPLETE, status.getPercentComplete());
			props.put(EVENT_PROP_COMPLETION_DATE, status.getCompletionDate());
			if ( result != null ) {
				props.put(EVENT_PROP_SUCCESS, result.isSuccess());
				props.put(EVENT_PROP_MESSAGE, result.getMessage());
			}
		}
		return new Event(EVENT_TOPIC_JOB_STATUS_CHANGED, props);
	}

}
