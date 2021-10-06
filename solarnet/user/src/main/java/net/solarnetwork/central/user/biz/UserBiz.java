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
 */

package net.solarnetwork.central.user.biz;

import java.time.Instant;
import java.util.List;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * API for registered user tasks.
 * 
 * @author matt
 * @version 3.0
 */
public interface UserBiz {

	/**
	 * Get a User object by its ID.
	 * 
	 * @param id
	 *        the ID of the User to get
	 * @return the User, or <em>null</em> if not found
	 */
	User getUser(Long id) throws AuthorizationException;

	/**
	 * Get a list of nodes belonging to a specific user.
	 * 
	 * Archived nodes will not be returned (see
	 * {@link #getArchivedUserNodes(Long)} for that).
	 * 
	 * @param userId
	 *        the ID of the user to get the nodes for
	 * @return list of UserNode objects, or an empty list if none found
	 */
	List<UserNode> getUserNodes(Long userId) throws AuthorizationException;

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
	 * Archive, or un-archive a user node.
	 * 
	 * An archived node will not be returned from {@link #getUserNodes(Long)}.
	 * Its data will remain intact and the node can be un-archived at a future
	 * date.
	 * 
	 * @param userId
	 *        the ID of the user to update the node for
	 * @param nodeIds
	 *        the IDs of the nodes to update
	 * @param archived
	 *        {@code true} to archive the nodes, {@code false} to un-archive
	 * @throws AuthorizationException
	 *         if the user is not authorized to access a given node
	 * @since 1.4
	 */
	void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds, boolean archived)
			throws AuthorizationException;

	/**
	 * Get a list of archived nodes belonging to a specific user.
	 * 
	 * @param userId
	 *        the ID of the user to get the nodes for
	 * @return list of UserNode objects, or an empty list if none found
	 * @since 1.4
	 */
	List<UserNode> getArchivedUserNodes(Long userId) throws AuthorizationException;

	/**
	 * Get a list of pending node confirmations belonging to a specific user.
	 * 
	 * @param userId
	 *        the user ID to get the nodes for
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
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @return the certificate, or <em>null</em> if not available
	 */
	UserNodeCertificate getUserNodeCertificate(Long userId, Long nodeId);

	/**
	 * Generate a new, unique {@link UserAuthToken} entity and return it.
	 * 
	 * @param userId
	 *        the user ID to generate the token for
	 * @param type
	 *        the type of token to create
	 * @param policy
	 *        an optional policy to attach, or {@code null} for no restrictions
	 * @return the generated token
	 * @since 1.3
	 */
	UserAuthToken generateUserAuthToken(Long userId, SecurityTokenType type, SecurityPolicy policy);

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

	/**
	 * Update the policy of a UserAuthToken.
	 * 
	 * @param userId
	 *        the user ID
	 * @param tokenId
	 *        the UserAuthToken ID to delete
	 * @param newPolicy
	 *        the new policy to apply
	 * @param replace
	 *        {@code true} to replace the token's policy with the provided one,
	 *        or {@code false} to merge the provided policy properties into the
	 *        existing policy
	 * @return the updated token
	 */
	UserAuthToken updateUserAuthTokenPolicy(Long userId, String tokenId, SecurityPolicy newPolicy,
			boolean replace);

	/**
	 * Create an authorization builder object with a populated signing key for a
	 * specific token.
	 * 
	 * <p>
	 * Use this method to create a new builder with a signing key populated for
	 * generating signed SNWS2 {@code Authorization} HTTP header values.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param tokenId
	 *        the UserAuthToken ID to use
	 * @param signingDate
	 *        the date to generate the signing key with
	 * @return the builder
	 * @since 2.0
	 */
	Snws2AuthorizationBuilder createSnws2AuthorizationBuilder(Long userId, String tokenId,
			Instant signingDate);
}
