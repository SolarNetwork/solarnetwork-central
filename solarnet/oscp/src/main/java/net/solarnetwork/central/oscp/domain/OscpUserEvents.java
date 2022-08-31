/* ==================================================================
 * OscpUserEvents.java - 18/08/2022 4:27:28 pm
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

package net.solarnetwork.central.oscp.domain;

import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Constants and helpers for OSCP user event handling.
 * 
 * @author matt
 * @version 1.0
 */
public interface OscpUserEvents {

	/** A user event tag for OSCP. */
	String OSCP_EVENT_TAG = "oscp";

	/** A user event tag for OSCP Flexibility Provider . */
	String FLEXIBILITY_PROVIDER_TAG = "fp";

	/** A user event tag for OSCP Capacity Provider. */
	String CAPACITY_PROVIDER_TAG = "cp";

	/** A user event tag for OSCP Capacity Optimizer . */
	String CAPACITY_OPTIMIZER_TAG = "co";

	/** A user event tag for OSCP registration . */
	String REGISTER_TAG = "reg";

	/** A user event tag for OSCP handshake . */
	String HANDSHAKE_TAG = "handshake";

	/** A user event tag for OSCP heartbeat . */
	String HEARTBEAT_TAG = "heartbeat";

	/** A user event tag for OSCP update group capacity forecast . */
	String UPDATE_GROUP_CAPACITY_FORECAST_TAG = "update-group-capacity-forecast";

	/** A user event tag for OSCP adjust group capacity forecast . */
	String ADJUST_GROUP_CAPACITY_FORECAST_TAG = "adjust-group-capacity-forecast";

	/** A user event tag for OSCP group capacity compliance error . */
	String GROUP_CAPACITY_COMPLIANCE_ERROR_TAG = "group-capacity-compliance-error";

	/** A user event tag for OSCP "error" . */
	String ERROR_TAG = "error";

	/** User event data key for a configuration ID. */
	String CONFIG_ID_DATA_KEY = "configId";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

	/** User event data key for a registration status. */
	String REGISTRATION_STATUS_DATA_KEY = "regStatus";

	/** User event data key for a URL. */
	String URL_DATA_KEY = "url";

	/** User event data key for a version. */
	String VERSION_DATA_KEY = "v";

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
	static LogEventInfo eventForConfiguration(BaseOscpExternalSystemConfiguration<?> config,
			String[] baseTags, String message, String... extraTags) {
		Map<String, Object> data = new HashMap<>(4);
		data.put(CONFIG_ID_DATA_KEY, config.getEntityId());
		data.put(REGISTRATION_STATUS_DATA_KEY, (char) config.getRegistrationStatus().getCode());
		data.put(VERSION_DATA_KEY, config.getOscpVersion());
		data.put(URL_DATA_KEY, config.getBaseUrl());
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

}
