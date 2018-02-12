/* ==================================================================
 * QuartzJobInfo.java - 24/01/2018 3:26:44 PM
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

package net.solarnetwork.central.scheduler.internal;

import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.quartz.CalendarIntervalTrigger;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import net.solarnetwork.central.scheduler.BasicJobInfo;
import net.solarnetwork.central.scheduler.JobStatus;

/**
 * Quartz implementation of {@code JobInfo}.
 * 
 * <p>
 * Note that Quartz differentiates between <i>jobs</i> and <i>triggers</i>. For
 * SolarNet we report <i>triggers</i> as "jobs" as triggers represent the
 * schedules that jobs run at.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class QuartzJobInfo extends BasicJobInfo {

	private final Trigger trigger;
	private final Scheduler scheduler;

	/**
	 * Construct from a Quartz trigger.
	 * 
	 * @param trigger
	 *        the trigger
	 * @param scheduler
	 *        the scheduler
	 */
	public QuartzJobInfo(Trigger trigger, Scheduler scheduler) {
		super(trigger.getKey().getGroup(), trigger.getKey().getName(),
				extractExecutionScheduleDescription(trigger));
		this.trigger = trigger;
		this.scheduler = scheduler;
	}

	private static final String extractExecutionScheduleDescription(Trigger trigger) {
		if ( trigger instanceof CronTrigger ) {
			CronTrigger cronTrigger = (CronTrigger) trigger;
			try {
				Cron cron = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
						.parse(cronTrigger.getCronExpression());
				CronDescriptor desc = CronDescriptor.instance();
				return desc.describe(cron);
			} catch ( IllegalArgumentException e ) {
				return ("cron: " + cronTrigger.getCronExpression());
			}
		} else if ( trigger instanceof CalendarIntervalTrigger ) {
			CalendarIntervalTrigger calTrigger = (CalendarIntervalTrigger) trigger;
			return String.format("every %d %s%s from %tY-%<tm-%<td %<tH:%<tM %<tz ",
					calTrigger.getRepeatInterval(),
					calTrigger.getRepeatIntervalUnit().toString().toLowerCase(),
					calTrigger.getRepeatInterval() != 1 ? "s" : "", calTrigger.getStartTime());
		} else if ( trigger instanceof SimpleTrigger ) {
			SimpleTrigger simpTrigger = (SimpleTrigger) trigger;
			final int repeatCount = simpTrigger.getRepeatCount();
			if ( repeatCount == 0 ) {
				return String.format("at %tY-%<tm-%<td %<tH:%<tM %<tz", simpTrigger.getStartTime());
			}
			return String.format("every %d seconds from %tY-%<tm-%<td %<tH:%<tM %<tz (%s)",
					Math.round(simpTrigger.getRepeatInterval() / 1000.0), simpTrigger.getStartTime(),
					simpTrigger.getRepeatCount() == SimpleTrigger.REPEAT_INDEFINITELY ? "indefinitely"
							: repeatCount + " time" + (repeatCount != 1 ? "s" : ""));
		}
		Date fireTime = trigger.getNextFireTime();
		if ( fireTime != null ) {
			return String.format("next execution at %tY-%<tm-%<td %<tH:%<tM %<tz",
					trigger.getNextFireTime());
		}
		return "Unknown schedule: " + trigger.toString();
	}

	@Override
	public JobStatus getJobStatus() {
		try {
			TriggerState state = scheduler.getTriggerState(trigger.getKey());
			if ( state != null ) {
				switch (state) {
					case BLOCKED:
					case NORMAL:
						return JobStatus.Scheduled;

					case COMPLETE:
						return JobStatus.Complete;

					case ERROR:
						return JobStatus.Error;

					case NONE:
						break;

					case PAUSED:
						return JobStatus.Paused;

				}
			}
		} catch ( SchedulerException e ) {
			// ignore this
		}
		return JobStatus.Unknown;
	}

	@Override
	public boolean isExecuting() {
		try {
			List<JobExecutionContext> executing = scheduler.getCurrentlyExecutingJobs();
			for ( JobExecutionContext jec : executing ) {
				if ( jec.getTrigger().getKey().equals(trigger.getKey()) ) {
					return true;
				}
			}
		} catch ( SchedulerException e ) {
			// ignore this
		}
		return false;
	}

	@Override
	public DateTime getPreviousExecutionTime() {
		Date d = trigger.getPreviousFireTime();
		return (d != null ? new DateTime(d) : null);
	}

	@Override
	public DateTime getNextExecutionTime() {
		Date d = trigger.getNextFireTime();
		return (d != null ? new DateTime(d) : null);
	}
}
