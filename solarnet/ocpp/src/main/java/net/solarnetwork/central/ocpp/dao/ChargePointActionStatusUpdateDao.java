/* ==================================================================
 * ChargePointActionStatusUpdateDao.java - 15/05/2024 7:28:37 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.time.Instant;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;

/**
 * DAO API for updating {@link ChargePointActionStatus} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointActionStatusUpdateDao {

	/**
	 * Update the timestamp for a specific charge point action.
	 * 
	 * <p>
	 * This method will create a new entity if one does not already exist.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param connectorId
	 *        the connector ID the message is related to, or {@literal null} or
	 *        {@literal 0} for charger-wide actions
	 * @param action
	 *        the action name
	 * @param messageId
	 *        the message ID
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code connectorId} is {@literal null}
	 */
	default void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer connectorId,
			String action, String messageId, Instant date) {
		updateActionTimestamp(userId, chargePointIdentifier, 0, connectorId, action, messageId, date);
	}

	/**
	 * Update the timestamp for a specific charge point action.
	 * 
	 * <p>
	 * This method will create a new entity if one does not already exist.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param evseId
	 *        the EVSE ID the message is related to, or {@literal null} for
	 *        charger-wide actions
	 * @param connectorId
	 *        the connector ID the message is related to, or {@literal null} or
	 *        {@literal 0} for EVSE-wide actions
	 * @param action
	 *        the action name
	 * @param messageId
	 *        the message ID
	 * @param date
	 *        the date
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code connectorId} is {@literal null}
	 */
	void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer evseId,
			Integer connectorId, String action, String messageId, Instant date);

}
