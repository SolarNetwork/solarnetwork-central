/* ==================================================================
 * DelegatingUserBiz.java - Oct 7, 2014 7:20:03 AM
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

package net.solarnetwork.central.user.support;

import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserNodeCertificate;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * Delegating implementation of {@link UserBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.5
 */
@SuppressWarnings("deprecation")
public class DelegatingUserBiz implements UserBiz {

	private final UserBiz delegate;

	/**
	 * Construct with a delegate;
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingUserBiz(UserBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public User getUser(Long id) {
		return delegate.getUser(id);
	}

	@Override
	public List<UserNode> getUserNodes(Long userId) {
		return delegate.getUserNodes(userId);
	}

	@Override
	public UserNode getUserNode(Long userId, Long nodeId) throws AuthorizationException {
		return delegate.getUserNode(userId, nodeId);
	}

	@Override
	public UserNode saveUserNode(UserNode userNodeEntry) throws AuthorizationException {
		return delegate.saveUserNode(userNodeEntry);
	}

	@Override
	public List<UserNodeConfirmation> getPendingUserNodeConfirmations(Long userId) {
		return delegate.getPendingUserNodeConfirmations(userId);
	}

	@Override
	public UserNodeConfirmation getPendingUserNodeConfirmation(Long userNodeConfirmationId) {
		return delegate.getPendingUserNodeConfirmation(userNodeConfirmationId);
	}

	@Override
	public UserNodeCertificate getUserNodeCertificate(Long userId, Long nodeId) {
		return delegate.getUserNodeCertificate(userId, nodeId);
	}

	@Override
	public UserAuthToken generateUserAuthToken(Long userId, UserAuthTokenType type, Set<Long> nodeIds) {
		return delegate.generateUserAuthToken(userId, type, nodeIds);
	}

	@Override
	public UserAuthToken generateUserAuthToken(Long userId, UserAuthTokenType type,
			SecurityPolicy policy) {
		return delegate.generateUserAuthToken(userId, type, policy);
	}

	@Override
	public List<UserAuthToken> getAllUserAuthTokens(Long userId) {
		return delegate.getAllUserAuthTokens(userId);
	}

	@Override
	public void deleteUserAuthToken(Long userId, String tokenId) {
		delegate.deleteUserAuthToken(userId, tokenId);
	}

	@Override
	public UserAuthToken updateUserAuthTokenStatus(Long userId, String tokenId,
			UserAuthTokenStatus newStatus) {
		return delegate.updateUserAuthTokenStatus(userId, tokenId, newStatus);
	}

	@Override
	public UserAuthToken updateUserAuthTokenPolicy(Long userId, String tokenId, SecurityPolicy newPolicy,
			boolean replace) {
		return delegate.updateUserAuthTokenPolicy(userId, tokenId, newPolicy, replace);
	}

	@Override
	public void updateUserNodeArchivedStatus(Long userId, Long[] nodeIds, boolean archived)
			throws AuthorizationException {
		delegate.updateUserNodeArchivedStatus(userId, nodeIds, archived);
	}

	@Override
	public List<UserNode> getArchivedUserNodes(Long userId) throws AuthorizationException {
		return delegate.getArchivedUserNodes(userId);
	}

	@Override
	public AuthorizationV2Builder createAuthorizationV2Builder(Long userId, String tokenId,
			DateTime signingDate) {
		return delegate.createAuthorizationV2Builder(userId, tokenId, signingDate);
	}

}
