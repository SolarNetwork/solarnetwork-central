/* ==================================================================
 * AuthTokenAuthorizationDao.java - 17/08/2022 10:05:19 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao;

import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;

/**
 * DAO API for token authorization support.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuthTokenAuthorizationDao {

	/**
	 * Get a valid authorization ID for a given authorization token.
	 * 
	 * @param token
	 *        the token to get the ID for
	 * @param oauth
	 *        {@literal true} if the token is an OAuth value, {@literal false}
	 *        if OSCP
	 * @return the ID, or {@literal null} if not available (or disabled)
	 */
	UserLongCompositePK idForToken(String token, boolean oauth);

	/**
	 * Get the role associated with an authorization ID.
	 * 
	 * @param authId
	 *        the authorization ID to get the role for, as previously returned
	 *        from {@link #idForToken(String)}
	 * @return the role, or {@literal null} if not available (or disabled)
	 */
	AuthRoleInfo roleForAuthorization(UserLongCompositePK authId);

}
