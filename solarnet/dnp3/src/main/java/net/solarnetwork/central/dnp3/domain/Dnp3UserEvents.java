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

import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.dao.Entity;

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

	/** User event data key for an index. */
	String INDEX_DATA_KEY = "idx";

	/** User event data key for a message. */
	String MESSAGE_DATA_KEY = "message";

	/** User event data key for a count. */
	String COUNT_DATA_KEY = "count";

	/** User event data key for a type. */
	String TYPE_DATA_KEY = "type";

	/** User event data key for a node ID. */
	String NODE_ID_DATA_KEY = "nodeId";

	/** User event data key for a control ID. */
	String CONTROL_ID_DATA_KEY = "controlId";

	/** User event data key for a source ID. */
	String SOURCE_ID_DATA_KEY = "sourceId";

	/** User event data key for a topic. */
	String TOPIC_DATA_KEY = "topic";

	/** User event data key for a value. */
	String VALUE_DATA_KEY = "value";

	/** User event data key for an update list. */
	String UPDATE_LIST_DATA_KEY = "updates";

	/** User event tags for authorization events. */
	String[] AUTHORIZATION_TAGS = new String[] { DNP3_TAG, AUTHORIZATION_TAG };

	/** User event tags for session events. */
	String[] SESSION_TAGS = new String[] { DNP3_TAG, SESSION_TAG };

	/** User event tags for datum events. */
	String[] DATUM_TAGS = new String[] { DNP3_TAG, DATUM_TAG };

	/** User event tags for instruction events. */
	String[] INSTRUCTION_TAGS = new String[] { DNP3_TAG, INSTRUCTION_TAG };

	/**
	 * Get a user log event for a configuration.
	 * 
	 * @param entity
	 *        the entity
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param extraTags
	 *        optional extra tags
	 * @return the log event
	 */
	static LogEventInfo eventWithEntity(Entity<? extends CompositeKey> entity, String[] baseTags,
			String message, String... extraTags) {
		return eventWithEntity(entity, baseTags, message, null, extraTags);
	}

	/**
	 * Get a user log event for a configuration.
	 * 
	 * @param entity
	 *        the entity
	 * @param baseTags
	 *        the base tags
	 * @param message
	 *        the message
	 * @param extraData
	 *        optional extra data elements
	 * @param extraTags
	 *        optional extra tags
	 * @return the log event
	 */
	static LogEventInfo eventWithEntity(Entity<? extends CompositeKey> entity, String[] baseTags,
			String message, Map<String, ?> extraData, String... extraTags) {
		Map<String, Object> data = eventDataForEntity(entity, extraData);
		return event(baseTags, message, getJSONString(data, null), extraTags);
	}

	/**
	 * Get an event data map from entity properties.
	 * 
	 * @param entity
	 *        the entity
	 * @return the data map
	 */
	static Map<String, Object> eventDataForEntity(Entity<? extends CompositeKey> entity) {
		return eventDataForEntity(entity, null);
	}

	/**
	 * Get an event data map from entity properties.
	 * 
	 * @param entity
	 *        the entity
	 * @param extraData
	 *        optional extra data to include, or {@literal null}
	 * @return the data map
	 */
	static Map<String, Object> eventDataForEntity(Entity<? extends CompositeKey> entity,
			Map<String, ?> extraData) {
		final CompositeKey id = entity.getId();
		Map<String, Object> data = new LinkedHashMap<>(
				id.keyComponentLength() + 2 + (extraData != null ? extraData.size() : 0));
		if ( extraData != null ) {
			data.putAll(extraData);
		}
		final boolean isCert = (entity instanceof TrustedIssuerCertificate c);
		final int keyLength = id.keyComponentLength();
		for ( int i = 1; i < keyLength; i++ ) { // skip first key: presume user ID
			Object v = id.keyComponent(i);
			String k = null;
			switch (i) {
				case 1:
					if ( isCert ) {
						k = IDENTIFIER_DATA_KEY;
					} else {
						k = SERVER_ID_DATA_KEY;
					}
					break;
				case 2:
					if ( v instanceof Integer ) {
						k = INDEX_DATA_KEY;
					} else {
						k = IDENTIFIER_DATA_KEY;
					}
					break;
				default:
					// ignore
			}
			if ( k != null ) {
				data.put(k, v);
			}
		}
		if ( entity instanceof ServerControlConfiguration c ) {
			data.put(NODE_ID_DATA_KEY, c.getNodeId());
			data.put(CONTROL_ID_DATA_KEY, c.getControlId());
			data.put(TYPE_DATA_KEY, c.getControlType());
		} else if ( entity instanceof ServerMeasurementConfiguration c ) {
			data.put(NODE_ID_DATA_KEY, c.getNodeId());
			data.put(SOURCE_ID_DATA_KEY, c.getSourceId());
			data.put(TYPE_DATA_KEY, c.getMeasurementType());
		}
		return data;
	}
}
