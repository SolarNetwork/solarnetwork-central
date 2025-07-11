/* ==================================================================
 * BasicDatumStreamFilterResults.java - 23/10/2020 10:13:55 am
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Basic implementation of {@link ObjectDatumStreamFilterResults}.
 *
 * @author matt
 * @version 2.0
 * @since 2.8
 */
public class BasicObjectDatumStreamFilterResults<M extends Identity<K>, K extends Comparable<K>>
		extends BasicFilterResults<M, K> implements ObjectDatumStreamFilterResults<M, K> {

	private final Map<UUID, ObjectDatumStreamMetadata> streamMetadata;

	/**
	 * Constructor.
	 *
	 * @param streamMetadata
	 *        the stream metadata to associate with the results
	 * @param results
	 *        the results iterable
	 * @param totalResults
	 *        the total available results, or {@literal null}
	 * @param startingOffset
	 *        the starting offset
	 * @param returnedResultCount
	 *        the count of objects in {@code results}
	 */
	public BasicObjectDatumStreamFilterResults(Map<UUID, ObjectDatumStreamMetadata> streamMetadata,
			Iterable<M> results, Long totalResults, long startingOffset, int returnedResultCount) {
		super(results, totalResults, startingOffset, returnedResultCount);
		this.streamMetadata = streamMetadata;
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This total results count will be set to {@literal null}, the starting
	 * offset to {@literal 0}, and the returned result count will be derived
	 * from the number of items in {@code results}.
	 * </p>
	 *
	 * @param streamMetadata
	 *        the stream metadata to associate with the results
	 * @param results
	 *        the results iterable
	 */
	public BasicObjectDatumStreamFilterResults(Map<UUID, ObjectDatumStreamMetadata> streamMetadata,
			Iterable<M> results) {
		super(results);
		this.streamMetadata = streamMetadata;
	}

	@Override
	public Collection<UUID> metadataStreamIds() {
		return (streamMetadata != null ? streamMetadata.keySet() : Collections.emptySet());
	}

	@Override
	public ObjectDatumStreamMetadata metadataForStreamId(UUID streamId) {
		return (streamMetadata != null ? streamMetadata.get(streamId) : null);
	}

}
