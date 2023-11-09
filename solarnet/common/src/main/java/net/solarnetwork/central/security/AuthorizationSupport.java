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

package net.solarnetwork.central.security;

import static net.solarnetwork.central.security.AuthorizationException.Reason.ACCESS_DENIED;
import static net.solarnetwork.central.security.AuthorizationException.Reason.UNKNOWN_OBJECT;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Helper class for authorization needs, e.g. aspect implementations.
 * 
 * @author matt
 * @version 1.1
 */
public class AuthorizationSupport {

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private PathMatcher pathMatcher;

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 * @throws IllegalArgumentException
	 *         if any argumnet is {@literal null}
	 */
	public AuthorizationSupport(SolarNodeOwnershipDao nodeOwnershipDao) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
	}

	/**
	 * Get the {@link SolarNodeOwnershipDao}.
	 * 
	 * @return The {@link SolarNodeOwnershipDao}.
	 */
	public SolarNodeOwnershipDao getNodeOwnershipDao() {
		return nodeOwnershipDao;
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
		requireNodeWriteAccess(nodeId, log);
	}

	/**
	 * Require the active user have "write" access to a given node ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param nodeId
	 *        the node ID to check
	 * @param log
	 *        the logger to use
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	public void requireNodeWriteAccess(Long nodeId, Logger log) {
		final SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(nodeId);
		if ( ownership == null ) {
			log.warn("Access DENIED to node {}; owner not found", nodeId);
			throw new AuthorizationException(UNKNOWN_OBJECT, nodeId);
		}

		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(ACCESS_DENIED, nodeId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode node ) {
			if ( ownership.isArchived() ) {
				throw new AuthorizationException(UNKNOWN_OBJECT, nodeId);
			}
			if ( !nodeId.equals(node.getNodeId()) ) {
				log.warn("Access DENIED to node {} for node {}; wrong node", nodeId, node.getNodeId());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			return;
		}

		if ( actor instanceof SecurityUser user ) {
			if ( !user.getUserId().equals(ownership.getUserId()) ) {
				log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, user.getEmail());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			return;
		}

		if ( actor instanceof SecurityToken token ) {
			if ( SecurityTokenType.User.equals(token.getTokenType()) ) {
				// node must be owned by token user
				if ( !token.getUserId().equals(ownership.getUserId()) ) {
					log.warn("Access DENIED to node {} for token {}; wrong user", nodeId,
							token.getToken());
					throw new AuthorizationException(ACCESS_DENIED, nodeId);
				}
				if ( token.getPolicy() != null && token.getPolicy().getNodeIds() != null
						&& !token.getPolicy().getNodeIds().contains(nodeId) ) {
					// policy specifies nodes, so policy must include the requested node ID
					log.warn("Access DENIED to node {} for token {}; node not included", nodeId,
							token.getToken());
					throw new AuthorizationException(ACCESS_DENIED, nodeId);
				}
				return;
			}
		}

		log.warn("Access DENIED to node {} for actor {}", nodeId, actor);
		throw new AuthorizationException(ACCESS_DENIED, nodeId);
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
		requireNodeReadAccess(nodeId, log);
	}

	/**
	 * Require the active user have "read" access to a given node ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param nodeId
	 *        the node ID to check
	 * @param log
	 *        the logger to use
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	public void requireNodeReadAccess(Long nodeId, Logger log) {
		final SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(nodeId);
		if ( ownership == null ) {
			log.warn("Access DENIED to node {}; owner not found", nodeId);
			throw new AuthorizationException(UNKNOWN_OBJECT, nodeId);
		}
		if ( !ownership.isRequiresAuthorization() ) {
			return;
		}

		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to node {} for non-authenticated user", nodeId);
			throw new AuthorizationException(ACCESS_DENIED, nodeId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode node ) {
			if ( ownership.isArchived() ) {
				throw new AuthorizationException(UNKNOWN_OBJECT, nodeId);
			}
			if ( !nodeId.equals(node.getNodeId()) ) {
				log.warn("Access DENIED to node {} for node {}; wrong node", nodeId, node.getNodeId());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			return;
		}

		if ( actor instanceof SecurityUser user ) {
			if ( !user.getUserId().equals(ownership.getUserId()) ) {
				log.warn("Access DENIED to node {} for user {}; wrong user", nodeId, user.getEmail());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			return;
		}

		if ( actor instanceof SecurityToken token ) {
			if ( SecurityTokenType.ReadNodeData.equals(token.getTokenType()) ) {
				if ( ownership.isArchived() ) {
					throw new AuthorizationException(UNKNOWN_OBJECT, nodeId);
				}
			}
			// node must be owned by token user
			if ( !token.getUserId().equals(ownership.getUserId()) ) {
				log.warn("Access DENIED to node {} for token {}; wrong user", nodeId, token.getToken());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			if ( token.getPolicy() != null && token.getPolicy().getNodeIds() != null
					&& !token.getPolicy().getNodeIds().contains(nodeId) ) {
				// policy specifies nodes, so policy must include the requested node ID
				log.warn("Access DENIED to node {} for token {}; node not included", nodeId,
						token.getToken());
				throw new AuthorizationException(ACCESS_DENIED, nodeId);
			}
			return;
		}

		log.warn("Access DENIED to node {} for actor {}", nodeId, actor);
		throw new AuthorizationException(ACCESS_DENIED, nodeId);
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
	 */
	protected void requireUserWriteAccess(Long userId) {
		requireUserWriteAccess(userId, log);
	}

	/**
	 * Require the active user have "write" access to a given user ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param userId
	 *        the user ID to check
	 * @param log
	 *        the logger to use
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	public void requireUserWriteAccess(Long userId, Logger log) {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to user {} for non-authenticated user", userId);
			throw new AuthorizationException(ACCESS_DENIED, userId);
		}

		if ( actor instanceof SecurityUser user ) {
			if ( !user.getUserId().equals(userId) ) {
				log.warn("Access DENIED to user {} for user {}; wrong user", userId, user.getEmail());
				throw new AuthorizationException(ACCESS_DENIED, userId);
			}
			return;
		}

		if ( actor instanceof SecurityToken token ) {
			if ( SecurityTokenType.User.equals(token.getTokenType()) ) {
				// user token, so user ID must match node user's ID
				if ( !token.getUserId().equals(userId) ) {
					log.warn("Access DENIED to user {} for token {}; wrong user", userId,
							token.getToken());
					throw new AuthorizationException(ACCESS_DENIED, userId);
				}
				return;
			}
		}

		log.warn("Access DENIED to user {} for actor {}", userId, actor);
		throw new AuthorizationException(ACCESS_DENIED, userId);
	}

	/**
	 * Get a {@link SecurityPolicy} for the active user, if available.
	 * 
	 * @return The active user's policy, or {@code null}.
	 */
	public SecurityPolicy getActiveSecurityPolicy() {
		return SecurityUtils.getActiveSecurityPolicy();
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
	 */
	protected void requireUserReadAccess(Long userId) {
		requireUserReadAccess(userId, log);
	}

	/**
	 * Require the active user have "read" access to a given user ID. If the
	 * active user is not authorized, a {@link AuthorizationException} will be
	 * thrown.
	 * 
	 * @param userId
	 *        the user ID to check
	 * @param log
	 *        the logger to use
	 * @throws AuthorizationException
	 *         if the authorization check fails
	 * @since 1.1
	 */
	public void requireUserReadAccess(Long userId, Logger log) {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			log.warn("Access DENIED to user {} for non-authenticated user", userId);
			throw new AuthorizationException(ACCESS_DENIED, userId);
		}

		// node requires authentication
		if ( actor instanceof SecurityNode node ) {
			final SolarNodeOwnership ownership = (node.getNodeId() != null
					? nodeOwnershipDao.ownershipForNodeId(node.getNodeId())
					: null);
			if ( ownership == null ) {
				log.warn("Access DENIED to user {} for node {}; not found", userId, node.getNodeId());
				throw new AuthorizationException(UNKNOWN_OBJECT, userId);
			}
			if ( !userId.equals(ownership.getUserId()) ) {
				log.warn("Access DENIED to user {} for node {}; wrong node", userId, node.getNodeId());
				throw new AuthorizationException(ACCESS_DENIED, userId);
			}
			return;
		}

		if ( actor instanceof SecurityUser user ) {
			if ( !user.getUserId().equals(userId) ) {
				log.warn("Access DENIED to user {} for user {}; wrong user", userId, user.getEmail());
				throw new AuthorizationException(ACCESS_DENIED, userId);
			}
			return;
		}

		if ( actor instanceof SecurityToken token ) {
			// token, so user ID must match token owner's ID
			if ( !token.getUserId().equals(userId) ) {
				log.warn("Access DENIED to user {} for token {}; wrong user", userId, token.getToken());
				throw new AuthorizationException(ACCESS_DENIED, userId);
			}
			if ( SecurityTokenType.ReadNodeData.equals(token.getTokenType()) ) {
				// data token, the token must include a user metadata policy that can be enforced
				if ( token.getPolicy() == null || token.getPolicy().getUserMetadataPaths() == null
						|| token.getPolicy().getUserMetadataPaths().isEmpty() ) {
					log.warn(
							"Access DENIED to user {} for token {}; user metadata not included in policy",
							userId, token.getToken());
					throw new AuthorizationException(ACCESS_DENIED, userId);
				}
			}
			return;
		}

		log.warn("Access DENIED to user {} for actor {}", userId, actor);
		throw new AuthorizationException(ACCESS_DENIED, userId);
	}

	/**
	 * Enforce a security policy on a domain object and
	 * {@code SecurityPolicyMetadataType#Node} metadata type.
	 * 
	 * @param <T>
	 *        the domain object type
	 * @param domainObject
	 *        The domain object to enforce the active policy on.
	 * @return The domain object to use.
	 * @throws AuthorizationException
	 *         If the policy check fails.
	 */
	public <T> T policyEnforcerCheck(T domainObject) {
		return policyEnforcerCheck(domainObject, SecurityPolicyMetadataType.Node);
	}

	/**
	 * Enforce a security policy on a domain object or collection of domain
	 * objects.
	 * 
	 * The {@link FilterResults} API is supported, as is {@link List}.
	 * 
	 * @param <T>
	 *        the domain object type
	 * @param domainObject
	 *        The domain object to enforce the active policy on.
	 * @param metadataType
	 *        The metadata type to enforce the active policy on.
	 * @return The domain object to use.
	 * @throws AuthorizationException
	 *         If the policy check fails.
	 */
	public <T> T policyEnforcerCheck(T domainObject, SecurityPolicyMetadataType metadataType) {
		Authentication authentication = SecurityUtils.getCurrentAuthentication();
		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null || domainObject == null ) {
			return domainObject;
		}

		final Object principal = (authentication != null ? authentication.getPrincipal() : null);

		if ( domainObject instanceof FilterResults<?> filterResults ) {
			Collection<Object> filteredObjects = policyEnforcedCollection(filterResults, policy,
					principal, metadataType);
			@SuppressWarnings("unchecked")
			T result = (T) new BasicFilterResults<Object>(filteredObjects,
					filterResults.getTotalResults(), filterResults.getStartingOffset(),
					filterResults.getReturnedResultCount());
			return result;
		} else if ( domainObject instanceof List<?> collectionResults ) {
			@SuppressWarnings("unchecked")
			T filteredObjects = (T) policyEnforcedCollection(collectionResults, policy, principal,
					metadataType);
			return filteredObjects;
		}

		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy,
				(authentication != null ? authentication.getPrincipal() : null), domainObject,
				pathMatcher, metadataType);
		enforcer.verify();
		return SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
	}

	private Collection<Object> policyEnforcedCollection(Iterable<?> input, SecurityPolicy policy,
			Object principal, SecurityPolicyMetadataType metadataType) {
		if ( input == null ) {
			return null;
		}
		List<Object> enforced = new ArrayList<Object>();
		for ( Object obj : input ) {
			SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, principal, obj,
					pathMatcher, metadataType);
			enforcer.verify();
			enforced.add(SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer));
		}
		return enforced;
	}

	/**
	 * Get the path matcher to use.
	 * 
	 * @return the path matcher
	 */
	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Set the path matcher to use.
	 * 
	 * @param pathMatcher
	 *        the matcher to use
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

}
