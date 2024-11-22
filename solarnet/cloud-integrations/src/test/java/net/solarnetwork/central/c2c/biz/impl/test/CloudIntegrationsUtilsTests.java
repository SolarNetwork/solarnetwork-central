/* ==================================================================
 * CloudIntegrationsUtilsTests.java - 22/11/2024 2:51:29 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils;

/**
 * Test cases for the {@link CloudIntegrationsUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class CloudIntegrationsUtilsTests {

	@Test
	public void truncateDate_5min() {
		// GIVEN
		Instant ts = Instant.now().truncatedTo(SECONDS);

		// WHEN
		Instant result = CloudIntegrationsUtils.truncateDate(ts, Duration.ofMinutes(5));

		// THEN
		// @formatter:off
		then(result)
			.as("Date truncated to 5min boundary start")
			.isEqualTo(ofEpochSecond(ts.getEpochSecond() - (ts.getEpochSecond() % (5 * 60))).truncatedTo(MINUTES))
			;
		// @formatter:on
	}

	@Test
	public void truncateDate_hour() {
		// GIVEN
		Instant ts = Instant.now();

		// WHEN
		Instant result = CloudIntegrationsUtils.truncateDate(ts, Duration.ofHours(1));

		// THEN
		// @formatter:off
		then(result)
			.as("Date truncated to hour boundary start")
			.isEqualTo(ts.truncatedTo(HOURS))
			;
		// @formatter:on
	}

	@Test
	public void truncateDate_week() {
		// GIVEN
		Instant ts = Instant.now();

		// WHEN
		Instant result = CloudIntegrationsUtils.truncateDate(ts, Period.ofWeeks(1), ZoneOffset.UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date truncated to Monday week boundary start")
			.isEqualTo(ts.atZone(ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
					.toInstant().truncatedTo(ChronoUnit.DAYS))
			;
		// @formatter:on
	}

	@Test
	public void truncateDate_month() {
		// GIVEN
		Instant ts = Instant.now();

		// WHEN
		Instant result = CloudIntegrationsUtils.truncateDate(ts, Period.ofMonths(1), ZoneOffset.UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date truncated to month boundary start")
			.isEqualTo(ts.atZone(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
					.toInstant().truncatedTo(ChronoUnit.DAYS))
			;
		// @formatter:on
	}

	@Test
	public void truncateDate_year() {
		// GIVEN
		Instant ts = Instant.now();

		// WHEN
		Instant result = CloudIntegrationsUtils.truncateDate(ts, Period.ofYears(1), ZoneOffset.UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date truncated to year boundary start")
			.isEqualTo(ts.atZone(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfYear())
					.toInstant().truncatedTo(ChronoUnit.DAYS))
			;
		// @formatter:on
	}

	@Test
	public void nextTickStart_5min() {
		// GIVEN
		Instant ts = CloudIntegrationsUtils.truncateDate(Instant.now(), Duration.ofMinutes(5));

		// WHEN
		Instant result = CloudIntegrationsUtils.nextTickStart(Duration.ofMinutes(5), ts, UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date shifted to next boundary start")
			.isEqualTo(ts.plus(Duration.ofMinutes(5)))
			;
		// @formatter:on
	}

	@Test
	public void nextTickStart_hour() {
		// GIVEN
		Instant ts = CloudIntegrationsUtils.truncateDate(Instant.now(), Duration.ofHours(1));

		// WHEN
		Instant result = CloudIntegrationsUtils.nextTickStart(Duration.ofHours(1), ts, UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date shifted to next boundary start")
			.isEqualTo(ts.plus(Duration.ofHours(1)))
			;
		// @formatter:on
	}

	@Test
	public void nextTickStart_week() {
		// GIVEN
		Period p = Period.ofWeeks(1);
		Instant ts = CloudIntegrationsUtils.truncateDate(Instant.now(), p, UTC);

		// WHEN
		Instant result = CloudIntegrationsUtils.nextTickStart(p, ts, UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date shifted to next boundary start")
			.isEqualTo(ts.atZone(UTC).plusWeeks(1).toInstant())
			;
		// @formatter:on
	}

	@Test
	public void nextTickStart_month() {
		// GIVEN
		Period p = Period.ofMonths(1);
		Instant ts = CloudIntegrationsUtils.truncateDate(Instant.now(), p, UTC);

		// WHEN
		Instant result = CloudIntegrationsUtils.nextTickStart(p, ts, UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date shifted to next boundary start")
			.isEqualTo(ts.atZone(UTC).plusMonths(1).toInstant())
			;
		// @formatter:on
	}

	@Test
	public void nextTickStart_year() {
		// GIVEN
		Period p = Period.ofYears(1);
		Instant ts = CloudIntegrationsUtils.truncateDate(Instant.now(), p, UTC);

		// WHEN
		Instant result = CloudIntegrationsUtils.nextTickStart(p, ts, UTC);

		// THEN
		// @formatter:off
		then(result)
			.as("Date shifted to next boundary start")
			.isEqualTo(ts.atZone(UTC).plusYears(1).toInstant())
			;
		// @formatter:on
	}

}
