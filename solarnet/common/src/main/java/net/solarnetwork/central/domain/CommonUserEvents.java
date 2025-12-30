/* ==================================================================
 * CloudIntegrationsUserEvents.java - 2/10/2024 11:03:48 am
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

package net.solarnetwork.central.domain;

import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Constants and helpers for common user event handling.
 *
 * @author matt
 * @version 1.0
 */
public interface CommonUserEvents {

	/** A user event tag for an "error". */
	String ERROR_TAG = "error";

	/** A user event tag for an authorization event. */
	String AUTHORIZATION_TAG = "auth";

	/** A user event tag for an expression event. */
	String EXPRESSION_TAG = "expr";

	/** A user event tag for a node event. */
	String NODE_TAG = "node";

	/** A user event tag for a request. */
	String REQUEST_TAG = "req";

	/** A user event tag for a response. */
	String RESPONSE_TAG = "res";

	/** User event data key for a configuration ID. */
	String CONFIG_ID_DATA_KEY = "configId";

	/**
	 * User event data key for a configuration sub ID (second component of a
	 * composite ID).
	 */
	String CONFIG_SUB_ID_DATA_KEY = "subId";

	/** User event data key for a node ID. */
	String NODE_ID_DATA_KEY = "nodeId";

	/** User event data key for a source ID. */
	String SOURCE_ID_DATA_KEY = "sourceId";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

	/**
	 * User event data key for the source of the event, such as the location of
	 * an error.
	 */
	String SOURCE_DATA_KEY = "source";

	/** User event data key for an internal error code. */
	String ERROR_CODE_DATA_KEY = "errorCode";

	/** User event data key for an execution date. */
	String EXECUTE_AT_DATA_KEY = "executeAt";

	/**
	 * Populate user-related composite key components to a parameter map.
	 *
	 * @param configId
	 *        the configuration ID
	 * @param parameters
	 *        the parameter to populate the ID components into
	 */
	static void populateUserRelatedKeyEventParameters(UserRelatedCompositeKey<?> configId,
			Map<String, Object> parameters) {
		if ( configId == null ) {
			return;
		}
		parameters.put(CONFIG_ID_DATA_KEY, configId.keyComponent(1));
		if ( configId.keyComponentLength() > 2 && configId.keyComponentIsAssigned(2) ) {
			parameters.put(CONFIG_SUB_ID_DATA_KEY, configId.keyComponent(2));
		}
	}

	/**
	 * Get a user log event for a user related ID.
	 *
	 * @param configId
	 *        the configuration ID
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param extraTags
	 *        optional extra tags
	 * @return the log event
	 */
	static LogEventInfo eventForUserRelatedKey(UserRelatedCompositeKey<?> configId,
			List<String> baseTags, String message, String... extraTags) {
		Map<String, Object> data = new HashMap<>(4);
		populateUserRelatedKeyEventParameters(configId, data);
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

	/**
	 * Get a user log event for a user related ID.
	 *
	 * @param configId
	 *        the configuration ID
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param parameters
	 *        extra event parameters
	 * @param extraTags
	 *        optional extra tags
	 * @return the log event
	 */
	static LogEventInfo eventForUserRelatedKey(UserRelatedCompositeKey<?> configId,
			List<String> baseTags, String message, Map<String, ?> parameters, String... extraTags) {
		Map<String, Object> data = new LinkedHashMap<>(parameters);
		populateUserRelatedKeyEventParameters(configId, data);
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

}
