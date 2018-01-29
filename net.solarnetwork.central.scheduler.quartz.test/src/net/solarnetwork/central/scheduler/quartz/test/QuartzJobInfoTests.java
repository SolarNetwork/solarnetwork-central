/* ==================================================================
 * QuartzJobInfoTests.java - 30/01/2018 8:39:36 AM
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

package net.solarnetwork.central.scheduler.quartz.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Date;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.CronTriggerImpl;
import net.solarnetwork.central.scheduler.JobStatus;
import net.solarnetwork.central.scheduler.internal.QuartzJobInfo;
import net.solarnetwork.central.test.AbstractCentralTest;

/**
 * Test cases for the {@link QuartzJobInfo} class.
 * 
 * @author matt
 * @version 1.0
 */
public class QuartzJobInfoTests extends AbstractCentralTest {

	private static final String TEST_TRIGGER_NAME = "test.trigger";
	private static final String TEST_TRIGGER_GROUP = "test.group";

	private Scheduler scheduler;

	@Before
	public void setup() {
		scheduler = EasyMock.createMock(Scheduler.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(scheduler);
	}

	private DateTime ceilMinutes(final DateTime dateTime, final int minutes) {
		final DateTime hour = dateTime.hourOfDay().roundFloorCopy();
		final long millisSinceHour = new Duration(hour, dateTime).getMillis();
		final int roundedMinutes = ((int) Math.ceil(millisSinceHour / 60000.0 / minutes)) * minutes;
		return hour.plusMinutes(roundedMinutes);
	}

	@Test
	public void cronEveryFiveMinutes() throws Exception {
		// given
		TriggerKey tk = new TriggerKey(TEST_TRIGGER_NAME, TEST_TRIGGER_GROUP);
		expect(scheduler.getTriggerState(tk)).andReturn(TriggerState.NORMAL);

		DateTime nextFireTime = ceilMinutes(new DateTime(), 5);

		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(tk)
				.withSchedule(CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0/5 * * * ?"))
				.build();
		Date firstFireTime = ((CronTriggerImpl) trigger).computeFirstFireTime(null);

		replay(scheduler);

		// when
		QuartzJobInfo info = new QuartzJobInfo(trigger, scheduler);

		// then
		assertThat("Description", info.getExecutionScheduleDescription(), equalTo("every 5 minutes"));
		assertThat("ID", info.getId(), equalTo(TEST_TRIGGER_NAME));
		assertThat("Group ID", info.getGroupId(), equalTo(TEST_TRIGGER_GROUP));
		assertThat("Status", info.getJobStatus(), equalTo(JobStatus.Scheduled));
		assertThat("First run time", firstFireTime, equalTo(nextFireTime.toDate()));
		assertThat("Next run time", info.getNextExecutionTime().toDate(), equalTo(firstFireTime));
		assertThat("Previous run time", info.getPreviousExecutionTime(), nullValue());
	}

}
