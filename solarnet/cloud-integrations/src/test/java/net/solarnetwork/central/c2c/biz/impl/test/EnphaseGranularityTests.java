/* ==================================================================
 * EnphaseGranularityTests.java - 5/03/2025 1:22:27 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.biz.impl.EnphaseGranularity;

/**
 * Test cases for the {@link EnphaseGranularity} class.
 *
 * @author matt
 * @version 1.0
 */
public class EnphaseGranularityTests {

	@Test
	public void forQueryDateRange_upTo15min() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
		for ( int i = 0; i <= 15; i += 5 ) {
			Instant end = start.plus(i, ChronoUnit.MINUTES);

			// WHEN
			EnphaseGranularity result = EnphaseGranularity.forQueryDateRange(start, end);

			// THEN
			then(result).as("15mins returned for diff %d <= 15 min", i)
					.isEqualTo(EnphaseGranularity.FifteenMinute);
		}
	}

	@Test
	public void forQueryDateRange_moreThan15minUpToDay() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
		for ( int i = 15; i <= 1440; i += 15 ) {
			if ( i == 15 ) {
				continue;
			}
			Instant end = start.plus(i, ChronoUnit.MINUTES);

			// WHEN
			EnphaseGranularity result = EnphaseGranularity.forQueryDateRange(start, end);

			// THEN
			then(result).as("Day returned for diff %d <= 1440 min", i).isEqualTo(EnphaseGranularity.Day);
		}
	}

	@Test
	public void forQueryDateRange_moreThanDay() {
		// GIVEN
		Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
		int tenDayMinutes = 10 * 24 * 60;
		for ( int i = 1440; i <= tenDayMinutes; i += 60 ) {
			if ( i == 1440 ) {
				continue;
			}
			Instant end = start.plus(i, ChronoUnit.MINUTES);

			// WHEN
			EnphaseGranularity result = EnphaseGranularity.forQueryDateRange(start, end);

			// THEN
			then(result).as("Week returned for diff %d > 1440 min", i)
					.isEqualTo(EnphaseGranularity.Week);
		}
	}

}
