/* ==================================================================
 * MyBatisUserNodeConfirmationDao.java - Nov 11, 2014 9:34:02 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * MyBatis implementation of {@link UserNodeConfirmationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserNodeConfirmationDao extends BaseMyBatisGenericDao<UserNodeConfirmation, Long>
		implements UserNodeConfirmationDao {

	/**
	 * The query name used for {@link #getConfirmationForKey(Long, String)}.
	 */
	public static final String QUERY_FOR_KEY = "get-UserNodeConfirmation-for-key";

	/**
	 * The query name used for {@link #findPendingConfirmationsForUser(User)}.
	 */
	public static final String QUERY_FOR_USER = "find-UserNodeConfirmation-for-User";

	/**
	 * Default constructor.
	 */
	public MyBatisUserNodeConfirmationDao() {
		super(UserNodeConfirmation.class, Long.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeConfirmation getConfirmationForKey(Long userId, String key) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("userId", userId);
		params.put("key", key);
		return selectFirst(QUERY_FOR_KEY, params);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeConfirmation> findPendingConfirmationsForUser(User user) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("user", user);
		params.put("pending", Boolean.TRUE);

		return getSqlSession().selectList(QUERY_FOR_USER, params);
	}

}
