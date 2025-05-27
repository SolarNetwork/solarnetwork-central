/* ==================================================================
 * SmaResolution.java - 31/03/2025 6:28:53 am
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

package net.solarnetwork.central.c2c.biz.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of time resolution values.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum SmaResolution {

	/** One minute resolution. */
	OneMinute("OneMinute", Duration.ofMinutes(1)),

	/** Five minute resolution. */
	FiveMinute("FiveMinutes", Duration.ofMinutes(5)),

	/** Fifteen minute resolution. */
	FifteenMinute("FifteenMinutes", Duration.ofMinutes(15)),

	/** One hour resolution. */
	Hour("OneHour", Duration.ofHours(1)),

	/** One day resolution. */
	Day("OneDay", Duration.ofDays(1)),

	/** One month resolution. */
	Month("OneMonth", Period.ofMonths(1)),

	/** One year resolution. */
	Year("OneYear", Period.ofYears(1)),

	;

	private final String key;
	private final TemporalAmount tickAmount;

	private SmaResolution(String key, TemporalAmount tickAmount) {
		this.key = key;
		this.tickAmount = tickAmount;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@literal null}
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get a clock tick duration appropriate for this granularity.
	 *
	 * @return the duration, never {@literal null}
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
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum; if {@code value} is {@literal null} or empty then
	 *         {@link #FiveMinute} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static SmaResolution fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return FiveMinute;
		}
		for ( SmaResolution e : SmaResolution.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown SmaResolution value [" + value + "]");
	}

}
