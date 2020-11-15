/* ==================================================================
 * MyBatisAuditDatumEntityDao.java - 14/11/2020 5:04:26 pm
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.AuditDatumDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * MyBatis implementation of {@link AuditDatumDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class MyBatisAuditDatumEntityDao extends BaseMyBatisDao implements AuditDatumDao {

	/** The query parameter for an {@code Aggregation} string value. */
	public static final String AGGREGATION_PROPERTY = "aggregation";

	/** The query parameter for an {@code Aggregation} string value. */
	public static final String ROLLUPS_PROPERTY = "rollups";

	/** Query name enumeration. */
	public enum QueryName {

		AuditDatumForFilter("find-AuditDatumEntity-for-filter"),

		AccumulativeAuditDatumForFilter("find-AuditDatumEntity-acc-for-filter");

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

	private Aggregation aggregationForAuditDatumCriteria(AuditDatumCriteria filter) {
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

	private Map<String, Object> sqlParametersForAuditDatumCriteria(AuditDatumCriteria filter) {
		final Map<String, Object> sqlProps = new HashMap<String, Object>(3);
		sqlProps.put(BaseMyBatisGenericDaoSupport.FILTER_PROPERTY, filter);

		if ( filter.getSorts() != null && filter.getSorts().size() > 0 ) {
			sqlProps.put(BaseMyBatisGenericDaoSupport.SORT_DESCRIPTORS_PROPERTY, filter.getSorts());
		}

		// limit aggregation to specific supported ones
		Aggregation aggregation = aggregationForAuditDatumCriteria(filter);
		sqlProps.put(AGGREGATION_PROPERTY, aggregation.name());

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
				sqlProps.put(ROLLUPS_PROPERTY, rollups);
			}
		}
		return sqlProps;
	}

	private FilterResults<AuditDatumRollup, DatumPK> filter(AuditDatumCriteria filter, QueryName query) {
		final String queryName = query.getQueryName();
		final Map<String, Object> sqlProps = sqlParametersForAuditDatumCriteria(filter);
		final List<AuditDatumRollup> data = selectList(queryName, sqlProps, null, null);
		return new BasicFilterResults<>(data);

	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter) {
		return filter(filter, QueryName.AuditDatumForFilter);
	}

	@Override
	public FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter) {
		return filter(filter, QueryName.AccumulativeAuditDatumForFilter);
	}

}
