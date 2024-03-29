/* ==================================================================
 * GeneralNodeDatumDao.java - Aug 22, 2014 6:49:58 AM
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

package net.solarnetwork.central.datum.dao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.dao.AggregationFilterableDao;
import net.solarnetwork.central.dao.BulkExportingDao;
import net.solarnetwork.central.dao.BulkLoadingDao;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.DateInterval;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link GeneralNodeDatum}.
 *
 * @author matt
 * @version 2.1
 */
public interface GeneralNodeDatumDao extends GenericDao<GeneralNodeDatum, GeneralNodeDatumPK>,
		FilterableDao<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK, GeneralNodeDatumFilter>,
		AggregationFilterableDao<ReportingGeneralNodeDatumMatch, AggregateGeneralNodeDatumFilter>,
		BulkLoadingDao<GeneralNodeDatum, GeneralNodeDatumPK>,
		BulkExportingDao<GeneralNodeDatumFilterMatch> {

	/**
	 * Get the interval of available data in the system. Note the returned
	 * interval will be configured with the node's local time zone, if
	 * available.
	 *
	 * @param nodeId
	 *        the node ID to search for
	 * @param sourceId
	 *        an optional source ID to limit the results to, or {@literal null}
	 *        for all sources
	 * @return interval, or {@literal null} if no data available
	 */
	DateInterval getReportableInterval(Long nodeId, String sourceId);

	/**
	 * Get the available source IDs for a given filter.
	 *
	 * <p>
	 * The filter is expected to provide a node ID. Multiple node IDs may be
	 * provided. A {@code startDate} (inclusive), {@code endDate} (exclusive),
	 * or both dates may be provided to limit the query to a specific date
	 * range.
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
	 * @since 1.2
	 */
	Set<String> getAvailableSources(GeneralNodeDatumFilter filter);

	/**
	 * Find the available source IDs for a given filter.
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
	 * @since 1.6
	 */
	Set<NodeSourcePK> findAvailableSources(GeneralNodeDatumFilter filter);

	/**
	 * Calculate the value of datum at a specific point in local-node time,
	 * using before and after records to derive values from.
	 *
	 * <p>
	 * Use this method to estimate the values a datum would have at a specific
	 * point in time. For example if you want to know the value of a datum at
	 * exactly midnight on the 1st of a month for the purposes of billing, you'd
	 * pass that date to this method and it will calculate the values based on
	 * the datum records on either side of that date under the assumption the
	 * datum records are near, not not exactly on, that date. If a datum does
	 * exist at exactly the given {@code date} that record will be returned
	 * directly. Otherwise the result will be derived from the found before and
	 * after records.
	 * </p>
	 *
	 * <p>
	 * This method will return one record for every node ID and source ID
	 * combination provided in {@code filter} and also found in the database.
	 * </p>
	 *
	 * <p>
	 * The {@code filter} must provide the following properties:
	 * </p>
	 *
	 * <ul>
	 * <li><b>nodeIds</b> - the node IDs to look for</li>
	 * <li><b>sourceIds</b> - the source IDs to look for</li>
	 * </ul>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param date
	 *        the date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation
	 * @return the calculated records, never {@literal null}
	 * @since 1.5
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> calculateAt(GeneralNodeDatumFilter filter,
			LocalDateTime date, Period tolerance);

	/**
	 * Calculate the value of datum at a specific point in time, using before
	 * and after records to derive values from.
	 *
	 * <p>
	 * Use this method to estimate the values a datum would have at a specific
	 * point in time. For example if you want to know the value of a datum at
	 * exactly midnight on the 1st of a month for the purposes of billing, you'd
	 * pass that date to this method and it will calculate the values based on
	 * the datum records on either side of that date under the assumption the
	 * datum records are near, not not exactly on, that date. If a datum does
	 * exist at exactly the given {@code date} that record will be returned
	 * directly. Otherwise the result will be derived from the found before and
	 * after records.
	 * </p>
	 *
	 * <p>
	 * This method will return one record for every node ID and source ID
	 * combination provided in {@code filter} and also found in the database.
	 * </p>
	 *
	 * <p>
	 * The {@code filter} must provide the following properties:
	 * </p>
	 *
	 * <ul>
	 * <li><b>nodeIds</b> - the node IDs to look for</li>
	 * <li><b>sourceIds</b> - the source IDs to look for</li>
	 * </ul>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param date
	 *        the date to calculate datum for
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation
	 * @return the calculated records, never {@literal null}
	 * @since 1.10
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> calculateAt(GeneralNodeDatumFilter filter,
			Instant date, Period tolerance);

	/**
	 * Calculate the change between two specific node-local dates.
	 *
	 * <p>
	 * This method calculates the change between datum calculated at two
	 * specific dates, as in
	 * {@link #calculateAt(GeneralNodeDatumFilter, LocalDateTime, Period)}. A
	 * single result for each node and source ID combination will be returned.
	 * </p>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param to
	 *        the second date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation
	 * @return the calculated records, never {@literal null}
	 * @since 1.5
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> calculateBetween(GeneralNodeDatumFilter filter,
			LocalDateTime from, LocalDateTime to, Period tolerance);

	/**
	 * Calculate the change between two specific dates.
	 *
	 * <p>
	 * This method calculates the change between datum calculated at two
	 * specific dates, as in
	 * {@link #calculateAt(GeneralNodeDatumFilter, LocalDateTime, Period)}. A
	 * single result for each node and source ID combination will be returned.
	 * </p>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for
	 * @param to
	 *        the second date to calculate datum for
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation
	 * @return the calculated records, never {@literal null}
	 * @since 1.10
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> calculateBetween(GeneralNodeDatumFilter filter,
			Instant from, Instant to, Period tolerance);

	/**
	 * Find the change of accumulation properties between two specific
	 * node-local dates.
	 *
	 * <p>
	 * This method calculates the change between datum with the nearest dates
	 * less than or equal to given start and end dates. A single result for each
	 * node and source ID combination will be returned, with the accumulation
	 * properties calculated as the difference between the start and end values.
	 * In addition some status properties will be included:
	 * </p>
	 *
	 * <dl>
	 * <dt>endDate</dt>
	 * <dd>The associated date found for the last datum record.</dd>
	 * <dt>timeZone</dt>
	 * <dd>The time zone of the node associated with the datum.</dd>
	 * <dt>localEndDate</dt>
	 * <dd>The <code>endDate</code> translated into the node's local time
	 * zone.</dd>
	 * </dl>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param to
	 *        the second date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation, or {@code null} for no limit
	 * @return the calculated records, never {@literal null}
	 * @since 1.10
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findAccumulation(GeneralNodeDatumFilter filter,
			LocalDateTime from, LocalDateTime to, Period tolerance);

	/**
	 * Find the change of accumulation properties within two specific dates.
	 *
	 * <p>
	 * This method calculates the change between datum with the nearest dates
	 * less than or equal to given start and end dates. A single result for each
	 * node and source ID combination will be returned, with the accumulation
	 * properties calculated as the difference between the start and end values.
	 * In addition some status properties will be included:
	 * </p>
	 *
	 * <dl>
	 * <dt>endDate</dt>
	 * <dd>The associated date found for the last datum record.</dd>
	 * <dt>timeZone</dt>
	 * <dd>The time zone of the node associated with the datum.</dd>
	 * <dt>localEndDate</dt>
	 * <dd>The <code>endDate</code> translated into the node's local time
	 * zone.</dd>
	 * </dl>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for
	 * @param to
	 *        the second date to calculate datum for
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation, or {@code null} for no limit
	 * @return the calculated records, never {@literal null}
	 * @since 1.10
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findAccumulation(GeneralNodeDatumFilter filter,
			Instant from, Instant to, Period tolerance);

	/**
	 * Find the change of accumulation properties within two specific node-local
	 * dates.
	 *
	 * <p>
	 * This method calculates the change between datum with the nearest dates
	 * within the given start and end dates. Data earlier or later than the
	 * given date range will <b>not</b> be considered. A single result for each
	 * node and source ID combination will be returned, with the accumulation
	 * properties calculated as the difference between the start and end values.
	 * In addition some status properties will be included:
	 * </p>
	 *
	 * <dl>
	 * <dt>endDate</dt>
	 * <dd>The associated date found for the last datum record.</dd>
	 * <dt>timeZone</dt>
	 * <dd>The time zone of the node associated with the datum.</dd>
	 * <dt>localEndDate</dt>
	 * <dd>The <code>endDate</code> translated into the node's local time
	 * zone.</dd>
	 * </dl>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param to
	 *        the second date to calculate datum for; each node ID will use its
	 *        associated time zone
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation, or {@code null} for no limit
	 * @return the calculated records, never {@literal null}
	 * @since 1.15
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findAccumulationWithin(GeneralNodeDatumFilter filter,
			LocalDateTime from, LocalDateTime to, Period tolerance);

	/**
	 * Find the change of accumulation properties within two specific dates.
	 *
	 * <p>
	 * This method calculates the change between datum with the nearest dates
	 * less than or equal to given start and end dates. Data earlier or later
	 * than the given date range will <b>not</b> be considered. A single result
	 * for each node and source ID combination will be returned, with the
	 * accumulation properties calculated as the difference between the start
	 * and end values. In addition some status properties will be included:
	 * </p>
	 *
	 * <dl>
	 * <dt>endDate</dt>
	 * <dd>The associated date found for the last datum record.</dd>
	 * <dt>timeZone</dt>
	 * <dd>The time zone of the node associated with the datum.</dd>
	 * <dt>localEndDate</dt>
	 * <dd>The <code>endDate</code> translated into the node's local time
	 * zone.</dd>
	 * </dl>
	 *
	 * @param filter
	 *        the node and source ID search criteria
	 * @param from
	 *        the first date to calculate datum for
	 * @param to
	 *        the second date to calculate datum for
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation, or {@code null} for no limit
	 * @return the calculated records, never {@literal null}
	 * @since 1.15
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findAccumulationWithin(GeneralNodeDatumFilter filter,
			Instant from, Instant to, Period tolerance);

	/**
	 * Query for aggregated datum readings over a time range.
	 *
	 * @param filter
	 *        the query filter
	 * @param type
	 *        the reading type
	 * @param tolerance
	 *        the maximum time span before and after {@code date} to consider
	 *        when looking for before and after records to perform the
	 *        calculation, or {@code null} for no limit
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 * @since 1.12
	 */
	FilterResults<ReportingGeneralNodeDatumMatch> findAggregationFilteredReadings(
			AggregateGeneralNodeDatumFilter filter, DatumReadingType type, Period tolerance,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);

	/**
	 * Get a count of datum records that match a search criteria.
	 *
	 * <p>
	 * At a minimum, the following criteria are supported:
	 * </p>
	 *
	 * <ul>
	 * <li>node IDs</li>
	 * <li>source IDs</li>
	 * <li>date range (start/end dates)</li>
	 * </ul>
	 *
	 * @param filter
	 *        the search criteria
	 * @return the count of matching records
	 * @since 1.8
	 */
	DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter);

	/**
	 * Delete datum matching a search criteria.
	 *
	 * <p>
	 * At a minimum, the following criteria are supported:
	 * </p>
	 *
	 * <ul>
	 * <li>node IDs</li>
	 * <li>source IDs</li>
	 * <li>date range (start/end dates)</li>
	 * </ul>
	 *
	 * @param filter
	 *        the search criteria
	 * @return the number of datum deleted
	 * @since 1.8
	 */
	long deleteFiltered(GeneralNodeDatumFilter filter);

	/**
	 * Mark a set of datum aggregate data as "stale" so the aggregates are
	 * re-processed.
	 *
	 * <p>
	 * The given criteria defines which data should be marked as stale. The
	 * following criteria are required:
	 * </p>
	 *
	 * <ul>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 *
	 * @param criteria
	 *        the criteria of datum to mark stale
	 * @since 1.13
	 */
	void markDatumAggregatesStale(GeneralNodeDatumFilter criteria);

	/**
	 * Find stale aggregate records matching a search criteria.
	 *
	 * @param criteria
	 *        the criteria
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 * @since 1.14
	 */
	FilterResults<StaleAggregateDatum> findStaleAggregateDatum(GeneralNodeDatumFilter criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);

}
