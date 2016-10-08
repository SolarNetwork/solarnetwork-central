/* ==================================================================
 * NotificationJob.java - Jun 29, 2011 3:17:13 PM
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
 */

package net.solarnetwork.central.scheduler.internal;

import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.scheduler.SchedulerConstants;

/**
 * Quartz Job that sends out an OSGi Event notification based on the data in the
 * job.
 * 
 * <p>
 * Expects {@link #finish(Event)} to be called from a different thread to signal
 * the completion of the job.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class NotificationJob implements Job {

	/**
	 * The {@link org.quartz.SchedulerContext} key for the {@link EventAdmin}.
	 */
	public static final String EVENT_ADMIN_CONTEXT_KEY = "EventAdmin";

	/** The default amount of time to wait for a job to complete or fail. */
	public static final long DEFAULT_MAX_JOB_WAIT = 900000;

	private boolean complete = false;
	private boolean success = true;
	private Throwable throwable = null;
	private JobExecutionContext ctx;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void execute(JobExecutionContext jobContext) throws JobExecutionException {
		SchedulerContext context;
		try {
			context = jobContext.getScheduler().getContext();
		} catch ( SchedulerException e ) {
			throw new JobExecutionException("Error getting EventAdmin from SchedulerContext", e);
		}
		final EventAdmin eventAdmin = (EventAdmin) context.get(EVENT_ADMIN_CONTEXT_KEY);
		if ( eventAdmin == null ) {
			throw new JobExecutionException("EventAdmin not found on SchedulerContext");
		}

		final JobDataMap jobDataMap = jobContext.getMergedJobDataMap();
		final String jobTopic = jobDataMap.getString(SchedulerConstants.JOB_TOPIC);

		final Event event = new Event(jobTopic, jobContext.getMergedJobDataMap());

		// save a ref to jobContext for finished callback
		ctx = jobContext;

		final long start = System.currentTimeMillis();
		final long maxWait = (jobDataMap.containsKey(SchedulerConstants.JOB_MAX_WAIT)
				? (Long) jobDataMap.get(SchedulerConstants.JOB_MAX_WAIT) : DEFAULT_MAX_JOB_WAIT);
		try {
			synchronized ( this ) {
				// post the job event now, waiting for our acknowledgment event
				// within maxWait milliseconds
				eventAdmin.postEvent(event);
				while ( !complete ) {
					this.wait(maxWait);
					if ( !complete && (System.currentTimeMillis() - start) > maxWait ) {
						throw new JobExecutionException(
								"Timeout waiting for job to complete (" + maxWait + "ms)");
					}
				}
			}
		} catch ( InterruptedException e ) {
			throw new JobExecutionException(e);
		}

		if ( !success ) {
			throw new JobExecutionException("Job did not complete successfully", throwable);
		}
	}

	/**
	 * Call to signal to this job that the job is finished.
	 * 
	 * <p>
	 * It is assumed that another thread has called
	 * {@link #execute(JobExecutionContext)} and is waiting for a different
	 * thread to call this method and signal the completion of the job.
	 * </p>
	 * 
	 * @param event
	 */
	public synchronized void finish(Event event) {
		complete = true;
		if ( SchedulerConstants.TOPIC_JOB_FAILURE.equals(event.getTopic()) ) {
			success = false;
		}

		@SuppressWarnings("unchecked")
		final Map<String, ?> jobProps = (Map<String, ?>) event
				.getProperty(SchedulerConstants.JOB_PROPERTIES);
		if ( jobProps != null && ctx != null ) {
			log.debug("Saving {} job result properties: {}", ctx.getJobDetail().getKey().getName(),
					jobProps);
			ctx.getJobDetail().getJobDataMap().put(SchedulerConstants.JOB_PROPERTIES, jobProps);
		}
		throwable = (Throwable) event.getProperty(SchedulerConstants.JOB_EXCEPTION);
		this.notifyAll();
	}

}
