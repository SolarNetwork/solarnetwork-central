/* ==================================================================
 * MyBatisPriceDatumDao.java - Nov 13, 2014 6:58:56 AM
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.dao.PriceDatumDao;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.PriceDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * MyBatis implementation of {@link PriceDatumDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisPriceDatumDao extends BaseMyBatisDatumDao<PriceDatum> implements PriceDatumDao {

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * minute-precise results.
	 */
	public static final String QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_MINUTE = "find-PriceDatum-for-agg-minute";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * hour-precise results.
	 */
	public static final String QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_HOUR = "find-PriceDatum-for-agg-hour";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * day-precise results.
	 */
	public static final String QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_DAY = "find-PriceDatum-for-agg-day";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * week-precise results.
	 */
	public static final String QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_WEEK = "find-PriceDatum-for-agg-week";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * month-precise results.
	 */
	public static final String QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_MONTH = "find-PriceDatum-for-agg-month";

	/**
	 * Query to find the first available PriceDatum within a date range.
	 */
	public static final String QUERY_PRICE_DATUM_MIN = "find-PriceDatum-min";

	private static final long MS_PER_WEEK = 1000L * 60L * 60L * 24L * 7L;
	private static final long MS_PER_8WEEKS = MS_PER_WEEK * 8L;

	/**
	 * Default constructor.
	 */
	public MyBatisPriceDatumDao() {
		super(PriceDatum.class);
	}

	@Override
	protected String setupAggregatedDatumQuery(DatumQueryCommand criteria, Map<String, Object> params) {
		long timeDiff = criteria.getEndDate().getMillis() - criteria.getStartDate().getMillis();
		String queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_MINUTE;
		if ( criteria.getAggregate() != null && criteria.getAggregate() != Aggregation.Minute ) {
			// if criteria specifies aggregate, use that
			switch (criteria.getAggregate()) {
				case Month:
					queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_MONTH;
					break;

				case Week:
					queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_WEEK;
					break;

				case Day:
					queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_DAY;
					break;

				case Hour:
					queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_HOUR;
					break;

				default:
					// default back to MINUTE level
			}
		} else if ( timeDiff > MS_PER_8WEEKS ) {
			queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_WEEK;
		} else if ( timeDiff > MS_PER_WEEK ) {
			queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_DAY;
		} else if ( criteria.getPrecision() != null && criteria.getPrecision() >= 60 ) {
			queryName = QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_HOUR;
		} else {
			params.put("precision", criteria.getPrecision());
		}
		if ( criteria.getLocationId() == null ) {
			// the location ID required but not provided, so look for the first
			// one found for this node within the given date range to try to use.
			// This is ONLY for a backwards-compatibility hack, when price data
			// was tied to nodes.

			if ( log.isWarnEnabled() ) {
				log.warn("PriceDatum query does not provide location ID, "
						+ "falling back to hack to first available location. Query params: " + params);
			}

			Map<String, Object> minPriceLocParams = new HashMap<String, Object>(params);
			minPriceLocParams.put("_PriceLocationRequired", Boolean.TRUE);
			PriceDatum min = getSqlSession().selectOne(QUERY_PRICE_DATUM_MIN, minPriceLocParams);
			if ( min == null ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Minimum PriceDatum not found from query [" + QUERY_PRICE_DATUM_MIN
							+ "] and parameters " + minPriceLocParams);
				}
				return null;
			}
			params.put(PARAM_LOCATION_ID, min.getLocationId());
		}
		return queryName;
	}

	@Override
	protected List<PriceDatum> postProcessDatumQuery(DatumQueryCommand criteria,
			Map<String, Object> params, List<PriceDatum> results) {
		// fill in the currency / unit for results using the "minimum"
		// (i.e. earliest result) currency and unit values. this logic
		// might need to evolve into something a bit more clever later...
		if ( results.size() < 1 ) {
			return results;
		}
		/*
		 * FIXME PriceDatum min =
		 * (PriceDatum)getSqlMapClientTemplate().queryForObject(
		 * QUERY_PRICE_DATUM_MIN_FOR_NODE, params); if ( min == null ) { if (
		 * log.isDebugEnabled() ) {
		 * log.debug("Minimum PriceDatum not found from query ["
		 * +QUERY_PRICE_DATUM_FOR_AGGREGATE_BY_MINUTE +"] and parameters "
		 * +params); } return results; }
		 * 
		 * for ( PriceDatum pd : results ) { pd.setCurrency(min.getCurrency());
		 * pd.setUnit(min.getUnit()); }
		 */
		return results;
	}

}
