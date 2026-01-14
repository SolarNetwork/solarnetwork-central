/* ==================================================================
 * ConnectorKeyExtractor.java - 18/02/2024 7:28:06 am
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

package net.solarnetwork.central.ocpp.v201.service;

import static net.solarnetwork.ocpp.domain.ChargePointConnectorKey.keyFor;
import java.util.function.Function;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import ocpp.v201.EVSE;
import ocpp.v201.MeterValuesRequest;
import ocpp.v201.StatusNotificationRequest;
import ocpp.v201.TransactionEventRequest;

/**
 * Extract a connector key from OCPP v2.0.1 request messages.
 *
 * @author matt
 * @version 1.1
 */
public class ConnectorKeyExtractor implements Function<Object, ChargePointConnectorKey> {

	private static int intOrZero(Number n) {
		return n != null ? n.intValue() : 0;
	}

	@Override
	public ChargePointConnectorKey apply(Object o) {
		if ( o == null ) {
			return null;
		}
		EVSE evse = null;
		if ( o instanceof MeterValuesRequest r ) {
			return keyFor(intOrZero(r.getEvseId()), 0);
		} else if ( o instanceof StatusNotificationRequest r ) {
			return keyFor(0, intOrZero(r.getEvseId()), intOrZero(r.getConnectorId()));
		} else if ( o instanceof TransactionEventRequest r ) {
			evse = r.getEvse();
		}
		if ( evse != null ) {
			return keyFor(0, intOrZero(evse.getId()), intOrZero(evse.getConnectorId()));
		}
		return null;
	}

}
