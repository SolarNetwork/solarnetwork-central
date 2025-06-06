/* ==================================================================
 * EnphaseGranularity.java - 4/03/2025 5:41:31 am
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of Enphase data granularity ("BinSize") values.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("ImmutableEnumChecker")
public enum EnphaseGranularity {

	/** Fifteen minutes. */
	FifteenMinute("15mins", Duration.ofMinutes(15)),

	/** One day. */
	Day("day", Duration.ofDays(1)),

	/** One month. */
	Week("week", Period.ofDays(7)),

	;

	private final String key;
	private final TemporalAmount tickAmount;

	private EnphaseGranularity(String key, TemporalAmount tickAmount) {
		this.key = key;
		this.tickAmount = tickAmount;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@code null}
	 */
	public String getKey() {
		return key;
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
		if ( tickAmount == null ) {
			return ts;
		}
		if ( tickAmount instanceof Duration d ) {
			return CloudIntegrationsUtils.truncateDate(ts, d, zone);
		} else if ( tickAmount instanceof Period p ) {
			return CloudIntegrationsUtils.truncateDate(ts, p, zone);
		}
		return ts;
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
	 *         {@link #FifteenMinute} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static EnphaseGranularity fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return FifteenMinute;
		}
		for ( EnphaseGranularity e : EnphaseGranularity.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown EnphaseGranularity value [" + value + "]");
	}

	/**
	 * Get the best-fit granularity for querying a given date range.
	 *
	 * @param from
	 *        the starting date
	 * @param to
	 *        the ending date
	 * @return the granularity to use, never {@code null}
	 */
	@SuppressWarnings("JavaPeriodGetDays")
	public static EnphaseGranularity forQueryDateRange(Instant from, Instant to) {
		final long mins = Duration.between(from, to).get(ChronoUnit.SECONDS);
		for ( EnphaseGranularity g : EnphaseGranularity.values() ) {
			final long secs = (g.tickAmount instanceof Period p ? TimeUnit.DAYS.toMinutes(p.getDays())
					: g.tickAmount.get(ChronoUnit.SECONDS));
			if ( mins <= secs ) {
				return g;
			}
		}
		return EnphaseGranularity.Week;
	}

}
