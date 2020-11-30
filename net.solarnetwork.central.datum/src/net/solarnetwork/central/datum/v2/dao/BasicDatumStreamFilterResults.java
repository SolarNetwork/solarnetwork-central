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
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Basic implementation of {@link DatumStreamFilterResults}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicDatumStreamFilterResults extends BasicFilterResults<Datum, DatumPK>
		implements DatumStreamFilterResults {

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
	public BasicDatumStreamFilterResults(Map<UUID, ObjectDatumStreamMetadata> streamMetadata,
			Iterable<Datum> results, Long totalResults, int startingOffset, int returnedResultCount) {
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
	 * @param results
	 *        the results iterable
	 */
	public BasicDatumStreamFilterResults(Map<UUID, ObjectDatumStreamMetadata> streamMetadata,
			Iterable<Datum> results) {
		super(results);
		this.streamMetadata = streamMetadata;
	}

	@Override
	public Collection<UUID> metadataStreamIds() {
		return (streamMetadata != null ? streamMetadata.keySet() : Collections.emptySet());
	}

	@Override
	public ObjectDatumStreamMetadata metadataForStream(UUID streamId) {
		return (streamMetadata != null ? streamMetadata.get(streamId) : null);
	}

}
