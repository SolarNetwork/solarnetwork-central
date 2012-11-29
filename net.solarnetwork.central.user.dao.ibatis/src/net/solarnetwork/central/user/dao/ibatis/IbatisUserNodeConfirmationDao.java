/* ==================================================================
 * IbatisUserNodeConfirmationDao.java - Sep 7, 2011 5:08:39 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.user.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.ibatis.IbatisGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * iBATIS implementation of {@link UserNodeConfirmationDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisUserNodeConfirmationDao extends IbatisGenericDaoSupport<UserNodeConfirmation>
		implements UserNodeConfirmationDao {

	/** The query name used for {@link #getConfirmationForKey(String, String)}. */
	public static final String QUERY_FOR_KEY = "get-UserNodeConfirmation-for-key";

	/** The query name used for {@link #findUserNodesForUser(User)}. */
	public static final String QUERY_FOR_USER = "find-UserNodeConfirmation-for-User";

	/**
	 * Default constructor.
	 */
	public IbatisUserNodeConfirmationDao() {
		super(UserNodeConfirmation.class);
	}

	@Override
	public UserNodeConfirmation getConfirmationForKey(Long userId, String key) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("userId", userId);
		params.put("key", key);
		UserNodeConfirmation result = (UserNodeConfirmation) getSqlMapClientTemplate().queryForObject(
				QUERY_FOR_KEY, params);
		return result;
	}

	@Override
	public List<UserNodeConfirmation> findPendingConfirmationsForUser(User user) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("user", user);
		params.put("pending", Boolean.TRUE);

		@SuppressWarnings("unchecked")
		List<UserNodeConfirmation> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_USER,
				params);

		return results;
	}

}
