/* ==================================================================
 * CentralChargePointDao.java - 25/02/2020 7:08:40 pm
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
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.ocpp.dao.ChargePointDao;
import net.solarnetwork.ocpp.domain.ChargePoint;

/**
 * Extension to {@link ChargePointDao} to support SolarNet.
 * 
 * <p>
 * This API implies
 * {@link net.solarnetwork.central.ocpp.domain.CentralChargePoint} entities are
 * used.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface CentralChargePointDao extends ChargePointDao {

	/**
	 * Get a charge point by its unique identifier.
	 * 
	 * @param userId
	 *        the owner user ID to find
	 * @param identifier
	 *        the charge point identifier to look for
	 * @return the matching charge point, or {@literal null} if not found
	 */
	ChargePoint getForIdentifier(Long userId, String identifier);

	/**
	 * Get all available charge points for a given owner.
	 * 
	 * @param userId
	 *        the owner ID
	 * @return the available charge points; never {@literal null}
	 */
	Collection<CentralChargePoint> findAllForOwner(Long userId);

}
