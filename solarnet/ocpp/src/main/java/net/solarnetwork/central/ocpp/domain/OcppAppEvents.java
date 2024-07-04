/* ==================================================================
 * OcppAppEvents.java - 3/07/2024 10:15:05 am
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import java.util.Map;
import net.solarnetwork.event.AppEvent;
import net.solarnetwork.event.BasicAppEvent;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;

/**
 * OCPP application event support.
 * 
 * @author matt
 * @version 1.0
 */
public final class OcppAppEvents {

	/** Event topic when a charge point connects to a central system. */
	public static final String EVENT_TOPIC_CHARGE_POINT_CONNECTED = "net/solarnetwork/central/ocpp/CHARGE_POINT_CONNECTED";

	/** Event topic when a charge point disconnects from a central system. */
	public static final String EVENT_TOPIC_CHARGE_POINT_DISCONNECTED = "net/solarnetwork/central/ocpp/CHARGE_POINT_DISCONNECTED";

	/**
	 * An event property for a
	 * {@link net.solarnetwork.ocpp.domain.ChargePointIdentity} instance.
	 */
	public static final String EVENT_PROP_CHARGE_POINT_IDENTITY = "cpIdentity";

	private OcppAppEvents() {
		// not available
	}

	/**
	 * Create an OCPP charge point connection event.
	 * 
	 * @param ts
	 *        the event timestamp
	 * @param identity
	 *        the identity of the charge point
	 * @param connected
	 *        {@literal true} if connected, {@literal false} if disconnected
	 * @return the event
	 */
	public static AppEvent connectionEvent(Instant ts, ChargePointIdentity identity, boolean connected) {
		return new BasicAppEvent(
				connected ? EVENT_TOPIC_CHARGE_POINT_CONNECTED : EVENT_TOPIC_CHARGE_POINT_DISCONNECTED,
				ts, Map.of(EVENT_PROP_CHARGE_POINT_IDENTITY, identity));
	}

}
