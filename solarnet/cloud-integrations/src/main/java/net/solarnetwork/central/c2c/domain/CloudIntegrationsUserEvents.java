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

package net.solarnetwork.central.c2c.domain;

import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Constants and helpers for cloud integration user event handling.
 *
 * @author matt
 * @version 1.3
 */
public interface CloudIntegrationsUserEvents {

	/** A user event tag for cloud integrations. */
	String CLOUD_INTEGRATIONS_TAG = "c2c";

	/**
	 * A user event tag for {@code CloudIntegrationService} related events.
	 *
	 * @since 1.2
	 */
	String CLOUD_INTEGRATION_TAG = "i9n";

	/**
	 * A user event tag for {@code CloudDatumStreamService} related events.
	 *
	 * @since 1.2
	 */
	String CLOUD_DATUM_STREAM_TAG = "ds";

	/** A user event tag for an "error". */
	String ERROR_TAG = "error";

	/** A user event tag for an authorization event. */
	String AUTHORIZATION_TAG = "auth";

	/** A user event tag for an HTTP event. */
	String HTTP_TAG = "http";

	/** A user event tag for an expression event. */
	String EXPRESSION_TAG = "expr";

	/** A user event tag for a datum stream poll event. */
	String POLL_TAG = "poll";

	/** User event data key for a configuration ID. */
	String CONFIG_ID_DATA_KEY = "configId";

	/**
	 * User event data key for a configuration sub ID (second component of a
	 * composite ID).
	 */
	String CONFIG_SUB_ID_DATA_KEY = "subId";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

	/**
	 * User event data key for the source of the event, such as the location of
	 * an error.
	 */
	String SOURCE_DATA_KEY = "source";

	/** Tags for an authorization error event. */
	List<String> INTEGRATION_AUTH_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_INTEGRATION_TAG, AUTHORIZATION_TAG);

	/** Tags for an HTTP error event. */
	List<String> INTEGRATION_HTTP_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_INTEGRATION_TAG, HTTP_TAG);

	/** Tags for an expression error event. */
	List<String> DATUM_STREAM_EXPRESSION_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_DATUM_STREAM_TAG, EXPRESSION_TAG);

	/** Tags for a poll error event. */
	List<String> INTEGRATION_POLL_ERROR_TAGS = List.of(CLOUD_INTEGRATIONS_TAG, ERROR_TAG,
			CLOUD_DATUM_STREAM_TAG, POLL_TAG);

	/**
	 * Tags for a non-error poll events.
	 *
	 * @since 1.1
	 */
	List<String> INTEGRATION_POLL_TAGS = INTEGRATION_POLL_ERROR_TAGS.stream()
			.filter(t -> !ERROR_TAG.equals(t)).toList();

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
	 * Get a user log event for a configuration ID.
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
	static LogEventInfo eventForConfiguration(UserRelatedCompositeKey<?> configId, List<String> baseTags,
			String message, String... extraTags) {
		Map<String, Object> data = new HashMap<>(4);
		populateUserRelatedKeyEventParameters(configId, data);
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

	/**
	 * Get a user log event for a configuration ID.
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
	static LogEventInfo eventForConfiguration(UserRelatedCompositeKey<?> configId, List<String> baseTags,
			String message, Map<String, ?> parameters, String... extraTags) {
		Map<String, Object> data = new LinkedHashMap<>(parameters);
		populateUserRelatedKeyEventParameters(configId, data);
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

	/**
	 * Get a user log event for a configuration.
	 *
	 * @param config
	 *        the configuration
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param extraTags
	 *        optional extra tags
	 * @return the log event
	 */
	static LogEventInfo eventForConfiguration(CloudIntegrationsConfigurationEntity<?, ?> config,
			List<String> baseTags, String message, String... extraTags) {
		return eventForConfiguration(config.getId(), baseTags, message, extraTags);
	}
}
