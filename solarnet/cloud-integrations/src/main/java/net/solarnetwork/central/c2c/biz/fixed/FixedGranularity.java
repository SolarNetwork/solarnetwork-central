/* ==================================================================
 * FixedGranularity.java - 13 Sept 2026 6:27:46 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.fixed;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Locale;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils;
import net.solarnetwork.util.DateUtils;

/**
 * The supported fixed granularity levels.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum FixedGranularity {

	/** Five minutes. */
	Minute(Duration.ofMinutes(1)),

	/** Five minutes. */
	FiveMinute(Duration.ofMinutes(5)),

	/** Ten minutes. */
	TenMinute(Duration.ofMinutes(10)),

	/** Fifteen minutes. */
	FifteenMinute(Duration.ofMinutes(15)),

	/** Thirty minutes. */
	TwentyMinute(Duration.ofMinutes(20)),

	/** Thirty minutes. */
	ThirtyMinute(Duration.ofMinutes(30)),

	/** One hour. */
	Hour(Duration.ofHours(1)),

	/** One day. */
	Day(Period.ofDays(1)),

	/** One month. */
	Month(Period.ofMonths(1)),

	/** One year. */
	Year(Period.ofYears(1)),

	;

	private final TemporalAmount tickAmount;

	FixedGranularity(TemporalAmount tickAmount) {
		this.tickAmount = tickAmount;
	}

	/**
	 * Get a clock tick duration appropriate for this granularity.
	 *
	 * @return the duration, or {@code null}
	 */
	public TemporalAmount getTickAmount() {
		return tickAmount;
	}

	/**
	 * Get the start of a tick boundary that includes a given instant.
	 *
	 * @param ts
	 *        the instant to get the tick boundary start for
	 * @param zone
	 *        the time zone, for tick amounts greater than a day
	 * @return the start instant
	 */
	public Instant tickStart(Instant ts, ZoneId zone) {
		if ( this == FixedGranularity.Day ) {
			return ts.atZone(zone).truncatedTo(ChronoUnit.DAYS).toInstant();
		}
		return CloudIntegrationsUtils.truncateDate(ts, tickAmount, zone);
	}

	/**
	 * Get the previous starting tick boundary.
	 *
	 * @param tickStart
	 *        the starting tick boundary
	 * @param zone
	 *        the time zone, for tick amounts greater than a day
	 * @return the starting tick boundary immediately before {@code tickStart}
	 */
	public Instant prevTickStart(Instant tickStart, ZoneId zone) {
		return CloudIntegrationsUtils.prevTickStart(tickAmount, tickStart, zone);
	}

	/**
	 * Get the next starting tick boundary.
	 *
	 * @param tickStart
	 *        the starting tick boundary
	 * @param zone
	 *        the time zone, for tick amounts greater than a day
	 * @return the starting tick boundary immediately after {@code tickStart}
	 */
	public Instant nextTickStart(Instant tickStart, ZoneId zone) {
		return CloudIntegrationsUtils.nextTickStart(tickAmount, tickStart, zone);
	}

	/**
	 * Get an enum instance for a name or ISO duration/period value.
	 *
	 * @param value
	 *        the enumeration name or ISO duration or period, case-insensitve
	 * @return the enum; if {@code value} is {@code null} or empty then
	 *         {@link #FifteenMinute} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static FixedGranularity fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return FifteenMinute;
		}
		TemporalAmount dur = null;
		try {
			dur = DateUtils.duration(value.toUpperCase(Locale.ROOT));
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		for ( FixedGranularity e : FixedGranularity.values() ) {
			if ( dur != null && dur.equals(e.tickAmount) ) {
				return e;
			}
			if ( value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown FixedGranularity value [" + value + "]");
	}

}
