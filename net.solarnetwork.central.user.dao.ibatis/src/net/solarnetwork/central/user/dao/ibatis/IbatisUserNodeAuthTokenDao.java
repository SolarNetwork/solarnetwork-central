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

import java.util.List;
import net.solarnetwork.central.dao.ibatis.IbatisBaseGenericDaoSupport;
import net.solarnetwork.central.user.dao.UserNodeAuthTokenDao;
import net.solarnetwork.central.user.domain.UserNodeAuthToken;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * iBATIS implementation of {@link UserNodeAuthTokenDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisUserNodeAuthTokenDao extends IbatisBaseGenericDaoSupport<UserNodeAuthToken, String>
		implements UserNodeAuthTokenDao {

	/** The query name used for {@link #findUserNodeAuthTokensForNode(Long)}. */
	public static final String QUERY_FOR_NODE_ID = "find-UserNodeAuthToken-for-NodeID";

	/**
	 * Default constructor.
	 */
	public IbatisUserNodeAuthTokenDao() {
		super(UserNodeAuthToken.class, String.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<UserNodeAuthToken> findUserNodeAuthTokensForNode(Long nodeId) {
		@SuppressWarnings("unchecked")
		List<UserNodeAuthToken> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_NODE_ID,
				nodeId);
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String store(UserNodeAuthToken datum) {
		return handleAssignedPrimaryKeyStore(datum);
	}

}
