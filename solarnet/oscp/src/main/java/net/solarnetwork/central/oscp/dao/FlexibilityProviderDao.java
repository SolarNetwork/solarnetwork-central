/* ==================================================================
 * FlexibilityProviderDao.java - 16/08/2022 5:28:23 pm
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

/**
 * DAO API for flexibility provider authorization.
 *
 * @author matt
 * @version 1.0
 */
public interface FlexibilityProviderDao extends AuthTokenAuthorizationDao {

	/**
	 * Create an authorization token for a given ID.
	 *
	 * <p>
	 * Calling this will replace any existing token for the given {@code id}, or
	 * create a new one if none already exists.
	 * </p>
	 *
	 * @param id
	 *        the ID to create the authorization token for
	 * @return the new token
	 */
	String createAuthToken(UserLongCompositePK id);

}
