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
import java.util.TimeZone;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.scheduler.EventHandlerSupport;
import net.solarnetwork.central.scheduler.SchedulerConstants;

/**
 * OSGi {@link EventHandler} that schedules job requests.
 * 
 * @author matt
 * @version 1.2
 */
public class NotificationScheduler extends EventHandlerSupport {

	private static final String NOTIFICATION_JOB_NAME = "NotificationJob";
	private static final String NOTIFICATION_JOB_GROUP = "Notification";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Scheduler scheduler;
	private String cronTimeZoneId = TimeZone.getDefault().getID();

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
		JobKey jobKey = new JobKey(jobName, NOTIFICATION_JOB_GROUP);
		JobDetail job = scheduler.getJobDetail(jobKey);
		if ( job == null ) {
			job = JobBuilder
					.newJob((jobCron != null ? StatefulNotificationJob.class : NotificationJob.class))
					.withIdentity(jobKey).requestRecovery().storeDurably(false).build();
			scheduler.addJob(job, false);
		}

		// set up trigger properties, copying all job request properties
		final JobDataMap jobMap = new JobDataMap();
		for ( String propName : event.getPropertyNames() ) {
			jobMap.put(propName, event.getProperty(propName));
		}
		if ( jobProps != null ) {
			jobMap.putAll(jobProps);
		}

		final TriggerKey triggerKey = new TriggerKey(jobId, jobGroup);
		final Trigger trigger;
		if ( jobDate != null ) {
			trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).forJob(jobKey)
					.usingJobData(jobMap).startAt(new Date(jobDate)).build();
		} else {
			try {
				CronScheduleBuilder cronBuilder = CronScheduleBuilder
						.cronScheduleNonvalidatedExpression(jobCron);
				if ( cronTimeZoneId != null ) {
					cronBuilder.inTimeZone(TimeZone.getTimeZone(cronTimeZoneId));
				}
				CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
						.forJob(jobKey).usingJobData(jobMap).withSchedule(cronBuilder).build();
				trigger = cronTrigger;
			} catch ( ParseException e ) {
				log.error("Bad cron expression [{}]: {}", jobCron, e.getMessage());
				return;
			}
		}

		Trigger t = scheduler.getTrigger(triggerKey);
		if ( t != null ) {
			// job already scheduled, check for same time to re-schedule
			if ( t instanceof CronTrigger ) {
				CronTrigger ct = (CronTrigger) t;
				if ( !ct.getCronExpression().equals(jobCron) ) {
					log.debug("Re-scheduling cron job {}.{} for {}",
							new Object[] { jobGroup, jobId, jobCron });
					scheduler.rescheduleJob(triggerKey, trigger);
				}
			} else {
				Date d = t.getStartTime();
				if ( d.getTime() != jobDate ) {
					log.debug("Re-scheduling job {}.{} for {}",
							new Object[] { jobGroup, jobId, new Date(jobDate) });
					scheduler.rescheduleJob(triggerKey, trigger);
				}
			}
		} else {
			log.debug("Scheduling job {}.{} for {}", new Object[] { jobGroup, jobId,
					(jobCron != null ? jobCron : new Date(jobDate).toString()) });
			scheduler.scheduleJob(trigger);
		}
	}

	public String getCronTimeZoneId() {
		return cronTimeZoneId;
	}

	public void setCronTimeZoneId(String cronTimeZoneId) {
		this.cronTimeZoneId = cronTimeZoneId;
	}

}
