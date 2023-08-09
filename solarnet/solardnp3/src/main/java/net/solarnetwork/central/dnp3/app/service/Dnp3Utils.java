/* ==================================================================
 * Dnp3Utils.java - 9/08/2023 1:20:52 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.service;

import com.automatak.dnp3.LinkLayerConfig;
import com.automatak.dnp3.OutstationConfig;

/**
 * DNP3 utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class Dnp3Utils {

	private Dnp3Utils() {
		// not available
	}

	/**
	 * Copy the link layer configuration from one object to another.
	 * 
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 */
	public static void copySettings(LinkLayerConfig from, LinkLayerConfig to) {
		to.isMaster = from.isMaster;
		to.keepAliveTimeout = from.keepAliveTimeout;
		to.localAddr = from.localAddr;
		to.numRetry = from.numRetry;
		to.remoteAddr = from.remoteAddr;
		to.responseTimeout = from.responseTimeout;
		to.useConfirms = from.useConfirms;
	}

	/**
	 * Copy the Outstation configuration from one object to another.
	 * 
	 * @param from
	 *        the settings to copy
	 * @param to
	 *        the destination to copy the settings to
	 */
	public static void copySettings(OutstationConfig from, OutstationConfig to) {
		to.allowUnsolicited = from.allowUnsolicited;
		to.indexMode = from.indexMode;
		to.maxControlsPerRequest = from.maxControlsPerRequest;
		to.maxRxFragSize = from.maxRxFragSize;
		to.maxTxFragSize = from.maxTxFragSize;
		to.selectTimeout = from.selectTimeout;
		to.solConfirmTimeout = from.solConfirmTimeout;
		to.unsolRetryTimeout = from.unsolRetryTimeout;
	}

}
