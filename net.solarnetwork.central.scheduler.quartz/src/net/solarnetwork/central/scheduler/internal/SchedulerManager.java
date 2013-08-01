/* ==================================================================
 * SchedulerManager.java - Jun 29, 2011 4:22:24 PM
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.scheduler.EventHandlerSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Manage the lifecycle of the Quartz Scheduler.
 * 
 * @author matt
 * @version 1.0
 */
public class SchedulerManager extends EventHandlerSupport implements
		ApplicationListener<ContextRefreshedEvent>, EventHandler {

	private static final String TEST_TOPIC = "net/solarnetwork/central/scheduler/TEST";

	private final Scheduler scheduler;
	private final EventAdmin eventAdmin;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the Scheduler
	 */
	public SchedulerManager(Scheduler scheduler, EventAdmin eventAdmin) {
		this.scheduler = scheduler;
		this.eventAdmin = eventAdmin;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			for ( String triggerGroup : scheduler.getTriggerGroupNames() ) {
				for ( String triggerName : scheduler.getTriggerNames(triggerGroup) ) {
					Trigger t = scheduler.getTrigger(triggerName, triggerGroup);
					log.debug("Found trigger: " + t);
				}
			}

			// fire off test Trigger to verify scheduler operational
			Map<String, Object> props = new HashMap<String, Object>(5);
			props.put(SchedulerConstants.JOB_ID, "SchedulerManager.Startup");
			props.put(SchedulerConstants.JOB_DATE, new Date().getTime() + 5000);
			props.put(SchedulerConstants.JOB_GROUP, "Test");
			props.put(SchedulerConstants.JOB_TOPIC, TEST_TOPIC);

			Event e = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, props);
			eventAdmin.postEvent(e);

		} catch ( SchedulerException e ) {
			log.error("Exception finding triggers", e);
		}
	}

	@Override
	protected void handleEventInternal(Event event) throws Exception {
		log.debug("Got event: {}", event.getTopic());
		if ( SchedulerConstants.TOPIC_JOB_COMPLETE.equals(event.getTopic()) ) {
			NotificationJob job = getRunningJob(event);
			job.finish(event);
		} else if ( SchedulerConstants.TOPIC_JOB_FAILURE.equals(event.getTopic()) ) {
			NotificationJob job = getRunningJob(event);
			job.finish(event);
		} else if ( TEST_TOPIC.equals(event.getTopic()) ) {
			Event ack = SchedulerUtils.createJobCompleteEvent(event);
			eventAdmin.postEvent(ack);

			// post "we're ready" event
			Map<String, Object> props = new HashMap<String, Object>(1);
			Event e = new Event(SchedulerConstants.TOPIC_SCHEDULER_READY, props);
			eventAdmin.postEvent(e);
		}
	}

	@SuppressWarnings("unchecked")
	private NotificationJob getRunningJob(Event event) throws SchedulerException {
		final String jobId = (String) event.getProperty(SchedulerConstants.JOB_ID);
		if ( jobId == null ) {
			log.debug("Can't find running job for event because JOB_ID missing");
			return null;
		}
		final String jobGroup = (String) event.getProperty(SchedulerConstants.JOB_GROUP);
		for ( JobExecutionContext jec : (List<JobExecutionContext>) scheduler
				.getCurrentlyExecutingJobs() ) {
			Trigger t = jec.getTrigger();
			if ( jobId.equals(t.getName()) && (jobGroup == null || jobGroup.equals(t.getGroup())) ) {
				return (NotificationJob) jec.getJobInstance();
			}
		}
		log.debug("Running job {} in group {} not found", jobId, jobGroup);
		return null;
	}

}
