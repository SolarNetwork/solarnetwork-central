/* ===================================================================
 * QueryBiz.java
 * 
 * Created Aug 5, 2009 11:39:52 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.query.biz;

import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.domain.ReportableInterval;
import org.joda.time.DateTime;

/**
 * API for querying business logic.
 * 
 * @author matt
 * @version 2.0
 */
public interface QueryBiz {

	/**
	 * Get a date interval of available data for a node, optionally limited to a
	 * source ID.
	 * 
	 * <p>
	 * This method can be used to find the earliest and latest dates data is
	 * available for a set of given {@link GeneralNodeDatum}. This could be
	 * useful for reporting UIs that want to display a view of the complete
	 * range of data available.
	 * </p>
	 * <p>
	 * If the {@code sourceId} parameter is <em>null</em> then the returned
	 * interval will be for the node as a whole, for any sources.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node to look for
	 * @param sourceId
	 *        an optional source ID to find the available interval for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 */
	ReportableInterval getReportableInterval(Long nodeId, String sourceId);

	/**
	 * Get the available source IDs for a given node, optionally limited to a
	 * date range.
	 * 
	 * @param nodeId
	 *        the node ID to search for
	 * @param start
	 *        an optional start date (inclusive) to filter on
	 * @param end
	 *        an optional end date (inclusive) to filter on
	 * @return the distinct source IDs available (never <em>null</em>)
	 */
	Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end);

	/**
	 * API for querying for a filtered set of GeneralNodeDatum results from all
	 * possible results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 * @since 1.4
	 */
	FilterResults<GeneralNodeDatumFilterMatch> findFilteredGeneralNodeDatum(
			GeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max);

	/**
	 * API for querying for a filtered set of aggregated GeneralNodeDatum
	 * results from all possible results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 * @since 1.4
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

	/**
	 * API for querying for a filtered set of
	 * {@link GeneralLocationDatumFilterMatch} results from all possible
	 * results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 * @since 1.5
	 */
	FilterResults<GeneralLocationDatumFilterMatch> findGeneralLocationDatum(
			GeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max);

	/**
	 * API for querying for a filtered set of aggregated
	 * {@link ReportingGeneralLocationDatumMatch} results from all possible
	 * results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 * @since 1.5
	 */
	FilterResults<ReportingGeneralLocationDatumMatch> findAggregateGeneralLocationDatum(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

	/**
	 * Get the available source IDs for a given location, optionally limited to
	 * a date range.
	 * 
	 * @param locationId
	 *        the location ID to search for
	 * @param start
	 *        an optional start date (inclusive) to filter on
	 * @param end
	 *        an optional end date (inclusive) to filter on
	 * @return the distinct source IDs available (never <em>null</em>)
	 * @since 1.5
	 */
	Set<String> getLocationAvailableSources(Long locationId, DateTime start, DateTime end);

	/**
	 * Get a date interval of available data for a location, optionally limited
	 * to a source ID.
	 * 
	 * <p>
	 * This method can be used to find the earliest and latest dates data is
	 * available for a set of given {@link GeneralLocationDatum}. This could be
	 * useful for reporting UIs that want to display a view of the complete
	 * range of data available.
	 * </p>
	 * <p>
	 * If the {@code sourceId} parameter is <em>null</em> then the returned
	 * interval will be for the node as a whole, for any sources.
	 * </p>
	 * 
	 * @param locationId
	 *        the ID of the location to look for
	 * @param sourceId
	 *        an optional source ID to find the available interval for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 * @since 1.5
	 */
	ReportableInterval getLocationReportableInterval(Long locationId, String sourceId);

	/**
	 * API for querying for a filtered set of locations from all possible
	 * results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * 
	 * @return the results, never <em>null</em>
	 * @since 1.4
	 */
	FilterResults<LocationMatch> findFilteredLocations(Location filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);
}
