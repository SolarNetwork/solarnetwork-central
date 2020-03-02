/* ==================================================================
 * UserOcppBiz.java - 29/02/2020 4:24:46 pm
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

package net.solarnetwork.central.user.ocpp.biz;

import java.util.Collection;
import net.solarnetwork.central.ocpp.domain.CentralAuthorization;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralSystemUser;

/**
 * Service API for SolarUser OCPP support.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserOcppBiz {

	/**
	 * Get an OCPP system user for a given username.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system user for
	 * @param username
	 *        the username to find
	 * @return the system user
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralSystemUser systemUserForUser(Long userId, String username);

	/**
	 * Get an OCPP system user for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system user for
	 * @param id
	 *        the system user ID to find
	 * @return the system user
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralSystemUser systemUserForUser(Long userId, Long id);

	/**
	 * Delete an OCPP system user for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP system user for
	 * @param id
	 *        the system user ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserSystemUser(Long userId, Long id);

	/**
	 * List the available OCPP system users for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP system users for
	 * @return all available system users; never {@literal null}
	 */
	Collection<CentralSystemUser> systemUsersForUser(Long userId);

	/**
	 * Create a new OCPP system user, or update an existing user.
	 * 
	 * @param systemUser
	 *        the details to save
	 * @return the persisted system user, with any default values populated and
	 *         ID assigned if creating a new entity
	 */
	CentralSystemUser saveSystemUser(CentralSystemUser systemUser);

	/**
	 * Get an OCPP system user for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP authorization for
	 * @param id
	 *        the authorization ID to find
	 * @return the authorization
	 * @throws RuntimeException
	 *         if not available
	 */
	CentralAuthorization authorizationForUser(Long userId, Long id);

	/**
	 * Delete an OCPP authorization for a given ID.
	 * 
	 * @param userId
	 *        the SolarUser user ID to delete the OCPP authorization for
	 * @param id
	 *        the authorization ID to delete
	 * @throws RuntimeException
	 *         if not available
	 */
	void deleteUserAuthorization(Long userId, Long id);

	/**
	 * Get the available OCPP authorizations for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get OCPP authorizations for
	 * @return all available authorizations; never {@literal null}
	 */
	Collection<CentralAuthorization> authorizationsForUser(Long userId);

	/**
	 * Create a new OCPP authorization, or update an existing authorization.
	 * 
	 * @param authorization
	 *        the details to save
	 * @return the persisted authorization, with any default values populated
	 *         and ID assigned if creating a new entity
	 */
	CentralAuthorization saveAuthorization(CentralAuthorization authorization);

	/**
	 * List the available charge points for a given user.
	 * 
	 * @param userId
	 *        the SolarUser user ID to get charge points for
	 * @return all available charge points; never {@literal null}
	 */
	Collection<CentralChargePoint> chargePointsForUser(Long userId);

	/**
	 * Create a new charge point registration, or update an existing
	 * registration.
	 * 
	 * @param chargePoint
	 *        the details to save
	 * @return the persisted charge point, with any default values populated and
	 *         ID assigned if creating a new entity
	 */
	CentralChargePoint saveChargePoint(CentralChargePoint chargePoint);

}
