/* ==================================================================
 * EnphaseDeviceType.java - 5/03/2025 6:23:09 am
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

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * An Enphase device type.
 *
 * @author matt
 * @version 1.0
 */
public enum EnphaseDeviceType {

	/** An inverter. */
	Inverter("inv", "Inverters"),

	/** A production meter. */
	Meter("met", "Meters"),

	;

	private final String key;
	private final String groupKey;

	private EnphaseDeviceType(String key, String groupKey) {
		this.key = key;
		this.groupKey = groupKey;
	}

	/**
	 * Get the key.
	 *
	 * @return the key, never {@literal null}
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
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static EnphaseDeviceType fromValue(String value) {
		if ( value != null ) {
			for ( EnphaseDeviceType e : EnphaseDeviceType.values() ) {
				if ( value.equalsIgnoreCase(e.key) || value.equalsIgnoreCase(e.name()) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Unknown EnphaseDeviceType value [" + value + "]");
	}

}
