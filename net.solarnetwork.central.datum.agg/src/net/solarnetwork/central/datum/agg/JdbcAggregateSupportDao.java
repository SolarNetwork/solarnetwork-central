/* ==================================================================
 * JdbcAggregateSupportDao.java - 5/11/2019 7:02:40 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.util.List;
import javax.cache.Cache;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * JDBC implementation of {@link AggregateSupportDao}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
public class JdbcAggregateSupportDao implements AggregateSupportDao {

	/** The default value for the {@code sqlUserIdForNodeId} property. */
	public static final String DEFAULT_SQL_USER_ID_FOR_NODE_ID = "SELECT user_id FROM solaruser.user_node WHERE node_id = ?";

	private final JdbcOperations jdbcOps;
	private String sqlUserIdForNodeId;
	private Cache<Long, Long> userNodeCache;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 */
	public JdbcAggregateSupportDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = jdbcOps;
		setSqlUserIdForNodeId(DEFAULT_SQL_USER_ID_FOR_NODE_ID);
	}

	@Override
	public Long userIdForNodeId(Long nodeId) {
		if ( nodeId == null ) {
			return null;
		}
		Long result = null;
		Cache<Long, Long> cache = getUserNodeCache();
		if ( cache != null ) {
			result = cache.get(nodeId);
			if ( result != null ) {
				return result;
			}
		}
		String sql = getSqlUserIdForNodeId();
		if ( sql != null ) {
			List<Long> results = getJdbcOps().queryForList(sql, Long.class, nodeId);
			if ( results != null && !results.isEmpty() ) {
				result = results.get(0);
				if ( result != null && cache != null ) {
					cache.put(nodeId, result);
				}
			}
		}
		return result;
	}

	/**
	 * @return the jdbcOps
	 */
	public JdbcOperations getJdbcOps() {
		return jdbcOps;
	}

	/**
	 * Get the SQL query for finding the user ID that owns a given node ID.
	 * 
	 * @return the SQL query; defaults to
	 *         {@link #DEFAULT_SQL_USER_ID_FOR_NODE_ID}
	 */
	public String getSqlUserIdForNodeId() {
		return sqlUserIdForNodeId;
	}

	/**
	 * Set the SQL query for finding the user ID that owns a given node ID.
	 * 
	 * <p>
	 * This query must accept a single {@code long} node ID parameter and must
	 * return a single {@code long} user ID parameter representing the owner of
	 * the given node.
	 * </p>
	 * 
	 * @param sqlUserIdForNodeId
	 *        the SQL query to set
	 */
	public void setSqlUserIdForNodeId(String sqlUserIdForNodeId) {
		this.sqlUserIdForNodeId = sqlUserIdForNodeId;
	}

	/**
	 * Get the cache of node IDs to user IDs.
	 * 
	 * @return the cache, or {@literal null} if not available
	 */
	public Cache<Long, Long> getUserNodeCache() {
		return userNodeCache;
	}

	/**
	 * Set the cache of node IDs to user IDs.
	 * 
	 * @param userNodeCache
	 *        the cache to set
	 */
	public void setUserNodeCache(Cache<Long, Long> userNodeCache) {
		this.userNodeCache = userNodeCache;
	}

}
