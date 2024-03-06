/* ==================================================================
 * SecurityToken.java - Mar 22, 2013 4:02:46 PM
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

import net.solarnetwork.central.domain.UserIdRelated;

/**
 * A token based actor.
 * 
 * @author matt
 * @version 2.1
 */
public interface SecurityToken extends SecurityActor, UserIdRelated {

	/**
	 * Get a unique user ID that owns the token.
	 * 
	 * @return the user ID
	 */
	@Override
	Long getUserId();

	/**
	 * Get the token value.
	 * 
	 * @return the token
	 */
	String getToken();

	/**
	 * Get the type of token.
	 * 
	 * @return the token type
	 */
	SecurityTokenType getTokenType();

	/**
	 * Get an optional security policy.
	 * 
	 * @return optional security policy
	 */
	SecurityPolicy getPolicy();
}
