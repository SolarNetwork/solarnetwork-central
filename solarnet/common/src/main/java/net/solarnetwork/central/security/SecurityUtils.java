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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.util.CollectionUtils;

/**
 * Security helper methods.
 *
 * @author matt
 * @version 3.1
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
	 * Clear the current authentication.
	 *
	 * @since 2.2
	 */
	public static void removeAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);
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
	public static SecurityToken becomeToken(String tokenId, SecurityTokenType type, Long userId,
			SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new User(tokenId, "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES), type,
				userId, policy);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(token, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
		return token;
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
	public static SecurityUser becomeUser(String username, String name, Long userId) {
		User userDetails = new User(username, "", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, name, false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
		return user;
	}

	/**
	 * Become a node with a {@code RUN_AS_ROLE_NODE} authority.
	 *
	 * @param nodeId
	 *        the node ID to become
	 * @since 1.4
	 */
	public static SecurityNode becomeNode(Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, NodeUserDetailsService.AUTHORITIES,
				false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("RUN_AS_ROLE_NODE"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(node, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
		return node;
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
		if ( auth == null || !auth.isAuthenticated() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED,
					null);
		}
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( roles.contains(role.getAuthority().toUpperCase(Locale.ENGLISH)) ) {
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
		if ( auth == null || !auth.isAuthenticated() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED,
					null);
		}
		Set<String> rolesCopy = new HashSet<>(roles);
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( !rolesCopy.remove(role.getAuthority().toUpperCase(Locale.ENGLISH)) ) {
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
			if ( rolesCopy.isEmpty() ) {
				return;
			}
		}
		if ( !rolesCopy.isEmpty() ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
	}

	/**
	 * Get the current active authentication.
	 *
	 * @return the active Authentication, or {@literal null} if none available
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
	 * @return the current actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available
	 */
	public static SecurityActor getCurrentActor() throws BasicSecurityException {
		return getActor(getCurrentAuthentication());
	}

	/**
	 * Get the actor for a given principal.
	 *
	 * @param principal
	 *        the principal
	 * @return the actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available
	 * @since 2.5
	 */
	public SecurityActor getActor(Principal principal) {
		if ( principal instanceof SecurityActor a ) {
			return a;
		} else if ( principal instanceof Authentication a ) {
			return getActor(a);
		}
		throw new BasicSecurityException("User ID not available.");
	}

	/**
	 * Get the actor for a given authentication.
	 *
	 * @param auth
	 *        the authentication
	 * @return the actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available
	 * @since 2.1
	 */
	public static SecurityActor getActor(Authentication auth) {
		if ( auth instanceof SecurityActor a ) {
			return a;
		} else if ( auth != null && auth.getPrincipal() instanceof SecurityActor a ) {
			return a;
		} else if ( auth != null && auth.getDetails() instanceof SecurityActor a ) {
			return a;
		}
		throw new BasicSecurityException("Actor not available");
	}

	/**
	 * Get the current {@link SecurityActor}'s {@code userId}.
	 *
	 * @return The user ID of the current {@link SecurityActor} (never
	 *         {@literal null}).
	 * @throws BasicSecurityException
	 *         If the user ID is not available.
	 * @since 1.3
	 */
	public static Long getCurrentActorUserId() throws BasicSecurityException {
		return getActorUserId(getCurrentAuthentication());
	}

	/**
	 * Get the ID of the user associated with a given authentication.
	 *
	 * @param principal
	 *        the user principal
	 * @return the ID of the user associated with the actor, never
	 *         {@literal null}
	 * @throws BasicSecurityException
	 *         if the user ID is not available
	 * @since 2.1
	 */
	public static Long getActorUserId(Principal principal) throws BasicSecurityException {
		if ( principal instanceof UserIdRelated u ) {
			return u.getUserId();
		} else if ( principal instanceof Authentication auth ) {
			if ( auth.getDetails() instanceof UserIdRelated u ) {
				return u.getUserId();
			} else {
				SecurityActor actor = getActor(auth);
				if ( actor instanceof UserIdRelated u ) {
					return u.getUserId();
				}
			}
		}
		throw new BasicSecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityToken}.
	 *
	 * @return the current actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available
	 */
	public static SecurityToken getCurrentToken() throws BasicSecurityException {
		return getToken(getCurrentAuthentication());
	}

	/**
	 * Get a {@link SecurityToken} for a given authentication.
	 *
	 * @param auth
	 *        the authentication
	 * @return the token actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available or not a token
	 * @since 2.1
	 */
	public static SecurityToken getToken(Authentication auth) throws BasicSecurityException {
		if ( auth != null && auth.getPrincipal() instanceof SecurityToken ) {
			return (SecurityToken) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityToken ) {
			return (SecurityToken) auth.getDetails();
		}
		throw new BasicSecurityException("Token not available");
	}

	/**
	 * Get the current {@link SecurityToken#getToken()}, if available.
	 *
	 * @return the token, or {@literal null} if a token is not available
	 * @since 2.2
	 */
	public static String currentTokenId() {
		try {
			return getCurrentToken().getToken();
		} catch ( BasicSecurityException e ) {
			return null;
		}
	}

	/**
	 * Get the current {@link SecurityUser}.
	 *
	 * @return the current user, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the user is not available
	 */
	public static SecurityUser getCurrentUser() throws BasicSecurityException {
		return getUser(getCurrentAuthentication());
	}

	/**
	 * Get a {@link SecurityUser} for a given authentication.
	 *
	 * @param auth
	 *        the authentication
	 * @return the user actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available or is not a user
	 * @since 2.1
	 */
	public static SecurityUser getUser(Authentication auth) throws BasicSecurityException {
		if ( auth != null && auth.getPrincipal() instanceof SecurityUser ) {
			return (SecurityUser) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityUser ) {
			return (SecurityUser) auth.getDetails();
		}
		throw new BasicSecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityNode}.
	 *
	 * @return the current node, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the node is not available
	 */
	public static SecurityNode getCurrentNode() throws BasicSecurityException {
		return getNode(getCurrentAuthentication());
	}

	/**
	 * Get a {@link SecurityNode} for a given authentication.
	 *
	 * @param auth
	 *        the authentication
	 * @return the node actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available or is not a node
	 * @since 2.1
	 */
	public static SecurityNode getNode(Authentication auth) throws BasicSecurityException {
		if ( auth != null && auth.getPrincipal() instanceof SecurityNode ) {
			return (SecurityNode) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityNode ) {
			return (SecurityNode) auth.getDetails();
		}
		throw new BasicSecurityException("Node not available");
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
		return authorizedNodeIds(getCurrentAuthentication(), nodeOwnershipDao);
	}

	/**
	 * Get all node IDs the given authentication is authorized to access.
	 *
	 * @param auth
	 *        the authentication
	 * @param nodeOwnershipDao
	 *        the DAO to use to fill in all available nodes for user-based
	 *        actors, or {@code null} to not fill in nodes
	 * @return the allowed node IDs
	 * @throws AuthorizationException
	 *         if no node IDs are allowed or there is no actor
	 * @since 2.1
	 */
	public static Long[] authorizedNodeIds(Authentication auth, SolarNodeOwnershipDao nodeOwnershipDao) {
		final SecurityActor actor;
		try {
			actor = getActor(auth);
		} catch ( BasicSecurityException e ) {
			LOG.warn("Access DENIED to nodes for non-authenticated user");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}

		if ( actor instanceof SecurityNode node ) {
			return new Long[] { node.getNodeId() };
		} else if ( actor instanceof SecurityUser user ) {
			// default to all nodes for actor
			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(user.getUserId());
			if ( ownerships != null && ownerships.length > 0 ) {
				return Arrays.stream(ownerships).map(SolarNodeOwnership::getNodeId).toArray(Long[]::new);
			}
		} else if ( actor instanceof SecurityToken token ) {
			Long[] result;
			// get full list to all nodes for actor; in future could optimize with query
			// that accepts policy node IDs to restrict result to
			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(token.getUserId());
			Long[] allNodeIds = (ownerships != null
					? Arrays.stream(ownerships).map(SolarNodeOwnership::getNodeId).toArray(Long[]::new)
					: null);
			Set<Long> restrictedToNodeIds = tokenRestrictedNodeIds(token);
			if ( restrictedToNodeIds != null && allNodeIds != null ) {
				result = Arrays.stream(allNodeIds).filter(restrictedToNodeIds::contains)
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

	/**
	 * Get a {@link SecurityPolicy} for the active user, if available.
	 *
	 * @return The active user's policy, or {@code null}.
	 * @since 2.2
	 */
	public static SecurityPolicy getActiveSecurityPolicy() {
		final SecurityActor actor;
		try {
			actor = SecurityUtils.getCurrentActor();
		} catch ( BasicSecurityException e ) {
			return null;
		}

		if ( actor instanceof SecurityToken token ) {
			return token.getPolicy();
		}

		return null;
	}

	/**
	 * Encrypt a set of map values associated with a set of key values.
	 *
	 * <p>
	 * This method will return a new map instance, unless no values need
	 * encrypting in which case {@code map} itself will be returned. For any key
	 * in {@code secureKeys} found in {@code map}, the returned map's value will
	 * be encrypted value computed by calling
	 * {@link TextEncryptor#encrypt(String)}.
	 * </p>
	 *
	 * <p>
	 * Any exception thrown by {@link TextEncryptor#encrypt(String)} will be
	 * ignored, and the original value will be used instead.
	 * </p>
	 *
	 * @param <K>
	 *        the key type
	 * @param <V>
	 *        the value type
	 * @param map
	 *        the map of values to encrypt
	 * @param secureKeys
	 *        the set of map keys whose values should be encrypted
	 * @param encryptor
	 *        the encryptor to use
	 * @return either a new map instance with one or more values encrypted, or
	 *         {@code map} when no values need encrypted
	 * @since 2.4
	 */
	public static <K, V> Map<K, V> encryptedMap(Map<K, V> map, Set<K> secureKeys,
			TextEncryptor encryptor) {
		assert encryptor != null;
		return encryptedMap(map, secureKeys, encryptor::encrypt);
	}

	/**
	 * Encrypt a set of map values associated with a set of key values.
	 *
	 * <p>
	 * This method will return a new map instance, unless no values need
	 * encrypting in which case {@code map} itself will be returned. For any key
	 * in {@code secureKeys} found in {@code map}, the returned map's value will
	 * be encrypted value computed by invoking {@code encryptor} on them.
	 * </p>
	 *
	 * <p>
	 * Any exception thrown by the {@code encryptor} function will be ignored,
	 * and the original value will be used instead.
	 * </p>
	 *
	 * @param <K>
	 *        the key type
	 * @param <V>
	 *        the value type
	 * @param map
	 *        the map of values to encrypt
	 * @param secureKeys
	 *        the set of map keys whose values should be encrypted
	 * @param encryptor
	 *        function to encrypt the secure key values with
	 * @return either a new map instance with one or more values encrypted, or
	 *         {@code map} when no values need encrypted
	 * @since 3.1
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> encryptedMap(Map<K, V> map, Set<K> secureKeys,
			Function<String, String> encryptor) {
		assert encryptor != null;
		return CollectionUtils.transformMap(map, secureKeys, (val) -> {
			var result = val;
			try {
				result = (V) (val == null ? null : encryptor.apply(val.toString()));
			} catch ( Exception e ) {
				// ignore and return input value
			}
			return result;
		});
	}

	/**
	 * Decrypt a set of map values associated with a set of key values.
	 *
	 * <p>
	 * This method will return a new map instance, unless no values need
	 * decrypting in which case {@code map} itself will be returned. For any key
	 * in {@code secureKeys} found in {@code map}, the returned map's value will
	 * be decrypted value computed by calling
	 * {@link TextEncryptor#decrypt(String)}.
	 * </p>
	 *
	 * <p>
	 * Any exception thrown by {@link TextEncryptor#decrypt(String)} will be
	 * ignored, and the original value will be used instead.
	 * </p>
	 *
	 * @param <K>
	 *        the key type
	 * @param <V>
	 *        the value type
	 * @param map
	 *        the map of values to encrypt
	 * @param secureKeys
	 *        the set of map keys whose values should be decrypted
	 * @param encryptor
	 *        the encryptor to use for decryption
	 * @return either a new map instance with one or more values encrypted, or
	 *         {@code map} when no values need encrypted
	 * @since 2.4
	 */
	public static <K, V> Map<K, V> decryptedMap(Map<K, V> map, Set<K> secureKeys,
			TextEncryptor encryptor) {
		assert encryptor != null;
		return decryptedMap(map, secureKeys, encryptor::decrypt);
	}

	/**
	 * Decrypt a set of map values associated with a set of key values.
	 *
	 * <p>
	 * This method will return a new map instance, unless no values need
	 * decrypting in which case {@code map} itself will be returned. For any key
	 * in {@code secureKeys} found in {@code map}, the returned map's value will
	 * be decrypted value computed by invoking {@code decryptor} on them.
	 * </p>
	 *
	 * <p>
	 * Any exception thrown by the {@code decryptor} function will be ignored,
	 * and the original value will be used instead.
	 * </p>
	 *
	 * @param <K>
	 *        the key type
	 * @param <V>
	 *        the value type
	 * @param map
	 *        the map of values to encrypt
	 * @param secureKeys
	 *        the set of map keys whose values should be decrypted
	 * @param decryptor
	 *        the decryptor to use
	 * @return either a new map instance with one or more values decrypted, or
	 *         {@code map} when no values need encrypted
	 * @since 3.1
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> decryptedMap(Map<K, V> map, Set<K> secureKeys,
			Function<String, String> decryptor) {
		assert decryptor != null;
		return CollectionUtils.transformMap(map, secureKeys, (val) -> {
			var result = val;
			try {
				result = (V) (val == null ? null : decryptor.apply(val.toString()));
			} catch ( Exception e ) {
				// ignore and return input value
			}
			return result;
		});
	}

}
