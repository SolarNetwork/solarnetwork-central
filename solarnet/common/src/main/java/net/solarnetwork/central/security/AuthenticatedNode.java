/* ==================================================================
 * AuthenticatedNode.java - Mar 17, 2011 1:16:26 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Implementation of Spring Security's {@link UserDetails} object for
 * authenticated nodes.
 * 
 * @author matt
 * @version 1.1
 */
public class AuthenticatedNode implements UserDetails, SecurityNode {

	private static final long serialVersionUID = -3196310376474763843L;

	private final Long nodeId;
	private final Collection<GrantedAuthority> authorities;
	private final String username;
	private final String password;
	private final boolean authenticatedWithToken;

	/**
	 * Construct from and a node ID.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param auths
	 *        the granted authorities
	 * @param authenticatedWithToken
	 *        the authenticated with token flag
	 */
	public AuthenticatedNode(Long nodeId, Collection<GrantedAuthority> auths,
			boolean authenticatedWithToken) {
		this(nodeId, nodeId.toString(), "", auths, authenticatedWithToken);
	}

	/**
	 * Construct from a node ID, username, and password.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param username
	 *        the username, e.g. auth token
	 * @param password
	 *        the password, e.g. auth secret
	 * @param auths
	 *        the granted authorities
	 * @param authenticatedWithToken
	 *        the authenticated with token flag
	 */
	public AuthenticatedNode(Long nodeId, String username, String password,
			Collection<GrantedAuthority> auths, boolean authenticatedWithToken) {
		super();
		this.username = username;
		this.password = password;
		this.nodeId = nodeId;
		this.authorities = auths;
		this.authenticatedWithToken = authenticatedWithToken;
	}

	/**
	 * @return the nodeId
	 */
	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isAuthenticatedWithToken() {
		return authenticatedWithToken;
	}

}
