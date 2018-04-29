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
import org.osgi.service.event.Event;

/**
 * The status of a datum export job.
 * 
 * <p>
 * This API is also a {@link Future} so you can get the results of the export
 * when it finishes.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public interface DatumExportStatus extends Future<DatumExportResult> {

	/** Topic for a job request notification. */
	String EVENT_TOPIC_JOB_STATUS_CHANGED = "net/solarnetwork/central/datum/export/JOB_STATUS_CHAGNED";

	String EVENT_PROP_JOB_ID = "jobId";

	String EVENT_PROP_JOB_STATE = "jobState";

	String EVENT_PROP_PERCENT_COMPLETE = "percentComplete";

	String EVENT_PROP_COMPLETION_DATE = "completionDate";

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
	default Event asJobStatusChagnedEvent() {
		return createJobStatusChagnedEvent(this);
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
	static Event createJobStatusChagnedEvent(DatumExportStatus status) {
		Map<String, Object> props = new HashMap<String, Object>(4);
		if ( status != null ) {
			props.put(EVENT_PROP_JOB_ID, status.getJobId());
			props.put(EVENT_PROP_JOB_STATE, status.getJobState() != null ? status.getJobState().getKey()
					: DatumExportState.Unknown.getKey());
			props.put(EVENT_PROP_PERCENT_COMPLETE, status.getPercentComplete());
			props.put(EVENT_PROP_COMPLETION_DATE, status.getCompletionDate());
		}
		return new Event(EVENT_TOPIC_JOB_STATUS_CHANGED, props);
	}

}
