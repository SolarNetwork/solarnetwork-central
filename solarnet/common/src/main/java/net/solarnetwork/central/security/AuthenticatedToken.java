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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * {@link SecurityUser} implementation for authenticated tokens.
 * 
 * @author matt
 * @version 2.1
 */
public class AuthenticatedToken extends User implements SecurityToken {

	private static final long serialVersionUID = -4857188995583662187L;

	private final SecurityTokenType tokenType;
	private final Long userId;
	private final SecurityPolicy policy;

	/**
	 * Construct with values.
	 * 
	 * @param user
	 *        the user details
	 * @param tokenType
	 *        the token type
	 * @param userId
	 *        the user ID (that the token belongs to)
	 * @param policy
	 *        optional policy associated with the token
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code policy} is {@literal null}
	 */
	public AuthenticatedToken(UserDetails user, SecurityTokenType tokenType, Long userId,
			SecurityPolicy policy) {
		super(requireNonNullArgument(user, "user").getUsername(), user.getPassword(), user.isEnabled(),
				user.isAccountNonExpired(), user.isCredentialsNonExpired(), user.isAccountNonLocked(),
				user.getAuthorities());
		this.tokenType = requireNonNullArgument(tokenType, "tokenType");
		this.userId = requireNonNullArgument(userId, "userId");
		this.policy = policy;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("AuthenticatedToken{type=");
		buf.append(tokenType);
		buf.append(",token=");
		buf.append(getToken());
		buf.append(",userId=");
		buf.append(userId);
		if ( policy != null ) {
			buf.append(",policy=");
			buf.append(policy);
		}
		buf.append("}");
		return buf.toString();
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
	public SecurityTokenType getTokenType() {
		return tokenType;
	}

	@Override
	public SecurityPolicy getPolicy() {
		return policy;
	}

}
