/* ===================================================================
 * QueryBiz.java
 *
 * Created Aug 5, 2009 11:39:52 AM
 *
 * Copyright (c) 2009 SolarNetwork.net Dev Team.
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

import java.io.IOException;
import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for querying business logic.
 *
 * @author matt
 * @version 4.2
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
	 * If the {@code sourceId} parameter is {@literal null} then the returned
	 * interval will be for the node as a whole, for any sources.
	 * </p>
	 *
	 * @param nodeId
	 *        the ID of the node to look for
	 * @param sourceId
	 *        an optional source ID to find the available interval for
	 * @return ReadableInterval instance, or {@literal null} if no data
	 *         available
	 */
	ReportableInterval getReportableInterval(Long nodeId, String sourceId);

	/**
	 * Get the available source IDs for a given filter.
	 *
	 * <p>
	 * The filter is expected to provide a node ID. Multiple node IDs may be
	 * provided. Start and end dates may be provided to limit the query to a
	 * specific date range.
	 * </p>
	 *
	 * <p>
	 * <b>Note</b> that the precision of dates may be rounded by implementations
	 * when executing the query, for performance reasons.
	 * </p>
	 *
	 * @param filter
	 *        the query filter
	 * @return the distinct source IDs available (never {@literal null})
	 * @since 2.1
	 * @see #findAvailableSources(GeneralNodeDatumFilter)
	 */
	Set<String> getAvailableSources(GeneralNodeDatumFilter filter);

	/**
	 * Get the available source IDs for a given filter.
	 *
	 * <p>
	 * The filter is expected to provide a node ID. Multiple node IDs may be
	 * provided. Start and end dates may be provided to limit the query to a
	 * specific date range.
	 * </p>
	 *
	 * <p>
	 * <b>Note</b> that the precision of dates may be rounded by implementations
	 * when executing the query, for performance reasons.
	 * </p>
	 *
	 * @param filter
	 *        the query filter
	 * @return the distinct node and source IDs available (never
	 *         {@literal null})
	 * @since 2.5
	 */
	Set<NodeSourcePK> findAvailableSources(GeneralNodeDatumFilter filter);

	/**
	 * Find all available nodes for a given actor.
	 *
	 * @param actor
	 *        the actor to get nodes for
	 * @return the results, never {@literal null}
	 * @since 2.2
	 */
	Set<Long> findAvailableNodes(SecurityActor actor);

	/**
	 * Find all available node sources for a given actor.
	 *
	 * @param actor
	 *        the actor to get node sources for
	 * @param filter
	 *        an optional filter, from which a start and/or end dates can be
	 *        provided
	 * @return the results, never {@literal null}
	 * @since 2.3
	 */
	Set<NodeSourcePK> findAvailableSources(SecurityActor actor, DatumFilter filter);

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
	 * @return the results, never {@literal null}
	 * @since 1.4
	 */
	FilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> findFilteredGeneralNodeDatum(
			GeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Long offset,
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
	 * @return the results, never {@literal null}
	 * @since 1.4
	 */
	FilterResults<ReportingGeneralNodeDatumMatch, GeneralNodeDatumPK> findFilteredAggregateGeneralNodeDatum(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Long offset,
			Integer max);

	/**
	 * API for querying for a filtered set of stream datum, streaming the
	 * results.
	 *
	 * @param filter
	 *        the query filter
	 * @param processor
	 *        the processor for the results
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 4.1
	 */
	void findFilteredStreamDatum(StreamDatumFilter filter, StreamDatumFilteredResultsProcessor processor,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max) throws IOException;

	/**
	 * API for querying for a filtered set of reading stream datum, streaming
	 * the results.
	 *
	 * @param filter
	 *        the query filter
	 * @param readingType
	 *        the type of reading to perform
	 * @param tolerance
	 *        a time period of tolerance to use when querying around the
	 *        start/end dates, or {@literal null} for a default tolerance based
	 *        on {@code readingType}
	 * @param processor
	 *        the processor for the results
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 4.1
	 */
	void findFilteredStreamReadings(StreamDatumFilter filter, DatumReadingType readingType,
			Period tolerance, StreamDatumFilteredResultsProcessor processor,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max) throws IOException;

	/**
	 * API for querying for a filtered set of "readings".
	 *
	 * <p>
	 * The {@code filter} must provide the required date(s) to use for the
	 * reading type. If the reading type only requires one date, then the
	 * {@link GeneralNodeDatumFilter#getLocalStartDate()} or
	 * {@link GeneralNodeDatumFilter#getStartDate()} value should be provided.
	 * </p>
	 *
	 * @param filter
	 *        the query filter, to specify the nodes/sources to find as well as
	 *        the start/end dates
	 * @param readingType
	 *        the type of reading to perform
	 * @param tolerance
	 *        a time period of tolerance to use when querying around the
	 *        start/end dates, or {@literal null} for a default tolerance based
	 *        on {@code readingType}
	 * @return the results, never {@literal null}
	 * @since 2.4
	 */
	FilterResults<ReportingGeneralNodeDatumMatch, GeneralNodeDatumPK> findFilteredReading(
			GeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance);

	/**
	 * API for querying for a filtered set of aggregate "readings".
	 *
	 * <p>
	 * The {@code filter} must provide the required date(s) to use for the
	 * reading type. If the reading type only requires one date, then the
	 * {@link GeneralNodeDatumFilter#getLocalStartDate()} or
	 * {@link GeneralNodeDatumFilter#getStartDate()} value should be provided.
	 * </p>
	 *
	 * @param filter
	 *        the query filter, to specify the nodes/sources to find as well as
	 *        the start/end dates
	 * @param readingType
	 *        the type of reading to perform
	 * @param tolerance
	 *        a time period of tolerance to use when querying around the
	 *        start/end dates, or {@literal null} for a default tolerance based
	 *        on {@code readingType}
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 * @since 2.7
	 */
	FilterResults<ReportingGeneralNodeDatumMatch, GeneralNodeDatumPK> findFilteredAggregateReading(
			AggregateGeneralNodeDatumFilter filter, DatumReadingType readingType, Period tolerance,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max);

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
	 * @return the results, never {@literal null}
	 * @since 1.5
	 */
	FilterResults<GeneralLocationDatumFilterMatch, GeneralLocationDatumPK> findGeneralLocationDatum(
			GeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors, Long offset,
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
	 * @return the results, never {@literal null}
	 * @since 1.5
	 */
	FilterResults<ReportingGeneralLocationDatumMatch, GeneralLocationDatumPK> findAggregateGeneralLocationDatum(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Long offset, Integer max);

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
	 * @return the distinct source IDs available (never {@literal null})
	 * @since 1.5
	 */
	Set<String> getLocationAvailableSources(Long locationId, Instant start, Instant end);

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
	 * If the {@code sourceId} parameter is {@literal null} then the returned
	 * interval will be for the node as a whole, for any sources.
	 * </p>
	 *
	 * @param locationId
	 *        the ID of the location to look for
	 * @param sourceId
	 *        an optional source ID to find the available interval for
	 * @return ReadableInterval instance, or {@literal null} if no data
	 *         available
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
	 * @return the results, never {@literal null}
	 * @since 1.4
	 */
	FilterResults<LocationMatch, Long> findFilteredLocations(Location filter,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max);
}
