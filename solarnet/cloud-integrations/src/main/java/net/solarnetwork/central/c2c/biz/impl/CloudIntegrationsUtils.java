/* ==================================================================
 * CloudIntegrationsUtils.java - 26/10/2024 6:31:28 am
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

package net.solarnetwork.central.c2c.biz.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;

/**
 * Helper methods for cloud integrations.
 *
 * @author matt
 * @version 1.2
 */
public final class CloudIntegrationsUtils {

	private CloudIntegrationsUtils() {
		// not available
	}

	/** Constant for 1e-3. */
	public static final BigDecimal MILLIS = new BigDecimal(BigInteger.ONE, 3);

	/** Constant for number of seconds per hour. */
	public static final BigDecimal SECS_PER_HOUR = new BigDecimal(3600);

	/**
	 * Truncate a date based on a temporal amount.
	 *
	 * <p>
	 * Only {@link Period} and {@link Duration} amount types are supported.
	 * </p>
	 *
	 * @param date
	 *        the date to truncate
	 * @param amount
	 *        the duration to truncate to
	 * @param zone
	 *        the time zone to use for {@link Period} based amounts
	 * @return the truncated date, or {@code date} if {@code amount} is not a
	 *         {@link Period} or {@link Duration}
	 * @since 1.1
	 */
	public static Instant truncateDate(Instant date, TemporalAmount amount, ZoneId zone) {
		return switch (amount) {
			case Period p -> truncateDate(date, p, zone);
			case Duration d -> truncateDate(date, d, zone);
			default -> date;
		};
	}

	/**
	 * Truncate a date based on a duration.
	 *
	 * @param date
	 *        the date to truncate
	 * @param period
	 *        the duration to truncate to
	 * @return the truncated date
	 */
	public static Instant truncateDate(Instant date, Duration period, ZoneId zone) {
		return Clock.tick(Clock.fixed(date, zone), period).instant();
	}

	/**
	 * Truncate a date based on a period.
	 *
	 * @param date
	 *        the date to truncate
	 * @param period
	 *        the period to truncate to; only week, month, and year are
	 *        supported
	 * @param zone
	 *        the zone
	 * @return the truncated date
	 */
	public static Instant truncateDate(Instant date, Period period, ZoneId zone) {
		TemporalAdjuster adj = period.getYears() > 0 ? TemporalAdjusters.firstDayOfYear()
				: period.getMonths() > 0 ? TemporalAdjusters.firstDayOfMonth()
						: TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY);
		return date.atZone(zone).with(adj).toInstant().truncatedTo(ChronoUnit.DAYS);
	}

	/**
	 * Get the previous starting tick boundary.
	 *
	 * @param tick
	 *        the tick amount; a valid time- or date-based unit is assumed; see
	 *        {@link java.time.temporal.TemporalUnit#isTimeBased()} and
	 *        {@link java.time.temporal.TemporalUnit#isDateBased()}
	 * @param tickStart
	 *        a starting tick boundary instant
	 * @param zone
	 *        the time zone, for ticks larger than 1 day
	 * @return the previous boundary start
	 */
	public static Instant prevTickStart(TemporalAmount tick, Instant tickStart, ZoneId zone) {
		if ( tick == null ) {
			return tickStart;
		}
		if ( tick.getUnits().getFirst().isTimeBased() ) {
			return tickStart.minus(tick);
		}
		ZonedDateTime zdt = tickStart.atZone(zone);
		return zdt.minus(tick).toInstant();
	}

	/**
	 * Get the next starting tick boundary.
	 *
	 * @param tick
	 *        the tick amount; a valid time- or date-based unit is assumed; see
	 *        {@link java.time.temporal.TemporalUnit#isTimeBased()} and
	 *        {@link java.time.temporal.TemporalUnit#isDateBased()}
	 * @param tickStart
	 *        a starting tick boundary instant
	 * @param zone
	 *        the time zone, for ticks larger than 1 day
	 * @return the next boundary start
	 */
	public static Instant nextTickStart(TemporalAmount tick, Instant tickStart, ZoneId zone) {
		if ( tick == null ) {
			return tickStart;
		}
		if ( tick.getUnits().getFirst().isTimeBased() ) {
			return tickStart.plus(tick);
		}
		ZonedDateTime zdt = tickStart.atZone(zone);
		return zdt.plus(tick).toInstant();
	}

}
