/* ==================================================================
 * JobSupport.java - Jun 30, 2011 5:09:59 PM
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

package net.solarnetwork.central.scheduler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Base helper class for a scheduled job.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>maximumWaitMs</dt>
 * <dd>The maximum time, in milliseconds, to allow for the job to execute before
 * it is considered a failed job. Defaults to <b>15 minutes</b>.</dd>
 * 
 * <dt>jobId</dt>
 * <dd>The unique ID of the job to schedule.</dd>
 * 
 * <dt>jobTopic</dt>
 * <dd>The {@link Event} topic to use for this job.</dd>
 * 
 * <dt>jobGroup</dt>
 * <dd>The job group to use. Defaults to <b>Datum</b>.</dd>
 * 
 * <dt>jobCron</dt>
 * <dd>The job cron expression to use for scheduling this job. Defaults to
 * <code>0 0/1 * * * ?</code> (once per minute)</dd>.
 * </dl>
 * 
 * @author matt
 * @version 1.5
 */
public abstract class JobSupport extends EventHandlerSupport {

	private final EventAdmin eventAdmin;
	private long maximumWaitMs = 15L * 60L * 1000L;
	private String jobId;
	private String jobTopic;
	private String jobGroup;
	private String jobCron = "0 0/1 * * * ?";
	private ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 */
	public JobSupport(EventAdmin eventAdmin) {
		super();
		this.eventAdmin = eventAdmin;
	}

	@Override
	protected final void handleEventInternal(final Event event) throws Exception {
		if ( event.getTopic().equals(SchedulerConstants.TOPIC_SCHEDULER_READY) ) {
			schedulerReady(event);
			return;
		}
		if ( jobId != null && !jobId.equals(event.getProperty(SchedulerConstants.JOB_ID)) ) {
			// same topic, wrong job
			return;
		}

		// kick off to another thread, so we don't block the event handler thread (and possibly get blacklisted)
		executorService.submit(new Runnable() {

			@Override
			public void run() {
				Event ack = null;
				Throwable thrown = null;
				boolean complete = false;
				if ( log.isDebugEnabled() ) {
					final String[] eventPropNames = event.getPropertyNames();
					final Map<String, Object> eventData = new LinkedHashMap<String, Object>(
							eventPropNames.length);
					for ( String propName : eventPropNames ) {
						eventData.put(propName, event.getProperty(propName));
					}
					log.debug("Executing job {}.{}; props = {}", jobGroup, jobId, eventData);
				}
				try {
					complete = handleJob(event);
				} catch ( Throwable e ) {
					log.warn("Exception in job {}", event.getTopic(), e);
					thrown = e;
				} finally {
					ack = handleJobCompleteEvent(event, complete, thrown);
					if ( ack != null ) {
						eventAdmin.postEvent(ack);
					}
				}
			}
		});
	}

	/**
	 * Handle the completion of a job.
	 * 
	 * This method is called internally by {@link #handleEventInternal(Event)}
	 * after {@link #handleJob(Event)} returns. Extending classes may want to
	 * customize the resulting job acknowledgement event.
	 * 
	 * @param jobEvent
	 *        The original job event that initiated the job.
	 * @param complete
	 *        The result of {@link #handleJob(Event)}, or <em>false</em> if that
	 *        method throws an exception.
	 * @param thrown
	 *        An exception thrown by {@link #handleJob(Event)}, or <em>null</em>
	 *        if none thrown.
	 * @return A new job acknowledgement event.
	 * @see SchedulerUtils#createJobCompleteEvent(Event)
	 * @see SchedulerUtils#createJobFailureEvent(Event, Throwable)
	 * @since 1.3
	 */
	protected Event handleJobCompleteEvent(Event jobEvent, boolean complete, Throwable thrown) {
		if ( log.isDebugEnabled() ) {
			final String[] eventPropNames = jobEvent.getPropertyNames();
			final Map<String, Object> eventData = new LinkedHashMap<String, Object>(
					eventPropNames.length);
			for ( String propName : eventPropNames ) {
				eventData.put(propName, jobEvent.getProperty(propName));
			}
			log.debug("Completed job {}.{}; success = {}; props = {}", jobGroup, jobId, complete,
					eventData);
		}
		if ( complete ) {
			return SchedulerUtils.createJobCompleteEvent(jobEvent);
		}
		return SchedulerUtils.createJobFailureEvent(jobEvent, thrown);
	}

	/**
	 * Handle the "scheduler ready" event, to give class a chance to perform
	 * startup tasks. This implementation generates a {@code TOPIC_JOB_REQUEST}
	 * event using the configured job properties and cron schedule on this
	 * class.
	 * 
	 * @param event
	 *        the event
	 * @throws Exception
	 *         if any error occurs
	 */
	protected void schedulerReady(Event event) throws Exception {
		Map<String, Object> props = new HashMap<String, Object>(5);
		props.put(SchedulerConstants.JOB_ID, jobId);
		props.put(SchedulerConstants.JOB_CRON_EXPRESSION, jobCron);
		props.put(SchedulerConstants.JOB_GROUP, jobGroup);
		props.put(SchedulerConstants.JOB_MAX_WAIT, maximumWaitMs);
		props.put(SchedulerConstants.JOB_TOPIC, jobTopic);

		Event e = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, props);
		getEventAdmin().postEvent(e);
	}

	/**
	 * Handle the job.
	 * 
	 * @param job
	 *        the job details
	 * @return <em>true</em> if job completed successfully, <em>false</em>
	 *         otherwise
	 * @throws Exception
	 *         if any error occurs
	 */
	protected abstract boolean handleJob(Event job) throws Exception;

	/**
	 * Get the EventAdmin.
	 * 
	 * @return the EventAdmin
	 */
	protected EventAdmin getEventAdmin() {
		return eventAdmin;
	}

	public long getMaximumWaitMs() {
		return maximumWaitMs;
	}

	public String getJobId() {
		return jobId;
	}

	public String getJobTopic() {
		return jobTopic;
	}

	public String getJobGroup() {
		return jobGroup;
	}

	public String getJobCron() {
		return jobCron;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public void setJobTopic(String jobTopic) {
		this.jobTopic = jobTopic;
	}

	public void setMaximumWaitMs(long maximumWaitMs) {
		this.maximumWaitMs = maximumWaitMs;
	}

	public void setJobCron(String jobCron) {
		this.jobCron = jobCron;
	}

	public void setJobGroup(String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

}
