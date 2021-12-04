/* ==================================================================
 * MyBatisChargePointSettingsDao.java - 27/02/2020 4:31:58 pm
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
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.ChargePointSettings;

/**
 * MyBatis implementation of {@link ChargePointSettingsDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisChargePointSettingsDao extends
		BaseMyBatisGenericDaoSupport<ChargePointSettings, Long> implements ChargePointSettingsDao {

	/** Query name enumeration. */
	public enum QueryName {

		/** Delete a settings for a given user ID and ID. */
		DeleteForUserAndId("delete-ChargePointSettings-for-user-and-id"),

		/** Resolve settings using user settings for defaults. */
		ResolveSettings("resolve-ChargePointSettings-for-id");

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
	public MyBatisChargePointSettingsDao() {
		super(ChargePointSettings.class, Long.class);
	}

	@Override
	protected boolean isAssignedPrimaryKeys() {
		return true;
	}

	@Override
	public Collection<ChargePointSettings> findAllForOwner(Long userId) {
		return selectList(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new ChargePointSettings(userId)), null, null);
	}

	@Override
	public ChargePointSettings get(Long userId, Long id) {
		ChargePointSettings result = selectFirst(getQueryForAll(),
				singletonMap(FILTER_PROPERTY, new ChargePointSettings(id, userId)));
		if ( result == null ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
		return result;
	}

	@Override
	protected Long saveWithAssignedPrimaryKey(ChargePointSettings entity) {
		// we override because we have no INSERT statement; if the update returns 0
		// that means we have an invalid user ID and we just ignore
		getSqlSession().update(getUpdate(), entity);
		return entity.getId();
	}

	@Override
	public void delete(Long userId, Long id) {
		int count = getLastUpdateCount(getSqlSession().delete(
				QueryName.DeleteForUserAndId.getQueryName(), new CentralAuthorization(id, userId)));
		if ( count < 1 ) {
			throw new DataRetrievalFailureException("Entity not found.");
		}
	}

	@Override
	public ChargePointSettings resolveSettings(Long userId, Long id) {
		return selectFirst(QueryName.ResolveSettings.getQueryName(),
				new ChargePointSettings(id, userId));
	}

}
