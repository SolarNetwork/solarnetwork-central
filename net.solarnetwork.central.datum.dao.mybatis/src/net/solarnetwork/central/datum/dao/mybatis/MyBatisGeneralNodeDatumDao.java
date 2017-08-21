/* ==================================================================
 * MyBatisGeneralNodeDatumDao.java - Nov 13, 2014 6:45:39 AM
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

package net.solarnetwork.central.datum.dao.mybatis;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.ReadableInterval;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * MyBatis implementation of {@link GeneralNodeDatumDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisGeneralNodeDatumDao
		extends BaseMyBatisGenericDao<GeneralNodeDatum, GeneralNodeDatumPK> implements
		FilterableDao<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK, GeneralNodeDatumFilter>,
		GeneralNodeDatumDao {

	/** The query parameter for a class name value. */
	public static final String PARAM_CLASS_NAME = "class";

	/** The query parameter for a node ID value. */
	public static final String PARAM_NODE_ID = "node";

	/** The query parameter for a source ID value. */
	public static final String PARAM_SOURCE_ID = "source";

	/** The query parameter for a general ID value. */
	public static final String PARAM_ID = "id";

	/** The query parameter for a date value. */
	public static final String PARAM_DATE = "date";

	/** The query parameter for a starting date value. */
	public static final String PARAM_START_DATE = "start";

	/** The query parameter for an ending date value. */
	public static final String PARAM_END_DATE = "end";

	/** The query parameter for an {@code Aggregation} string value. */
	public static final String PARAM_AGGREGATION = "aggregation";

	/** The query parameter for a general {@link Filter} object value. */
	public static final String PARAM_FILTER = "filter";

	/** The default query name used for {@link #getReportableInterval(Long)}. */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-general-reportable-interval";

	/**
	 * The default query name used for
	 * {@link #getAvailableSources(Long, DateTime, DateTime)}.
	 */
	public static final String QUERY_FOR_DISTINCT_SOURCES = "find-general-distinct-sources";

	/**
	 * The default query name used for
	 * {@link #findFiltered(GeneralNodeDatumFilter, List, Integer, Integer)}
	 * where {@link GeneralNodeDatumFilter#isMostRecent()} is set to
	 * <em>true</em>.
	 */
	public static final String QUERY_FOR_MOST_RECENT = "find-general-most-recent";

	/**
	 * The default query name used for
	 * {@link #findFiltered(GeneralNodeDatumFilter, List, Integer, Integer)}
	 * where {@link GeneralNodeDatumFilter#isMostRecent()} is set to
	 * <em>true</em> and {@link AggregationFilter#getAggregation()} is used.
	 * 
	 * @since 1.1
	 */
	public static final String QUERY_FOR_MOST_RECENT_REPORTING = "find-general-reporting-most-recent";

	/**
	 * The default query name for
	 * {@link #getAuditPropertyCountTotal(GeneralNodeDatumFilter)}.
	 * 
	 * @since 1.2
	 */
	public static final String QUERY_FOR_AUDIT_HOURLY_PROP_COUNT = "find-general-audit-hourly-prop-count";

	private String queryForReportableInterval;
	private String queryForDistinctSources;
	private String queryForMostRecent;
	private String queryForMostRecentReporting;
	private String queryForAuditHourlyPropertyCount;

	/**
	 * Default constructor.
	 */
	public MyBatisGeneralNodeDatumDao() {
		super(GeneralNodeDatum.class, GeneralNodeDatumPK.class);
		this.queryForReportableInterval = QUERY_FOR_REPORTABLE_INTERVAL;
		this.queryForDistinctSources = QUERY_FOR_DISTINCT_SOURCES;
		this.queryForMostRecent = QUERY_FOR_MOST_RECENT;
		this.queryForMostRecentReporting = QUERY_FOR_MOST_RECENT_REPORTING;
		this.queryForAuditHourlyPropertyCount = QUERY_FOR_AUDIT_HOURLY_PROP_COUNT;
	}

	/**
	 * Get the filter query name for a given domain.
	 * 
	 * @param filter
	 *        the filter
	 * @return query name
	 */
	protected String getQueryForFilter(GeneralNodeDatumFilter filter) {
		Aggregation aggregation = null;
		if ( filter instanceof AggregationFilter ) {
			aggregation = ((AggregationFilter) filter).getAggregation();
		}
		if ( filter.isMostRecent() ) {
			if ( aggregation != null ) {
				return queryForMostRecentReporting;
			}
			return queryForMostRecent;
		}
		if ( aggregation == null ) {
			return getQueryForAll() + "-GeneralNodeDatumMatch";
		} else if ( aggregation.compareTo(Aggregation.Hour) < 0 ) {
			// all *Minute aggregates are mapped to the Minute query name
			aggregation = Aggregation.Minute;
		}
		return (getQueryForAll() + "-ReportingGeneralNodeDatum-" + aggregation.toString());
	}

	private void setupAggregationParam(GeneralNodeDatumFilter filter, Map<String, Object> sqlProps) {
		if ( filter.isMostRecent() && filter instanceof net.solarnetwork.central.domain.AggregationFilter
				&& ((AggregationFilter) filter).getAggregation() != null ) {
			Aggregation aggregation = ((AggregationFilter) filter).getAggregation();
			if ( aggregation.compareLevel(Aggregation.Hour) < 1 ) {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Hour.name());
			} else if ( aggregation.compareLevel(Aggregation.Day) < 1 ) {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Day.name());
			} else {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Month.name());
			}
		}
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<GeneralNodeDatumFilterMatch> findFiltered(GeneralNodeDatumFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		final String query = getQueryForFilter(filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		if ( filter.isMostRecent() && filter instanceof net.solarnetwork.central.domain.AggregationFilter
				&& ((AggregationFilter) filter).getAggregation() != null ) {
			throw new IllegalArgumentException(
					"Aggregation not allowed on a filter for most recent datum");
		}
		//postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1 and NOT a mostRecent query
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && filter.isMostRecent() == false ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<GeneralNodeDatumFilterMatch> rows = selectList(query, sqlProps, offset, max);

		//rows = postProcessFilterQuery(filter, rows);

		BasicFilterResults<GeneralNodeDatumFilterMatch> results = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset,
				rows.size());

		return results;
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAggregationFiltered(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		final String query = getQueryForFilter(filter);
		final Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		setupAggregationParam(filter, sqlProps);
		//postProcessAggregationFilterProperties(filter, sqlProps);

		// attempt count first, if NOT mostRecent query and max NOT specified as -1
		// and NOT a *Minute, *DayOfWeek, or *HourOfDay, or RunningTotal aggregate levels
		Long totalCount = null;
		final Aggregation agg = filter.getAggregation();
		if ( !filter.isMostRecent() && max != null && max.intValue() != -1
				&& agg.compareTo(Aggregation.Hour) >= 0 && agg != Aggregation.DayOfWeek
				&& agg != Aggregation.SeasonalDayOfWeek && agg != Aggregation.HourOfDay
				&& agg != Aggregation.SeasonalHourOfDay && agg != Aggregation.RunningTotal ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}
		if ( agg != null && agg.compareLevel(Aggregation.Hour) < 1 ) {
			// make sure start/end date provided for minute level aggregation queries as query expects it
			DateTime forced = null;
			if ( filter.getStartDate() == null || filter.getEndDate() == null ) {
				forced = new DateTime();
				int minutes = agg.getLevel() / 60;
				forced = forced.withMinuteOfHour((forced.getMinuteOfHour() / minutes) * minutes)
						.minuteOfHour().roundFloorCopy();
			}
			sqlProps.put(PARAM_START_DATE,
					filter.getStartDate() != null ? filter.getStartDate() : forced);
			sqlProps.put(PARAM_END_DATE, filter.getEndDate() != null ? filter.getEndDate() : forced);
		}

		List<ReportingGeneralNodeDatumMatch> rows = selectList(query, sqlProps, offset, max);

		// rows = postProcessAggregationFilterQuery(filter, rows);

		BasicFilterResults<ReportingGeneralNodeDatumMatch> results = new BasicFilterResults<ReportingGeneralNodeDatumMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset,
				rows.size());

		return results;
	}

	private Long executeCountQuery(final String countQueryName, final Map<String, ?> sqlProps) {
		Number n = getSqlSession().selectOne(countQueryName, sqlProps);
		if ( n != null ) {
			return n.longValue();
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReadableInterval getReportableInterval(Long nodeId, String sourceId) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( nodeId != null ) {
			params.put(PARAM_NODE_ID, nodeId);
		}
		if ( sourceId != null ) {
			params.put(PARAM_SOURCE_ID, sourceId);
		}
		getSqlSession().selectOne(this.queryForReportableInterval, params);
		Timestamp start = (Timestamp) params.get("ts_start");
		Timestamp end = (Timestamp) params.get("ts_end");
		if ( start == null || end == null ) {
			return null;
		}
		long d1 = start.getTime();
		long d2 = end.getTime();
		DateTimeZone tz = null;
		if ( params.containsKey("node_tz") ) {
			tz = DateTimeZone.forID(params.get("node_tz").toString());
		}
		Interval interval = new Interval(d1 < d2 ? d1 : d2, d2 > d1 ? d2 : d1, tz);
		return interval;

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( nodeId != null ) {
			params.put(PARAM_NODE_ID, nodeId);
		}
		if ( start != null ) {
			params.put(PARAM_START_DATE, start);
		}
		if ( end != null ) {
			params.put(PARAM_END_DATE, end);
		}
		List<String> results = getSqlSession().selectList(this.queryForDistinctSources, params);
		return new LinkedHashSet<String>(results);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public long getAuditPropertyCountTotal(GeneralNodeDatumFilter filter) {
		Long result = selectLong(queryForAuditHourlyPropertyCount, filter);
		return (result != null ? result : 0L);
	}

	public String getQueryForReportableInterval() {
		return queryForReportableInterval;
	}

	public void setQueryForReportableInterval(String queryForReportableInterval) {
		this.queryForReportableInterval = queryForReportableInterval;
	}

	public String getQueryForDistinctSources() {
		return queryForDistinctSources;
	}

	public void setQueryForDistinctSources(String queryForDistinctSources) {
		this.queryForDistinctSources = queryForDistinctSources;
	}

	public String getQueryForMostRecent() {
		return queryForMostRecent;
	}

	public void setQueryForMostRecent(String queryForMostRecent) {
		this.queryForMostRecent = queryForMostRecent;
	}

	public String getQueryForMostRecentReporting() {
		return queryForMostRecentReporting;
	}

	public void setQueryForMostRecentReporting(String queryForMostRecentReporting) {
		this.queryForMostRecentReporting = queryForMostRecentReporting;
	}

	/**
	 * Set the statement name for the
	 * {@link #getAuditPropertyCountTotal(GeneralNodeDatumFilter)} method.
	 * 
	 * @param statementName
	 *        the statement name
	 * @since 1.2
	 */
	public void setQueryForAuditHourlyPropertyCount(String statementName) {
		this.queryForAuditHourlyPropertyCount = statementName;
	}

}
