/* ==================================================================
 * CentralOcppUserEvents.java - 6/08/2022 8:14:28 am
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

package net.solarnetwork.central.ocpp.domain;

/**
 * Constants for central OCPP user events.
 * 
 * @author matt
 * @version 1.3
 */
public interface CentralOcppUserEvents {

	/** A user event tag for OCPP. */
	String OCPP_EVENT_TAG = "ocpp";

	/** A user event tag for OCPP "charger". */
	String CHARGER_EVENT_TAG = "charger";

	/** A user event tag for OCPP "instruction" . */
	String INSTRUCTION_EVENT_TAG = "instruction";

	/** A user event tag for OCPP "error" . */
	String ERROR_TAG = "error";

	/** A user event tag for OCPP "message". */
	String MESSAGE_EVENT_TAG = "message";

	/** User event tags for OCPP connection established events. */
	String[] CHARGE_POINT_CONNECTED_TAGS = new String[] { OCPP_EVENT_TAG, CHARGER_EVENT_TAG,
			"connected" };

	/** User event tags for OCPP connection ended events. */
	String[] CHARGE_POINT_DISCONNECTED_TAGS = new String[] { OCPP_EVENT_TAG, CHARGER_EVENT_TAG,
			"disconnected" };

	/** User event tags for OCPP instruction general events. */
	String[] CHARGE_POINT_INSTRUCTION_TAGS = new String[] { OCPP_EVENT_TAG, INSTRUCTION_EVENT_TAG };

	/** User event tags for OCPP instruction queued events. */
	String[] CHARGE_POINT_INSTRUCTION_QUEUED_TAGS = new String[] { OCPP_EVENT_TAG, INSTRUCTION_EVENT_TAG,
			"queued" };

	/** User event tags for OCPP instruction acknowledged events. */
	String[] CHARGE_POINT_INSTRUCTION_ACKNOWLEDGED_TAGS = new String[] { OCPP_EVENT_TAG,
			INSTRUCTION_EVENT_TAG, "ack" };

	/** User event tags for OCPP instruction error events. */
	String[] CHARGE_POINT_INSTRUCTION_ERROR_TAGS = new String[] { OCPP_EVENT_TAG, INSTRUCTION_EVENT_TAG,
			ERROR_TAG };

	/** User event tags for OCPP message received events. */
	String[] CHARGE_POINT_MESSAGE_RECEIVED_TAGS = new String[] { OCPP_EVENT_TAG, MESSAGE_EVENT_TAG,
			"received" };

	/** User event tags for OCPP message sent events. */
	String[] CHARGE_POINT_MESSAGE_SENT_TAGS = new String[] { OCPP_EVENT_TAG, MESSAGE_EVENT_TAG, "sent" };

	/** User event tags for OCPP message sent events. */
	String[] CHARGE_POINT_MESSAGE_SENT_ERROR_TAGS = new String[] { OCPP_EVENT_TAG, MESSAGE_EVENT_TAG,
			"sent", ERROR_TAG };

	/** User event data key for a charge point identifier. */
	String CHARGE_POINT_DATA_KEY = "cp";

	/** User event data key for an error. */
	String ERROR_DATA_KEY = "error";

	/** User event data key for an OCPP action. */
	String ACTION_DATA_KEY = "action";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

	/** User event data key for a message ID. */
	String MESSAGE_ID_DATA_KEY = "messageId";

	/** User event data key for a session ID. */
	String SESSION_ID_DATA_KEY = "sessionId";

}
