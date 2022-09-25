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
 * @version 1.2
 */
public interface CentralOcppUserEvents {

	/** A user event tag for OCPP. */
	public static final String OCPP_EVENT_TAG = "ocpp";

	/** A user event tag for OCPP "charger". */
	public static final String CHARGER_EVENT_TAG = "charger";

	/** A user event tag for OCPP "instruction" . */
	public static final String INSTRUCTION_EVENT_TAG = "instruction";

	/** A user event tag for OCPP "error" . */
	public static final String ERROR_TAG = "error";

	/** A user event tag for OCPP "message". */
	public static final String MESSAGE_EVENT_TAG = "message";

	/** User event tags for OCPP connection established events. */
	public static final String[] CHARGE_POINT_CONNECTED_TAGS = new String[] { OCPP_EVENT_TAG,
			CHARGER_EVENT_TAG, "connected" };

	/** User event tags for OCPP connection ended events. */
	public static final String[] CHARGE_POINT_DISCONNECTED_TAGS = new String[] { OCPP_EVENT_TAG,
			CHARGER_EVENT_TAG, "disconnected" };

	/** User event tags for OCPP instruction general events. */
	public static final String[] CHARGE_POINT_INSTRUCTION_TAGS = new String[] { OCPP_EVENT_TAG,
			INSTRUCTION_EVENT_TAG };

	/** User event tags for OCPP instruction queued events. */
	public static final String[] CHARGE_POINT_INSTRUCTION_QUEUED_TAGS = new String[] { OCPP_EVENT_TAG,
			INSTRUCTION_EVENT_TAG, "queued" };

	/** User event tags for OCPP instruction acknowledged events. */
	public static final String[] CHARGE_POINT_INSTRUCTION_ACKNOWLEDGED_TAGS = new String[] {
			OCPP_EVENT_TAG, INSTRUCTION_EVENT_TAG, "ack" };

	/** User event tags for OCPP instruction error events. */
	public static final String[] CHARGE_POINT_INSTRUCTION_ERROR_TAGS = new String[] { OCPP_EVENT_TAG,
			INSTRUCTION_EVENT_TAG, ERROR_TAG };

	/** User event tags for OCPP message received events. */
	public static final String[] CHARGE_POINT_MESSAGE_RECEIVED_TAGS = new String[] { OCPP_EVENT_TAG,
			MESSAGE_EVENT_TAG, "received" };

	/** User event tags for OCPP message sent events. */
	public static final String[] CHARGE_POINT_MESSAGE_SENT_TAGS = new String[] { OCPP_EVENT_TAG,
			MESSAGE_EVENT_TAG, "sent" };

	/** User event tags for OCPP message sent events. */
	public static final String[] CHARGE_POINT_MESSAGE_SENT_ERROR_TAGS = new String[] { OCPP_EVENT_TAG,
			MESSAGE_EVENT_TAG, "sent", ERROR_TAG };

	/** User event data key for a charge point identifier. */
	public static final String CHARGE_POINT_DATA_KEY = "cp";

	/** User event data key for an error. */
	public static final String ERROR_DATA_KEY = "error";

	/** User event data key for an OCPP action. */
	public static final String ACTION_DATA_KEY = "action";

	/** User event data key for a message. */
	public static final String MESSAGE_DATA_KEY = "message";

	/** User event data key for a message ID. */
	public static final String MESSAGE_ID_DATA_KEY = "messageId";

}
