/* ==================================================================
 * BasicDatumStreamsAccessor.java - 15/11/2024 7:47:30 am
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

package net.solarnetwork.central.datum.support;

import static java.util.Collections.emptyMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.ObjectUtils;

/**
 * Basic implementation of {@link DatumStreamsAccessor}.
 *
 * <p>
 * This class is <b>not</b> thread safe.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public class BasicDatumStreamsAccessor implements DatumStreamsAccessor {

	private final PathMatcher pathMatcher;
	private final Collection<? extends Datum> datum;

	private Map<ObjectDatumKind, Map<Long, Map<String, List<Datum>>>> timeSortedDatumBySource;

	/**
	 * Constructor.
	 *
	 * @param pathMatcher
	 *        the path matcher
	 * @param datum
	 *        the datum collection
	 * @throws IllegalArgumentException
	 *         if {@code PathMatcher} is {@code null}
	 */
	public BasicDatumStreamsAccessor(PathMatcher pathMatcher, Collection<? extends Datum> datum) {
		super();
		this.pathMatcher = ObjectUtils.requireNonNullArgument(pathMatcher, "pathMatcher");
		this.datum = (datum != null ? datum : Collections.emptyList());
	}

	/**
	 * Get all datum grouped by kind, object ID, source ID, and then sorted by
	 * timestamp in reverse (newest to oldest).
	 *
	 * @return the sorted datum, never {@code null}
	 */
	private Map<ObjectDatumKind, Map<Long, Map<String, List<Datum>>>> sortedDatumStreams() {
		if ( timeSortedDatumBySource == null ) {
			Map<ObjectDatumKind, Map<Long, Map<String, List<Datum>>>> map = new HashMap<>(2);
			for ( Datum d : datum ) {
				map.computeIfAbsent(d.getKind(), k -> new HashMap<>(2))
						.computeIfAbsent(d.getObjectId(), k -> new HashMap<>(8))
						.computeIfAbsent(d.getSourceId(), k -> new ArrayList<>(8)).add(d);
			}
			for ( Map<Long, Map<String, List<Datum>>> nodeMap : map.values() ) {
				for ( Map<String, List<Datum>> sourceMap : nodeMap.values() ) {
					for ( List<Datum> list : sourceMap.values() ) {
						list.sort((l, r) -> r.getTimestamp().compareTo(l.getTimestamp()));
					}
				}
			}
			timeSortedDatumBySource = map;
		}
		return timeSortedDatumBySource;
	}

	private Map<String, List<Datum>> sortedDatumStreams(ObjectDatumKind kind, Long objectId) {
		if ( kind == null || objectId == null ) {
			return emptyMap();
		}
		final var maps = sortedDatumStreams();
		return maps.getOrDefault(kind, new HashMap<>(2)).getOrDefault(objectId, new HashMap<>());
	}

	@Override
	public Collection<Datum> offsetMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			int offset) {
		final var map = sortedDatumStreams(kind, objectId);
		final var result = new ArrayList<Datum>(map.size());
		for ( Entry<String, List<Datum>> e : map.entrySet() ) {
			if ( sourceIdPattern == null || sourceIdPattern.isEmpty()
					|| pathMatcher.match(sourceIdPattern, e.getKey()) ) {
				List<Datum> list = e.getValue();
				Datum d = offset(kind, objectId, e.getKey(), list, offset);
				if ( d != null ) {
					result.add(d);
				}
			}
		}
		return result;
	}

	@Override
	public Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, int offset) {
		final var map = sortedDatumStreams(kind, objectId);
		final List<Datum> list = map.computeIfAbsent(sourceId, k -> new ArrayList<>(2));
		return offset(kind, objectId, sourceId, list, offset);
	}

	@Override
	public Collection<Datum> offsetMatching(ObjectDatumKind kind, Long objectId, String sourceIdPattern,
			Instant timestamp, int offset) {
		final var map = sortedDatumStreams(kind, objectId);
		final var result = new ArrayList<Datum>(map.size());
		for ( Entry<String, List<Datum>> e : map.entrySet() ) {
			if ( sourceIdPattern == null || sourceIdPattern.isEmpty()
					|| pathMatcher.match(sourceIdPattern, e.getKey()) ) {
				List<Datum> list = e.getValue();
				Datum d = offset(kind, objectId, e.getKey(), list, timestamp, offset);
				if ( d != null ) {
					result.add(d);
				}
			}
		}
		return result;
	}

	@Override
	public Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp,
			int offset) {
		final var map = sortedDatumStreams(kind, objectId);
		final List<Datum> list = map.computeIfAbsent(sourceId, k -> new ArrayList<>(2));
		return offset(kind, objectId, sourceId, list, timestamp, offset);
	}

	/**
	 * Hook to handle an indexed offset "miss", to resolve a datum.
	 *
	 * @param kind
	 *        the datum stream kind
	 * @param objectId
	 *        the datum object ID
	 * @param sourceId
	 *        the datum source ID
	 * @param list
	 *        the list of available datum
	 * @param offset
	 *        the desired offset (will be higher than {@code list.size()})
	 * @return the resolved datum, or {@literal null}
	 * @since 2.0
	 */
	protected Datum offsetMiss(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			int offset) {
		return null;
	}

	/**
	 * Hook to handle an indexed offset "miss", to resolve a datum.
	 *
	 * @param kind
	 *        the datum stream kind
	 * @param objectId
	 *        the datum object ID
	 * @param sourceId
	 *        the datum source ID
	 * @param list
	 *        the list of available datum
	 * @param timestamp
	 *        the datum timestamp to offset from
	 * @param offset
	 *        the desired offset
	 * @param referenceIndex
	 *        the index within {@code list} for a datum found already for the
	 *        given {@code timestamp}, or {@code -1} if not found
	 * @return the resolved datum, or {@literal null}
	 * @since 2.0
	 */
	protected Datum offsetMiss(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			Instant timestamp, int offset, int referenceIndex) {
		return null;
	}

	private Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			int offset) {
		assert list != null;
		if ( offset < 0 ) {
			return null;
		}
		if ( offset < list.size() ) {
			return list.get(offset);
		}
		return offsetMiss(kind, objectId, sourceId, list, offset);
	}

	private Datum offset(ObjectDatumKind kind, Long objectId, String sourceId, List<Datum> list,
			Instant timestamp, int offset) {
		assert list != null;
		for ( int idx = 0, len = list.size(); idx < len; idx++ ) {
			Datum d = list.get(idx);
			if ( !d.getTimestamp().isAfter(timestamp) ) {
				if ( offset == 0 ) {
					return d;
				}
				idx += offset;
				if ( idx < list.size() ) {
					return list.get(idx);
				}
				return offsetMiss(kind, objectId, sourceId, list, timestamp, offset, idx - offset);
			}
		}
		return offsetMiss(kind, objectId, sourceId, list, timestamp, offset, -1);
	}

}
