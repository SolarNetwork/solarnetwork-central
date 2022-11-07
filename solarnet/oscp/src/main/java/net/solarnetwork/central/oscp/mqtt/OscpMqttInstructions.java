/* ==================================================================
 * OscpMqttInstructions.java - 7/10/2022 6:15:25 pm
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

package net.solarnetwork.central.oscp.mqtt;

import net.solarnetwork.central.oscp.domain.OscpUserEvents;

/**
 * Constants for OSCP MQTT instructions.
 * 
 * @author matt
 * @version 1.0
 */
public interface OscpMqttInstructions extends OscpUserEvents {

	/** The MQTT topic for OSCP instruction messages. */
	String MQTT_TOPIC_V20 = "instr/OSCP_v20";

	/** The instruction ID parameter. */
	String INSTRUCTION_ID_PARAM = "id";

	/** The node ID parameter. */
	String NODE_ID_PARAM = "nodeId";

	/** The user ID parameter. */
	String USER_ID_PARAM = "userId";

	/** The correlation ID parameter. */
	String CORRELATION_ID_PARAM = "correlationId";

	/** User event tags for OSCP instruction general events. */
	String[] OSCP_INSTRUCTION_TAGS = new String[] { OSCP_TAG, INSTRUCTION_TAG };

	/** User event tags for OSCP instruction error events. */
	String[] OSCP_INSTRUCTION_ERROR_TAGS = new String[] { OSCP_TAG, INSTRUCTION_TAG, ERROR_TAG };

	/** User event tags for OSCP instruction input events. */
	String[] OSCP_INSTRUCTION_IN_TAGS = new String[] { OSCP_TAG, INSTRUCTION_TAG, INPUT_TAG };

	/** User event tags for OSCP instruction output events. */
	String[] OSCP_INSTRUCTION_OUT_TAGS = new String[] { OSCP_TAG, INSTRUCTION_TAG, OUTPUT_TAG };

}
