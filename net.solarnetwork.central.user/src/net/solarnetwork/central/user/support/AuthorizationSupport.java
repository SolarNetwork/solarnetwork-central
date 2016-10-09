/* ==================================================================
 * AuthorizationSupport.java - Oct 3, 2014 4:24:23 PM
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.central.security.SecurityNode;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Helper class for authorization needs, e.g. aspect impelmentations.
 * 
 * @author matt
 * @version 1.2
 */
public abstract class AuthorizationSupport {

	private final UserNodeDao userNodeDao;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public AuthorizationSupport(UserNodeDao userNodeDao) {
		super();
		this.userNodeDao = userNodeDao;
	}

	/**
	 * Get the {@link UserNodeDao}.
	 * 
	 * @return The {@link UserNodeDao}.
	 * @since 1.1
	 */
	protected UserNodeDao getUserNodeDao() {
		return userNodeDao;
	}

	/**
	 * Require the active user have "write" access to a given node ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param nodeId
	 *        the node ID to check
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 */
	protected void requireNodeWriteAccess(Long nodeId) {
		UserNode userNode = (nodeId == null ? null : userNodeDao.get(nodeId));
		if ( userNode == null ) {
			log.warn("Access DENIED to node {}; not found", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, nodeId);
		}

		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			if ( !nodeId.equals(node.getNodeId()) ) {
				log.warn("Access DENIED to node {} for node {}; wrong node", nodeId, node.getNodeId());
				throw new AuthorizationException(node.getNodeId().toString(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			if ( !user.getUserId().equals(userNode.getUser().getId()) ) {
				log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, user.getEmail());
				throw new AuthorizationException(user.getEmail(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			if ( UserAuthTokenType.User.toString().equals(token.getTokenType()) ) {
				// user token, so user ID must match node user's ID
				if ( !token.getUserId().equals(userNode.getUser().getId()) ) {
					log.warn("Access DENIED to node {} for token {}; wrong user", nodeId,
							token.getToken());
					throw new AuthorizationException(token.getToken(),
							AuthorizationException.Reason.ACCESS_DENIED);
				}
				return;
			}
		}

		log.warn("Access DENIED to node {} for actor {}", nodeId, actor);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
	}

	/**
	 * Require the active user have "read" access to a given node ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param nodeId
	 *        the node ID to check
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 */
	protected void requireNodeReadAccess(Long nodeId) {
		UserNode userNode = (nodeId == null ? null : userNodeDao.get(nodeId));
		if ( userNode == null ) {
			log.warn("Access DENIED to node {}; not found", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, nodeId);
		}
		if ( !userNode.isRequiresAuthorization() ) {
			return;
		}

		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			if ( !nodeId.equals(node.getNodeId()) ) {
				log.warn("Access DENIED to node {} for node {}; wrong node", nodeId, node.getNodeId());
				throw new AuthorizationException(node.getNodeId().toString(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			if ( !user.getUserId().equals(userNode.getUser().getId()) ) {
				log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, user.getEmail());
				throw new AuthorizationException(user.getEmail(),
						AuthorizationException.Reason.ACCESS_DENIED);
			}
			return;
		}

		if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			if ( UserAuthTokenType.User.toString().equals(token.getTokenType()) ) {
				// user token, so user ID must match node user's ID
				if ( !token.getUserId().equals(userNode.getUser().getId()) ) {
					log.warn("Access DENIED to node {} for token {}; wrong user", nodeId,
							token.getToken());
					throw new AuthorizationException(token.getToken(),
							AuthorizationException.Reason.ACCESS_DENIED);
				}
				return;
			}
			if ( UserAuthTokenType.ReadNodeData.toString().equals(token.getTokenType()) ) {
				// data token, so token must include the requested node ID
				if ( token.getPolicy() == null || token.getPolicy().getNodeIds() == null
						|| !token.getPolicy().getNodeIds().contains(nodeId) ) {
					log.warn("Access DENIED to node {} for token {}; node not included", nodeId,
							token.getToken());
					throw new AuthorizationException(token.getToken(),
							AuthorizationException.Reason.ACCESS_DENIED);
				}
				return;
			}
		}

		log.warn("Access DENIED to node {} for actor {}", nodeId, actor);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, nodeId);
	}

	/**
	 * Require the active user have "write" access to a given user ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param userId
	 *        the user ID to check
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	protected void requireUserWriteAccess(Long userId) {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to user {} for non-authenticated user", userId);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, userId);
		}

		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			if ( !user.getUserId().equals(userId) ) {
				log.warn("Access DENIED to user {} for user {}; wrong user", userId, user.getEmail());
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, userId);
			}
			return;
		}

		if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			if ( UserAuthTokenType.User.toString().equals(token.getTokenType()) ) {
				// user token, so user ID must match node user's ID
				if ( !token.getUserId().equals(userId) ) {
					log.warn("Access DENIED to user {} for token {}; wrong user", userId,
							token.getToken());
					throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED,
							userId);
				}
				return;
			}
		}

		log.warn("Access DENIED to user {} for actor {}", userId, actor);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, userId);
	}

	/**
	 * Require the active user have "read" access to a given user ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param userId
	 *        the user ID to check
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	protected void requireUserReadAccess(Long userId) {
		requireUserWriteAccess(userId);
	}

}
