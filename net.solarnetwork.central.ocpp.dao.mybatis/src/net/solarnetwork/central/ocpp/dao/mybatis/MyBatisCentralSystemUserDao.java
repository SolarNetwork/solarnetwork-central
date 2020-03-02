/* ==================================================================
 * MyBatisSystemUserDao.java - 24/02/2020 9:11:47 pm
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
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.ocpp.dao.CentralSystemUserDao;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.ocpp.dao.SystemUserDao;
import net.solarnetwork.ocpp.domain.SystemUser;

/**
 * MyBatis implementation of {@link SystemUserDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisCentralSystemUserDao extends BaseMyBatisGenericDaoSupport<SystemUser, Long>
		implements CentralSystemUserDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Get a system user for a given username. */
		GetForUsername("get-CentralSystemUser-for-username");

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
	public MyBatisCentralSystemUserDao() {
		super(CentralSystemUser.class, Long.class);
	}

	@Override
	public SystemUser getForUsername(String username) {
		return selectFirst(QueryName.GetForUsername.getQueryName(), username);
	}

	@Override
	public Collection<CentralSystemUser> findAllForOwner(Long userId) {
		return selectList(getQueryForAll(), singletonMap(FILTER_PROPERTY, new CentralSystemUser(userId)),
				null, null);
	}

}
