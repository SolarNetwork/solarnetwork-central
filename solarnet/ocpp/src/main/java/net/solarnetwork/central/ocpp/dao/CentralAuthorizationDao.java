/* ==================================================================
 * CentralAuthorizationDao.java - 25/02/2020 2:09:25 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao;

import java.util.Collection;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.ocpp.dao.AuthorizationDao;
import net.solarnetwork.ocpp.domain.Authorization;

/**
 * Extension to {@link AuthorizationDao} to support SolarNet.
 * 
 * <p>
 * This API implies
 * {@link net.solarnetwork.central.ocpp.domain.CentralAuthorization} entities
 * are used.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CentralAuthorizationDao extends AuthorizationDao {

	/**
	 * Get an authorization for a token.
	 * 
	 * @param userId
	 *        the owner user ID to find
	 * @param token
	 *        the token to find
	 * @return the matching entity, or {@literal null} if not found
	 */
	Authorization getForToken(Long userId, String token);

	/**
	 * Find all available authorizations for a given owner.
	 * 
	 * @param userId
	 *        the owner ID
	 * @return the available authorizations; never {@literal null}
	 */
	Collection<CentralAuthorization> findAllForOwner(Long userId);

	/**
	 * Get an authorization by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @return the matching entity; never {@literal null}
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	CentralAuthorization get(Long userId, Long id);

	/**
	 * Delete an authorization by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	void delete(Long userId, Long id);

}
