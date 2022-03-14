/* ==================================================================
 * CentralChargePointConnectorDao.java - 26/02/2020 8:42:34 am
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
import net.solarnetwork.central.ocpp.domain.CentralChargePointConnector;
import net.solarnetwork.ocpp.dao.ChargePointConnectorDao;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;

/**
 * Extension to {@link ChargePointConnectorDao} to support SolarNet.
 * 
 * <p>
 * This API implies
 * {@link net.solarnetwork.central.ocpp.domain.CentralChargePointConnector}
 * entities are used.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CentralChargePointConnectorDao extends ChargePointConnectorDao {

	/**
	 * Find all available connectors for a given owner.
	 * 
	 * @param userId
	 *        the owner ID
	 * @return the available connectors; never {@literal null}
	 */
	Collection<CentralChargePointConnector> findAllForOwner(Long userId);

	/**
	 * Get an connector by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @return the matching entity; never {@literal null}
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	CentralChargePointConnector get(Long userId, ChargePointConnectorKey id);

	/**
	 * Find all available connectors for a given Charge Point ID.
	 * 
	 * @param userId
	 *        the onwer ID
	 * @param chargePointId
	 *        the ID of the Charge Point to find connectors for
	 * @return the connectors, ordered by connector ID in ascending order
	 */
	Collection<CentralChargePointConnector> findByChargePointId(Long userId, long chargePointId);

	/**
	 * Delete an connector by its unique ID.
	 * 
	 * @param userId
	 *        the owner ID
	 * @param id
	 *        the ID to look for
	 * @throws RuntimeException
	 *         if the entity cannot be found
	 */
	void delete(Long userId, ChargePointConnectorKey id);

}
