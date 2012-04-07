/* ==================================================================
 * SchedulerUtils.java - Jun 30, 2011 3:30:05 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;

/**
 * Utility methods for working with scheduled jobs.
 * 
 * @author matt
 * @version $Revision$
 */
public final class SchedulerUtils {

	/**
	 * Create an Event suitable for a successful job completion acknowledgment.
	 * 
	 * @param job the event job
	 * @return the acknowledgment event
	 */
	public static Event createJobCompleteEvent(Event job) {
		return createJobFinishedEvent(
				SchedulerConstants.TOPIC_JOB_COMPLETE,
				(String)job.getProperty(SchedulerConstants.JOB_ID), 
				(String)job.getProperty(SchedulerConstants.JOB_GROUP),
				null);
	}
	
	/**
	 * Create an Event suitable for a job failure acknowledgment.
	 * 
	 * @param job the event job
	 * @return the acknowledgment event
	 */
	public static Event createJobFailureEvent(Event job, Throwable t) {
		return createJobFinishedEvent(
				SchedulerConstants.TOPIC_JOB_FAILURE,
				(String)job.getProperty(SchedulerConstants.JOB_ID), 
				(String)job.getProperty(SchedulerConstants.JOB_GROUP),
				t);
	}
	
	/**
	 * Create an Event suitable for a job acknowledgment.
	 * 
	 * @param topic the job topic
	 * @param jobId the job ID
	 * @param jobGroup the optional job group
	 * @param e the optional exception
	 * @return the acknowledgment event
	 */
	public static Event createJobFinishedEvent(String topic, String jobId, 
			String jobGroup, Throwable e) {
		Map<String, Object> props = new HashMap<String, Object>(2);
		props.put(SchedulerConstants.JOB_ID, jobId);
		if ( jobGroup != null ) {
			props.put(SchedulerConstants.JOB_GROUP, jobGroup);
		}
		if ( e != null ) {
			props.put(SchedulerConstants.JOB_EXCEPTION, e);
		}
		return new Event(topic, props);
	}
	
	// can't create me
	private SchedulerUtils() {
		super();
	}
	
}
