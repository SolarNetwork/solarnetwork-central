/* ===================================================================
 * IbatisDatumDaoSupport.java
 * 
 * Created Jul 29, 2009 11:09:09 AM
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

package net.solarnetwork.central.datum.dao.ibatis;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.solarnetwork.central.dao.ibatis.IbatisGenericDaoSupport;
import net.solarnetwork.central.datum.dao.DatumDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;

import org.joda.time.MutableInterval;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInterval;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for standardized Ibatis DAO implementations.
 *
 * @author matt
 * @version $Revision$ $Date$
 * @param <T> the domain object type
 */
public abstract class IbatisDatumDaoSupport<T extends Datum>
extends IbatisGenericDaoSupport<T> implements DatumDao<T> {
	
	/**
	 * The {@link DatumQueryCommand#getProperties()} key for a 
	 * {@link ConsumptionDatum#getSourceId()} to filter the query by.
	 * 
	 * <p>This can be used to limit the results to just a specific source. The key
	 * value is expected to be a String.</p>
	 * 
	 * @deprecated use {@link DatumQueryCommand#getSourceIds()}
	 */
	@Deprecated
	public static final String CRITERIA_PROPERTY_SOURCE_ID = "sourceId";
	
	/** 
	 * The query name used for {@link #getDatumForDate(Long, ReadableDateTime)}.
	 * 
	 * <p>This query expects two named parameters: {@code node} (a Long value) 
	 * and {@code date} a {@link Timestamp} value).</p>
	 */
	public static final String QUERY_DATUM_FOR_NODE_AND_DATE = "find-%s-for-date";
	
	/** The query name used for {@link #getDatum(Long)}. */
	public static final String QUERY_FOR_REPORTABLE_INTERVAL = "find-reportable-interval";
	
	/** The query name used for {@link #getMostRecentDatum(DatumQueryCommand)}. */
	public static final String QUERY_FOR_MOST_RECENT = "find-%s-most-recent";
	
	/** The query parameter for a class name value. */
	public static final String PARAM_CLASS_NAME = "class";
	
	/** The query parameter for a node ID value. */
	public static final String PARAM_NODE_ID = "node";
	
	/** The query parameter for a location ID value. */
	public static final String PARAM_LOCATION_ID = "location";
	
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
	
	private String queryForNodeAndDate;
	private String queryForReportableInterval;
	private String queryForMostRecent;
	
	/**
	 * Constructor.
	 * 
	 * @param domainClass
	 */
	public IbatisDatumDaoSupport(Class<? extends T> domainClass) {
		super(domainClass);
		this.queryForNodeAndDate = String.format(QUERY_DATUM_FOR_NODE_AND_DATE, 
				domainClass.getSimpleName());
		this.queryForReportableInterval = QUERY_FOR_REPORTABLE_INTERVAL;
		this.queryForMostRecent = String.format(QUERY_FOR_MOST_RECENT, domainClass.getSimpleName());
	}

	public Class<? extends T> getDatumType() {
		return getObjectType();
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public T getDatum(Long id) {
		return get(id);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public T getDatumForDate(Long id, ReadableDateTime date) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(PARAM_ID, id);
		params.put(PARAM_DATE, new Timestamp(date.getMillis()));
		List<T> results = getSqlMapClientTemplate().queryForList(
				this.queryForNodeAndDate, params);
		if ( results != null && results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Long storeDatum(T datum) {
		return store(datum);
	}
	
	@Override
	public void delete(T domainObject) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("deprecation")
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<T> getAggregatedDatum(DatumQueryCommand criteria) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( criteria.getNodeId() != null ) {
			params.put(PARAM_NODE_ID, criteria.getNodeId());
		}
		if ( criteria.getLocationId() != null ) {
			params.put(PARAM_LOCATION_ID, criteria.getLocationId());
		}
		if ( criteria.getSourceIds() != null && criteria.getSourceIds().length > 0 ) {
			params.put("sources", criteria.getSourceIds());
		} else if ( criteria.getProperties() != null 
				&& criteria.getProperties().containsKey(CRITERIA_PROPERTY_SOURCE_ID) ) {
			params.put("source", criteria.getProperties().get(CRITERIA_PROPERTY_SOURCE_ID));
		}
		params.put(PARAM_START_DATE, new java.sql.Timestamp(criteria.getStartDate().getMillis()));
		params.put(PARAM_END_DATE, new java.sql.Timestamp(criteria.getEndDate().getMillis()));
		String queryName = setupAggregatedDatumQuery(criteria, params);
		if ( queryName == null ) {
			return Collections.emptyList();
		}
		@SuppressWarnings("unchecked")
		List<T> results = getSqlMapClientTemplate().queryForList(queryName, params);
		results = postProcessDatumQuery(criteria, params, results);
		return results;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<T> getMostRecentDatum(DatumQueryCommand criteria) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( criteria.getNodeId() != null ) {
			params.put(PARAM_NODE_ID, criteria.getNodeId());
		}
		if ( criteria.getLocationId() != null ) {
			params.put(PARAM_LOCATION_ID, criteria.getLocationId());
		}
		if ( criteria.getSourceId() != null ) {
			params.put(PARAM_SOURCE_ID, criteria.getSourceId());
		}
		if ( criteria.getEndDate() != null ) {
			params.put(PARAM_DATE, new java.sql.Timestamp(criteria.getEndDate().getMillis()));
		}
		@SuppressWarnings("unchecked")
		List<T> results = getSqlMapClientTemplate().queryForList(queryForMostRecent, params);
		results = postProcessDatumQuery(criteria, params, results);
		return results;
	}

	/**
	 * Setup aggregate query parameters and return the name of the query to use.
	 * 
	 * <p>When this method is called by {@link #getAggregatedDatum(DatumQueryCommand)}
	 * the {@code params} will have the following values set:</p>
	 * 
	 * <ul>
	 *   <li><b>node</b> - the node ID</li>
	 *   <li><b>start</b> - the start date, as a {@link Timestamp}</li>
	 *   <li><b>end</b> - the end date, as a {@link Timestamp}</li>
	 * </li>
	 * 
	 * <p>This method can add/remove params as needed, and then should return the
	 * name of the query to execute.</p>
	 * 
	 * @param criteria the criteria
	 * @param params the query parameters
	 * @return the query name
	 */
	protected abstract String setupAggregatedDatumQuery(DatumQueryCommand criteria, 
			Map<String, Object> params);

	/**
	 * Post-process query results.
	 * 
	 * <p>This method is called by {@link #getAggregatedDatum(DatumQueryCommand)}.
	 * This implementation does nothing but return the {@code results} passed in.
	 * Extending classes may like to do something with the results.</p>
	 * 
	 * @param criteria the query criteria
	 * @param params the query params
	 * @param results the query results
	 * @return the post-processed results
	 */
	protected List<T> postProcessDatumQuery(DatumQueryCommand criteria,
			Map<String, Object> params, List<T> results) {
		return results;
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ReadableInterval getReportableInterval(Long nodeId) {
		Map<String, Object> params = new HashMap<String, Object>();
		if ( nodeId != null ) {
			params.put(PARAM_NODE_ID, nodeId);
		}
		params.put(PARAM_CLASS_NAME, getDomainClass().getSimpleName());
		List<? extends Date> results = getSqlMapClientTemplate().queryForList(
				this.queryForReportableInterval, params);
		if ( results.size() < 2 || results.get(0) == null ) {
			return null;
		}
		long d1 = results.get(0).getTime();
		long d2 = results.get(1).getTime();
		MutableInterval interval = new MutableInterval(
				d1 < d2 ? d1 : d2,
				d2 > d1 ? d2 : d1
				);
		return interval;
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ReadableInterval getReportableInterval() {
		return getReportableInterval(null);
	}

	/**
	 * @return the queryForNodeAndDate
	 */
	public String getQueryForNodeAndDate() {
		return queryForNodeAndDate;
	}
	
	/**
	 * @param queryForNodeAndDate the queryForNodeAndDate to set
	 */
	public void setQueryForNodeAndDate(String queryForNodeAndDate) {
		this.queryForNodeAndDate = queryForNodeAndDate;
	}
	
	/**
	 * @return the queryForReportableInterval
	 */
	public String getQueryForReportableInterval() {
		return queryForReportableInterval;
	}
	
	/**
	 * @param queryForReportableInterval the queryForReportableInterval to set
	 */
	public void setQueryForReportableInterval(String queryForReportableInterval) {
		this.queryForReportableInterval = queryForReportableInterval;
	}

}
