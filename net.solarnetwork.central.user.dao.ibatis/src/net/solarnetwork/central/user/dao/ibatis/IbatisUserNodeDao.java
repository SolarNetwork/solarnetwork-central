/* ==================================================================
 * IbatisUserNodeDao.java - Jan 29, 2010 11:56:18 AM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import java.util.List;
import net.solarnetwork.central.dao.ibatis.IbatisGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * iBATIS implementation of {@link UserNodeDao}.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisUserNodeDao extends IbatisGenericDaoSupport<UserNode> implements UserNodeDao {

	/** The query name used for {@link #findUserNodesForUser(User)}. */
	public static final String QUERY_FOR_USER = "find-UserNode-for-User";

	/** The query name used for {@link #findNodesAndCertificatesForUser(Long)}. */
	public static final String QUERY_FOR_USER_WITH_CERT = "find-UserNode-for-user-with-certs";

	/**
	 * Default constructor.
	 */
	public IbatisUserNodeDao() {
		super(UserNode.class);
	}

	@Override
	protected Long handleInsert(UserNode datum) {
		super.handleInsert(datum);
		// as our primary key is actually the node ID, return that
		assert datum.getNode() != null;
		return datum.getNode().getId();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<UserNode> findUserNodesForUser(User user) {
		List<UserNode> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_USER, user.getId());
		for ( UserNode userNode : results ) {
			userNode.setUser(user);
		}
		return results;
	}

	@Override
	public List<UserNode> findNodesAndCertificatesForUser(Long userId) {
		@SuppressWarnings("unchecked")
		List<UserNode> results = getSqlMapClientTemplate()
				.queryForList(QUERY_FOR_USER_WITH_CERT, userId);
		return results;
	}

}
