/* ==================================================================
 * CentralChargePointConnector.java - 26/02/2020 8:52:41 am
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
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;

/**
 * A Charge Point connector entity.
 * 
 * <p>
 * A connector ID of {@literal 0} represents the Charge Point as a whole.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CentralChargePointConnector extends ChargePointConnector {

	/**
	 * Constructor.
	 */
	public CentralChargePointConnector() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 */
	public CentralChargePointConnector(ChargePointConnectorKey id) {
		super(id);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 */
	public CentralChargePointConnector(ChargePointConnectorKey id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param connectorId
	 *        the connector ID
	 * @param created
	 *        the created date
	 */
	public CentralChargePointConnector(long chargePointId, int connectorId, Instant created) {
		super(new ChargePointConnectorKey(chargePointId, connectorId), created);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the other charge point to copy
	 */
	public CentralChargePointConnector(ChargePointConnector other) {
		super(other);
	}

	@Override
	public boolean isSameAs(ChargePointConnector other) {
		if ( !(other instanceof CentralChargePointConnector) ) {
			return false;
		}
		return super.isSameAs(other);
	}

}
