/* ==================================================================
 * Dnp3UserEvents.java - 11/08/2023 6:42:44 am
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

package net.solarnetwork.central.dnp3.domain;

/**
 * Constants and helpers for DNP3 user event handling.
 * 
 * @author matt
 * @version 1.0
 */
public interface Dnp3UserEvents {

	/** A user event tag for DNP3. */
	String DNP3_TAG = "dnp3";

	/** A user event tag for DNP3 authorization . */
	String AUTHORIZATION_TAG = "auth";

	/** A user event tag for DNP3 session . */
	String SESSION_TAG = "session";

	/** A user event tag for DNP3 error . */
	String ERROR_TAG = "error";

	/** A user event tag for DNP3 start event . */
	String START_TAG = "start";

	/** A user event tag for DNP3 end event . */
	String END_TAG = "end";

	/** A user event tag for DNP3 input state . */
	String INPUT_TAG = "in";

	/** A user event tag for DNP3 output state . */
	String OUTPUT_TAG = "out";

	/** A user event tag for DNP3 datum handling . */
	String DATUM_TAG = "datum";

	/** A user event tag for DNP3 instruction handling. */
	String INSTRUCTION_TAG = "instruction";

	/** User event data key for a server ID. */
	String SERVER_ID_DATA_KEY = "serverId";

	/** User event data key for a configuration ID. */
	String CONFIG_ID_DATA_KEY = "configId";

	/** User event data key for an identifier. */
	String IDENTIFIER_DATA_KEY = "ident";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "ident";

	/** User event data key for a count. */
	String COUNT_DATA_KEY = "count";

}
