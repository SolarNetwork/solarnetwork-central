/* ==================================================================
 * IbatisGeneralNodeDatumDao.java - Aug 22, 2014 7:16:29 AM
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
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.NodeDatumFilter;
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
 * iBATIS implementation of {@link GeneralNodeDatumDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisGeneralNodeDatumDao extends
		IbatisBaseGenericDaoSupport<GeneralNodeDatum, GeneralNodeDatumPK> implements
		FilterableDao<GeneralNodeDatumMatch, GeneralNodeDatumPK, NodeDatumFilter>, GeneralNodeDatumDao {

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

	/** The query parameter for a general {@link Filter} object value. */
	public static final String PARAM_FILTER = "filter";

	/** The default query name used for {@link #getReportableInterval(Long)}. */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-general-reportable-interval";

	/**
	 * The default query name used for
	 * {@link #getAvailableSources(Long, DateTime, DateTime)}.
	 */
	public static final String QUERY_FOR_DISTINCT_SOURCES = "find-general-distinct-sources";

	private String queryForReportableInterval;
	private String queryForDistinctSources;

	/**
	 * Default constructor.
	 */
	public IbatisGeneralNodeDatumDao() {
		super(GeneralNodeDatum.class, GeneralNodeDatumPK.class);
		this.queryForReportableInterval = QUERY_FOR_REPORTABLE_INTERVAL;
		this.queryForDistinctSources = QUERY_FOR_DISTINCT_SOURCES;
	}

	/**
	 * Get the filter query name for a given domain.
	 * 
	 * @param filterDomain
	 *        the domain
	 * @return query name
	 */
	protected String getQueryForFilter() {
		return getQueryForAll() + "-GeneralNodeDatumMatch";
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<GeneralNodeDatumMatch> findFiltered(NodeDatumFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		final String query = getQueryForFilter();
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(PARAM_FILTER, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		//postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			final String countQuery = query + "-count";
			Number n = null;
			n = (Number) getSqlMapClientTemplate().queryForObject(countQuery, sqlProps, Number.class);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<GeneralNodeDatumMatch> rows = executeQueryForList(query, sqlProps, offset, max);

		//rows = postProcessFilterQuery(filter, rows);

		BasicFilterResults<GeneralNodeDatumMatch> results = new BasicFilterResults<GeneralNodeDatumMatch>(
				rows, (totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> executeQueryForList(final String query, Map<String, Object> sqlProps,
			Integer offset, Integer max) {
		List<T> rows = null;
		if ( offset != null && offset >= 0 && max != null && max > 0 ) {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps, offset, max);
		} else {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps);
		}
		return rows;
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
		getSqlMapClientTemplate().queryForObject(this.queryForReportableInterval, params);
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

}
