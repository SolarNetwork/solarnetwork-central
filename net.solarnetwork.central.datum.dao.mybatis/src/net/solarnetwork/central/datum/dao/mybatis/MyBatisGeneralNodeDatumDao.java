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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.ReadableInterval;
import org.joda.time.ReadablePartial;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.common.dao.jdbc.BulkLoadingDaoSupport;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicBulkExportResult;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.util.JsonUtils;

/**
 * MyBatis implementation of {@link GeneralNodeDatumDao}.
 * 
 * @author matt
 * @version 2.5
 */
public class MyBatisGeneralNodeDatumDao
		extends BaseMyBatisGenericDao<GeneralNodeDatum, GeneralNodeDatumPK> implements
		FilterableDao<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK, GeneralNodeDatumFilter>,
		GeneralNodeDatumDao, ConfigurableBulkLoadingDao {

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

	/** The query parameter for a general filter object value. */
	public static final String PARAM_FILTER = "filter";

	/**
	 * The query parameter for {@link Period} object value.
	 * 
	 * @since 1.9
	 */
	public static final String PARAM_TOLERANCE = "tolerance";

	/**
	 * The query parameter for a generic qualifier to pass to queries.
	 * 
	 * @since 1.20
	 */
	public static final String PARAM_QUALIFIER = "qualifier";

	/**
	 * The query parameter for a {@link CombiningConfig} data structure.
	 * 
	 * @since 1.5
	 */
	public static final String PARAM_COMBINING = "combine";

	/**
	 * The query parameter for a list of {@link NodeSourceRange} objects.
	 * 
	 * @since 2.1
	 */
	public static final String PARAM_RANGES = "ranges";

	/**
	 * The qualifier for a "within" style query.
	 * 
	 * @since 1.20
	 */
	public static final String QUALIFIER_WITHIN = "within";

	/**
	 * The default query name used for
	 * {@link #getReportableInterval(Long, String)}.
	 */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-general-reportable-interval";

	/**
	 * The default query name used for
	 * {@link #getAvailableSources(GeneralNodeDatumFilter)}.
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
	 * The default query name for {@link #getAuditInterval(Long, String)}.
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
	 * {@link #calculateAt(GeneralNodeDatumFilter, LocalDateTime, Period)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_AT = "find-general-reporting-at-local";

	/**
	 * The default query name for
	 * {@link #calculateAt(GeneralNodeDatumFilter, DateTime, Period)}.
	 * 
	 * @since 1.13
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_AT_ABSOLUTE = "find-general-reporting-at";

	/**
	 * The default query name for
	 * {@link #calculateBetween(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_BETWEEN = "find-general-reporting-between-local";

	/**
	 * The default query name for
	 * {@link #calculateBetween(GeneralNodeDatumFilter, DateTime, DateTime, Period)}.
	 * 
	 * @since 1.13
	 */
	public static final String QUERY_FOR_DATUM_RECORDS_BETWEEN_ABSOLUTE = "find-general-reporting-between";

	/**
	 * The default query name for
	 * {@link #findAccumulation(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}.
	 * 
	 * @since 1.9
	 */
	public static final String QUERY_FOR_DATUM_ACCUMULATION = "find-general-reporting-diff-within-local";

	/**
	 * The default query name for
	 * {@link #findAccumulation(GeneralNodeDatumFilter, DateTime, DateTime, Period)}.
	 * 
	 * @since 1.13
	 */
	public static final String QUERY_FOR_DATUM_ACCUMULATION_ABSOLUTE = "find-general-reporting-diff-within";

	/**
	 * The default value for the {@code BulkLoadingDaoSupport.jdbcCall}
	 * property.
	 * 
	 * @since 1.11
	 */
	public static final String DEFAULT_BULK_LOADING_JDBC_CALL = "{call solardatum.store_datum(?, ?, ?, ?, ?, FALSE)}";

	/**
	 * The default query name for the
	 * {@link #countDatumRecords(GeneralNodeDatumFilter)} method.
	 * 
	 * @since 1.12
	 */
	public static final String QUERY_FOR_DATUM_RECORD_COUNTS = "find-datum-record-counts-for-filter";

	/**
	 * The default query name for the
	 * {@link #deleteFiltered(GeneralNodeDatumFilter)} method.
	 * 
	 * @since 1.12
	 */
	public static final String DELETE_FILTERED = "delete-GeneralNodeDatum-for-filter";

	/**
	 * The default value for the {@code updateDatumRangeDates} property.
	 * 
	 * @since 1.17
	 */
	public static final String UPDATE_DATUM_RANGE_DATES = "update-datum-range-dates";

	/**
	 * The default query name for the
	 * {@link #markDatumAggregatesStale(GeneralNodeDatumFilter)}.
	 * 
	 * @since 1.18
	 */
	public static final String UPDATE_AGGREGATES_STALE = "update-GeneralNodeDatum-aggregates-stale";

	/**
	 * The default query name for marking a node/source/range as stale..
	 * 
	 * @since 2.4
	 */
	public static final String UPDATE_AGGREGATES_STALE_RANGE = "update-GeneralNodeDatum-aggregates-stale-range";

	/**
	 * The default query name for the
	 * {@link #markDatumAggregatesStale(GeneralNodeDatumFilter)}.
	 * 
	 * @since 1.18
	 */
	public static final String QUERY_FOR_AGGREGATES_STALE = "find-StaleAggregateDatum-for-filter";

	/**
	 * The query name suffix added to "partial" aggregation style queries.
	 * 
	 * @since 2.1
	 */
	public static final String QUERY_PARTIAL_AGGREGATION_SUFFIX = "-partial";

	/**
	 * The {@code maxMinuteAggregationHours} property default value.
	 * 
	 * <p>
	 * This represents a 5 week time span to that full months can be queried.
	 * </p>
	 * 
	 * @since 2.2
	 */
	public static final int DEFAULT_MAX_MINUTE_AGG_HOURS = (24 * 7 * 5);

	private final BulkLoadingDaoSupport loadingSupport;

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
	private String queryForDatumAtAbsolute;
	private String queryForDatumBetweenAbsolute;
	private String queryForDatumAccumulationAbsolute;
	private String queryForDatumRecordCounts;
	private String deleteFiltered;
	private String updateDatumRangeDates;
	private String updateDatumAggregatesStale;
	private String updateDatumAggregatesStaleRange;
	private String queryForAggregatesStale;
	private ObjectMapper filterObjectMapper;
	private int maxMinuteAggregationHours;

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
		this.queryForDatumAtAbsolute = QUERY_FOR_DATUM_RECORDS_AT_ABSOLUTE;
		this.queryForDatumBetweenAbsolute = QUERY_FOR_DATUM_RECORDS_BETWEEN_ABSOLUTE;
		this.queryForDatumAccumulationAbsolute = QUERY_FOR_DATUM_ACCUMULATION_ABSOLUTE;
		this.loadingSupport = new BulkLoadingDaoSupport(log);
		this.loadingSupport.setJdbcCall(DEFAULT_BULK_LOADING_JDBC_CALL);
		this.queryForDatumRecordCounts = QUERY_FOR_DATUM_RECORD_COUNTS;
		this.deleteFiltered = DELETE_FILTERED;
		this.updateDatumRangeDates = UPDATE_DATUM_RANGE_DATES;
		this.updateDatumAggregatesStale = UPDATE_AGGREGATES_STALE;
		this.updateDatumAggregatesStaleRange = UPDATE_AGGREGATES_STALE_RANGE;
		this.queryForAggregatesStale = QUERY_FOR_AGGREGATES_STALE;
		this.maxMinuteAggregationHours = DEFAULT_MAX_MINUTE_AGG_HOURS;
	}

	/**
	 * Store datum.
	 * 
	 * <p>
	 * This method throws an {@link UnsupportedOperationException}. Use
	 * {@link net.solarnetwork.central.datum.v2.dao.mybatis.MyBatisDatumEntityDao#save(net.solarnetwork.central.datum.v2.dao.DatumEntity)}
	 * instead.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to store
	 */
	@Override
	public GeneralNodeDatumPK store(GeneralNodeDatum datum) {
		throw new UnsupportedOperationException(
				"Update calling code to use the net.solarnetwork.central.datum.v2.dao.DatumEntityDao API!");
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

	/**
	 * Get the filter query name for a reading query.
	 * 
	 * @param filter
	 *        the filter
	 * @param type
	 *        the reading type
	 * @return query name
	 */
	protected String getQueryForReadingFilter(GeneralNodeDatumFilter filter, DatumReadingType type) {
		Aggregation aggregation = null;
		if ( filter instanceof AggregationFilter ) {
			aggregation = ((AggregationFilter) filter).getAggregation();
		}
		if ( aggregation.compareTo(Aggregation.Hour) < 0 ) {
			// all *Minute aggregates are mapped to the Minute query name
			aggregation = Aggregation.Minute;
		}
		return ("findall-general-reading-" + type + "-ReportingGeneralNodeDatum-"
				+ aggregation.toString());
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
		final Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		final List<NodeSourceRange> ranges = partialAggregationRanges(filter);
		if ( ranges != null ) {
			sqlProps.put(PARAM_RANGES, ranges);
		}
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		setupAggregationParam(filter, sqlProps);
		//postProcessAggregationFilterProperties(filter, sqlProps);

		final Aggregation agg = filter.getAggregation();
		if ( agg != null && agg.getLevel() > 0 && agg.compareLevel(Aggregation.Hour) < 0 ) {
			// make sure start/end date provided for minute level aggregation queries as query expects it
			setupMinuteAggregationTimeRange(filter, agg, sqlProps);
		} else if ( agg != null && agg == Aggregation.RunningTotal && filter.getSourceId() == null ) {
			// source ID is required for RunningTotal currently
			throw new IllegalArgumentException("sourceId is required for RunningTotal aggregation");
		}

		CombiningConfig combining = getCombiningFilterProperties(filter);
		if ( combining != null ) {
			sqlProps.put(PARAM_COMBINING, combining);
		}

		final String query = getQueryForFilter(filter)
				+ (ranges != null ? QUERY_PARTIAL_AGGREGATION_SUFFIX : "");

		// attempt count first, if NOT mostRecent query and max NOT specified as -1
		// and NOT a *DayOfWeek, or *HourOfDay, or RunningTotal aggregate levels
		Long totalCount = null;
		if ( !filter.isMostRecent() && !filter.isWithoutTotalResultsCount()
				&& (max == null || max.intValue() != -1) && agg != Aggregation.DayOfWeek
				&& agg != Aggregation.SeasonalDayOfWeek && agg != Aggregation.HourOfDay
				&& agg != Aggregation.SeasonalHourOfDay && agg != Aggregation.RunningTotal ) {
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

	private static DateTimeFieldType dateTimeFieldType(Aggregation agg) {
		final DateTimeFieldType field;
		switch (agg) {
			case Year:
				field = DateTimeFieldType.year();
				break;

			case Month:
				field = DateTimeFieldType.monthOfYear();
				break;

			case Day:
				field = DateTimeFieldType.dayOfMonth();
				break;

			case Hour:
				field = DateTimeFieldType.hourOfDay();
				break;

			default:
				field = null;
				break;
		}
		return field;
	}

	public static List<NodeSourceRange> partialAggregationRanges(
			AggregateGeneralNodeDatumFilter filter) {
		final Aggregation main = filter.getAggregation();
		final Aggregation partial = filter.getPartialAggregation();
		if ( main == null || partial == null || partial.compareLevel(main) >= 0 ) {
			return null;
		}
		final DateTimeFieldType mainField = dateTimeFieldType(main);
		final DateTimeFieldType partialField = dateTimeFieldType(partial);
		if ( mainField == null || partialField == null ) {
			return null;
		}
		List<NodeSourceRange> result = new ArrayList<>(3);
		if ( filter.getLocalStartDate() != null ) {
			LocalDateTime start = filter.getLocalStartDate();
			LocalDateTime end = (filter.getLocalEndDate() != null ? filter.getLocalEndDate()
					: new LocalDateTime().property(partialField).roundFloorCopy());
			LocalDateTime curr = start.property(mainField).roundFloorCopy();
			if ( curr.isBefore(start) ) {
				curr = curr.property(mainField).addToCopy(1);
				if ( curr.isAfter(end) ) {
					curr = end.property(partialField).roundFloorCopy();
				}
				result.add(NodeSourceRange.range(start.property(partialField).roundFloorCopy(), curr,
						partial));
			}
			LocalDateTime next = end.property(mainField).roundFloorCopy();
			if ( curr.isBefore(end) ) {
				if ( curr.isBefore(next) ) {
					result.add(NodeSourceRange.range(curr, next, main));
					curr = next;
				}
				next = end.property(partialField).roundFloorCopy();
				if ( curr.isBefore(next) ) {
					result.add(NodeSourceRange.range(curr, next, partial));
				}
			}
		} else if ( filter.getStartDate() != null ) {
			DateTime start = filter.getStartDate();
			DateTime end = (filter.getEndDate() != null ? filter.getEndDate()
					: new DateTime().property(partialField).roundFloorCopy());
			DateTime curr = start.property(mainField).roundFloorCopy();
			if ( curr.isBefore(start) ) {
				curr = curr.property(mainField).addToCopy(1);
				if ( curr.isAfter(end) ) {
					curr = end.property(partialField).roundFloorCopy();
				}
				result.add(NodeSourceRange.range(start.property(partialField).roundFloorCopy(), curr,
						partial));
			}
			DateTime next = end.property(mainField).roundFloorCopy();
			if ( curr.isBefore(end) ) {
				if ( curr.isBefore(next) ) {
					result.add(NodeSourceRange.range(curr, next, main));
					curr = next;
				}
				next = end.property(partialField).roundFloorCopy();
				if ( curr.isBefore(next) ) {
					result.add(NodeSourceRange.range(curr, next, partial));
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReadableInterval getReportableInterval(Long nodeId, String sourceId) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		filter.setSourceId(sourceId);
		NodeSourceRange range = selectFirst(this.queryForReportableInterval, filter);
		return (range != null ? range.getInterval() : null);
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
	 * @since 1.13
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> calculateAt(GeneralNodeDatumFilter filter,
			DateTime date, Period tolerance) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_DATE, date);
		sqlProps.put(PARAM_TOLERANCE, tolerance);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryForDatumAtAbsolute, sqlProps, null,
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
	 * @since 1.13
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> calculateBetween(GeneralNodeDatumFilter filter,
			DateTime from, DateTime to, Period tolerance) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_START_DATE, from);
		sqlProps.put(PARAM_END_DATE, to);
		sqlProps.put(PARAM_TOLERANCE, tolerance);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryForDatumBetweenAbsolute, sqlProps,
				null, null);
		return new BasicFilterResults<ReportingGeneralNodeDatumMatch>(rows, (long) rows.size(), 0,
				rows.size());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.15
	 */
	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAggregationFilteredReadings(
			AggregateGeneralNodeDatumFilter filter, DatumReadingType type, Period tolerance,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		final String query = getQueryForReadingFilter(filter, type);
		final Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		setupAggregationParam(filter, sqlProps);

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
				log.debug("Unsupported query [{}]", query, e);
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

	private FilterResults<ReportingGeneralNodeDatumMatch> findAccumulation(GeneralNodeDatumFilter filter,
			Object from, Object to, Period tolerance, String qualifier) {
		assert from != null;
		Map<String, Object> sqlProps = new HashMap<String, Object>(4);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put(PARAM_START_DATE, from);
		sqlProps.put(PARAM_END_DATE, to);
		if ( tolerance != null ) {
			sqlProps.put(PARAM_TOLERANCE, tolerance);
		}
		if ( qualifier != null ) {
			sqlProps.put(PARAM_QUALIFIER, qualifier);
		}
		String queryName = (from instanceof ReadablePartial ? queryForDatumAccumulation
				: queryForDatumAccumulationAbsolute);
		List<ReportingGeneralNodeDatumMatch> rows = selectList(queryName, sqlProps, null, null);
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
		return findAccumulation(filter, from, to, tolerance, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.13
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAccumulation(GeneralNodeDatumFilter filter,
			DateTime from, DateTime to, Period tolerance) {
		return findAccumulation(filter, from, to, tolerance, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.20
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAccumulationWithin(
			GeneralNodeDatumFilter filter, LocalDateTime from, LocalDateTime to, Period tolerance) {
		return findAccumulation(filter, from, to, tolerance, QUALIFIER_WITHIN);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.20
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralNodeDatumMatch> findAccumulationWithin(
			GeneralNodeDatumFilter filter, DateTime from, DateTime to, Period tolerance) {
		return findAccumulation(filter, from, to, tolerance, QUALIFIER_WITHIN);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.12
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter) {
		Map<String, Object> sqlProps = new HashMap<>(2);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put("filterJson", filterJson(filter));
		return selectFirst(queryForDatumRecordCounts, sqlProps);
	}

	private String filterJson(Object filter) {
		if ( filter != null ) {
			ObjectMapper mapper = getFilterObjectMapper();
			try {
				return (mapper != null ? mapper.writeValueAsString(filter)
						: JsonUtils.getJSONString(filter, null));
			} catch ( Exception e ) {
				log.warn("Error mapping filter {} to JSON: {}", filter, e.toString());
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.12
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long deleteFiltered(GeneralNodeDatumFilter filter) {
		Map<String, Object> sqlProps = new HashMap<>(2);
		sqlProps.put(PARAM_FILTER, filter);
		sqlProps.put("filterJson", filterJson(filter));
		return selectLong(deleteFiltered, sqlProps);
	}

	private class GeneralNodeDatumBulkLoadingContext
			extends BulkLoadingDaoSupport.BulkLoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> {

		private final Timestamp start;

		private final Map<NodeSourcePK, NodeSourceRange> dateRanges = new HashMap<>(32);

		private GeneralNodeDatumBulkLoadingContext(LoadingOptions options,
				LoadingExceptionHandler<GeneralNodeDatum, GeneralNodeDatumPK> exceptionHandler)
				throws SQLException {
			loadingSupport.super(options, exceptionHandler);
			start = new Timestamp(System.currentTimeMillis());
		}

		@Override
		protected boolean doLoad(GeneralNodeDatum d, PreparedStatement stmt, long index)
				throws SQLException {
			stmt.setTimestamp(1, new Timestamp(d.getCreated().getMillis()));
			stmt.setLong(2, d.getNodeId());
			stmt.setString(3, d.getSourceId());
			stmt.setTimestamp(4,
					d.getPosted() != null ? new Timestamp(d.getPosted().getMillis()) : start);
			stmt.setString(5, d.getSampleJson());
			stmt.executeUpdate();

			// keep track of import min/max date ranges, so they can be updated at end
			NodeSourcePK nsKey = new NodeSourcePK(d.getNodeId(), d.getSourceId());
			dateRanges.compute(nsKey, (k, v) -> {
				NodeSourceRange r = v;
				if ( r == null ) {
					r = new NodeSourceRange();
					r.setNodeId(k.getNodeId());
					r.setSourceId(k.getSourceId());
				}
				if ( r.getStartDate() == null || d.getCreated().isBefore(r.getStartDate()) ) {
					r.setStartDate(d.getCreated());
				}
				if ( r.getEndDate() == null || d.getCreated().isAfter(r.getEndDate()) ) {
					r.setEndDate(d.getCreated());
				}
				return r;
			});

			return true;
		}

		@Override
		public void commit() {
			commitDateRanges();
			super.commit();
		}

		private void commitDateRanges() {
			DatumFilterCommand filter = new DatumFilterCommand();
			Map<String, Object> staleRangeParams = Collections.singletonMap(PARAM_FILTER, filter);
			GeneralNodeDatumPK key = new GeneralNodeDatumPK();
			for ( NodeSourceRange range : dateRanges.values() ) {
				key.setNodeId(range.getNodeId());
				key.setSourceId(range.getSourceId());

				key.setCreated(range.getStartDate());
				getSqlSession().update(updateDatumRangeDates, key);

				if ( !range.getStartDate().isEqual(range.getEndDate()) ) {
					key.setCreated(range.getEndDate());
					getSqlSession().update(updateDatumRangeDates, key);
				}

				// mark the range of data as stale for aggregate processing
				filter.setNodeId(range.getNodeId());
				filter.setSourceId(range.getSourceId());
				filter.setStartDate(range.getStartDate());
				filter.setEndDate(range.getEndDate());
				getSqlSession().update(updateDatumAggregatesStaleRange, staleRangeParams);
			}
			dateRanges.clear();
		}

	}

	/**
	 * Bulk import general node datum.
	 * 
	 * {@inheritDoc}
	 * 
	 * <p>
	 * This implementation relies on the JDBC statement configured in
	 * {@link BulkLoadingDaoSupport#setJdbcCall(String)}. That statement must
	 * accept the following parameters, in the following order:
	 * </p>
	 * 
	 * <ol>
	 * <li>datum date - the date of the datum as a {@link Timestamp}</li>
	 * <li>node ID</li>
	 * <li>source ID</li>
	 * <li>post date - the "post" or import date as a {@link Timestamp}</li>
	 * <li>sample data - the datum sample data, in JSON form</li>
	 * </ol>
	 * 
	 * @since 1.11
	 */
	@Override
	public LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> createBulkLoadingContext(
			LoadingOptions options,
			LoadingExceptionHandler<GeneralNodeDatum, GeneralNodeDatumPK> exceptionHandler) {
		try {
			return new GeneralNodeDatumBulkLoadingContext(options, exceptionHandler);
		} catch ( SQLException e ) {
			throw new SQLExceptionSubclassTranslator()
					.translate("Bulk loading [" + options.getName() + "]", null, e);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.14
	 */
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public ExportResult batchExport(ExportCallback<GeneralNodeDatumFilterMatch> callback,
			ExportOptions options) {
		if ( options == null ) {
			throw new IllegalArgumentException("ExportOptions is required");
		}

		Map<String, Object> params = options.getParameters();

		// filter
		GeneralNodeDatumFilter filter = (params != null
				&& params.get("filter") instanceof GeneralNodeDatumFilter
						? (GeneralNodeDatumFilter) params.get("filter")
						: null);
		if ( filter == null ) {
			throw new IllegalArgumentException(
					"GeneralNodeDatumFilter is required for parameter 'filter'");
		}

		// sorts
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<SortDescriptor> sortDescriptors = (params != null && params.get("sorts") instanceof List
				? (List) params.get("sorts")
				: null);

		Map<String, Object> sqlProps = new HashMap<String, Object>(2);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}

		Aggregation agg = null;
		if ( filter instanceof AggregationFilter ) {
			agg = ((AggregationFilter) filter).getAggregation();
		}

		if ( filter.isMostRecent() && agg != null ) {
			throw new IllegalArgumentException(
					"Aggregation not allowed on a filter for most recent datum");
		}

		if ( agg != null ) {
			setupAggregationParam(filter, sqlProps);

			if ( agg.getLevel() > 0 && agg.compareLevel(Aggregation.Hour) < 0 ) {
				// make sure start/end date provided for minute level aggregation queries as query expects it
				setupMinuteAggregationTimeRange(filter, agg, sqlProps);
			} else if ( agg == Aggregation.RunningTotal && filter.getSourceId() == null ) {
				// source ID is required for RunningTotal currently
				throw new IllegalArgumentException("sourceId is required for RunningTotal aggregation");
			}
		}

		// combining
		CombiningConfig combining = getCombiningFilterProperties(filter);
		if ( combining != null ) {
			sqlProps.put(PARAM_COMBINING, combining);
		}

		// get query name to execute
		String query = getQueryForFilter(filter);

		// attempt count first, if NOT mostRecent query and NOT a *Minute, *DayOfWeek, or *HourOfDay, or RunningTotal aggregate levels
		Long totalCount = null;
		if ( agg == null ) {
			agg = Aggregation.None;
		}
		if ( !filter.isMostRecent() && !filter.isWithoutTotalResultsCount()
				&& (agg.getLevel() < 1 || agg.compareTo(Aggregation.Hour) >= 0)
				&& agg != Aggregation.DayOfWeek && agg != Aggregation.SeasonalDayOfWeek
				&& agg != Aggregation.HourOfDay && agg != Aggregation.SeasonalHourOfDay
				&& agg != Aggregation.RunningTotal ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		callback.didBegin(totalCount);

		ExportResultHandler handler = new ExportResultHandler(callback);
		getSqlSession().select(query, sqlProps, handler);
		return new BasicBulkExportResult(handler.getCount());
	}

	/**
	 * Configure the start/end dates required by minute aggregation queries.
	 * 
	 * <p>
	 * 
	 * This method will enforce the configured
	 * {@link #getMaxMinuteAggregationHours()} setting, truncating the requested
	 * time range to that many hours if needed.
	 * </p>
	 * 
	 * @param filter
	 *        the filter that provides the requested start/end dates
	 * @param agg
	 *        the aggregate level
	 * @param sqlProps
	 *        the query parameters to populate with {@link #PARAM_START_DATE}
	 *        and {@link #PARAM_END_DATE} properties
	 * @throws IllegalArgumentException
	 *         if {@code agg} is less than {@link Aggregation#FiveMinute}
	 * @since 2.2
	 */
	private void setupMinuteAggregationTimeRange(GeneralNodeDatumFilter filter, Aggregation agg,
			Map<String, Object> sqlProps) {
		if ( agg.compareLevel(Aggregation.FiveMinute) < 0 ) {
			throw new IllegalArgumentException(
					"Must be FiveMinute aggregation or more. For finer granularity results, request without any aggregation.");
		}

		DateTime start = filter.getStartDate();
		DateTime end = filter.getEndDate();
		if ( start == null ) {
			start = new DateTime().minuteOfHour().roundFloorCopy();
		}
		if ( end == null ) {
			int minutes = agg.getLevel() / 60;
			end = new DateTime();
			end = end.withMinuteOfHour((end.getMinuteOfHour() / minutes) * minutes).minuteOfHour()
					.roundFloorCopy();
		}

		// restrict Minute level aggregation to maximum length
		final int maxHours = getMaxMinuteAggregationHours();
		if ( maxHours > 0 ) {
			long hours = new Duration(start, end).getStandardHours();
			if ( hours > maxHours ) {
				throw new IllegalArgumentException(
						"Minute level aggregation time span must be at most " + maxHours + " hours.");
			}
		}

		sqlProps.put(PARAM_START_DATE, start);
		sqlProps.put(PARAM_END_DATE, end);
	}

	private static class ExportResultHandler implements ResultHandler<GeneralNodeDatumFilterMatch> {

		private final ExportCallback<GeneralNodeDatumFilterMatch> callback;
		private long count = 0;

		private ExportResultHandler(ExportCallback<GeneralNodeDatumFilterMatch> callback) {
			super();
			this.callback = callback;
		}

		@Override
		public void handleResult(ResultContext<? extends GeneralNodeDatumFilterMatch> context) {
			GeneralNodeDatumFilterMatch match = context.getResultObject();
			count++;
			ExportCallbackAction action = callback.handle(match);
			if ( action == ExportCallbackAction.STOP ) {
				context.stop();
			}
		}

		private long getCount() {
			return count;
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.18
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void markDatumAggregatesStale(GeneralNodeDatumFilter criteria) {
		Map<String, Object> params = Collections.singletonMap(PARAM_FILTER, criteria);
		getSqlSession().update(updateDatumAggregatesStale, params);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.19
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public FilterResults<StaleAggregateDatum> findStaleAggregateDatum(GeneralNodeDatumFilter criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		Map<String, Object> sqlProps = sqlParametersForStaleAggregateDatum(criteria, sortDescriptors);

		// execute count query first, if max NOT specified as -1
		Long totalCount = null;
		if ( (max == null || (max != null && max.intValue() != -1))
				&& !criteria.isWithoutTotalResultsCount() ) {
			totalCount = executeCountQuery(queryForAggregatesStale + "-count", sqlProps);
		}

		// execute actual query
		List<StaleAggregateDatum> rows = selectList(queryForAggregatesStale, sqlProps, offset, max);

		BasicFilterResults<StaleAggregateDatum> results = new BasicFilterResults<StaleAggregateDatum>(
				rows,
				(totalCount != null ? totalCount
						: criteria.isWithoutTotalResultsCount() ? null : Long.valueOf(rows.size())),
				offset, rows.size());
		return results;
	}

	private Map<String, Object> sqlParametersForStaleAggregateDatum(GeneralNodeDatumFilter filter,
			List<SortDescriptor> sortDescriptors) {
		final Map<String, Object> sqlProps = new HashMap<String, Object>(3);
		sqlProps.put(PARAM_FILTER, filter);

		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		return sqlProps;
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
	 * {@link #getAuditCountTotal(GeneralNodeDatumFilter)} method.
	 * 
	 * @param statementName
	 *        the statement name
	 * @since 1.2
	 */
	public void setQueryForAuditHourlyPropertyCount(String statementName) {
		this.queryForAuditHourlyPropertyCount = statementName;
	}

	/**
	 * Set the statement name for the {@link #getAuditInterval(Long, String)}
	 * method.
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
	 * {@link #calculateAt(GeneralNodeDatumFilter, LocalDateTime, Period)}
	 * method.
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
	 * {@link #findAccumulation(GeneralNodeDatumFilter, LocalDateTime, LocalDateTime, Period)}
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

	/**
	 * Get the bulk loading support.
	 * 
	 * @return the support
	 * @since 1.11
	 */
	@Override
	public BulkLoadingDaoSupport getLoadingSupport() {
		return loadingSupport;
	}

	/**
	 * Set the statement name for the
	 * {@link #countDatumRecords(GeneralNodeDatumFilter)} method.
	 * 
	 * @param queryForDatumRecordCounts
	 *        the statement name; defaults to
	 *        {@link QUERY_FOR_DATUM_RECORD_COUNTS}
	 * @since 1.12
	 */
	public void setQueryForDatumRecordCounts(String queryForDatumRecordCounts) {
		this.queryForDatumRecordCounts = queryForDatumRecordCounts;
	}

	/**
	 * Set the statement name for the
	 * {@link #deleteFiltered(GeneralNodeDatumFilter)} method.
	 * 
	 * @param deleteFiltered
	 *        the statement name; defaults to {@link DELETE_FILTERED}
	 * @since 1.12
	 */
	public void setDeleteDatumRecordCounts(String deleteFiltered) {
		this.deleteFiltered = deleteFiltered;
	}

	/**
	 * Set the statement name for the
	 * {@link #calculateAt(GeneralNodeDatumFilter, DateTime, Period)} method.
	 * 
	 * @param queryForDatumAtAbsolute
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_RECORDS_AT_ABSOLUTE}
	 * @since 1.13
	 */
	public void setQueryForDatumAtAbsolute(String queryForDatumAtAbsolute) {
		this.queryForDatumAtAbsolute = queryForDatumAtAbsolute;
	}

	/**
	 * Set the statement name for the
	 * {@link #calculateBetween(GeneralNodeDatumFilter, DateTime, DateTime, Period)}
	 * method.
	 * 
	 * @param queryForDatumBetweenAbsolute
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_RECORDS_BETWEEN_ABSOLUTE}
	 * @since 1.13
	 */
	public void setQueryForDatumBetweenAbsolute(String queryForDatumBetweenAbsolute) {
		this.queryForDatumBetweenAbsolute = queryForDatumBetweenAbsolute;
	}

	/**
	 * Set the statement name for the
	 * {@link #findAccumulation(GeneralNodeDatumFilter, DateTime, DateTime, Period)}
	 * method.
	 * 
	 * @param queryForDatumAccumulationAbsolute
	 *        the statement name; defaults to
	 *        {@link #QUERY_FOR_DATUM_ACCUMULATION_ABSOLUTE}
	 * @since 1.13
	 */
	public void setQueryForDatumAccumulationAbsolute(String queryForDatumAccumulationAbsolute) {
		this.queryForDatumAccumulationAbsolute = queryForDatumAccumulationAbsolute;
	}

	/**
	 * Set the statement name for updating datum date ranges.
	 * 
	 * @param updateDatumRangeDates
	 *        the statement name; defaults to {@link #UPDATE_DATUM_RANGE_DATES}
	 * @since 1.17
	 */
	public void setUpdateDatumRangeDates(String updateDatumRangeDates) {
		this.updateDatumRangeDates = updateDatumRangeDates;
	}

	/**
	 * Set the statement name for updating a set of datum data's aggregates as
	 * stale.
	 * 
	 * @param updateDatumAggregatesStale
	 *        the query name; defaults to {@link #UPDATE_AGGREGATES_STALE}
	 * @since 1.18
	 */
	public void setUpdateDatumAggregatesStale(String updateDatumAggregatesStale) {
		this.updateDatumAggregatesStale = updateDatumAggregatesStale;
	}

	/**
	 * Set the statement name for updating a range of datum data's aggregates as
	 * stale.
	 * 
	 * @param updateDatumAggregatesStaleRange
	 *        the query name; defaults to {@link #UPDATE_AGGREGATES_STALE_RANGE}
	 * @since 2.4
	 */
	public void setUpdateDatumAggregatesStaleRange(String updateDatumAggregatesStaleRange) {
		this.updateDatumAggregatesStaleRange = updateDatumAggregatesStaleRange;
	}

	/**
	 * Set the statement name for querying a set of datum data's stale
	 * aggregates.
	 * 
	 * @param queryForAggregatesStale
	 *        the query name; defaults to {@link #QUERY_FOR_AGGREGATES_STALE}
	 * @since 1.19
	 */
	public void setQueryForAggregatesStale(String queryForAggregatesStale) {
		this.queryForAggregatesStale = queryForAggregatesStale;
	}

	/**
	 * Get the filter object mapper.
	 * 
	 * @return the object mapper
	 * @since 1.21
	 */
	public ObjectMapper getFilterObjectMapper() {
		return filterObjectMapper;
	}

	/**
	 * Set the filter object mapper.
	 * 
	 * <p>
	 * If this is left unconfigured
	 * {@link JsonUtils#getJSONString(Object, String)} will be used to serialize
	 * filters to JSON.
	 * </p>
	 * 
	 * @param filterObjectMapper
	 *        the mapper to use
	 */
	public void setFilterObjectMapper(ObjectMapper filterObjectMapper) {
		this.filterObjectMapper = filterObjectMapper;
	}

	/**
	 * Get the maximum number of hours to allow in minute-level aggregation
	 * queries.
	 * 
	 * @return the maximum hours; defaults to
	 *         {@link #DEFAULT_MAX_MINUTE_AGG_HOURS}
	 * @since 2.2
	 */
	public int getMaxMinuteAggregationHours() {
		return maxMinuteAggregationHours;
	}

	/**
	 * Set the maximum number of hours to allow in minute-level aggregation
	 * queries.
	 * 
	 * @param maxMinuteAggregationHours
	 *        the maximum hours to set; anything less than {@literal 1} means
	 *        there is no maximum
	 * @since 2.2
	 */
	public void setMaxMinuteAggregationHours(int maxMinuteAggregationHours) {
		this.maxMinuteAggregationHours = maxMinuteAggregationHours;
	}

}
