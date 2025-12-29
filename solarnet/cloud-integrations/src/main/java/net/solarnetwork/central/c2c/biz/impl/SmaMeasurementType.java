/* ==================================================================
 * SmaMeasurementType.java - 30/03/2025 2:46:47 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.function.Function;
import net.solarnetwork.util.NumberUtils;
import tools.jackson.databind.JsonNode;

/**
 * A measurement set type.
 *
 * @param <T>
 *        the value type
 * @author matt
 * @version 2.0
 */
public record SmaMeasurementType<T>(String name, String description, Function<JsonNode, T> parser) {

	/**
	 * Create a new string measurement type.
	 *
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the measurement type
	 */
	public static SmaMeasurementType<String> stringType(String name, String description) {
		return new SmaMeasurementType<>(name, description, JsonNode::stringValue);
	}

	/**
	 * Create a new local date time measurement type.
	 *
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the measurement type
	 */
	public static SmaMeasurementType<LocalDateTime> localDateTimeType(String name, String description) {
		return new SmaMeasurementType<>(name, description,
				json -> LocalDateTime.parse(json.stringValue()));
	}

	/**
	 * Create a new number measurement type.
	 *
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the measurement type
	 */
	public static SmaMeasurementType<Number> numberType(String name, String description) {
		return new SmaMeasurementType<>(name, description,
				json -> NumberUtils.narrow(json.numberValue(), 2));
	}

	/**
	 * Create a new indexed number measurement type.
	 *
	 * @param name
	 *        the name
	 * @param description
	 *        the description
	 * @return the measurement type
	 */
	public static SmaMeasurementType<SequencedMap<String, Number>> indexedNumberType(String name,
			String description) {
		return new SmaMeasurementType<>(name, description, json -> {
			var result = new LinkedHashMap<String, Number>(8);
			for ( JsonNode indexNode : json ) {
				var key = indexNode.path("index").stringValue();
				var val = indexNode.path("value").numberValue();
				if ( key != null && val != null ) {
					result.put(key, NumberUtils.narrow(val, 2));
				}
			}
			return result;
		});
	}

}
