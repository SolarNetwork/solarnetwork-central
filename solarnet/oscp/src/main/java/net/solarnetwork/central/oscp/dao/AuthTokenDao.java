/* ==================================================================
 * AuthTokenDao.java - 16/08/2022 4:08:49 pm
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
 * API for an OSCP authorization token DAO.
 * 
 * @author matt
 * @version 1.0
 */
public interface AuthTokenDao {

	/**
	 * Save an authorization token for a given ID.
	 * 
	 * <p>
	 * Calling this will replace any existing token for the given {@code id}, or
	 * create a new one if none already exists.
	 * </p>
	 * 
	 * @param id
	 *        the ID to save the authorization token for
	 * @param token
	 *        the authorization token to save
	 */
	void saveAuthToken(UserLongCompositePK id, String token);
}
