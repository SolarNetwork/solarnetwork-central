/* ==================================================================
 * NotificationScheduler.java - Jun 29, 2011 3:21:35 PM
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

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import net.solarnetwork.central.scheduler.EventHandlerSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi {@link EventHandler} that schedules job requests.
 * 
 * @author matt
 * @version 1.0
 */
public class NotificationScheduler extends EventHandlerSupport {

	private static final String NOTIFICATION_JOB_NAME = "NotificationJob";
	private static final String NOTIFICATION_JOB_GROUP = "Notification";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Scheduler scheduler;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler
	 */
	public NotificationScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	protected void handleEventInternal(Event event) throws SchedulerException {

		final String jobId = (String) event.getProperty(SchedulerConstants.JOB_ID);
		if ( jobId == null ) {
			log.debug("Ignoring OSGi event without {}", SchedulerConstants.JOB_ID);
			return;
		}

		final String jobGroup = (String) event.getProperty(SchedulerConstants.JOB_GROUP);

		final Long jobDate = (Long) event.getProperty(SchedulerConstants.JOB_DATE);
		final String jobCron = (String) event.getProperty(SchedulerConstants.JOB_CRON_EXPRESSION);
		if ( jobDate == null && jobCron == null ) {
			log.debug("Ignoring OSGi event without {} or {} specified", SchedulerConstants.JOB_DATE,
					SchedulerConstants.JOB_CRON_EXPRESSION);
			return;
		}

		final String jobName = (jobCron != null ? jobId : NOTIFICATION_JOB_NAME);

		final String jobTopic = (String) event.getProperty(SchedulerConstants.JOB_TOPIC);
		if ( jobTopic == null ) {
			log.debug("Ignoring OSGi event without {}", SchedulerConstants.JOB_TOPIC);
			return;
		}

		@SuppressWarnings("unchecked")
		final Map<String, ?> jobProps = (Map<String, ?>) event
				.getProperty(SchedulerConstants.JOB_PROPERTIES);

		JobDetail job = scheduler.getJobDetail(jobName, NOTIFICATION_JOB_GROUP);
		if ( job == null ) {
			job = new JobDetail(jobName, NOTIFICATION_JOB_GROUP,
					(jobCron != null ? StatefulNotificationJob.class : NotificationJob.class), false,
					true, false);
			scheduler.addJob(job, false);
		}

		final Trigger trigger;
		if ( jobDate != null ) {
			trigger = new SimpleTrigger(jobId, jobGroup, new Date(jobDate));
		} else {
			try {
				trigger = new CronTrigger(jobId, jobGroup, jobCron);
			} catch ( ParseException e ) {
				log.error("Bad cron expression [{}]: {}", jobCron, e.getMessage());
				return;
			}
		}
		trigger.setJobName(jobName);
		trigger.setJobGroup(NOTIFICATION_JOB_GROUP);

		// set up trigger properties, copying all job request properties
		JobDataMap jobMap = new JobDataMap();
		for ( String propName : event.getPropertyNames() ) {
			jobMap.put(propName, event.getProperty(propName));
		}
		if ( jobProps != null ) {
			jobMap.putAll(jobProps);
		}
		trigger.setJobDataMap(jobMap);

		Trigger t = scheduler.getTrigger(jobId, jobGroup);
		if ( t != null ) {
			// job already scheduled, check for same time to re-schedule
			if ( t instanceof CronTrigger ) {
				CronTrigger ct = (CronTrigger) t;
				if ( !ct.getCronExpression().equals(jobCron) ) {
					log.debug("Re-scheduling cron job {}.{} for {}", new Object[] { jobGroup, jobId,
							jobCron });
					scheduler.rescheduleJob(jobId, jobGroup, trigger);
				}
			} else {
				Date d = t.getStartTime();
				if ( d.getTime() != jobDate ) {
					log.debug("Re-scheduling job {}.{} for {}", new Object[] { jobGroup, jobId,
							new Date(jobDate) });
					scheduler.rescheduleJob(jobId, jobGroup, trigger);
				}
			}
		} else {
			log.debug("Scheduling job {}.{} for {}", new Object[] { jobGroup, jobId,
					(jobCron != null ? jobCron : new Date(jobDate).toString()) });
			scheduler.scheduleJob(trigger);
		}
	}

}
