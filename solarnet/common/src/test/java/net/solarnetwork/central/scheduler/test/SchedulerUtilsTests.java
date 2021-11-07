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

import static org.assertj.core.api.BDDAssertions.then;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
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
		PeriodicTrigger t = new PeriodicTrigger(10L, TimeUnit.MINUTES);
		t.setFixedRate(false);

		// WHEN
		String desc = SchedulerUtils.extractExecutionScheduleDescription(t);

		// THEN
		then(desc).isEqualTo("every 10 minutes (delay)");
	}

	@Test
	public void periodicTriggerDescription_fixedRate() {
		// GIVEN
		PeriodicTrigger t = new PeriodicTrigger(10L, TimeUnit.MINUTES);
		t.setFixedRate(true);

		// WHEN
		String desc = SchedulerUtils.extractExecutionScheduleDescription(t);

		// THEN
		then(desc).isEqualTo("every 10 minutes (fix)");
	}

}
