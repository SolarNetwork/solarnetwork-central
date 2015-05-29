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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserAlertDao;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyBatis implementation of {@link UserAlertDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisUserAlertDao extends BaseMyBatisGenericDao<UserAlert, Long> implements UserAlertDao {

	/**
	 * The query name used for
	 * {@link #findAlertsToProcess(UserAlertType, Long, Integer)}.
	 */
	public static final String QUERY_FOR_PROCESSING = "find-UserAlert-for-processing";

	/** The query name used for {@link #findAlertsForUser(Long)}. */
	public static final String QUERY_FOR_USER_WITH_SITUATION = "find-UserAlert-for-user-with-situation";

	/** The query name used for {@link #getAlertSituation(Long)}. */
	public static final String QUERY_FOR_SITUATION = "get-UserAlert-with-situation";

	/** The query name used for {@link #deleteAllAlertsForNode(Long, Long)}. */
	public static final String DELETE_FOR_NODE = "delete-UserAlert-for-node";

	/** The query name used for {@link #updateValidTo(Long, DateTime)}. */
	public static final String UPDATE_VALID_TO = "update-UserAlert-valid-to";

	/**
	 * Default constructor.
	 */
	public MyBatisUserAlertDao() {
		super(net.solarnetwork.central.user.domain.UserAlert.class, Long.class);
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<UserAlert> findAlertsToProcess(UserAlertType type, Long startingId, DateTime validDate,
			Integer max) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("type", type);
		if ( startingId != null ) {
			params.put("startingId", startingId);
		}
		params.put("validDate", (validDate == null ? new DateTime() : validDate));
		return selectList(QUERY_FOR_PROCESSING, params, null, max);
	}

	@Override
	// Propagation.REQUIRED for server-side cursors
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<UserAlert> findAlertsForUser(Long userId) {
		return selectList(QUERY_FOR_USER_WITH_SITUATION, userId, null, null);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public int deleteAllAlertsForNode(Long userId, Long nodeId) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("user", userId);
		params.put("node", nodeId);
		return getSqlSession().delete(DELETE_FOR_NODE, params);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserAlert getAlertSituation(Long alertId) {
		return selectFirst(QUERY_FOR_SITUATION, alertId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateValidTo(Long alertId, DateTime validTo) {
		Map<String, Object> params = new HashMap<String, Object>(3);
		params.put("id", alertId);
		params.put("validDate", (validTo == null ? new DateTime() : validTo));
		getSqlSession().update(UPDATE_VALID_TO, params);
	}

}
