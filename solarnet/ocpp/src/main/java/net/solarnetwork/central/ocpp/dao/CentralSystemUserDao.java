/* ==================================================================
 * CentralSystemDao.java - 25/02/2020 10:39:18 am
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
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;
import net.solarnetwork.ocpp.dao.SystemUserDao;

/**
 * Extension of {@link SystemUserDao} to support SolarNet.
 * 
 * <p>
 * This API implies
 * {@link net.solarnetwork.central.ocpp.domain.CentralSystemUser} entities are
 * used.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CentralSystemUserDao extends SystemUserDao {

	/**
	 * Find all available system users for a given owner.
	 * 
	 * @param userId
	 *        the owner ID
	 * @return the available system users; never {@literal null}
	 */
	Collection<CentralSystemUser> findAllForOwner(Long userId);

	/**
	 * Get a system user by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @return the matching system user; never {@literal null}
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	CentralSystemUser get(Long userId, Long id);

	/**
	 * Delete a system user by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	void delete(Long userId, Long id);

	/**
	 * Get a system user by its unique username.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param username
	 *        the username to look for
	 * @return the matching system user; never {@literal null}
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	CentralSystemUser getForUsername(Long userId, String username);

}
