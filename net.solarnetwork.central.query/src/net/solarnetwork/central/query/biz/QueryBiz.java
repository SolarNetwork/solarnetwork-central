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
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.domain.WeatherConditions;
import org.joda.time.LocalDate;

/**
 * API for querying business logic.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public interface QueryBiz {

	/**
	 * Get a date interval of available data for a type of NodeDatum.
	 * 
	 * <p>
	 * This method can be used to find the earliest and latest dates data is
	 * available for a set of given {@link NodeDatum} types. When more than one
	 * NodeDatum are provided, the returned interval will be the union of all
	 * ranges, that is the earliest date of all specified NodeDatum values and
	 * the maximum date of all specified NodeDatum values. This could be useful
	 * for reporting UIs that want to display a view of the complete range of
	 * data available.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node to look for
	 * @param types
	 *        the set of NodeDatum types to look for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 */
	ReportableInterval getReportableInterval(Long nodeId, Class<? extends NodeDatum>[] types);

	/**
	 * Get a date interval of available data for a type of NodeDatum, across all
	 * nodes in the system.
	 * 
	 * <p>
	 * This method can be used to find the earliest and latest dates data is
	 * available for a set of given {@link NodeDatum} types. When more than one
	 * NodeDatum are provided, the returned interval will be the union of all
	 * ranges, that is the earliest date of all specified NodeDatum values and
	 * the maximum date of all specified NodeDatum values. This could be useful
	 * for reporting UIs that want to display a view of the complete range of
	 * data available.
	 * </p>
	 * 
	 * @param types
	 *        the set of NodeDatum types to look for
	 * @return ReadableInterval instance, or <em>null</em> if no data available
	 */
	ReportableInterval getNetworkReportableInterval(Class<? extends NodeDatum>[] types);

	/**
	 * Get the available source IDs for a given node, optionally limited to a
	 * date range.
	 * 
	 * @param nodeId
	 *        the node ID to search for
	 * @param type
	 *        the NodeDatum type to look for
	 * @param start
	 *        an optional start date (inclusive) to filter on
	 * @param end
	 *        an optional end date (inclusive) to filter on
	 * @return the distinct source IDs available (never <em>null</em>)
	 */
	Set<String> getAvailableSources(Long nodeId, Class<? extends NodeDatum> type, LocalDate start,
			LocalDate end);

	/**
	 * Query for a list of aggregated datum objects.
	 * 
	 * <p>
	 * The returned domain objects are not generally persisted objects, they
	 * represent aggregated results, most likely aggregated over time.
	 * </p>
	 * 
	 * @param datumClass
	 *        the type of NodeDatum to query for
	 * @param criteria
	 *        the query criteria
	 * @return the query results
	 */
	List<? extends NodeDatum> getAggregatedDatum(Class<? extends NodeDatum> datumClass,
			DatumQueryCommand criteria);

	/**
	 * Get the most recently available weather conditions for a particular node.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @return the conditions, or <em>null</em> if none available
	 */
	WeatherConditions getMostRecentWeatherConditions(Long nodeId);

	/**
	 * API for querying for a filtered set of results from all possible results.
	 * 
	 * @param datumClass
	 *        the type of NodeDatum to query for
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 */
	<F extends DatumFilter> FilterResults<? extends EntityMatch> findFilteredDatum(
			Class<? extends Datum> datumClass, F filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

}
