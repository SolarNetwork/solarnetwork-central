/* ==================================================================
 * DatumStreamAccessor.java - 15/11/2024 6:55:46 am
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

package net.solarnetwork.central.datum.biz;

import java.time.Instant;
import java.util.Collection;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * API for accessing datum streams.
 *
 * <p>
 * This API is meant for specialized use cases where access to some subset of
 * datum is needed for processing, for example a set of recent datum used with
 * expression calculations.
 * </p>
 *
 * @author matt
 * @version 2.2
 */
public interface DatumStreamsAccessor {

	/**
	 * Get a datum at exactly a given timestamp matching a specific source ID.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the datum for
	 * @param sourceId
	 *        the source ID to find the datum for
	 * @param timestamp
	 *        the timestamp to find the datum for
	 * @return the matching datum, or {@code null} if not available
	 * @since 2.1
	 */
	Datum at(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp);

	/**
	 * Get all datum at exactly a given timestamp, optionally matching a source
	 * ID pattern.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param timestamp
	 *        the timestamp to find the datum for
	 * @return the matching datum, never {@code null}
	 * @since 2.1
	 */
	Collection<Datum> atMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			Instant timestamp);

	/**
	 * Get an earlier offset from the latest available datum per source ID.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, never {@code null}
	 */
	Collection<Datum> offsetMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			int offset);

	/**
	 * Get the latest available datum per source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offsetMatching(sourceIdFilter, 0)}.
	 * </p>
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @return the matching datum, never {@code null}
	 * @see #offsetMatching(ObjectDatumKind, Long, String, int)
	 */
	default Collection<Datum> latestMatching(ObjectDatumKind kind, Long objectId,
			String sourceIdPattern) {
		return offsetMatching(kind, objectId, sourceIdPattern, 0);
	}

	/**
	 * Get an offset from the latest available datum matching a specific source
	 * ID.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, or {@code null} if not available
	 */
	Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, int offset);

	/**
	 * Get the latest available datum matching a specific source IDs.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, 0)}.
	 * </p>
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceId
	 *        the source ID to find
	 * @return the matching datum, or {@code null} if not available
	 * @see #offset(ObjectDatumKind, Long, String, int)
	 */
	default Datum latest(ObjectDatumKind kind, Long objectId, String sourceId) {
		return offset(kind, objectId, sourceId, 0);
	}

	/**
	 * Get a set of datum offset from a given timestamp, optionally matching a
	 * source ID pattern.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @return the matching datum, never {@code null}
	 * @since 1.1
	 */
	Collection<Datum> offsetMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			Instant timestamp, int offset);

	/**
	 * Get the latest available datum offset from a given timestamp, optionally
	 * matching a source ID pattern.
	 *
	 * <p>
	 * This is equivalent to calling
	 * {@code offsetMatching(sourceIdFilter, timestamp, 0)}.
	 * </p>
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, never {@code null}
	 * @see #offsetMatching(ObjectDatumKind, Long, String, int)
	 * @since 1.1
	 */
	default Collection<Datum> latestMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			Instant timestamp) {
		return offsetMatching(kind, objectId, sourceIdPattern, timestamp, 0);
	}

	/**
	 * Get a datum offset from a given timestamp matching a specific source ID.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @param offset
	 *        the offset from the reference timestamp, {@code 0} being the
	 *        latest and {@code 1} the next later, and so on
	 * @return the matching datum, or {@code null} if not available
	 * @since 1.1
	 */
	Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp, int offset);

	/**
	 * Get the latest available datum up to a given timestamp, matching a
	 * specific source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, timestamp, 0)}.
	 * </p>
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param timestamp
	 *        the timestamp to reference the offset from
	 * @return the matching datum, or {@code null} if not available
	 * @since 1.1
	 */
	default Datum latest(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp) {
		return offset(kind, objectId, sourceId, timestamp, 0);
	}

	/**
	 * Find datum matching a source ID pattern over a time range.
	 *
	 * @param kind
	 *        the datum kind
	 * @param objectId
	 *        the object ID to find the offset datum for
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param from
	 *        the minimum datum timestamp (inclusive)
	 * @param to
	 *        the maximum datum timestamp (exclusive)
	 * @return the matching datum, never {@code null}
	 * @since 2.2
	 */
	Collection<Datum> rangeMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			Instant from, Instant to);

	/**
	 * Find datum streams matching a general query, source ID pattern, and
	 * optional tags.
	 *
	 * @param kind
	 *        the datum kind
	 * @param query
	 *        the general query, to match the stream name, location, etc.
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @param tags
	 *        optional tags to match
	 * @return the matching datum stream metadata, never {@code null}
	 * @since 2.2
	 */
	Collection<ObjectDatumStreamMetadata> findStreams(ObjectDatumKind kind, String query,
			String sourceIdPattern, String... tags);

}
