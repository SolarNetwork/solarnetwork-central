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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Extension of Spring Security's {@link User} object to add SolarNetwork
 * attributes.
 * 
 * @author matt
 * @version $Revision$
 */
public class AuthenticatedNode implements UserDetails, SecurityNode {

	private static final long serialVersionUID = -7909158037651579050L;

	private final Long nodeId;
	private final Collection<GrantedAuthority> authorities;

	/**
	 * Construct from existing {@link User} and a node ID.
	 * 
	 * @param user
	 *        the user
	 * @param nodeId
	 *        the node ID
	 */
	public AuthenticatedNode(Long nodeId, Collection<GrantedAuthority> auths) {
		this.nodeId = nodeId;
		authorities = auths;
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
		return "";
	}

	@Override
	public String getUsername() {
		return nodeId.toString();
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

}
