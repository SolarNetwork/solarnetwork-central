/* ==================================================================
 * MyBatisUserDao.java - Nov 11, 2014 6:15:26 AM
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
import java.util.Set;
import java.util.TreeSet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.domain.UserFilter;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserMatch;
import net.solarnetwork.codec.JsonUtils;

/**
 * MyBatis implementation of {@link UserDao}.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDao extends BaseMyBatisFilterableDao<User, UserFilterMatch, UserFilter, Long>
		implements UserDao {

	/** The query name used for {@link #getUserByEmail(String)}. */
	public static final String QUERY_FOR_EMAIL = "get-User-for-email";

	/** The query name used for {@link #getUserRoles(User)}. */
	public static final String QUERY_FOR_ROLES = "find-roles-for-User";

	/** The delete query name used in {@link #storeUserRoles(User, Set)}. */
	public static final String DELETE_ROLES = "delete-roles-for-User";

	/** The insert query name used in {@link #storeUserRoles(User, Set)}. */
	public static final String INSERT_ROLE_FOR_USER = "insert-role-for-User";

	/**
	 * The select query name used in {@link #getInternalData(Long)}.
	 * 
	 * The statement is passed a {@code userId} parameter.
	 * 
	 * @since 1.2
	 */
	public static final String QUERY_INTERNAL_DATA = "get-User-internal-data";

	/**
	 * The update query name used in {@link #storeInternalData(Long, Map)}.
	 * 
	 * The statement is passed {@code userId} and {@code dataJson} parameters.
	 * 
	 * @since 1.2
	 */
	public static final String UPDATE_INTERNAL_DATA = "update-internal-data";

	/**
	 * The query parameter for a general filter object value.
	 * 
	 * @since 1.2
	 */
	public static final String PARAM_FILTER = "filter";

	/**
	 * Default constructor.
	 */
	public MyBatisUserDao() {
		super(User.class, Long.class, UserMatch.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public User getUserByEmail(String email) {
		return selectFirst(QUERY_FOR_EMAIL, email == null ? null : email.trim());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<String> getUserRoles(User user) {
		final List<String> results = getSqlSession().selectList(QUERY_FOR_ROLES, user);
		return new TreeSet<String>(results);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeUserRoles(final User user, final Set<String> roles) {
		getSqlSession().delete(DELETE_ROLES, user);
		if ( roles != null ) {
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("userId", user.getId());
			for ( String role : roles ) {
				params.put("role", role);
				getSqlSession().insert(INSERT_ROLE_FOR_USER, params);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	public Map<String, Object> getInternalData(Long userId) {
		User user = selectFirst(QUERY_INTERNAL_DATA, userId);
		return (user != null ? user.getInternalData() : null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void storeInternalData(Long userId, Map<String, Object> data) {
		Map<String, Object> sqlParams = new HashMap<String, Object>(3);
		sqlParams.put("userId", userId);
		sqlParams.put("dataJson", JsonUtils.getJSONString(data, "{}"));
		getSqlSession().update(UPDATE_INTERNAL_DATA, sqlParams);
	}

}
