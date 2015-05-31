/* ==================================================================
 * MyBatisUserAlertSituationDao.java - 16/05/2015 5:23:40 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.mybatis;

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserAlertSituationDao;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis implementation of {@link UserAlertSituationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserAlertSituationDao extends BaseMyBatisGenericDao<UserAlertSituation, Long>
		implements UserAlertSituationDao {

	/**
	 * The query name used for {@link #getActiveAlertSituationForAlert(Long)}.
	 */
	public static final String QUERY_ACTIVE_FOR_ALERT = "get-UserAlertSituation-for-active-alert";

	/**
	 * The DELETE query name used for {@link #purgeResolvedSituations(DateTime)}
	 * .
	 */
	public static final String UPDATE_PURGE_RESOLVED = "delete-UserAlertSituation-resolved";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAlertSituationDao() {
		super(UserAlertSituation.class, Long.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserAlertSituation getActiveAlertSituationForAlert(Long alertId) {
		return selectFirst(QUERY_ACTIVE_FOR_ALERT, alertId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long purgeResolvedSituations(DateTime olderThanDate) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("date", olderThanDate);
		getSqlSession().update(UPDATE_PURGE_RESOLVED, params);
		Long result = (Long) params.get("result");
		return (result == null ? 0 : result.longValue());
	}

}
