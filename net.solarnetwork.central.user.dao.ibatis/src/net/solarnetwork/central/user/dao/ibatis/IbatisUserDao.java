/* ==================================================================
 * IbatisUserDao.java - Dec 18, 2009 3:10:02 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.orm.ibatis.SqlMapClientCallback;

import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.event.RowHandler;

import net.solarnetwork.central.dao.ibatis.IbatisGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * iBATIS implementation of {@link UserDao}.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisUserDao extends IbatisGenericDaoSupport<User>
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
	 * Default constructor.
	 */
	public IbatisUserDao() {
		super(User.class);
	}

	@Override
	public User getUserByEmail(String email) {
		return (User)getSqlMapClientTemplate().queryForObject(
				QUERY_FOR_EMAIL, email);
	}

	@Override
	public Set<String> getUserRoles(User user) {
		final SortedSet<String> roles = new TreeSet<String>();
		getSqlMapClientTemplate().queryWithRowHandler(
				QUERY_FOR_ROLES, user, new RowHandler() {
			@Override
			public void handleRow(Object valueObject) {
				roles.add((String)valueObject);
			}
		});
		return roles;
	}

	@Override
	public void storeUserRoles(final User user, final Set<String> roles) {
		getSqlMapClientTemplate().execute(new SqlMapClientCallback<Object>() {
			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor)
					throws SQLException {
				executor.startBatch();
				executor.delete(DELETE_ROLES, user);
				if ( roles != null ) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("userId", user.getId());
					for ( String role : roles ) {
						params.put("role", role);
						executor.insert(INSERT_ROLE_FOR_USER, params);
					}
				}
				executor.executeBatch();
				return null;
			}
		});
	}

}
