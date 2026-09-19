/* ==================================================================
 * SolarEdgeResolution.java - 15 Aug 2026 7:11:34 am
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

package net.solarnetwork.central.c2c.biz.solaredge;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils;

/**
 * Enumeration of SolarEdge data resolution values.
 *
 * @author matt
 * @version 1.0
 */
public enum SolarEdgeResolution {

	/** 15 minute resolution. */
	FifteenMinute("QUARTER_HOUR", Duration.ofMinutes(15), Duration.ofHours(12)),

	/** Hour resolution. */
	Hour("HOUR", Duration.ofHours(1), Duration.ofHours(24)),

	/** Hour resolution. */
	Day("DAY", Duration.ofDays(1), Period.ofMonths(1)),

	/** Hour resolution. */
	Week("WEEK", Period.ofWeeks(1), Period.ofYears(1)),

	Month("MONTH", Period.ofMonths(1), Period.ofYears(3)),

	Year("YEAR", Period.ofYears(1), null),

	Total("TOTAL", Period.ofYears(Integer.MAX_VALUE), null)

	;

	private final String key;

	@SuppressWarnings("ImmutableEnumChecker")
	private final TemporalAmount tickAmount;

	@SuppressWarnings("ImmutableEnumChecker")
	private final @Nullable TemporalAmount queryMax;

	SolarEdgeResolution(String key, TemporalAmount tickAmount, @Nullable TemporalAmount queryMax) {
		this.key = key;
		this.tickAmount = tickAmount;
		this.queryMax = queryMax;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@code null}
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get a clock tick duration appropriate for this granularity.
	 *
	 * @return the duration, never {@code null}
	 */
	public TemporalAmount getTickAmount() {
		return tickAmount;
	}

	/**
	 * Get the maximum query range for this resolution.
	 *
	 * @return the maximum range, or {@code null} if there is no limit
	 */
	public final @Nullable TemporalAmount getQueryMax() {
		return queryMax;
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
	 * @return the enum; if {@code value} is {@code null} or empty then
	 *         {@link #FifteenMinute} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static SolarEdgeResolution fromValue(@Nullable String value) {
		if ( value == null || value.isEmpty() ) {
			return FifteenMinute;
		}
		for ( SolarEdgeResolution e : SolarEdgeResolution.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown SolarEdgeResolution value [" + value + "]");
	}

}
