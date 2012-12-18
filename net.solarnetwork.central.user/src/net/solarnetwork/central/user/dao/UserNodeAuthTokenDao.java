/* ==================================================================
 * UserNodeAuthTokenDao.java - Dec 18, 2012 3:05:38 PM
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

package net.solarnetwork.central.user.dao;

import java.util.List;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.UserNodeAuthToken;

/**
 * DAO API for {@link UserNodeAuthToken} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeAuthTokenDao extends GenericDao<UserNodeAuthToken, String> {

	/**
	 * Find a list of all UserNodeAuthToken objects for a particular node.
	 * 
	 * @param nodeId
	 *        the node ID to get all tokens for
	 * @return list of {@link UserNodeAuthToken} objects, or an empty list if
	 *         none found
	 */
	List<UserNodeAuthToken> findUserNodeAuthTokensForNode(Long nodeId);

}
