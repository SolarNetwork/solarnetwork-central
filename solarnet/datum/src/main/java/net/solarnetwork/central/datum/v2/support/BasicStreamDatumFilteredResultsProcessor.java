/* ==================================================================
 * BasicStreamDatumFilteredResultsProcessor.java - 1/05/2022 8:44:12 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * A simple in-memory {@link StreamDatumFilteredResultsProcessor}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class BasicStreamDatumFilteredResultsProcessor implements StreamDatumFilteredResultsProcessor {

	private final List<StreamDatum> data = new ArrayList<>(8);

	private ObjectDatumStreamMetadataProvider metadataProvider;

	@Override
	public void start(Long totalResultCount, Integer startingOffset, Integer expectedResultCount,
			Map<String, ?> attributes) throws IOException {
		if ( attributes == null || !(attributes
				.get(METADATA_PROVIDER_ATTR) instanceof ObjectDatumStreamMetadataProvider) ) {
			throw new IllegalArgumentException("No metadata provider provided.");
		}
		this.metadataProvider = (ObjectDatumStreamMetadataProvider) attributes
				.get(METADATA_PROVIDER_ATTR);
	}

	@Override
	public void handleResultItem(StreamDatum resultItem) throws IOException {
		data.add(resultItem);
	}

	@Override
	public void close() throws IOException {
		// NO-OP
	}

	@Override
	public void flush() throws IOException {
		// NO-OP
	}

	/**
	 * Get the collected data.
	 * 
	 * @return the data, never {@literal null}
	 */
	public List<StreamDatum> getData() {
		return data;
	}

	/**
	 * Get the metadata provider.
	 * 
	 * @return the metadata provider
	 */
	public ObjectDatumStreamMetadataProvider getMetadataProvider() {
		return metadataProvider;
	}

}
