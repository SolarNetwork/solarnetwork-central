/* ==================================================================
 * SolarNetOcppConfiguration.java - 12/11/2021 1:35:32 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.config;

/**
 * Marker interface for SolarNetwork OCPP configuration.
 * 
 * @author matt
 * @version 1.0
 */
public interface SolarNetOcppConfiguration {

	/** A qualifier for OCPP v1.6 support. */
	String OCPP_V16 = "ocpp-v16";

	/** A qualifier for OCPP charge session support. */
	String OCPP_CHARGE_SESSION = "ocpp-charge-session";

	/** A qualifier for OCPP v1.6 support and charge session support. */
	String OCPP_V16_CHARGE_SESSION = OCPP_V16 + " & " + OCPP_CHARGE_SESSION;

	/** A qualifier for OCPP instruction support. */
	String OCPP_INSTRUCTION = "instr";

}
