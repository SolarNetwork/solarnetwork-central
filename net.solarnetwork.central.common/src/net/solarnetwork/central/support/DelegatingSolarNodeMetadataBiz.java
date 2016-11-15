/* ==================================================================
 * DelegatingSolarNodeMetadataBiz.java - 11/11/2016 1:23:08 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.util.List;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Implementation of {@link SolarNodeMetadataBiz} that delegates to another
 * {@link SolarNodeMetadataBiz}. Designed for use with AOP.
 * 
 * @author matt
 * @version 1.0
 * @since 1.32
 */
public class DelegatingSolarNodeMetadataBiz implements SolarNodeMetadataBiz {

	private final SolarNodeMetadataBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingSolarNodeMetadataBiz(SolarNodeMetadataBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		delegate.addSolarNodeMetadata(nodeId, meta);
	}

	@Override
	public void storeSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		delegate.storeSolarNodeMetadata(nodeId, meta);
	}

	@Override
	public void removeSolarNodeMetadata(Long nodeId) {
		delegate.removeSolarNodeMetadata(nodeId);
	}

	@Override
	public FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(
			SolarNodeMetadataFilter criteria, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findSolarNodeMetadata(criteria, sortDescriptors, offset, max);
	}

}
