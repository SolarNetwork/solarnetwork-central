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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.security;

import java.util.Collection;
import java.util.Collections;
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

/**
 * Security helper methods.
 * 
 * @author matt
 * @version 1.1
 */
public class SecurityUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

	/**
	 * Authenticate a user.
	 * 
	 * @param authenticationManager
	 *        the {@link AuthenticationManager}
	 * @param user
	 *        the user to authenticate
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
	 * Become a user with a {@code RUN_AS_ROLE_USER} authority.
	 * 
	 * @param username
	 *        the username (email) to use
	 * @param name
	 *        the name
	 * @param userId
	 *        the user ID
	 * @since 1.1
	 */
	public static void becomeUser(String username, String name, Long userId) {
		User userDetails = new User(username, "", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, name, false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton((GrantedAuthority) new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
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

}
