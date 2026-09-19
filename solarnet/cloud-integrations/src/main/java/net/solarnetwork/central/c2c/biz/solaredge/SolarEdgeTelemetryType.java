/* ==================================================================
 * SolarEdgeTelemetryType.java - 16 Aug 2026 9:51:20 am
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

/**
 * API for a SolarEdge telemetry type.
 *
 * @author matt
 * @version 1.0
 */
public interface SolarEdgeTelemetryType {

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	String name();

	/**
	 * Get a key value.
	 *
	 * @return the key
	 */
	String key();

	/**
	 * Get a description.
	 *
	 * @return the description
	 */
	String description();

	/**
	 * Get the window timestamp alignment.
	 *
	 * @return {@code true} if aggregate window timestamps represent the end of
	 *         the window, or {@code false} for the start
	 */
	default boolean endingWindowTimestamps() {
		return false;
	}

	/**
	 * Resolve a window timestamp.
	 * 
	 * @param resolution
	 *        the resolution
	 * @param zone
	 *        the time zone
	 * @param value
	 *        the timestamp string value to parse
	 * @return the timestamp, or {@code null} if {@code value} can not be parsed
	 */
	default @Nullable Instant resolveWindowTimestamp(SolarEdgeResolution resolution, ZoneId zone,
			@Nullable String value) {
		if ( value == null ) {
			return null;
		}
		try {
			Instant ts = Instant.parse(value);
			if ( endingWindowTimestamps() ) {
				ts = resolution.nextTickStart(ts, zone);
			}
			return ts;
		} catch ( DateTimeParseException e ) {
			return null;
		}
	}

}
