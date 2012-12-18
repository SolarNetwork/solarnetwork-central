/* ==================================================================
 * BaseAuthToken.java - Dec 18, 2012 2:59:46 PM
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

import net.solarnetwork.central.domain.BaseStringEntity;
import org.joda.time.DateTime;

/**
 * Base class for auth tokens.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseAuthToken extends BaseStringEntity {

	private static final long serialVersionUID = 2983876404718478096L;

	private String authSecret;
	private UserAuthTokenStatus status;

	/**
	 * Default constructor.
	 */
	public BaseAuthToken() {
		super();
	}

	/**
	 * Create a new, active token.
	 * 
	 * <p>
	 * The status will be set to {@link UserAuthTokenStatus#v} and the
	 * {@code created} date will be set to the current time.
	 * </p>
	 * 
	 * @param token
	 *        the token value
	 * @param secret
	 *        the secret
	 */
	public BaseAuthToken(String token, String secret) {
		super();
		setId(token);
		setAuthSecret(secret);
		setStatus(UserAuthTokenStatus.v);
		setCreated(new DateTime());
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

}
