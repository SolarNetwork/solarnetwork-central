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
import java.util.Set;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodePK;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * DAO API for UserNode objects.
 * 
 * @author matt
 * @version 1.4
 */
public interface UserNodeDao extends GenericDao<UserNode, Long> {

	/**
	 * Find a list of all UserNode objects for a particular user.
	 * 
	 * This will not return archived nodes (see
	 * {@link #findArchivedUserNodesForUser(Long)}).
	 * 
	 * @param user
	 *        the user to get all nodes for
	 * @return list of {@link UserNode} objects, or an empty list if none found
	 */
	List<UserNode> findUserNodesForUser(User user);

	/**
	 * Find a set of node IDs for a user ID.
	 * 
	 * This will not return archived nodes (see
	 * {@link #findArchivedUserNodesForUser(Long)}).
	 * 
	 * @param userId
	 *        the user ID to get all node IDs for
	 * @return set of node IDs, or an empty set if none found
	 * @since 1.4
	 */
	Set<Long> findNodeIdsForUser(Long userId);

	/**
	 * Find all UserNodes for a given user.
	 * 
	 * <p>
	 * The returned nodes will have {@link UserNodeCertificate} values populated
	 * in {@link UserNode#getCertificate()}, with the priority being requested,
	 * active, disabled. The {@link UserNodeTransfer} values will be populated
	 * in {@link UserNode#getTransfer()} as well.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @return the nodes
	 */
	List<UserNode> findUserNodesAndCertificatesForUser(Long userId);

	/**
	 * Find a list of all archived UserNode objects for a particular user.
	 * 
	 * @param userId
	 *        the user ID to get all archived nodes for
	 * @return list of {@link UserNode} objects, or an empty list if none found
	 * @since 1.3
	 */
	List<UserNode> findArchivedUserNodesForUser(Long userId);

	/**
	 * Update the archived status of a set of node IDs.
	 * 
	 * @param userId
	 *        The user ID of the nodes to update the status for.
	 * @param nodeIds
	 *        The IDs of the nodes to update the archived status for.
	 * @param archived
	 *        {@code true} to archive the nodes, {@code false} to un-archive
	 *        them.
	 * @since 1.3
	 */
	void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds, boolean archived);

	/**
	 * Store a {@link UserNodeTransfer}.
	 * 
	 * @param transfer
	 *        The transfer to store.
	 * @since 1.2
	 */
	void storeUserNodeTransfer(UserNodeTransfer transfer);

	/**
	 * Get a {@link UserNodeTransfer} by primary key.
	 * 
	 * @param pk
	 *        The ID of the transfer to get.
	 * @return The matching UserNodeTransfer, or <em>null</em> if not available.
	 * @since 1.2
	 */
	UserNodeTransfer getUserNodeTransfer(UserNodePK pk);

	/**
	 * Delete a {@link UserNodeTransfer}.
	 * 
	 * @param transfer
	 *        The transfer to delete.
	 * @since 1.2
	 */
	void deleteUserNodeTrasnfer(UserNodeTransfer transfer);

	/**
	 * Get all {@link UserNodeTransfer} instances for a given email address.
	 * 
	 * @param email
	 *        The email of the requested recipient of the ownership trasnfer.
	 * @return The available node transfers, never <em>null</em>.
	 * @since 1.2
	 */
	List<UserNodeTransfer> findUserNodeTransferRequestsForEmail(String email);

}
