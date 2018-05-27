/* ==================================================================
 * DelegatingQueryBiz.java - Dec 18, 2012 4:35:14 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.support;

import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;

/**
 * Delegating implementation of {@link QueryBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 2.1
 */
public class DelegatingQueryBiz implements QueryBiz {

	private final QueryBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingQueryBiz(QueryBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public ReportableInterval getReportableInterval(Long nodeId, String sourceId) {
		return delegate.getReportableInterval(nodeId, sourceId);
	}

	@Override
	public Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end) {
		return delegate.getAvailableSources(nodeId, start, end);
	}

	@Override
	public Set<String> getAvailableSources(GeneralNodeDatumFilter filter) {
		return delegate.getAvailableSources(filter);
	}

	@Override
	public FilterResults<GeneralNodeDatumFilterMatch> findFilteredGeneralNodeDatum(
			GeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findFilteredGeneralNodeDatum(filter, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findFilteredAggregateGeneralNodeDatum(filter, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<LocationMatch> findFilteredLocations(Location filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return delegate.findFilteredLocations(filter, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<GeneralLocationDatumFilterMatch> findGeneralLocationDatum(
			GeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findGeneralLocationDatum(filter, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<ReportingGeneralLocationDatumMatch> findAggregateGeneralLocationDatum(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findAggregateGeneralLocationDatum(filter, sortDescriptors, offset, max);
	}

	@Override
	public Set<String> getLocationAvailableSources(Long locationId, DateTime start, DateTime end) {
		return delegate.getLocationAvailableSources(locationId, start, end);
	}

	@Override
	public ReportableInterval getLocationReportableInterval(Long locationId, String sourceId) {
		return delegate.getLocationReportableInterval(locationId, sourceId);
	}

}
