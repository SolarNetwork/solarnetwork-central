/* ==================================================================
 * DatumExportStatus.java - 29/03/2018 5:56:23 PM
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

package net.solarnetwork.central.datum.export.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import net.solarnetwork.event.AppEvent;
import net.solarnetwork.event.BasicAppEvent;

/**
 * The status of a datum export job.
 * 
 * <p>
 * This API is also a {@link Future} so you can get the results of the export
 * when it finishes.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.23
 */
public interface DatumExportStatus extends Future<DatumExportResult> {

	/** Topic for a job request notification. */
	String EVENT_TOPIC_JOB_STATUS_CHANGED = "net/solarnetwork/central/datum/export/JOB_STATUS_CHANGED";

	String EVENT_PROP_JOB_ID = "jobId";

	String EVENT_PROP_JOB_STATE = "jobState";

	String EVENT_PROP_PERCENT_COMPLETE = "percentComplete";

	String EVENT_PROP_COMPLETION_DATE = "completionDate";

	String EVENT_PROP_SUCCESS = "success";

	String EVENT_PROP_MESSAGE = "message";

	/**
	 * Get a unique ID for this export job.
	 * 
	 * @return the unique ID of this export job
	 */
	String getJobId();

	/**
	 * Get the state of the export job.
	 * 
	 * @return the state, never {@literal null}
	 */
	DatumExportState getJobState();

	/**
	 * Get a percentage complete for the job overall.
	 * 
	 * @return a percentage complete, or {@literal -1} if not known
	 */
	double getPercentComplete();

	/**
	 * Get the completion date, as milliseconds since the epoch.
	 * 
	 * @return the completion date, or {@literal 0} if not complete.
	 */
	long getCompletionDate();

	/**
	 * Create a job status changed event out of this instance.
	 * 
	 * @return the event, never {@literal null}
	 * @see #createJobStatusChagnedEvent(DatumExportStatus)
	 */
	default AppEvent asJobStatusChagnedEvent() {
		return createJobStatusChagnedEvent(this);
	}

	/**
	 * Create a job status changed event out of this instance.
	 * 
	 * @param result
	 *        a specific result to use
	 * @return the event, never {@literal null}
	 * @see #createJobStatusChagnedEvent(DatumExportStatus, DatumExportResult)
	 */
	default AppEvent asJobStatusChagnedEvent(DatumExportResult result) {
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
	static AppEvent createJobStatusChagnedEvent(DatumExportStatus status) {
		DatumExportResult result = null;
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
	static AppEvent createJobStatusChagnedEvent(DatumExportStatus status, DatumExportResult result) {
		Map<String, Object> props = new HashMap<>(4);
		if ( status != null ) {
			props.put(EVENT_PROP_JOB_ID, status.getJobId());
			props.put(EVENT_PROP_JOB_STATE, status.getJobState() != null ? status.getJobState().getKey()
					: DatumExportState.Unknown.getKey());
			props.put(EVENT_PROP_PERCENT_COMPLETE, status.getPercentComplete());
			props.put(EVENT_PROP_COMPLETION_DATE, status.getCompletionDate());
			if ( result != null ) {
				props.put(EVENT_PROP_SUCCESS, result.isSuccess());
				props.put(EVENT_PROP_MESSAGE, result.getMessage());
			}
		}
		return new BasicAppEvent(EVENT_TOPIC_JOB_STATUS_CHANGED, props);
	}

}
