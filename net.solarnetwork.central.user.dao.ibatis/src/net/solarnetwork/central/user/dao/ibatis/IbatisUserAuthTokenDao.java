/* ==================================================================
 * IbatisUserAuthTokenDao.java - Dec 12, 2012 4:10:06 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.ibatis;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.ibatis.IbatisBaseGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.domain.UserAuthToken;
import org.springframework.orm.ibatis.SqlMapClientCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ibatis.sqlmap.client.SqlMapExecutor;

/**
 * iBATIS implementation of {@link UserAuthTokenDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisUserAuthTokenDao extends IbatisBaseGenericDaoSupport<UserAuthToken, String> implements
		UserAuthTokenDao {

	/** The query name used for {@link #findUserAuthTokensForUser(Long)}. */
	public static final String QUERY_FOR_USER_ID = "find-UserAuthToken-for-UserID";

	/**
	 * The delete statement name used by to delete all node IDs for a given
	 * token during {@link #store(Long)}.
	 */
	public static final String DELETE_NODES_FOR_TOKEN = "delete-UserAuthToken-nodes";

	/**
	 * The insert statement name used by to insert a single node ID for a given
	 * token during {@link #store(Long)}.
	 */
	public static final String INSERT_NODE_ID_FOR_TOKEN = "insert-UserAuthToken-node";

	/**
	 * Default constructor.
	 */
	public IbatisUserAuthTokenDao() {
		super(UserAuthToken.class, String.class);
	}

	@Override
	public List<UserAuthToken> findUserAuthTokensForUser(Long userId) {
		@SuppressWarnings("unchecked")
		List<UserAuthToken> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_USER_ID, userId);
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String store(final UserAuthToken datum) {
		final String pk = handleAssignedPrimaryKeyStore(datum);
		getSqlMapClientTemplate().execute(new SqlMapClientCallback<Object>() {

			@Override
			public Object doInSqlMapClient(SqlMapExecutor executor) throws SQLException {
				executor.startBatch();
				executor.delete(DELETE_NODES_FOR_TOKEN, pk);
				if ( datum.getNodeIds() != null ) {
					Map<String, Object> params = new HashMap<String, Object>(2);
					params.put("id", pk);
					for ( Long nodeId : datum.getNodeIds() ) {
						params.put("nodeId", nodeId);
						executor.insert(INSERT_NODE_ID_FOR_TOKEN, params);
					}
				}
				executor.executeBatch();
				return null;
			}
		});
		return pk;
	}
}
