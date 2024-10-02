/* ==================================================================
 * CloudIntegrationUserEvents.java - 2/10/2024 11:03:48 am
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
import java.util.Map;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Constants and helpers for cloud integration user event handling.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudIntegrationUserEvents {

	/** A user event tag for cloud integration. */
	String CLOUD_INTEGRATION_TAG = "c2c";

	/** A user event tag for an "error" . */
	String ERROR_TAG = "error";

	/** A user event tag for an authorization event. */
	String AUTHORIZATION_TAG = "auth";

	/** User event data key for a configuration ID. */
	String CONFIG_ID_DATA_KEY = "configId";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

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
	static LogEventInfo eventForConfiguration(UserLongCompositePK configId, String[] baseTags,
			String message, String... extraTags) {
		Map<String, Object> data = new HashMap<>(4);
		data.put(CONFIG_ID_DATA_KEY, configId.getEntityId());
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

}
