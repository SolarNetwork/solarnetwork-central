/* ==================================================================
 * UserAuthToken.java - Dec 12, 2012 1:21:40 PM
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

package net.solarnetwork.central.user.domain;

import java.util.HashSet;
import java.util.Set;
import net.solarnetwork.central.domain.BaseStringEntity;

/**
 * A user authorization token.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAuthToken extends BaseStringEntity {

	private static final long serialVersionUID = 899722294031488962L;

	private Long userId;
	private String authSecret;
	private UserAuthTokenStatus status;
	private UserAuthTokenType type;
	private Set<Long> nodeIds = new HashSet<Long>(5);

	/**
	 * Default constructor.
	 */
	public UserAuthToken() {
		super();
	}

	/**
	 * Create a new, active token.
	 * 
	 * @param token
	 *        the token value
	 * @param userId
	 *        the user ID
	 * @param secret
	 *        the secret
	 * @param type
	 *        the type
	 */
	public UserAuthToken(String token, Long userId, String secret, UserAuthTokenType type) {
		super();
		setId(token);
		setUserId(userId);
		setAuthSecret(secret);
		setStatus(UserAuthTokenStatus.Active);
		setType(type);
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the ID value.
	 * 
	 * <p>
	 * This is just an alias for {@link BaseStringEntity#getId()}.
	 * </p>
	 * 
	 * @return the auth token
	 */
	public String getAuthToken() {
		return getId();
	}

	/**
	 * Set the ID value.
	 * 
	 * <p>
	 * This is just an alias for {@link BaseStringEntity#setId(String)}.
	 * </p>
	 * 
	 * @param authToken
	 *        the auth token
	 */
	public void setAuthToken(String authToken) {
		setId(authToken);
	}

	public String getAuthSecret() {
		return authSecret;
	}

	public void setAuthSecret(String authSecret) {
		this.authSecret = authSecret;
	}

	public UserAuthTokenStatus getStatus() {
		return status;
	}

	public void setStatus(UserAuthTokenStatus status) {
		this.status = status;
	}

	public UserAuthTokenType getType() {
		return type;
	}

	public void setType(UserAuthTokenType type) {
		this.type = type;
	}

	public Set<Long> getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(Set<Long> nodeIds) {
		this.nodeIds = nodeIds;
	}

}
