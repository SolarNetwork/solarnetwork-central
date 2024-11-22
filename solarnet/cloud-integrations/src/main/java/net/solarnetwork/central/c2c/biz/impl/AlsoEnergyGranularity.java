/* ==================================================================
 * AlsoEnergyGranularity.java - 22/11/2024 2:19:11 pm
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

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of AlsoEnergy data granularity ("BinSize") values.
 *
 * @author matt
 * @version 1.0
 */
public enum AlsoEnergyGranularity {

	/** Raw data. */
	Raw("Raw", Duration.ofMinutes(1)),

	/** Five minutes. */
	FiveMinute("5Min", Duration.ofMinutes(5)),

	/** Fifteen minutes. */
	FifteenMinute("15Min", Duration.ofMinutes(15)),

	/** One hour. */
	Hour("1Hour", Duration.ofHours(1)),

	/** One day. */
	Day("Day", Duration.ofDays(1)),

	/** One month. */
	Month("Month", Period.ofMonths(1)),

	/** One year. */
	Year("Year", Period.ofYears(1)),

	;

	private final String key;
	private final String queryKey;
	private final TemporalAmount tickAmount;

	private AlsoEnergyGranularity(String key, TemporalAmount tickAmount) {
		this.key = key;
		this.queryKey = "Bin" + key;
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
	 * Get the query key.
	 *
	 * @return the query key, never {@code null}
	 */
	public String getQueryKey() {
		return queryKey;
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
			return CloudIntegrationsUtils.truncateDate(ts, d);
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
	 *         {@link #Latest} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static AlsoEnergyGranularity fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return Raw;
		}
		for ( AlsoEnergyGranularity e : AlsoEnergyGranularity.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown AlsoEnergyGranularity value [" + value + "]");
	}

}
