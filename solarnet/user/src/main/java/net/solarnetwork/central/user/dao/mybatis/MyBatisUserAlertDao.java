/* ==================================================================
 * MyBatisUserAlertDao.java - 16/05/2015 4:23:44 pm
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertType;

/**
 * MyBatis implementation of {@link UserAlertDao}.
 * 
 * @author matt
 * @version 2.1
 */
public class MyBatisUserAlertDao extends BaseMyBatisGenericDao<UserAlert, Long> implements UserAlertDao {

	/**
	 * The query name used for
	 * {@link #findAlertsToProcess(UserAlertType, Long, Instant, Integer)}.
	 */
	public static final String QUERY_FOR_PROCESSING = "find-UserAlert-for-processing";

	/** The query name used for {@link #findAlertsForUser(Long)}. */
	public static final String QUERY_FOR_USER_WITH_SITUATION = "find-UserAlert-for-user-with-situation";

	/** The query name used for {@link #getAlertSituation(Long)}. */
	public static final String QUERY_FOR_SITUATION = "get-UserAlert-with-situation";

	/** The query name used for {@link #deleteAllAlertsForNode(Long, Long)}. */
	public static final String DELETE_FOR_NODE = "delete-UserAlert-for-node";

	/** The query name used for {@link #updateValidTo(Long, Instant)}. */
	public static final String UPDATE_VALID_TO = "update-UserAlert-valid-to";

	/**
	 * The query name used for {@link #findActiveAlertSituationsForNode(Long)}.
	 */
	public static final String QUERY_ACTIVE_SITUATIONS_FOR_NODE = "find-UserAlert-active-for-node";

	/**
	 * The query name used for {@link #findActiveAlertSituationsForUser(Long)}.
	 */
	public static final String QUERY_ACTIVE_SITUATIONS_FOR_USER = "find-UserAlert-active-for-user";

	/** The query name used for {@link #alertSituationCountForUser(Long)}. */
	public static final String QUERY_ACTIVE_SITUATIONS_FOR_USER_COUNT = "find-UserAlert-active-for-user-count";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAlertDao() {
		super(net.solarnetwork.central.user.domain.UserAlert.class, Long.class);
	}

	@Override
	public List<UserAlert> findAlertsToProcess(UserAlertType type, Long startingId, Instant validDate,
			Integer max) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("type", type);
		if ( startingId != null ) {
			params.put("startingId", startingId);
		}
		params.put("validDate", (validDate == null ? Instant.now() : validDate));
		return selectList(QUERY_FOR_PROCESSING, params, null, max);
	}

	@Override
	public List<UserAlert> findAlertsForUser(Long userId) {
		return selectList(QUERY_FOR_USER_WITH_SITUATION, userId, null, null);
	}

	@Override
	public int deleteAllAlertsForNode(Long userId, Long nodeId) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("user", userId);
		params.put("node", nodeId);
		return getSqlSession().delete(DELETE_FOR_NODE, params);
	}

	@Override
	public UserAlert getAlertSituation(Long alertId) {
		return selectFirst(QUERY_FOR_SITUATION, alertId);
	}

	@Override
	public void updateValidTo(Long alertId, Instant validTo) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("id", alertId);
		params.put("validDate", (validTo == null ? Instant.now() : validTo));
		getSqlSession().update(UPDATE_VALID_TO, params);
	}

	@Override
	public List<UserAlert> findActiveAlertSituationsForUser(Long userId) {
		return selectList(QUERY_ACTIVE_SITUATIONS_FOR_USER, userId, null, null);
	}

	@Override
	public List<UserAlert> findActiveAlertSituationsForNode(Long nodeId) {
		return selectList(QUERY_ACTIVE_SITUATIONS_FOR_NODE, nodeId, null, null);
	}

	@Override
	public int alertSituationCountForUser(Long userId) {
		Number n = getSqlSession().selectOne(QUERY_ACTIVE_SITUATIONS_FOR_USER_COUNT, userId);
		if ( n != null ) {
			return n.intValue();
		}
		return 0;
	}

}
