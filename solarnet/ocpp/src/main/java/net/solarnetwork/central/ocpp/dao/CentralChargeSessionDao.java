/* ==================================================================
 * CentralChargeSessionDao.java - 26/02/2020 11:41:19 am
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
import java.util.UUID;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.domain.ChargeSession;

/**
 * Extension to {@link CentralChargeSessionDao} to support SolarNet.
 * 
 * <p>
 * This API implies
 * {@link net.solarnetwork.central.ocpp.domain.CentralChargeSession} entities
 * are used.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface CentralChargeSessionDao extends ChargeSessionDao {

	/**
	 * Get all <em>incomplete</em> charge session for a given user ID and charge
	 * point ID. An <em>incomplete</em> session is one that has no {@code ended}
	 * date.
	 * 
	 * @param userId
	 *        the charge point owner ID to look for
	 * @param chargePointId
	 *        the charge point ID to look for
	 * @return all available incomplete charge session for the given charge
	 *         point, never {@literal null}
	 * @since 1.1
	 */
	Collection<ChargeSession> getIncompleteChargeSessionsForUserForChargePoint(long userId,
			long chargePointId);

	/**
	 * Get a persisted domain object by its primary key and the owner's user ID.
	 * 
	 * @param id
	 *        the primary key to retrieve
	 * @param userId
	 *        the ID of the owner
	 * @return the domain object
	 * @since 1.1
	 */
	ChargeSession get(UUID id, Long userId);

}
