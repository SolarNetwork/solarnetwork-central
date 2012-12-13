/* ==================================================================
 * UserBiz.java - Jan 11, 2010 5:18:04 PM
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

package net.solarnetwork.central.user.biz;

import java.util.List;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * API for registered user tasks.
 * 
 * @author matt
 * @version $Id$
 */
public interface UserBiz {

	/**
	 * Authenticate a user by their email and password.
	 * 
	 * @param email
	 *        the email of the user to log on
	 * @param password
	 *        the attempted password
	 * @return the User if found and password matches
	 * @throws AuthorizationException
	 *         if user not found or password does not match
	 */
	User logonUser(String email, String password) throws AuthorizationException;

	/**
	 * Get a User object by its ID.
	 * 
	 * @param id
	 *        the ID of the User to get
	 * @return the User, or <em>null</em> if not found
	 */
	User getUser(Long id);

	/**
	 * Get a User object by its email.
	 * 
	 * @param email
	 *        the email of the User to get
	 * @return the User, or <em>null</em> if not found
	 */
	User getUser(String email);

	/**
	 * Get a list of nodes belonging to a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to get the nodes for
	 * @return list of UserNode objects, or an empty list if none found
	 */
	List<UserNode> getUserNodes(Long userId);

	/**
	 * Get a specific node belonging to a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to get the node for
	 * @param nodeId
	 *        the ID of the node to get
	 * @return the matching UserNode object
	 * @throws AuthorizationException
	 *         if the user is not authorized to access the given node
	 */
	UserNode getUserNode(Long userId, Long nodeId) throws AuthorizationException;

	/**
	 * Update a specific node belonging to a specific user.
	 * 
	 * <p>
	 * The {@link SolarNode#getId()} and {@link User#getId()} values are
	 * expected to be set on the entry object.
	 * </p>
	 * 
	 * @param userNodeEntry
	 *        the UserNode data to save
	 * @return the updated UserNode object
	 * @throws AuthorizationException
	 *         if the user is not authorized to access the given node
	 */
	UserNode saveUserNode(UserNode userNodeEntry) throws AuthorizationException;

	/**
	 * Get a list of pending node confirmations belonging to a specific user.
	 * 
	 * @param user
	 *        the user to get the nodes for
	 * @return list of UserNodeConfirmation objects, or an empty list if none
	 *         found
	 */
	List<UserNodeConfirmation> getPendingUserNodeConfirmations(Long userId);

	/**
	 * Get a specific pending confirmation.
	 * 
	 * @param userNodeConfirmationId
	 *        the ID of the pending confirmation
	 * @return the pending confirmation, or <em>null</em> if not found
	 */
	UserNodeConfirmation getPendingUserNodeConfirmation(Long userNodeConfirmationId);

	/**
	 * Get a specific UserNodeCertificate object.
	 * 
	 * @param certId
	 *        the cert ID
	 * @return the certificate, or <em>null</em> if not available
	 */
	UserNodeCertificate getUserNodeCertificate(Long certId);

	/**
	 * Generate a new, unique {@link UserAuthToken} entity and return it.
	 * 
	 * @param userId
	 *        the user ID to generate the token for
	 * @return the generated token
	 */
	UserAuthToken generateUserAuthToken(Long userId);

	/**
	 * Get all {@link UserAuthToken} entities for a given user.
	 * 
	 * @param userId
	 *        the ID to get the tokens for
	 * @return the tokens, or an empty list if none available
	 */
	List<UserAuthToken> getAllUserAuthTokens(Long userId);

	/**
	 * Delete a user auth token.
	 * 
	 * @param userId
	 *        the user ID
	 * @param tokenId
	 *        the UserAuthToken ID to delete
	 */
	void deleteUserAuthToken(Long userId, String tokenId);

	/**
	 * Update the status of a UserAuthToken.
	 * 
	 * @param userId
	 *        the user ID
	 * @param tokenId
	 *        the UserAuthToken ID to delete
	 * @param newStatus
	 *        the desired status
	 * @return the updated token
	 */
	UserAuthToken updateUserAuthTokenStatus(Long userId, String tokenId, UserAuthTokenStatus newStatus);
}
