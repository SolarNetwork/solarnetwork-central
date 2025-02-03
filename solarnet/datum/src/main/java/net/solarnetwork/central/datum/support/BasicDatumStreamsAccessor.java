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
import net.solarnetwork.util.ObjectUtils;

/**
 * Basic implementation of {@link DatumStreamsAccessor}.
 *
 * <p>
 * This class is <b>not</b> thread safe.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class BasicDatumStreamsAccessor implements DatumStreamsAccessor {

	private final PathMatcher pathMatcher;
	private final Collection<? extends Datum> datum;

	private Map<String, List<Datum>> timeSortedDatumBySource;

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
	 * Get all datum grouped by source ID, sorted by timestamp in reverse
	 * (newest to oldest).
	 *
	 * @return the sorted datum, never {@code null}
	 */
	private Map<String, List<Datum>> sortedDatumStreams() {
		if ( timeSortedDatumBySource == null ) {
			Map<String, List<Datum>> map = new HashMap<>(8);
			for ( Datum d : datum ) {
				map.computeIfAbsent(d.getSourceId(), k -> new ArrayList<>(8)).add(d);
			}
			for ( List<Datum> list : map.values() ) {
				list.sort((l, r) -> r.getTimestamp().compareTo(l.getTimestamp()));
			}
			timeSortedDatumBySource = map;
		}
		return timeSortedDatumBySource;
	}

	@Override
	public Collection<Datum> offsetMatching(String sourceIdPattern, int offset) {
		final var map = sortedDatumStreams();
		final var result = new ArrayList<Datum>(map.size());
		for ( Entry<String, List<Datum>> e : map.entrySet() ) {
			if ( sourceIdPattern == null || sourceIdPattern.isEmpty()
					|| pathMatcher.match(sourceIdPattern, e.getKey()) ) {
				List<Datum> list = e.getValue();
				if ( offset < list.size() ) {
					result.add(list.get(offset));
				}
			}
		}
		return result;
	}

	@Override
	public Datum offset(String sourceId, int offset) {
		final var map = sortedDatumStreams();
		final List<Datum> list = map.get(sourceId);
		if ( list == null ) {
			return null;
		}
		return (offset < list.size() ? list.get(offset) : null);
	}

	@Override
	public Collection<Datum> offsetMatching(String sourceIdPattern, Instant timestamp, int offset) {
		final var map = sortedDatumStreams();
		final var result = new ArrayList<Datum>(map.size());
		for ( Entry<String, List<Datum>> e : map.entrySet() ) {
			if ( sourceIdPattern == null || sourceIdPattern.isEmpty()
					|| pathMatcher.match(sourceIdPattern, e.getKey()) ) {
				List<Datum> list = e.getValue();
				Datum d = offset(list, timestamp, offset);
				if ( d != null ) {
					result.add(d);
				}
			}
		}
		return result;
	}

	@Override
	public Datum offset(String sourceId, Instant timestamp, int offset) {
		final var map = sortedDatumStreams();
		final List<Datum> list = map.get(sourceId);
		if ( list == null ) {
			return null;
		}
		return offset(list, timestamp, offset);
	}

	private Datum offset(List<Datum> list, Instant timestamp, int offset) {
		for ( int idx = 0, len = list.size(); idx < len; idx++ ) {
			Datum d = list.get(idx);
			if ( !d.getTimestamp().isAfter(timestamp) ) {
				if ( offset == 0 ) {
					return d;
				}
				idx += offset;
				return idx < list.size() ? list.get(idx) : null;
			}
		}
		return null;
	}

}
