/* ==================================================================
 * AuthenticatedToken.java - Mar 22, 2013 3:23:20 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link SecurityUser} implementation for authenticated tokens.
 * 
 * @author matt
 * @version 1.1
 */
public class AuthenticatedToken extends User implements SecurityToken {

	private static final long serialVersionUID = -4857188995583662187L;

	private final String tokenType;
	private final Long userId;
	private final SecurityPolicy policy;

	/**
	 * Construct with values.
	 * 
	 * @param tokenType
	 *        the token type
	 * @param userId
	 *        the user ID (that the token belongs to)
	 * @param policy
	 *        optional policy associated with the token
	 */
	public AuthenticatedToken(UserDetails user, String tokenType, Long userId, SecurityPolicy policy) {
		super(user.getUsername(), user.getPassword(), user.isEnabled(), user.isAccountNonExpired(),
				user.isCredentialsNonExpired(), user.isAccountNonLocked(), user.getAuthorities());
		this.tokenType = tokenType;
		this.userId = userId;
		this.policy = policy;
	}

	@Override
	public boolean isAuthenticatedWithToken() {
		return true;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public String getToken() {
		return getUsername();
	}

	@Override
	public String getTokenType() {
		return tokenType;
	}

	@Override
	public SecurityPolicy getPolicy() {
		return policy;
	}

}
