/* ==================================================================
 * ScheduleTypeTests.java - 19/04/2018 7:48:32 AM
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

package net.solarnetwork.central.datum.export.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * Test cases for the {@link ScheduleType} class.
 * 
 * @author matt
 * @version 2.0
 */
public class ScheduleTypeTests {

	private static final ZonedDateTime TEST_DATE = ZonedDateTime.of(2018, 4, 19, 7, 43, 22,
			(int) TimeUnit.MILLISECONDS.toNanos(501), ZoneId.systemDefault());

	@Test
	public void hourlyExportDate() {
		assertThat(ScheduleType.Hourly.exportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 19, 7, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void dailyExportDate() {
		assertThat(ScheduleType.Daily.exportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 19, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void weeklyExportDate() {
		assertThat(ScheduleType.Weekly.exportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 16, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void monthlyExportDate() {
		assertThat(ScheduleType.Monthly.exportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void hourlyNextExportDate() {
		assertThat(ScheduleType.Hourly.nextExportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 19, 8, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void dailyNextExportDate() {
		assertThat(ScheduleType.Daily.nextExportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 20, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void weeklyNextExportDate() {
		assertThat(ScheduleType.Weekly.nextExportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 4, 23, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

	@Test
	public void monthlyNextExportDate() {
		assertThat(ScheduleType.Monthly.nextExportDate(TEST_DATE),
				equalTo(ZonedDateTime.of(2018, 5, 1, 0, 0, 0, 0, ZoneId.systemDefault())));
	}

}
