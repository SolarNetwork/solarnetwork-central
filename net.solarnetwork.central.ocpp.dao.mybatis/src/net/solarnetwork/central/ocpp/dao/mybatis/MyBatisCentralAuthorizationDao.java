/* ==================================================================
 * MyBatisCentralAuthorizationDao.java - 25/02/2020 2:16:55 pm
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
import java.util.Collection;
import org.springframework.dao.DataRetrievalFailureException;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * MyBatis implementation of {@link CentralAuthorizationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralAuthorizationDao extends BaseMyBatisGenericDaoSupport<Authorization, Long>
		implements CentralAuthorizationDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Delete an authorization for a given user ID and ID. */
		DeleteForUserAndId("delete-CentralAuthorization-for-user-and-id");

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

	/**
	 * Constructor.
	 */
	public MyBatisCentralAuthorizationDao() {
		super(CentralAuthorization.class, Long.class);
	}

	@Override
	public Authorization getForToken(String token) {
		throw new UnsupportedOperationException("Must call getForToken(userId,token) instead.");
	}

	@Override
	public Authorization getForToken(Long userId, String token) {
		return selectFirst(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new CentralAuthorization(userId, null, token)));
	}

	@Override
	public Collection<CentralAuthorization> findAllForOwner(Long userId) {
		return selectList(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new CentralAuthorization(userId)), null, null);
	}

	@Override
	public CentralAuthorization get(Long userId, Long id) {
		CentralAuthorization result = selectFirst(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new CentralAuthorization(id, userId)));
		if ( result == null ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
		return result;
	}

	@Override
	public void delete(Long userId, Long id) {
		int count = getLastUpdateCount(getSqlSession().delete(
				QueryName.DeleteForUserAndId.getQueryName(), new CentralAuthorization(id, userId)));
		if ( count < 1 ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
	}

}
