/* ==================================================================
 * MyBatisReadingDatumEntityDao.java - 17/11/2020 8:05:54 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.mybatis;

import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.FilterResults;

/**
 * MyBatis implementation of {@link ReadingDatumDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class MyBatisReadingDatumEntityDao extends BaseMyBatisDao implements ReadingDatumDao {

	/** The query parameter for an {@code Aggregation} string value. */
	public static final String AGGREGATION_PROPERTY = "aggregation";

	/** Query name enumeration. */
	public enum QueryName {

		ReadingDatumForFilter("find-ReadingDatumEntity-for-filter");

		private final String queryName;

		private QueryName(String queryName) {
			this.queryName = queryName;
		}

		/**
		 * Get the query name.
		 * 
		 * @return the query name
		 */
		public String getQueryName() {
			return queryName;
		}
	}

	private static Aggregation aggregationForReadingDatumCriteria(ReadingDatumCriteria filter) {
		// limit aggregation to specific supported ones
		Aggregation aggregation = null;
		if ( filter != null && filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Hour:
				case Day:
				case Month:
					aggregation = filter.getAggregation();
					break;

				default:
					// force to Day for any other
					aggregation = Aggregation.Day;
			}
		}
		return aggregation;
	}

	private void processSqlParametersForReadingDatumCriteria(ReadingDatumCriteria filter,
			Map<String, Object> sqlProps) {
		// limit aggregation to specific supported ones
		Aggregation aggregation = aggregationForReadingDatumCriteria(filter);
		if ( aggregation != null ) {
			sqlProps.put(AGGREGATION_PROPERTY, aggregation.name());
		}
	}

	@Override
	public FilterResults<ReadingDatum, DatumPK> findDatumReadingFiltered(ReadingDatumCriteria filter) {
		final String queryName = QueryName.ReadingDatumForFilter.getQueryName();
		return selectFiltered(queryName, filter, null, null, null,
				this::processSqlParametersForReadingDatumCriteria);
	}

}
