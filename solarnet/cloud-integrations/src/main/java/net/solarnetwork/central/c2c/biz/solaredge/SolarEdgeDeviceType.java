/* ==================================================================
 * SolarEdgeDeviceType.java - 12 Aug 2026 10:25:36 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.solaredge;

import java.util.Arrays;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A SolarEdge device type.
 *
 * @author matt
 * @version 1.0
 */
public enum SolarEdgeDeviceType {

	Inverter("inv", "Inverters", "INVERTER", "inverters"),

	/** A meter. */
	Meter("met", "Meters", "METER", "meters"),

	/** A battery. */
	Battery("bat", "Batteries", "BATTERY", "storage"),

	;

	/** A comma-delimited list of all API type values. */
	public static final String ALL_API_TYPES = Arrays.stream(SolarEdgeDeviceType.values())
			.map(SolarEdgeDeviceType::getApiType).collect(Collectors.joining(","));

	private final String key;
	private final String groupKey;
	private final String apiType;
	private final String telemetryType;

	SolarEdgeDeviceType(String key, String groupKey, String apiType, String telemetryType) {
		this.key = key;
		this.groupKey = groupKey;
		this.apiType = apiType;
		this.telemetryType = telemetryType;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@code null}
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get the group key.
	 *
	 * @return the group key
	 */
	public final String getGroupKey() {
		return groupKey;
	}

	/**
	 * Get the API type value.
	 *
	 * @return the API type
	 */
	public final String getApiType() {
		return apiType;
	}

	/**
	 * Get the telemetry type.
	 *
	 * @return the telemetry type
	 */
	public final String getTelemetryType() {
		return telemetryType;
	}

	/**
	 * Get an enum instance for a name or key or API value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static SolarEdgeDeviceType fromValue(String value) {
		if ( value != null ) {
			for ( SolarEdgeDeviceType e : SolarEdgeDeviceType.values() ) {
				if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name())
						|| value.equalsIgnoreCase(e.apiType) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Unknown SolarEdgeDeviceType value [" + value + "]");
	}

}
