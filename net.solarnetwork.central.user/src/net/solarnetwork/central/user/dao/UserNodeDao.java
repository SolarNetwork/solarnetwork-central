/* ==================================================================
 * UserNodeDao.java - Jan 29, 2010 11:45:43 AM
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

package net.solarnetwork.central.user.dao;

import java.util.List;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;

/**
 * DAO API for UserNode objects.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserNodeDao extends GenericDao<UserNode, Long> {

	/**
	 * Find a list of all UserNode objects for a particular user.
	 * 
	 * @param user
	 *        the user to get all nodes for
	 * @return list of {@link UserNode} objects, or an empty list if none found
	 */
	List<UserNode> findUserNodesForUser(User user);

	/**
	 * Find all UserNodes for a given user.
	 * 
	 * <p>
	 * The returned nodes will have {@link UserNodeCertificate} values populated
	 * in {@link UserNode#getCertificate()}, with the priority being requested,
	 * active, disabled.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @return the nodes
	 */
	List<UserNode> findUserNodesAndCertificatesForUser(Long userId);

}
