/* ==================================================================
 * NodeOwnershipBiz.java - Apr 20, 2015 8:16:26 PM
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz;

import java.util.List;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.domain.UserNodeTransfer;

/**
 * API for node owner tasks.
 * 
 * @author matt
 * @version 1.0
 */
public interface NodeOwnershipBiz {

	/**
	 * Get a specific transfer for a given node.
	 * 
	 * @param userId
	 *        The ID of the user making the request.
	 * @param nodeId
	 *        The ID of the node to transfer.
	 * @return The transfer, or <em>null</em> if none exists.
	 */
	UserNodeTransfer getNodeOwnershipTransfer(Long userId, Long nodeId);

	/**
	 * Get a complete list of transfers for a given recipient.
	 * 
	 * <p>
	 * This provides a view into all transfer requests awaiting confirmation by
	 * a single user.
	 * </p>
	 * 
	 * @param email
	 *        The email address of the requested ownership recipient.
	 * @return A list of all ownership requests associated with the given
	 *         recipient (never <em>null</em>).
	 */
	List<UserNodeTransfer> pendingNodeOwnershipTransfersForEmail(String email);

	/**
	 * Request a transfer of ownership of a node.
	 * 
	 * When requesting a new owner for a node, the email address of the new
	 * owner is provided and an email will be sent a message with a special link
	 * for confirming or rejecting the transfer request. The node will not be
	 * transferred until the request is confirmed, via the
	 * {@link #confirmNodeOwnershipTransfer()}.
	 * 
	 * @param userId
	 *        The ID of the user making the request.
	 * @param nodeId
	 *        The ID of the node to transfer.
	 * @param newOwnerEmail
	 *        The email address of the user requested to take ownership of the
	 *        node.
	 * @throws AuthorizationException
	 *         If the active user is not authorized to transfer ownership of the
	 *         given node.
	 */
	void requestNodeOwnershipTransfer(Long userId, Long nodeId, String newOwnerEmail)
			throws AuthorizationException;

	/**
	 * Cancel a node ownership transfer request.
	 * 
	 * After a node transfer request has been made via
	 * {@link #requestNodeOwnershipTransfer(Long, Long, String)} but before the
	 * new owner has accepted the request, the original owner can cancel the
	 * request by calling this method.
	 * 
	 * @param userId
	 *        The ID of the user making the request.
	 * @param nodeId
	 *        The ID of the node to transfer.
	 * @throws AuthorizationException
	 *         If the active user is not authorized to transfer ownership of the
	 *         given node.
	 */
	void cancelNodeOwnershipTransfer(Long userId, Long nodeId) throws AuthorizationException;

	/**
	 * Confirm or reject a node transfer request.
	 * 
	 * After a node transfer request has been made via
	 * {@link #requestNodeOwnershipTransfer(Long, Long, String)} the recipient
	 * of the transfer request can confirm or reject the request by calling this
	 * method.
	 * 
	 * <b>Note:</b> the active user's email address must match the one used in
	 * the original transfer request.
	 * 
	 * @param userId
	 *        The ID of the user making the request.
	 * @param nodeId
	 *        The node ID if the node to accept or reject ownership of.
	 * @param accept
	 *        If <em>true</em> then accept the transfer request, otherwise
	 *        reject (canceling the request).
	 * @returns The original transfer entity.
	 * @throws AuthorizationException
	 *         If the active user is not authorized to confirm (or reject) the
	 *         ownership transfer.
	 */
	UserNodeTransfer confirmNodeOwnershipTransfer(Long userId, Long nodeId, boolean accept)
			throws AuthorizationException;

}
