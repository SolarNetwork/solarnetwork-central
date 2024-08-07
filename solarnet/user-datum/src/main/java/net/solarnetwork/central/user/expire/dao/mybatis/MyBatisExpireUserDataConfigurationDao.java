/* ==================================================================
 * MyBatisExpireUserDataConfigurationDao.java - 9/07/2018 11:16:11 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.dao.mybatis;

import java.util.List;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.user.dao.mybatis.BaseMyBatisUserRelatedGenericDao;
import net.solarnetwork.central.user.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.domain.ExpireUserDataConfiguration;

/**
 * MyBatis implementation of {@link ExpireUserDataConfigurationDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisExpireUserDataConfigurationDao
		extends BaseMyBatisUserRelatedGenericDao<ExpireUserDataConfiguration, Long>
		implements ExpireUserDataConfigurationDao {

	/** The query name used for {@link #findConfigurationsForUser(Long)}. */
	public static final String QUERY_CONFIGURATIONS_FOR_USER = "find-ExpireUserDataConfiguration-for-user";

	/**
	 * The query name used for
	 * {@link #countExpiredDataForConfiguration(ExpireUserDataConfiguration)}.
	 */
	public static final String QUERY_COUNTS_FOR_CONFIG = "count-expired-data-for-ExpireUserDataConfiguration";

	/**
	 * The query name used for
	 * {@link #deleteExpiredDataForConfiguration(ExpireUserDataConfiguration)}.
	 */
	public static final String QUERY_DELETE_FOR_CONFIG = "delete-expired-data-for-ExpireUserDataConfiguration";

	/**
	 * Default constructor.
	 */
	public MyBatisExpireUserDataConfigurationDao() {
		super(ExpireUserDataConfiguration.class, Long.class);
	}

	@Override
	public List<ExpireUserDataConfiguration> findConfigurationsForUser(Long userId) {
		return selectList(QUERY_CONFIGURATIONS_FOR_USER, userId, null, null);
	}

	@Override
	public DatumRecordCounts countExpiredDataForConfiguration(ExpireUserDataConfiguration config) {
		return selectFirst(QUERY_COUNTS_FOR_CONFIG, config);
	}

	@Override
	public long deleteExpiredDataForConfiguration(ExpireUserDataConfiguration config) {
		return selectLong(QUERY_DELETE_FOR_CONFIG, config);
	}

}
