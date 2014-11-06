/* ==================================================================
 * IbatisGeneralLocationDatumDao.java - Oct 17, 2014 3:36:40 PM
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

package net.solarnetwork.central.datum.dao.ibatis;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.ibatis.IbatisBaseGenericDaoSupport;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.ReadableInterval;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * iBATIS implementation of {@link GeneralLocationDatumDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisGeneralLocationDatumDao extends
		IbatisBaseGenericDaoSupport<GeneralLocationDatum, GeneralLocationDatumPK>
		implements
		FilterableDao<GeneralLocationDatumFilterMatch, GeneralLocationDatumPK, GeneralLocationDatumFilter>,
		GeneralLocationDatumDao {

	/** The query parameter for a class name value. */
	public static final String PARAM_CLASS_NAME = "class";

	/** The query parameter for a location ID value. */
	public static final String PARAM_LOC_ID = "location";

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

	/** The query parameter for a general {@link Filter} object value. */
	public static final String PARAM_FILTER = "filter";

	/** The default query name used for {@link #getReportableInterval(Long)}. */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-general-loc-reportable-interval";

	/**
	 * The default query name used for
	 * {@link #getAvailableSources(Long, DateTime, DateTime)}.
	 */
	public static final String QUERY_FOR_DISTINCT_SOURCES = "find-general-loc-distinct-sources";

	/**
	 * The default query name used for
	 * {@link #findFiltered(GeneralLocationDatumFilter, List, Integer, Integer)}
	 * where {@link GeneralLocationDatumFilter#isMostRecent()} is set to
	 * <em>true</em>.
	 */
	public static final String QUERY_FOR_MOST_RECENT = "find-general-loc-most-recent";

	private String queryForReportableInterval;
	private String queryForDistinctSources;
	private String queryForMostRecent;

	/**
	 * Default constructor.
	 */
	public IbatisGeneralLocationDatumDao() {
		super(GeneralLocationDatum.class, GeneralLocationDatumPK.class);
		this.queryForReportableInterval = QUERY_FOR_REPORTABLE_INTERVAL;
		this.queryForDistinctSources = QUERY_FOR_DISTINCT_SOURCES;
		this.queryForMostRecent = QUERY_FOR_MOST_RECENT;
	}

	/**
	 * Get the filter query name for a given domain.
	 * 
	 * @param filter
	 *        the filter
	 * @return query name
	 */
	protected String getQueryForFilter(GeneralLocationDatumFilter filter) {
		if ( filter.isMostRecent() ) {
			return queryForMostRecent;
		}
		Aggregation aggregation = null;
		if ( filter instanceof AggregationFilter ) {
			aggregation = ((AggregationFilter) filter).getAggregation();
		}
		if ( aggregation == null ) {
			return getQueryForAll() + "-GeneralLocationDatumMatch";
		} else if ( aggregation.compareTo(Aggregation.Hour) < 0 ) {
			// all *Minute aggregates are mapped to the Minute query name
			aggregation = Aggregation.Minute;
		}
		return (getQueryForAll() + "-ReportingGeneralLocationDatum-" + aggregation.toString());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<GeneralLocationDatumFilterMatch> findFiltered(
			GeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		final String query = getQueryForFilter(filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		//postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1 and NOT a mostRecent query
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && filter.isMostRecent() == false ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<GeneralLocationDatumFilterMatch> rows = executeQueryForList(query, sqlProps, offset, max);

		//rows = postProcessFilterQuery(filter, rows);

		BasicFilterResults<GeneralLocationDatumFilterMatch> results = new BasicFilterResults<GeneralLocationDatumFilterMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<ReportingGeneralLocationDatumMatch> findAggregationFiltered(
			AggregateGeneralLocationDatumFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		final String query = getQueryForFilter(filter);
		final Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		//postProcessAggregationFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		// and NOT a *Minute, *DayOfWeek, or *HourOfDay aggregate level
		Long totalCount = null;
		final Aggregation agg = filter.getAggregation();
		if ( max != null && max.intValue() != -1 && agg.compareTo(Aggregation.Hour) >= 0
				&& agg != Aggregation.DayOfWeek && agg != Aggregation.SeasonalDayOfWeek
				&& agg != Aggregation.HourOfDay && agg != Aggregation.SeasonalHourOfDay ) {
			totalCount = executeCountQuery(query + "-count", sqlProps);
		}

		List<ReportingGeneralLocationDatumMatch> rows = executeQueryForList(query, sqlProps, offset, max);

		// rows = postProcessAggregationFilterQuery(filter, rows);

		BasicFilterResults<ReportingGeneralLocationDatumMatch> results = new BasicFilterResults<ReportingGeneralLocationDatumMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	private Long executeCountQuery(final String countQueryName, final Map<String, ?> sqlProps) {
		Number n = (Number) getSqlMapClientTemplate().queryForObject(countQueryName, sqlProps,
				Number.class);
		if ( n != null ) {
			return n.longValue();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> executeQueryForList(final String query, Map<String, Object> sqlProps,
			Integer offset, Integer max) {
		List<T> rows = null;
		if ( max != null && max > 0 ) {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps,
					(offset == null || offset.intValue() < 0 ? 0 : offset.intValue()), max);
		} else {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps);
		}
		return rows;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public ReadableInterval getReportableInterval(Long locationId, String sourceId) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( locationId != null ) {
			params.put(PARAM_LOC_ID, locationId);
		}
		if ( sourceId != null ) {
			params.put(PARAM_SOURCE_ID, sourceId);
		}
		getSqlMapClientTemplate().queryForObject(this.queryForReportableInterval, params);
		Timestamp start = (Timestamp) params.get("ts_start");
		Timestamp end = (Timestamp) params.get("ts_end");
		if ( start == null || end == null ) {
			return null;
		}
		long d1 = start.getTime();
		long d2 = end.getTime();
		DateTimeZone tz = null;
		if ( params.containsKey("location_tz") ) {
			tz = DateTimeZone.forID(params.get("location_tz").toString());
		}
		Interval interval = new Interval(d1 < d2 ? d1 : d2, d2 > d1 ? d2 : d1, tz);
		return interval;

	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getAvailableSources(Long locationId, DateTime start, DateTime end) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( locationId != null ) {
			params.put(PARAM_LOC_ID, locationId);
		}
		if ( start != null ) {
			params.put(PARAM_START_DATE, start);
		}
		if ( end != null ) {
			params.put(PARAM_END_DATE, end);
		}
		@SuppressWarnings("unchecked")
		List<String> results = getSqlMapClientTemplate().queryForList(this.queryForDistinctSources,
				params);
		return new LinkedHashSet<String>(results);
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

}
