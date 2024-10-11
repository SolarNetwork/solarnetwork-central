/* ==================================================================
 * SchedulerUtilsTests.java - 7/11/2021 2:32:31 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.scheduler.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import net.solarnetwork.central.scheduler.SchedulerUtils;

/**
 * Test cases for the {@link SchedulerUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SchedulerUtilsTests {

	@Test
	public void cronTriggerDescription() {
		// GIVEN
		CronTrigger t = new CronTrigger("0 */5 * * * *");

		// WHEN
		String desc = SchedulerUtils.extractExecutionScheduleDescription(t);

		// THEN
		then(desc).isEqualTo("every 5 minutes");
	}

	@Test
	public void periodicTriggerDescription() {
		// GIVEN
		PeriodicTrigger t = new PeriodicTrigger(Duration.ofMinutes(10L));
		t.setFixedRate(false);

		// WHEN
		String desc = SchedulerUtils.extractExecutionScheduleDescription(t);

		// THEN
		then(desc).isEqualTo("every PT10M (delay)");
	}

	@Test
	public void periodicTriggerDescription_fixedRate() {
		// GIVEN
		PeriodicTrigger t = new PeriodicTrigger(Duration.ofMinutes(10L));
		t.setFixedRate(true);

		// WHEN
		String desc = SchedulerUtils.extractExecutionScheduleDescription(t);

		// THEN
		then(desc).isEqualTo("every PT10M (fix)");
	}

	@Test
	public void trigger_cron() {
		// GIVEN
		final String schedule = "0 */5 * * * *";

		// WHEN
		Trigger t = SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);

		// THEN
		// @formatter:off
		then(t)
			.as("Cron trigger created")
			.isInstanceOf(CronTrigger.class)
			.asInstanceOf(type(CronTrigger.class))
			.returns(schedule, from(CronTrigger::getExpression))
			;
		// @formatter:on
	}

	@Test
	public void periodic_fixed() {
		// GIVEN
		final String schedule = "60";

		// WHEN
		Trigger t = SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);

		// THEN
		// @formatter:off
		then(t)
			.as("Periodic trigger created from simple number")
			.isInstanceOf(PeriodicTrigger.class)
			.asInstanceOf(type(PeriodicTrigger.class))
			.returns(Duration.ofSeconds(60), from(PeriodicTrigger::getPeriodDuration))
			.as("When period >= 60s then fixed rate is used")
			.returns(true, from(PeriodicTrigger::isFixedRate))
			;
		// @formatter:on
	}

	@Test
	public void periodic_delay() {
		// GIVEN
		final String schedule = "59";

		// WHEN
		Trigger t = SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);

		// THEN
		// @formatter:off
		then(t)
			.as("Periodic trigger created from simple number")
			.isInstanceOf(PeriodicTrigger.class)
			.asInstanceOf(type(PeriodicTrigger.class))
			.returns(Duration.ofSeconds(59), from(PeriodicTrigger::getPeriodDuration))
			.as("When period < 60s then delay is used")
			.returns(false, from(PeriodicTrigger::isFixedRate))
			;
		// @formatter:on
	}

}
