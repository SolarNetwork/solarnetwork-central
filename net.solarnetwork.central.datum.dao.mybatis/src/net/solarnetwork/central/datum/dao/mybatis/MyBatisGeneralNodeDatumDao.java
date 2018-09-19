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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInterval;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
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
 * @version 1.10
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

	/**
	 * The query parameter for {@link Period} object value.
	 * 
	 * @since 1.9
	 */
	public static final String PARAM_TOLERANCE = "tolerance";

	/**
	 * The query parameter for a {@link CombiningConfig} data structure.
	 * 
	 * @since 1.5
	 */
	public static final String PARAM_COMBINING = "combine";

	/** The default query name used for {@link #getReportableInterval(Long)}. */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-general-reportable-interval";

	/**
	 * The default query name used for
	 * {@link #getAvailableSources(Long, DateTime, DateTime)}.
	 */
	public static final String QUERY_FOR_DISTINCT_SOURCES = "find-general-distinct-sources";

	/**
	 * The default query name used for
	 * {@link #findAvailableSources(GeneralNodeDatumFilter)}.
	 */
	public static final String QUERY_FOR_DISTINCT_NODE_SOURCES = "find-general-distinct-node-sources";

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
	 * The default query name for {@link #getAuditInterval(Long)}.
	 * 
	 * @since 1.2
	 */
	public static final String QUERY_FOR_AUDIT_INTERVAL = "find-general-audit-interval";

	/**
	 * The default query name for
	 * {@link #getAuditCountTotal(GeneralNodeDatumFilter)}.
	 * 
	 * @since 1.2
	 */
	public static final String QUERY_FOR_AUDIT_HOURLY_PROP_COUNT = "find-general-audit-hourly-prop-count";

	/**
	 * The default query name for
	 * {@link #getAuditCountTotal(GeneralNodeDatumFilter)} when the
	 * {@code dataPath} value is {@literal DatumStored}.
	 * 
	 * @since 1.7
	 */
	public static final String QUERY_FOR_AUDIT_DATUM_STORED_COUNT = "find-general-audit-datum-stored-count";

	/**
	 * The default query name for
	 * {@link #findAuditRecordCountsFiltered(AggregateGeneralNodeDatumFilter, List, Integer, Integer)}.
	 * 
	 * @since 1.8
	 */
	public static final String QUERY_FOR_AUDIT_DATUM_RECORD_COUNTS = "findall-AuditNodeDatum-AuditDatumRecordCounts";

	/**
	 * The default query name for
	 * {@link #findAccumulativeAuditRecordCountsFiltered(AggregateGeneralNodeDatumFilter, List, Integer, Integer)}.
	 * 
	 * @since 1.8
	 */
	public static final String QUERY_FOR_ACCUMULATIVE_AUDIT_DATUM_RECORD_COUNTS = "findall-AccumulativeAuditNodeDatum-AuditDatumRecordCounts";

	/**
	 * The default query name for
	 * {@link #calculateAt(GeneralNodeDatumFilter, DateTime, Period)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_AT = "find-general-reporting-at-local";

	/**
	 * The default query name for
	 * {@link #calculateBetween(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_BETWEEN = "find-general-reporting-between-local";

	/**
	 * The default query name for
	 * {@link #findAccumulation(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_ACCUMULATION = "find-general-reporting-diff-within-local";

	private String queryForReportableInterval;
	private String queryForDistinctSources;
	private String queryForDistinctNodeSources;
	private String queryForMostRecent;
	private String queryForMostRecentReporting;
	private String queryForAuditInterval;
	private String queryForAuditHourlyPropertyCount;
	private String queryForAuditDatumStoredCount;
	private String queryForAuditDatumRecordCounts;
	private String queryForAccumulativeAuditDatumRecordCounts;
	private String queryForDatumAt;
	private String queryForDatumBetween;
	private String queryForDatumAccumulation;

	/**
	 * Default constructor.
	 */
	public MyBatisGeneralNodeDatumDao() {
		super(GeneralNodeDatum.class, GeneralNodeDatumPK.class);
		this.queryForReportableInterval = QUERY_FOR_REPORTABLE_INTERVAL;
		this.queryForDistinctSources = QUERY_FOR_DISTINCT_SOURCES;
		this.queryForDistinctNodeSources = QUERY_FOR_DISTINCT_NODE_SOURCES;
		this.queryForMostRecent = QUERY_FOR_MOST_RECENT;
		this.queryForMostRecentReporting = QUERY_FOR_MOST_RECENT_REPORTING;
		this.queryForAuditInterval = QUERY_FOR_AUDIT_INTERVAL;
		this.queryForAuditHourlyPropertyCount = QUERY_FOR_AUDIT_HOURLY_PROP_COUNT;
		this.queryForAuditDatumStoredCount = QUERY_FOR_AUDIT_DATUM_STORED_COUNT;
		this.queryForAuditDatumRecordCounts = QUERY_FOR_AUDIT_DATUM_RECORD_COUNTS;
		this.queryForAccumulativeAuditDatumRecordCounts = QUERY_FOR_ACCUMULATIVE_AUDIT_DATUM_RECORD_COUNTS;
		this.queryForDatumAt = QUERY_FOR_DATUM_RECORDS_AT;
		this.queryForDatumBetween = QUERY_FOR_DATUM_RECORDS_BETWEEN;
		this.queryForDatumAccumulation = QUERY_FOR_DATUM_ACCUMULATION;
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
		if ( aggregation == null || aggregation.getLevel() < 1 ) {
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
			if ( aggregation.getLevel() < 1 ) {
				// support None
				return;
			}
			if ( aggregation.compareLevel(Aggregation.Hour) < 1 ) {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Hour.name());
			} else if ( aggregation.compareLevel(Aggregation.Day) < 1 ) {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Day.name());
			} else {
				sqlProps.put(PARAM_AGGREGATION, Aggregation.Month.name());
			}
		}
	}

	private CombiningConfig getCombiningFilterProperties(GeneralNodeDatumFilter filter) {
		Map<Long, Set<Long>> nodeMappings = filter.getNodeIdMappings();
		Map<String, Set<String>> sourceMappings = filter.getSourceIdMappings();
		if ( (nodeMappings == null || nodeMappings.isEmpty())
				&& (sourceMappings == null || sourceMappings.isEmpty()) ) {
			return null;
		}
		if ( !filter.isWithoutTotalResultsCount() ) {
			throw new IllegalArgumentException(
					"Total results not allowed on combining query; set withoutTotalResultsCount to true");
		}
		List<CombineIdsConfig<Object>> configs = new ArrayList<CombineIdsConfig<Object>>(2);
		if ( nodeMappings != null && !nodeMappings.isEmpty() ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			CombineIdsConfig<Object> config = (CombineIdsConfig) new CombineIdsConfig<Long>(
					CombiningConfig.NODE_IDS_CONFIG, nodeMappings, Long.class);
			configs.add(config);
		}
		if ( sourceMappings != null && !sourceMappings.isEmpty() ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			CombineIdsConfig<Object> config = (CombineIdsConfig) new CombineIdsConfig<String>(
					CombiningConfig.SOURCE_IDS_CONFIG, sourceMappings, String.class);
			configs.add(config);
		}

		CombiningType type = filter.getCombiningType();
		if ( type == null ) {
			type = CombiningType.Sum;
		}

		return new CombiningConfig(type, configs);
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<GeneralNodeDatumFilterMatch> findFiltered(GeneralNodeDatumFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		// force withoutTotalResultsCount if combining

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
		CombiningConfig combining = getCombiningFilterProperties(filter);
		if ( combining != null ) {
			sqlProps.put(PARAM_COMBINING, combining);
		}
		//postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1 and NOT a mostRecent query
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && !filter.isMostRecent()
				&& !filter.isWithoutTotalResultsCount() ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<GeneralNodeDatumFilterMatch> rows = selectList(query, sqlProps, offset, max);

		//rows = postProcessFilterQuery(filter, rows);

		BasicFilterResults<GeneralNodeDatumFilterMatch> results = new BasicFilterResults<GeneralNodeDatumFilterMatch>(
				rows,
				(totalCount != null ? totalCount
						: filter.isWithoutTotalResultsCount() ? null : Long.valueOf(rows.size())),
				offset, rows.size());

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

		final Aggregation agg = filter.getAggregation();
		if ( agg != null && agg.getLevel() > 0 && agg.compareLevel(Aggregation.Hour) < 1 ) {
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
		} else if ( agg != null && agg == Aggregation.RunningTotal && filter.getSourceId() == null ) {
			// source ID is required for RunningTotal currently
			throw new IllegalArgumentException("sourceId is required for RunningTotal aggregation");
		}

		CombiningConfig combining = getCombiningFilterProperties(filter);
		if ( combining != null ) {
			sqlProps.put(PARAM_COMBINING, combining);
		}

		// attempt count first, if NOT mostRecent query and max NOT specified as -1
		// and NOT a *Minute, *DayOfWeek, or *HourOfDay, or RunningTotal aggregate levels
		Long totalCount = null;
		if ( !filter.isMostRecent() && !filter.isWithoutTotalResultsCount() && max != null
				&& max.intValue() != -1 && (agg.getLevel() < 1 || agg.compareTo(Aggregation.Hour) >= 0)
				&& agg != Aggregation.DayOfWeek && agg != Aggregation.SeasonalDayOfWeek
				&& agg != Aggregation.HourOfDay && agg != Aggregation.SeasonalHourOfDay
				&& agg != Aggregation.RunningTotal ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<ReportingGeneralNodeDatumMatch> rows;
		try {
			rows = selectList(query, sqlProps, offset, max);
		} catch ( RuntimeException e ) {
			Throwable cause = e;
			while ( cause.getCause() != null ) {
				cause = cause.getCause();
			}
			if ( cause instanceof IllegalArgumentException ) {
				// assume this is "query not found" so aggregate not supported
				throw new IllegalArgumentException("Aggregate " + agg + " not supported");
			}
			throw e;
		}

		// rows = postProcessAggregationFilterQuery(filter, rows);

		BasicFilterResults<ReportingGeneralNodeDatumMatch> results = new BasicFilterResults<ReportingGeneralNodeDatumMatch>(
				rows,
				(totalCount != null ? totalCount
						: filter.isWithoutTotalResultsCount() ? null : Long.valueOf(rows.size())),
				offset, rows.size());

		return results;
	}

	private Long executeCountQuery(final String countQueryName, final Map<String, ?> sqlProps) {
		try {
			Number n = getSqlSession().selectOne(countQueryName, sqlProps);
			if ( n != null ) {
				return n.longValue();
			}
		} catch ( RuntimeException e ) {
			Throwable cause = e;
			while ( cause.getCause() != null ) {
				cause = cause.getCause();
			}
			if ( cause instanceof IllegalArgumentException ) {
				log.warn("Count query not supported: {}", countQueryName, e);
			} else {
				throw e;
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReadableInterval getReportableInterval(Long nodeId, String sourceId) {
		return selectInterval(this.queryForReportableInterval, nodeId, sourceId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long nodeId, DateTime start, DateTime end) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		filter.setStartDate(start);
		filter.setEndDate(end);
		return getAvailableSources(filter);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(GeneralNodeDatumFilter filter) {
		GeneralNodeDatumFilter f = filter;
		if ( f.getStartDate() != null || f.getEndDate() != null ) {
			// round dates to days, because we are searching day data
			DatumFilterCommand c = new DatumFilterCommand();
			c.setNodeIds(f.getNodeIds());
			if ( f.getStartDate() != null ) {
				c.setStartDate(f.getStartDate().dayOfMonth().roundFloorCopy());
			}
			if ( f.getEndDate() != null ) {
				c.setEndDate(f.getEndDate().dayOfMonth().roundCeilingCopy());
			}
			f = c;
		}
		List<String> results = getSqlSession().selectList(this.queryForDistinctSources, f);
		return new LinkedHashSet<String>(results);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<NodeSourcePK> findAvailableSources(GeneralNodeDatumFilter filter) {
		GeneralNodeDatumFilter f = filter;
		if ( f.getStartDate() != null || f.getEndDate() != null ) {
			// round dates to days, because we are searching day data
			DatumFilterCommand c = new DatumFilterCommand();
			c.setNodeIds(f.getNodeIds());
			if ( f.getStartDate() != null ) {
				c.setStartDate(f.getStartDate().dayOfMonth().roundFloorCopy());
			}
			if ( f.getEndDate() != null ) {
				c.setEndDate(f.getEndDate().dayOfMonth().roundCeilingCopy());
			}
			f = c;
		}
		List<NodeSourcePK> results = getSqlSession().selectList(this.queryForDistinctNodeSources, f);
		return new LinkedHashSet<NodeSourcePK>(results);
	}

	private ReadableInterval selectInterval(String statement, Long nodeId, String sourceId) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( nodeId != null ) {
			params.put(PARAM_NODE_ID, nodeId);
		}
		if ( sourceId != null ) {
			params.put(PARAM_SOURCE_ID, sourceId);
		}
		getSqlSession().selectOne(statement, params);
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

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReadableInterval getAuditInterval(Long nodeId, String sourceId) {
		return selectInterval(this.queryForAuditInterval, nodeId, sourceId);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public long getAuditPropertyCountTotal(GeneralNodeDatumFilter filter) {
		return getAuditCountTotal(filter);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The following {@code dataPath} values are supported:
	 * </p>
	 * 
	 * <dl>
	 * <dt>DatumQuery</dt>
	 * <dd>The count of datum queried.</dd>
	 * 
	 * <dt>Property</dt>
	 * <dd>The count of datum properties added.</dd>
	 * 
	 * <dt>DatumStored</dt>
	 * <dd>The total count of datum stored across all time.</dd>
	 * </dl>
	 * 
	 * <p>
	 * If {@code dataPath} is missing, or not one of the above values, then
	 * {@literal Property} will be assumed.
	 * </p>
	 * 
	 * @since 1.6
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public long getAuditCountTotal(GeneralNodeDatumFilter filter) {
		String auditType = filter.getDataPath();
		String stmtName;
		if ( "DatumStored".equals(auditType) ) {
			stmtName = queryForAuditDatumStoredCount;
		} else {
			stmtName = queryForAuditHourlyPropertyCount;
		}
		Long result = selectLong(stmtName, filter);
		return (result != null ? result : 0L);
	}

	private Aggregation aggregationForAuditDatumRecordCounts(AggregateGeneralNodeDatumFilter filter) {
		// limit aggregation to specific supported ones
		Aggregation aggregation = Aggregation.Day;
		if ( filter != null && filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Hour:
				case Day:
				case Month:
					aggregation = filter.getAggregation();
					break;

				default:
					// ignore all others
			}
		}
		return aggregation;
	}

	private Map<String, Object> sqlParametersForAuditDatumRecordCounts(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors) {
		final Map<String, Object> sqlProps = new HashMap<String, Object>(3);
		sqlProps.put(PARAM_FILTER, filter);

		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}

		// limit aggregation to specific supported ones
		Aggregation aggregation = aggregationForAuditDatumRecordCounts(filter);
		sqlProps.put(PARAM_AGGREGATION, aggregation.name());

		// setup rollup flags for query to use in form of map with keys that can be tested
		DatumRollupType[] rollupTypes = filter.getDatumRollupTypes();
		if ( rollupTypes != null && rollupTypes.length > 0 ) {
			Map<String, Boolean> rollups = new LinkedHashMap<String, Boolean>(4);
			for ( DatumRollupType type : rollupTypes ) {
				switch (type) {
					case None:
						rollups.clear();
						break;
					default:
						rollups.put(type.name(), true);
				}
			}
			if ( !rollups.isEmpty() ) {
				sqlProps.put("rollups", rollups);
			}
		}
		return sqlProps;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.8
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<AuditDatumRecordCounts> findAuditRecordCountsFiltered(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		Map<String, Object> sqlProps = sqlParametersForAuditDatumRecordCounts(filter, sortDescriptors);

		// execute count query first, if max NOT specified as -1 and NOT a mostRecent query
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && !filter.isMostRecent()
				&& !filter.isWithoutTotalResultsCount() ) {
			totalCount = executeCountQuery(queryForAuditDatumRecordCounts + "-count", sqlProps);
		}

		// execute actual query
		List<AuditDatumRecordCounts> rows = selectList(queryForAuditDatumRecordCounts, sqlProps, offset,
				max);

		BasicFilterResults<AuditDatumRecordCounts> results = new BasicFilterResults<AuditDatumRecordCounts>(
				rows,
				(totalCount != null ? totalCount
						: filter.isWithoutTotalResultsCount() ? null : Long.valueOf(rows.size())),
				offset, rows.size());
		return results;

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.8
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<AuditDatumRecordCounts> findAccumulativeAuditRecordCountsFiltered(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		Map<String, Object> sqlProps = sqlParametersForAuditDatumRecordCounts(filter, sortDescriptors);

		// execute count query first, if max NOT specified as -1 and NOT a mostRecent query
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && !filter.isMostRecent()
				&& !filter.isWithoutTotalResultsCount() ) {
			totalCount = executeCountQuery(queryForAccumulativeAuditDatumRecordCounts + "-count",
					sqlProps);
		}

		// execute actual query
		List<AuditDatumRecordCounts> rows = selectList(queryForAccumulativeAuditDatumRecordCounts,
				sqlProps, offset, max);

		BasicFilterResults<AuditDatumRecordCounts> results = new BasicFilterResults<AuditDatumRecordCounts>(
				rows,
				(totalCount != null ? totalCount
						: filter.isWithoutTotalResultsCount() ? null : Long.valueOf(rows.size())),
				offset, rows.size());
		return results;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.9
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> calculateAt(GeneralNodeDatumFilter filter,
			LocalDateTime date, Period tolerance) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_DATE, date);
		sqlProps.put(PARAM_TOLERANCE, tolerance);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryForDatumAt, sqlProps, null, null);
		return new BasicFilterResults<ReportingGeneralNodeDatumMatch>(rows, (long) rows.size(), 0,
				rows.size());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.9
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> calculateBetween(GeneralNodeDatumFilter filter,
			LocalDateTime from, LocalDateTime to, Period tolerance) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_START_DATE, from);
		sqlProps.put(PARAM_END_DATE, to);
		sqlProps.put(PARAM_TOLERANCE, tolerance);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryForDatumBetween, sqlProps, null,
				null);
		return new BasicFilterResults<ReportingGeneralNodeDatumMatch>(rows, (long) rows.size(), 0,
				rows.size());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.9
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAccumulation(GeneralNodeDatumFilter filter,
			LocalDateTime from, LocalDateTime to, Period tolerance) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_START_DATE, from);
		sqlProps.put(PARAM_END_DATE, to);
		sqlProps.put(PARAM_TOLERANCE, tolerance);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryForDatumAccumulation, sqlProps, null,
				null);
		return new BasicFilterResults<ReportingGeneralNodeDatumMatch>(rows, (long) rows.size(), 0,
				rows.size());
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

	/**
	 * Set the statement name for the {@link #getAuditInterval(Long)} method.
	 * 
	 * @param statementName
	 *        the statement name
	 * @since 1.2
	 */
	public void setQueryForAuditInterval(String statementName) {
		this.queryForAuditInterval = statementName;
	}

	/**
	 * Set the statement name for the
	 * {@link #getAuditCountTotal(GeneralNodeDatumFilter)} method when
	 * {@code dataPath} is {@literal DatumStored}.
	 * 
	 * @param queryForAuditDatumStoredCount
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_AUDIT_DATUM_STORED_COUNT}
	 * @since 1.7
	 */
	public void setQueryForAuditDatumStoredCount(String queryForAuditDatumStoredCount) {
		this.queryForAuditDatumStoredCount = queryForAuditDatumStoredCount;
	}

	/**
	 * Set the statement name for the
	 * {@link #findAuditRecordCountsFiltered(AggregateGeneralNodeDatumFilter, List, Integer, Integer)}
	 * method.
	 * 
	 * @param queryForAuditDatumRecordCounts
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_AUDIT_DATUM_RECORD_COUNTS}
	 * @since 1.8
	 */
	public void setQueryForAuditDatumRecordCounts(String queryForAuditDatumRecordCounts) {
		this.queryForAuditDatumRecordCounts = queryForAuditDatumRecordCounts;
	}

	/**
	 * Set the statement name for the
	 * {@link #findAccumulativeAuditRecordCountsFiltered(AggregateGeneralNodeDatumFilter, List, Integer, Integer)}
	 * method.
	 * 
	 * @param queryForAccumulativeAuditDatumRecordCounts
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_ACCUMULATIVE_AUDIT_DATUM_RECORD_COUNTS}
	 * @since 1.8
	 */
	public void setQueryForAccumulativeAuditDatumRecordCounts(
			String queryForAccumulativeAuditDatumRecordCounts) {
		this.queryForAccumulativeAuditDatumRecordCounts = queryForAccumulativeAuditDatumRecordCounts;
	}

	/**
	 * Set the statement name for the
	 * {@link #calculateAt(GeneralNodeDatumFilter, DateTime, Period)} method.
	 * 
	 * @param queryForDatumAt
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_RECORDS_AT}
	 * @since 1.9
	 */
	public void setQueryForDatumAt(String queryForDatumAt) {
		this.queryForDatumAt = queryForDatumAt;
	}

	/**
	 * Set the statement name for the
	 * {@link #calculateBetween(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}
	 * method.
	 * 
	 * @param queryForDatumBetween
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_RECORDS_BETWEEN}
	 * @since 1.9
	 */
	public void setQueryForDatumBetween(String queryForDatumBetween) {
		this.queryForDatumBetween = queryForDatumBetween;
	}

	/**
	 * Set the statement name for the
	 * {@link #findAccumulation(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime)}
	 * method.
	 * 
	 * @param queryForDatumAccumulation
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_ACCUMULATION}
	 * @since 1.9
	 */
	public void setQueryForDatumAccumulation(String queryForDatumAccumulation) {
		this.queryForDatumAccumulation = queryForDatumAccumulation;
	}

	/**
	 * Set the statement name for the
	 * {@link #findAvailableSources(GeneralNodeDatumFilter)} method.
	 * 
	 * @param queryForDistinctNodeSources
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DISTINCT_NODE_SOURCES}
	 * @since 1.10
	 */
	public void setQueryForDistinctNodeSources(String queryForDistinctNodeSources) {
		this.queryForDistinctNodeSources = queryForDistinctNodeSources;
	}

}
