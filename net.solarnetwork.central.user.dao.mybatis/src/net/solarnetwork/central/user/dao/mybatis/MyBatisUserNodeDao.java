/* ==================================================================
 * MyBatisUserNodeDao.java - Nov 11, 2014 7:29:04 AM
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * MyBatis implementation of {@link UserNodeDao}.
 * 
 * @author matt
 * @version 1.3
 */
public class MyBatisUserNodeDao extends BaseMyBatisGenericDao<UserNode, Long> implements UserNodeDao {

	/** The query name used for {@link #findUserNodesForUser(User)}. */
	public static final String QUERY_FOR_USER = "find-UserNode-for-User";

	/**
	 * The query name used for {@link #findArchivedUserNodesForUser(Long)}.
	 * 
	 * @since 1.1
	 */
	public static final String QUERY_FOR_USER_ARCHIVED = "find-archived-UserNode-for-User";

	/**
	 * The query name used for
	 * {@link #updateUserNodeArchivedStatus(Long, Long[], boolean)}.
	 * 
	 * @since 1.1
	 */
	public static final String UPDATE_ARCHIVED_STATUS = "update-archived-UserNode-status";

	/**
	 * The query name used for
	 * {@link #findUserNodesAndCertificatesForUser(Long)}.
	 */
	public static final String QUERY_FOR_USER_WITH_CERT = "find-UserNode-for-user-with-certs";

	/**
	 * The callable statement used in
	 * {@link #storeUserNodeTransfer(UserNodeTransfer)}.
	 */
	public static final String CALL_STORE_USER_NODE_TRANSFER = "store-UserNodeTransfer";

	/** The query name for {@link #deleteUserNodeTrasnfer(UserNodeTransfer)}. */
	public static final String DELETE_USER_NODE_TRANSFER = "delete-UserNodeTransfer";

	/**
	 * The query name used for {@link #getUserNodeTransfer(UserNodePK)}.
	 */
	public static final String QUERY_USER_NODE_TRANSFERS_FOR_ID = "get-UserNodeTransfer-for-id";

	/**
	 * The query name used for
	 * {@link #findUserNodeTransferRequestsForEmail(String)}.
	 */
	public static final String QUERY_USER_NODE_TRANSFERS_FOR_EMAIL = "find-UserNodeTransfer-for-email";

	/**
	 * The query name used for {@link #findNodeIdsForUser(Long)}.
	 * 
	 * @since 1.2
	 */
	public static final String QUERY_NODE_IDS_FOR_USER = "find-node-ids-for-user-id";

	/**
	 * The query name used for {@link #findNodeIdsForToken(String)}.
	 * 
	 * @since 1.3
	 */
	public static final String QUERY_NODE_IDS_FOR_TOKEN = "find-node-ids-for-token-id";

	/**
	 * Default constructor.
	 */
	public MyBatisUserNodeDao() {
		super(UserNode.class, Long.class);
	}

	@Override
	protected Long handleInsert(UserNode datum) {
		super.handleInsert(datum);
		// as our primary key is actually the node ID, return that
		assert datum.getNode() != null;
		return datum.getNode().getId();
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> findUserNodesForUser(User user) {
		List<UserNode> results = getSqlSession().selectList(QUERY_FOR_USER, user.getId());
		for ( UserNode userNode : results ) {
			userNode.setUser(user);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> findUserNodesAndCertificatesForUser(Long userId) {
		return getSqlSession().selectList(QUERY_FOR_USER_WITH_CERT, userId);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	public void storeUserNodeTransfer(UserNodeTransfer transfer) {
		getSqlSession().update(CALL_STORE_USER_NODE_TRANSFER, transfer);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public UserNodeTransfer getUserNodeTransfer(UserNodePK pk) {
		return getSqlSession().selectOne(QUERY_USER_NODE_TRANSFERS_FOR_ID, pk);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	public void deleteUserNodeTrasnfer(UserNodeTransfer transfer) {
		int count = getSqlSession().delete(DELETE_USER_NODE_TRANSFER, transfer.getId());
		log.debug("Deleted {} UserNodeTransfer entities for ID {}", count, transfer.getId());
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeTransfer> findUserNodeTransferRequestsForEmail(String email) {
		return getSqlSession().selectList(QUERY_USER_NODE_TRANSFERS_FOR_EMAIL, email);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNode> findArchivedUserNodesForUser(Long userId) {
		return getSqlSession().selectList(QUERY_FOR_USER_ARCHIVED, userId);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds, boolean archived) {
		Map<String, Object> sqlProperties = new HashMap<String, Object>(3);
		sqlProperties.put("userId", userId);
		sqlProperties.put("nodeIds", nodeIds);
		sqlProperties.put("archived", archived);
		getSqlSession().update(UPDATE_ARCHIVED_STATUS, sqlProperties);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.2
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<Long> findNodeIdsForUser(Long userId) {
		List<Long> ids = selectList(QUERY_NODE_IDS_FOR_USER, userId, null, null);
		return (ids == null || ids.isEmpty() ? Collections.<Long> emptySet()
				: new LinkedHashSet<Long>(ids));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.3
	 */
	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Set<Long> findNodeIdsForToken(String tokenId) {
		List<Long> ids = selectList(QUERY_NODE_IDS_FOR_TOKEN, tokenId, null, null);
		return (ids == null || ids.isEmpty() ? Collections.<Long> emptySet()
				: new LinkedHashSet<Long>(ids));
	}

}
