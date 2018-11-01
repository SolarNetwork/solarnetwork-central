/* ==================================================================
 * GlobalMetricTrackerDataCollectorBiz.java - 1/11/2018 7:25:01 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.tracker.support;

import java.util.List;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.tracker.biz.GlobalMetricTrackerCollector;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * {@link DataCollectorBiz} that integrates a
 * {@link GlobalMetricTrackerCollector}.
 * 
 * <p>
 * This class delegates all methods to the {@link DataCollectorBiz} passed to
 * the constructor. The {@link #postGeneralNodeDatum(Iterable)} method will
 * additionally call
 * {@link GlobalMetricTrackerCollector#addGeneralNodeDatum(Iterable)}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class GlobalMetricTrackerDataCollectorBiz implements DataCollectorBiz {

	private final DataCollectorBiz delegate;
	private final GlobalMetricTrackerCollector tracker;

	public GlobalMetricTrackerDataCollectorBiz(DataCollectorBiz delegate,
			GlobalMetricTrackerCollector tracker) {
		super();
		this.delegate = delegate;
		this.tracker = tracker;
	}

	@SuppressWarnings("deprecation")
	@Override
	public <D extends Datum> D postDatum(D datum) {
		return delegate.postDatum(datum);
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<Datum> postDatum(Iterable<Datum> datums) {
		return delegate.postDatum(datums);
	}

	@Override
	public void postGeneralNodeDatum(Iterable<GeneralNodeDatum> datums) {
		delegate.postGeneralNodeDatum(datums);
		tracker.addGeneralNodeDatum(datums);
	}

	@Override
	public void postGeneralLocationDatum(Iterable<GeneralLocationDatum> datums) {
		delegate.postGeneralLocationDatum(datums);
	}

	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		delegate.addGeneralNodeDatumMetadata(nodeId, sourceId, meta);
	}

	@Override
	public void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta) {
		delegate.addSolarNodeMetadata(nodeId, meta);
	}

	@Override
	public FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(
			SolarNodeMetadataFilter criteria, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findSolarNodeMetadata(criteria, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findGeneralNodeDatumMetadata(criteria, sortDescriptors, offset, max);
	}

	@Override
	public FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter metadataCriteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		return delegate.findGeneralLocationDatumMetadata(metadataCriteria, sortDescriptors, offset, max);
	}

	@Override
	public List<SourceLocationMatch> findPriceLocations(SourceLocation criteria) {
		return delegate.findPriceLocations(criteria);
	}

	@Override
	public FilterResults<SourceLocationMatch> findPriceLocations(SourceLocation criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return delegate.findPriceLocations(criteria, sortDescriptors, offset, max);
	}

	@Override
	public List<SourceLocationMatch> findWeatherLocations(SourceLocation criteria) {
		return delegate.findWeatherLocations(criteria);
	}

	@Override
	public FilterResults<SourceLocationMatch> findWeatherLocations(SourceLocation criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return delegate.findWeatherLocations(criteria, sortDescriptors, offset, max);
	}

	@Override
	public List<LocationMatch> findLocations(Location criteria) {
		return delegate.findLocations(criteria);
	}

}
