/* ==================================================================
 * MyBatisCentralChargePointDao.java - 25/02/2020 2:16:55 pm
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

package net.solarnetwork.central.ocpp.dao.mybatis;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataRetrievalFailureException;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralChargePointFilter;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;

/**
 * MyBatis implementation of {@link CentralChargePointDao}.
 *
 * @author matt
 * @version 1.1
 */
public class MyBatisCentralChargePointDao extends
		BaseMyBatisFilterableDaoSupport<ChargePoint, Long, CentralChargePoint, CentralChargePointFilter>
		implements CentralChargePointDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Get a charge point based on a given {@link ChargePointIdentity}. */
		GetForIdentity("get-CentralChargePoint-for-identity"),

		/** Delete a charge point for a given user ID and ID. */
		DeleteForUserAndId("delete-CentralChargePoint-for-user-and-id");

		private final String queryName;

		QueryName(String queryName) {
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

	/**
	 * Constructor.
	 */
	public MyBatisCentralChargePointDao() {
		super(CentralChargePoint.class, Long.class, CentralChargePoint.class);
	}

	@Override
	public ChargePoint getForIdentity(ChargePointIdentity identity) {
		return selectFirst(QueryName.GetForIdentity.getQueryName(), identity);
	}

	@Override
	public ChargePoint getForIdentifier(Long userId, String identifier) {
		return selectFirst(getQueryForAll(), singletonMap(FILTER_PROPERTY,
				new CentralChargePoint(null, userId, null, null, new ChargePointInfo(identifier))));
	}

	@Override
	public Collection<CentralChargePoint> findAllForOwner(Long userId) {
		return selectList(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new CentralChargePoint(userId, null)), null, null);
	}

	@Override
	public CentralChargePoint get(Long userId, Long id) {
		CentralChargePoint result = selectFirst(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new CentralChargePoint(id, userId, null)));
		if ( result == null ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
		return result;
	}

	@Override
	public void delete(Long userId, Long id) {
		int count = getLastUpdateCount(getSqlSession().delete(
				QueryName.DeleteForUserAndId.getQueryName(), new CentralChargePoint(id, userId, null)));
		if ( count < 1 ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
	}

	@Override
	public FilterResults<CentralChargePoint, Long> findFiltered(CentralChargePointFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(), "filter.userId");
		return doFindFiltered(filter, sorts, offset, max);
	}

}
