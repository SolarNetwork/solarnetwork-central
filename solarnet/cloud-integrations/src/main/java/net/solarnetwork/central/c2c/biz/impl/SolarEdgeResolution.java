/* ==================================================================
 * SolarEdgeResolution.java - 23/10/2024 9:45:52 am
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

import static java.time.ZoneOffset.UTC;
import java.time.Duration;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of SolarEdge data resolution values.
 *
 * @author matt
 * @version 1.1
 */
public enum SolarEdgeResolution {

	/** 15 minute resolution. */
	FifteenMinute("QUARTER_OF_AN_HOUR", Duration.ofMinutes(15)),

	/** Hour resolution. */
	Hour("HOUR", Duration.ofHours(1)),

	;

	private final String key;
	private final Duration tickDuration;

	private SolarEdgeResolution(String key) {
		this(key, null);
	}

	private SolarEdgeResolution(String key, Duration tickDuration) {
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
	 * Truncate a date based on the tick duration of this resolution.
	 *
	 * @param date
	 *        the date to truncate
	 * @return the truncated date
	 */
	public Instant truncateDate(Instant date) {
		return CloudIntegrationsUtils.truncateDate(date, tickDuration, UTC);
	}

	/**
	 * Get a date exactly the resolution's {@code tickDuration} after a given
	 * date.
	 *
	 * @param date
	 *        the starting date
	 * @return the next later date
	 */
	public Instant nextDate(Instant date) {
		return date.plus(tickDuration);
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
	public static SolarEdgeResolution fromValue(String value) {
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
