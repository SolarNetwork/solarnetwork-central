/* ==================================================================
 * CentralChargeSession.java - 26/02/2020 11:40:36 am
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.ocpp.domain.ChargeSession;

/**
 * An entity for tracking an OCPP transaction, which represents a single
 * charging cycle from authorization to end of charging.
 * 
 * @author matt
 * @version 1.2
 */
public class CentralChargeSession extends ChargeSession {

	private static final long serialVersionUID = -5000462916671943747L;

	/**
	 * Constructor.
	 * 
	 * @param authId
	 *        the authorization ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @param transactionId
	 *        the transactionID
	 */
	public CentralChargeSession(String authId, long chargePointId, int connectorId, int transactionId) {
		super(authId, chargePointId, connectorId, transactionId);
	}

	/**
	 * Constructor.
	 * 
	 * @param authId
	 *        the authorization ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @param transactionId
	 *        the transactionID
	 * @since 1.2
	 */
	public CentralChargeSession(String authId, long chargePointId, int evseId, int connectorId,
			int transactionId) {
		super(authId, chargePointId, evseId, connectorId, transactionId);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 * @param authId
	 *        the authorization ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @param transactionId
	 *        the transactionID
	 */
	public CentralChargeSession(UUID id, Instant created, String authId, long chargePointId,
			int connectorId, int transactionId) {
		super(id, created, authId, chargePointId, connectorId, transactionId);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 * @param authId
	 *        the authorization ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @param transactionId
	 *        the transactionID
	 * @since 1.2
	 */
	public CentralChargeSession(UUID id, Instant created, String authId, long chargePointId, int evseId,
			int connectorId, int transactionId) {
		super(id, created, authId, chargePointId, evseId, connectorId, transactionId);
	}

	/**
	 * Create a session filter for a charge point.
	 * 
	 * @param chargePointId
	 *        the Charge Point ID
	 * @return the session instance, suitable for using as a filter
	 */
	public static CentralChargeSession forChargePoint(long chargePointId) {
		return new CentralChargeSession(null, chargePointId, 0, 0);
	}

	/**
	 * Create a session filter for a transaction.
	 * 
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param transactionId
	 *        the transactionID
	 * @return the session instance, suitable for using as a filter
	 */
	public static CentralChargeSession forTransaction(long chargePointId, int transactionId) {
		return new CentralChargeSession(null, chargePointId, 0, transactionId);
	}

	/**
	 * Create a session filter for a connector.
	 * 
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @return the session instance, suitable for using as a filter
	 */
	public static CentralChargeSession forConnector(long chargePointId, int connectorId) {
		return forConnector(chargePointId, 0, connectorId);
	}

	/**
	 * Create a session filter for a connector.
	 * 
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the Charge Point connector ID
	 * @return the session instance, suitable for using as a filter
	 * @since 1.2
	 */
	public static CentralChargeSession forConnector(long chargePointId, int evseId, int connectorId) {
		return new CentralChargeSession(null, chargePointId, evseId, connectorId, 0);
	}

}
