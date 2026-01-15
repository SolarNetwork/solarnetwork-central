/* ==================================================================
 * SolrenViewGranularity.java - 17/10/2024 11:47:09 am
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
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of SolrenView data granularity values.
 *
 * @author matt
 * @version 1.0
 */
public enum SolrenViewGranularity {

	/** One minute granularity. */
	OneMinute("1min", Duration.ofMinutes(1)),

	/** Five minute granularity. */
	FiveMinute("5min", Duration.ofMinutes(5)),

	/** Ten minute granularity. */
	TenMinute("10min", Duration.ofMinutes(10)),

	/** Fifteen minute granularity. */
	FifteenMinute("15min", Duration.ofMinutes(15)),

	/** Twenty minute granularity. */
	TwentyMinute("20min", Duration.ofMinutes(20)),

	/** Thirty minute granularity. */
	ThirtyMinute("30min", Duration.ofMinutes(30)),

	/** One hour granularity. */
	Hour("hourly", Duration.ofHours(1)),

	/** One day granularity. */
	Day("daily", Duration.ofDays(1)),

	/** One month granularity. */
	Month("monthly", Duration.ofDays(1)),

	/** One year granularity. */
	Year("yearly", Duration.ofDays(1)),

	;

	private final String key;
	private final Duration tickDuration;

	SolrenViewGranularity(String key, Duration tickDuration) {
		this.key = key;
		this.tickDuration = tickDuration;
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
	public Duration getTickDuration() {
		return tickDuration;
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
	public static SolrenViewGranularity fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return FiveMinute;
		}
		for ( SolrenViewGranularity e : SolrenViewGranularity.values() ) {
			if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown SolrenViewGranularity value [" + value + "]");
	}

}
