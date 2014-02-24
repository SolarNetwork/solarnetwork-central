/* ===================================================================
 * IbatisConsumptionDatumDao.java
 * 
 * Created Jul 29, 2009 10:51:10 AM
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

import java.util.List;
import java.util.Map;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.domain.AggregateNodeDatumFilter;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.ConsumptionDatumMatch;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatumFilter;
import net.solarnetwork.central.datum.domain.ReportingConsumptionDatum;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * iBATIS implementation of {@link ConsumptionDatumDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class IbatisConsumptionDatumDao extends
		IbatisFilterableDatumDatoSupport<ConsumptionDatum, ConsumptionDatumMatch, NodeDatumFilter>
		implements ConsumptionDatumDao {

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * minute-precise results.
	 */
	public static final String QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_MINUTE = "find-ConsumptionDatum-for-agg-minute";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * hour-precise results.
	 */
	public static final String QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_HOUR = "find-ConsumptionDatum-for-agg-hour";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * day-precise results.
	 */
	public static final String QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_DAY = "find-ConsumptionDatum-for-agg-day";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * week-precise results.
	 */
	public static final String QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_WEEK = "find-ConsumptionDatum-for-agg-week";

	/**
	 * The query name used in
	 * {@link #setupAggregatedDatumQuery(DatumQueryCommand, Map)} for
	 * month-precise results.
	 */
	public static final String QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_MONTH = "find-ConsumptionDatum-for-agg-month";

	private static final long MS_PER_WEEK = 1000L * 60L * 60L * 24L * 7L;
	private static final long MS_PER_8WEEKS = MS_PER_WEEK * 8L;

	/**
	 * Default constructor.
	 */
	public IbatisConsumptionDatumDao() {
		super(ConsumptionDatum.class, ConsumptionDatumMatch.class);
	}

	@Override
	protected String setupAggregatedDatumQuery(DatumQueryCommand criteria, Map<String, Object> params) {
		long timeDiff = criteria.getEndDate().getMillis() - criteria.getStartDate().getMillis();
		String queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_MINUTE;
		if ( criteria.getAggregate() != null ) {
			// if criteria specifies aggregate, use that
			switch (criteria.getAggregate()) {
				case Month:
					queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_MONTH;
					break;

				case Week:
					queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_WEEK;
					break;

				case Day:
					queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_DAY;
					break;

				case Hour:
					queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_HOUR;
					break;

				default:
					// default back to MINUTE level
			}
		} else if ( timeDiff > MS_PER_8WEEKS ) {
			queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_WEEK;
		} else if ( timeDiff > MS_PER_WEEK ) {
			queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_DAY;
		} else if ( criteria.getPrecision() != null && criteria.getPrecision() >= 60 ) {
			queryName = QUERY_CONSUMPTION_DATUM_FOR_AGGREGATE_BY_HOUR;
		} else {
			params.put("precision", criteria.getPrecision());
		}
		return queryName;
	}

	@Override
	public FilterResults<ReportingConsumptionDatum> findAggregationFiltered(
			AggregateNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		// TODO Auto-generated method stub
		return null;
	}

}
