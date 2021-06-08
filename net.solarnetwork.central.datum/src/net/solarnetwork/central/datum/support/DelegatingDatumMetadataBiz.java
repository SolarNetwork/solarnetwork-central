/* ==================================================================
 * DelegatingDatumMetadataBiz.java - Oct 3, 2014 4:42:47 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Implementation of {@link DatumMetadataBiz} that delgates to another
 * {@link DatumMetadataBiz}. Designed for use with AOP.
 * 
 * @author matt
 * @version 1.3
 */
public class DelegatingDatumMetadataBiz implements DatumMetadataBiz {

	private final DatumMetadataBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingDatumMetadataBiz(DatumMetadataBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		delegate.addGeneralNodeDatumMetadata(nodeId, sourceId, meta);
	}

	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findGeneralNodeDatumMetadata(criteria, sortDescriptors, offset, max);
	}

	@Override
	public void removeGeneralNodeDatumMetadata(Long nodeId, String sourceId) {
		delegate.removeGeneralNodeDatumMetadata(nodeId, sourceId);
	}

	@Override
	public void storeGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		delegate.storeGeneralNodeDatumMetadata(nodeId, sourceId, meta);
	}

	@Override
	public void addGeneralLocationDatumMetadata(Long locationId, String sourceId,
			GeneralDatumMetadata meta) {
		delegate.addGeneralLocationDatumMetadata(locationId, sourceId, meta);
	}

	@Override
	public void storeGeneralLocationDatumMetadata(Long locationId, String sourceId,
			GeneralDatumMetadata meta) {
		delegate.storeGeneralLocationDatumMetadata(locationId, sourceId, meta);
	}

	@Override
	public void removeGeneralLocationDatumMetadata(Long locationId, String sourceId) {
		delegate.removeGeneralLocationDatumMetadata(locationId, sourceId);
	}

	@Override
	public FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findGeneralLocationDatumMetadata(criteria, sortDescriptors, offset, max);
	}

	@Override
	public Set<NodeSourcePK> getGeneralNodeDatumMetadataFilteredSources(Long[] nodeIds,
			String metadataFilter) {
		return delegate.getGeneralNodeDatumMetadataFilteredSources(nodeIds, metadataFilter);
	}

	@Override
	public Set<LocationSourcePK> getGeneralLocationDatumMetadataFilteredSources(Long[] locationIds,
			String metadataFilter) {
		return delegate.getGeneralLocationDatumMetadataFilteredSources(locationIds, metadataFilter);
	}

	@Override
	public Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectStreamCriteria filter) {
		return delegate.findDatumStreamMetadata(filter);
	}

}
