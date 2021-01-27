/* ==================================================================
 * BasicDatumStream.java - 23/10/2020 10:25:21 am
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

package net.solarnetwork.central.datum.v2.domain;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

/**
 * Basic implementation of {@link DatumStream}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicDatumStream implements DatumStream {

	private final UUID streamId;
	private final DatumStreamMetadata metadata;
	private final Iterable<Datum> results;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * This total results count will be set to {@literal null}, the starting
	 * offset to {@literal 0}, and the returned result count will be derived
	 * from the number of items in {@code results}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param metadata
	 *        the stream metadata
	 * @param results
	 *        the results iterable
	 */
	public BasicDatumStream(UUID streamId, DatumStreamMetadata metadata, Iterable<Datum> results) {
		this.streamId = streamId;
		this.metadata = metadata;
		this.results = results;
	}

	@Override
	public UUID getStreamId() {
		return streamId;
	}

	@Override
	public DatumStreamMetadata getMetadata() {
		return metadata;
	}

	@Override
	public Iterator<Datum> iterator() {
		return (results == null ? Collections.emptyIterator() : results.iterator());
	}

}
