/* ==================================================================
 * ConnectorIdExtractor.java - 18/11/2022 7:49:09 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v16.util;

import static net.solarnetwork.ocpp.domain.ChargePointConnectorKey.keyFor;
import java.util.function.Function;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import ocpp.v16.jakarta.cs.MeterValuesRequest;
import ocpp.v16.jakarta.cs.StartTransactionRequest;
import ocpp.v16.jakarta.cs.StatusNotificationRequest;

/**
 * Extract a connector ID from OCPP v1.6 request messages.
 * 
 * @author matt
 * @version 1.1
 */
public class ConnectorIdExtractor implements Function<Object, ChargePointConnectorKey> {

	@Override
	public ChargePointConnectorKey apply(Object o) {
		if ( o == null ) {
			return null;
		}
		if ( o instanceof MeterValuesRequest r ) {
			return keyFor(0, r.getConnectorId());
		} else if ( o instanceof StartTransactionRequest r ) {
			return keyFor(0, r.getConnectorId());
		} else if ( o instanceof StatusNotificationRequest r ) {
			return keyFor(0, r.getConnectorId());
		}
		return null;
	}

}
