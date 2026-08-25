/* ==================================================================
 * SolarEdgeMeterTelemtry.java - 16 Aug 2026 7:17:51 am
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

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Inverter telemetry data value types.
 *
 * @author matt
 * @version 1.0
 */
public enum SolarEdgeInverterTelemtry implements SolarEdgeTelemetryType {

	/** Power delivered. */
	Pdel("power", "AC active power exported"),

	/** Energy delivered. */
	Edel("energy", "AC active energy exported"),

	/** Power received. */
	Prec("consumedAcPower", "AC active power imported"),

	/** Energy received. */
	Erec("consumedAcEnergy", "AC active energy imported"),

	/** Generated power. */
	V("voltage", "AC average L-N voltage", true),

	/** Generated energy. */
	I("current", "AC current"),

	/** Consumed power. */
	Hz("frequency", "Frequency average", true),

	;

	private final String key;
	private final String description;
	private final boolean endingWindowTimestamps;

	private SolarEdgeInverterTelemtry(String key, String description) {
		this(key, description, false);
	}

	private SolarEdgeInverterTelemtry(String key, String description, boolean endingWindowTimestamps) {
		this.key = key;
		this.description = description;
		this.endingWindowTimestamps = endingWindowTimestamps;
	}

	/**
	 * Get they key.
	 *
	 * @return the key
	 */
	@Override
	public final String key() {
		return key;
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	@Override
	public final String description() {
		return description;
	}

	@Override
	public boolean endingWindowTimestamps() {
		return endingWindowTimestamps;
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
	public static SolarEdgeInverterTelemtry fromValue(String value) {
		if ( value != null ) {
			for ( SolarEdgeInverterTelemtry e : SolarEdgeInverterTelemtry.values() ) {
				if ( value.equalsIgnoreCase(e.name()) || value.equalsIgnoreCase(e.key()) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Unknown SolarEdgeInverterTelemtry value [" + value + "]");
	}

}
