/* ==================================================================
 * DateTimeUtils.java - 15/05/2026 11:19:33 am
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

package net.solarnetwork.central.support;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.jspecify.annotations.Nullable;
import org.springframework.format.Formatter;
import org.threeten.extra.Interval;
import net.solarnetwork.util.DateUtils;

/**
 * Common date and time utility helper methods.
 * 
 * @author matt
 * @version 1.0
 */
public final class DateTimeUtils {

	/**
	 * A standard {@link Instant} formatter formats as SolarNetwork standard
	 * timestamp and parses that or standard ISO 8601 instants.
	 */
	public static final Formatter<Instant> STD_INSTANT_FORMATTER = new InstantFormatter(
			DateUtils.ISO_DATE_TIME_ALT_UTC, DateTimeFormatter.ISO_INSTANT);

	private DateTimeUtils() {
		// not available
	}

	/**
	 * Parse an instance using the standard instance formatter.
	 * 
	 * @param text
	 *        the text to parse
	 * @return the parsed instant, or {@code null} if not able to be parsed
	 */
	public static @Nullable Instant parseInstant(@Nullable String text) {
		if ( text == null || text.isEmpty() ) {
			return null;
		}
		try {
			return STD_INSTANT_FORMATTER.parse(text, Locale.ROOT);
		} catch ( Exception e ) {
			return null;
		}
	}

	/**
	 * Resolve a string mapping to {@link Interval} instances from a service
	 * property value.
	 *
	 * <p>
	 * The interval syntax is a string like {@code "[INV1]/[INV2]"} where
	 * {@code [INV1]} and {@code [INV2]} are optional interval strings. A
	 * missing interval represents "all time". {@code INV1} represents an
	 * inclusive date, {@code INV2} represents an exclusive date. Each interval
	 * is formatted as an ISO 8601 timestamp, although a space may be used
	 * instead of a {@code T} date/time separator.
	 * </p>
	 *
	 * <p>
	 * Examples of intervals are:
	 * </p>
	 *
	 * <ul>
	 * <li>{@code "2020-01-01T05:00:00Z/2025-01-01T05:00:00Z"} - a fixed
	 * range</li>
	 * <li>{@code "/2025-01-01T05:00:00Z"} - a range with only an end date</li>
	 * <li>{@code "2020-01-01T05:00:00Z/"} - a range with only a start date</li>
	 * <li>{@code "/"} - an infinite range
	 * </ul>
	 *
	 * @param key
	 *        the service property key to extract
	 * @return the mapping, or {@code null}
	 */
	public static @Nullable Map<String, Interval> intervalMap(@Nullable Map<String, String> mapping) {
		if ( mapping == null || mapping.isEmpty() ) {
			return null;
		}
		final Map<String, Interval> result = new LinkedHashMap<>(mapping.size());
		for ( Entry<String, String> e : mapping.entrySet() ) {
			final String val = e.getValue();
			if ( e.getKey() == null || e.getKey().isEmpty() || val == null || val.isEmpty()
					|| val.length() < 2 ) {
				continue;
			}
			int slashIdx = val.indexOf('/');
			if ( slashIdx < 0 ) {
				continue;
			} else if ( slashIdx == 0 ) {
				Instant end = parseInstant(val.substring(1));
				if ( end != null ) {
					result.put(e.getKey(), Interval.endingAt(end));
				}
			} else if ( slashIdx == val.length() - 1 ) {
				Instant start = parseInstant(val.substring(0, slashIdx));
				if ( start != null ) {
					result.put(e.getKey(), Interval.startingAt(start));
				}
			} else if ( slashIdx < val.length() ) {
				Instant start = parseInstant(val.substring(0, slashIdx));
				Instant end = parseInstant(val.substring(slashIdx + 1));
				if ( start != null && end != null ) {
					result.put(e.getKey(), Interval.of(start, end));
				}
			}

		}
		return (result.isEmpty() ? null : result);
	}

}
