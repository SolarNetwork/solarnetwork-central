/* ==================================================================
 * ProviderObjectDatumStreamFilterResults.java - 23/10/2020 10:13:55 am
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
import java.util.UUID;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.Identity;

/**
 * Implementation of {@link ObjectDatumStreamFilterResults} that delegates to a
 * {@link ObjectDatumStreamMetadataProvider} for metadata information.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class ProviderObjectDatumStreamFilterResults<M extends Identity<K>, K>
		extends BasicFilterResults<M, K>
		implements ObjectDatumStreamFilterResults<M, K>, ObjectDatumStreamMetadataProvider {

	private final ObjectDatumStreamMetadataProvider metadataProvider;

	/**
	 * Constructor.
	 * 
	 * @param metadataProvider
	 *        the stream metadata provider
	 * @param results
	 *        the results iterable
	 * @param totalResults
	 *        the total available results, or {@literal null}
	 * @param startingOffset
	 *        the starting offset
	 * @param returnedResultCount
	 *        the count of objects in {@code results}
	 */
	public ProviderObjectDatumStreamFilterResults(ObjectDatumStreamMetadataProvider metadataProvider,
			Iterable<M> results, Long totalResults, int startingOffset, int returnedResultCount) {
		super(results, totalResults, startingOffset, returnedResultCount);
		this.metadataProvider = metadataProvider;
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
	 * @param metadataProvider
	 *        the stream metadata provider
	 * @param results
	 *        the results iterable
	 */
	public ProviderObjectDatumStreamFilterResults(ObjectDatumStreamMetadataProvider metadataProvider,
			Iterable<M> results) {
		super(results);
		this.metadataProvider = metadataProvider;
	}

	@Override
	public Collection<UUID> metadataStreamIds() {
		return metadataProvider.metadataStreamIds();
	}

	@Override
	public ObjectDatumStreamMetadata metadataForStreamId(UUID streamId) {
		return metadataProvider.metadataForStreamId(streamId);
	}

	@Override
	public ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId) {
		return metadataProvider.metadataForObjectSource(objectId, sourceId);
	}

}
