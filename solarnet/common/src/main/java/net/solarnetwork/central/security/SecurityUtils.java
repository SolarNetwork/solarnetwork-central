/* ==================================================================
 * SecurityUtils.java - Nov 22, 2012 7:34:27 AM
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

package net.solarnetwork.central.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * Security helper methods.
 * 
 * @author matt
 * @version 2.0
 */
public class SecurityUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

	/**
	 * Authenticate a user.
	 * 
	 * @param authenticationManager
	 *        the {@link AuthenticationManager}
	 * @param username
	 *        the username to authenticate
	 * @param password
	 *        the password to authenticate
	 */
	public static void authenticate(AuthenticationManager authenticationManager, Object username,
			Object password) {
		try {
			UsernamePasswordAuthenticationToken usernameAndPassword = new UsernamePasswordAuthenticationToken(
					username, password);
			Authentication auth = authenticationManager.authenticate(usernameAndPassword);
			SecurityContextHolder.getContext().setAuthentication(auth);
		} catch ( AuthenticationException e ) {
			SecurityContextHolder.getContext().setAuthentication(null);
			throw e;
		}
	}

	/**
	 * Become an authenticated token with a {@code RUN_AS_ROLE_USER} authority.
	 * 
	 * @param tokenId
	 *        the token ID to use
	 * @param type
	 *        the token type
	 * @param userId
	 *        the user ID
	 * @param policy
	 *        the security policy to use
	 * @since 2.0
	 */
	public static void becomeToken(String tokenId, SecurityTokenType type, Long userId,
			SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES), type,
				userId, policy);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(token, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Become a user with a {@code RUN_AS_ROLE_USER} authority.
	 * 
	 * @param username
	 *        the username (email) to use
	 * @param name
	 *        the name
	 * @param userId
	 *        the user ID
	 * @since 2.0
	 */
	public static void becomeUser(String username, String name, Long userId) {
		User userDetails = new User(username, "", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, name, false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Become a node with a {@code RUN_AS_ROLE_NODE} authority.
	 * 
	 * @param nodeId
	 *        the node ID to become
	 * @since 1.4
	 */
	public static void becomeNode(Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, NodeUserDetailsService.AUTHORITIES,
				false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_NODE"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(node, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Require any one of a set of roles for the current actor. The actor's
	 * roles are converted to upper case before testing for inclusion in the
	 * {@code roles} argument.
	 * 
	 * @param roles
	 *        the roles, one of which is required
	 * @since 1.2
	 */
	public static void requireAnyRole(final Set<String> roles) {
		Authentication auth = getCurrentAuthentication();
		if ( auth == null || auth.isAuthenticated() == false ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED,
					null);
		}
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( roles.contains(role.getAuthority().toUpperCase()) ) {
				return;
			}
		}
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
	}

	/**
	 * Require any one of a set of roles for the current actor. The actor's
	 * roles are converted to upper case before testing for inclusion in the
	 * {@code roles} argument.
	 * 
	 * @param roles
	 *        the required roles
	 * @since 1.2
	 */
	public static void requireAllRoles(final Set<String> roles) {
		Authentication auth = getCurrentAuthentication();
		if ( auth == null || auth.isAuthenticated() == false ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED,
					null);
		}
		Set<String> rolesCopy = new HashSet<String>(roles);
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( rolesCopy.remove(role.getAuthority().toUpperCase()) == false ) {
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
			if ( rolesCopy.size() < 1 ) {
				return;
			}
		}
		if ( rolesCopy.size() > 0 ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
	}

	/**
	 * Get the current active authentication.
	 * 
	 * @return the active Authentication, or <em>null</em> if none available
	 */
	public static Authentication getCurrentAuthentication() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth == null ) {
			LOG.debug("No Authentication available, cannot tell current user");
			return null;
		}
		return auth;
	}

	/**
	 * Get the current {@link SecurityActor}.
	 * 
	 * @return the current actor, never <em>null</em>
	 * @throws SecurityException
	 *         if the actor is not available
	 */
	public static SecurityActor getCurrentActor() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityActor ) {
			return (SecurityActor) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityActor ) {
			return (SecurityActor) auth.getDetails();
		}
		throw new SecurityException("Actor not available");
	}

	/**
	 * Get the current {@link SecurityActor}'s {@code userId}.
	 * 
	 * @return The user ID of the current {@link SecurityActor} (never
	 *         <em>null</em>).
	 * @throws SecurityException
	 *         If the actor is not available.
	 * @since 1.3
	 */
	public static Long getCurrentActorUserId() throws SecurityException {
		SecurityActor actor = getCurrentActor();
		Long userId = null;
		if ( actor instanceof SecurityUser ) {
			userId = ((SecurityUser) actor).getUserId();
		} else if ( actor instanceof SecurityToken ) {
			userId = ((SecurityToken) actor).getUserId();
		}
		if ( userId != null ) {
			return userId;
		}
		throw new SecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityToken}.
	 * 
	 * @return the current actor, never <em>null</em>
	 * @throws SecurityException
	 *         if the actor is not available
	 */
	public static SecurityToken getCurrentToken() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityToken ) {
			return (SecurityToken) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityToken ) {
			return (SecurityToken) auth.getDetails();
		}
		throw new SecurityException("Token not available");
	}

	/**
	 * Get the current {@link SecurityUser}.
	 * 
	 * @return the current user, never <em>null</em>
	 * @throws SecurityException
	 *         if the user is not available
	 */
	public static SecurityUser getCurrentUser() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityUser ) {
			return (SecurityUser) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityUser ) {
			return (SecurityUser) auth.getDetails();
		}
		throw new SecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityNode}.
	 * 
	 * @return the current node, never <em>null</em>
	 * @throws SecurityException
	 *         if the node is not available
	 */
	public static SecurityNode getCurrentNode() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityNode ) {
			return (SecurityNode) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityNode ) {
			return (SecurityNode) auth.getDetails();
		}
		throw new SecurityException("Node not available");
	}

	/**
	 * Get all node IDs the current actor is authorized to access.
	 * 
	 * @param nodeOwnershipDao
	 *        The DAO to use to fill in all available nodes for user-based
	 *        actors, or {@code null} to not fill in nodes.
	 * @return The allowed node IDs.
	 * @throws AuthorizationException
	 *         if no node IDs are allowed or there is no actor
	 * @since 2.0
	 */
	public static Long[] authorizedNodeIdsForCurrentActor(SolarNodeOwnershipDao nodeOwnershipDao) {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( SecurityException e ) {
			LOG.warn("Access DENIED to node {} for non-authenticated user");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		if ( actor instanceof SecurityNode ) {
			SecurityNode node = (SecurityNode) actor;
			return new Long[] { node.getNodeId() };
		} else if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			// default to all nodes for actor
			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(user.getUserId());
			if ( ownerships != null && ownerships.length > 0 ) {
				return Arrays.stream(ownerships).map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
			}
		} else if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			Long[] result = null;
			// get full list to all nodes for actor; in future could optimize with query 
			// that accepts policy node IDs to restrict result to
			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(token.getUserId());
			Long[] allNodeIds = (ownerships != null
					? Arrays.stream(ownerships).map(SolarNodeOwnership::getNodeId).toArray(Long[]::new)
					: null);
			Set<Long> restrictedToNodeIds = tokenRestrictedNodeIds(token);
			if ( restrictedToNodeIds != null ) {
				result = Arrays.stream(allNodeIds).filter(e -> restrictedToNodeIds.contains(e))
						.toArray(Long[]::new);
			} else {
				result = allNodeIds;
			}
			if ( result != null && result.length > 0 ) {
				return result;
			}
		}
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
	}

	private static Set<Long> tokenRestrictedNodeIds(SecurityToken token) {
		Set<Long> restrictedToNodeIds = null;
		if ( SecurityTokenType.User == token.getTokenType() ) {
			restrictedToNodeIds = (token.getPolicy() != null && token.getPolicy().getNodeIds() != null
					? token.getPolicy().getNodeIds()
					: null);
		} else if ( SecurityTokenType.ReadNodeData == token.getTokenType() ) {
			// all node IDs in token
			restrictedToNodeIds = (token.getPolicy() != null && token.getPolicy().getNodeIds() != null
					? token.getPolicy().getNodeIds()
					: Collections.emptySet());
		}
		return restrictedToNodeIds;
	}

}
