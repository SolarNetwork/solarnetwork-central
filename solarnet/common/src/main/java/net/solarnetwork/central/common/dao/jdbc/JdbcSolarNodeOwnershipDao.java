/* ==================================================================
 * JdbcSolarNodeOwnershipDao.java - 28/02/2020 2:57:32 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc;

import java.util.List;
import javax.cache.Cache;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectSolarNodeOwnership;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectUserAuthTokenNodes;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * JDBC implementation of {@link SolarNodeOwnershipDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcSolarNodeOwnershipDao implements SolarNodeOwnershipDao {

	private final JdbcOperations jdbcOps;
	private Cache<Long, SolarNodeOwnership> nodeOwnershipCache;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 */
	public JdbcSolarNodeOwnershipDao(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = jdbcOps;
	}

	@Override
	public SolarNodeOwnership ownershipForNodeId(Long nodeId) {
		if ( nodeId == null ) {
			return null;
		}
		SolarNodeOwnership result = null;
		Cache<Long, SolarNodeOwnership> cache = getNodeOwnershipCache();
		if ( cache != null ) {
			result = cache.get(nodeId);
			if ( result != null ) {
				return result;
			}
		}
		List<SolarNodeOwnership> results = getJdbcOps().query(
				SelectSolarNodeOwnership.selectForNode(nodeId),
				BasicSolarNodeOwnershipRowMapper.INSTANCE);
		if ( results != null && !results.isEmpty() ) {
			result = results.get(0);
			if ( result != null && cache != null ) {
				cache.put(nodeId, result);
			}
		}
		return result;
	}

	@Override
	public SolarNodeOwnership[] ownershipsForUserId(Long userId) {
		if ( userId == null ) {
			return null;
		}
		SolarNodeOwnership[] result = null;
		List<SolarNodeOwnership> results = getJdbcOps().query(
				SelectSolarNodeOwnership.selectForUser(userId),
				BasicSolarNodeOwnershipRowMapper.INSTANCE);
		if ( results != null && !results.isEmpty() ) {
			result = results.toArray(new SolarNodeOwnership[results.size()]);
		}
		return result;
	}

	@Override
	public Long[] nonArchivedNodeIdsForToken(String tokenId) {
		if ( tokenId == null ) {
			return new Long[0];
		}
		List<Long> results = getJdbcOps().query(new SelectUserAuthTokenNodes(tokenId),
				new ColumnRowMapper<>(2, Long.class));
		return (results != null ? results.toArray(Long[]::new) : new Long[0]);
	}

	/**
	 * Get the JDBC operations.
	 * 
	 * @return the ops
	 */
	public JdbcOperations getJdbcOps() {
		return jdbcOps;
	}

	/**
	 * Get the cache of node IDs to associated node ownership.
	 * 
	 * @return the cache, or {@literal null} if not available
	 */
	public Cache<Long, SolarNodeOwnership> getNodeOwnershipCache() {
		return nodeOwnershipCache;
	}

	/**
	 * Set the cache of node IDs to associated node ownership.
	 * 
	 * @param nodeOwnershipCache
	 *        the cache to set
	 */
	public void setUserNodeCache(Cache<Long, SolarNodeOwnership> nodeOwnershipCache) {
		this.nodeOwnershipCache = nodeOwnershipCache;
	}

}
