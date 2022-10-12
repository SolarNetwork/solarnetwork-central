/* ==================================================================
 * MeasurementStyle.java - 19/08/2022 2:23:21 pm
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

import static java.lang.String.format;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;
import oscp.v20.MeasurementConfiguration;

/**
 * An enumeration of measurement styles.
 * 
 * @author matt
 * @version 1.0
 */
public enum MeasurementStyle implements CodedValue {

	/** The accumulated measurements are never reset. */
	Continuous('c'),

	/**
	 * The accumulation of the measurements are reset at certain points in time
	 * (i.e. when a charging session starts).
	 */
	Intermittent('i'),

	;

	private final char code;

	private MeasurementStyle(char code) {
		this.code = code;
	}

	/**
	 * Get the number of seconds represented by this period.
	 * 
	 * @return the number of seconds
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an OSCP 2.0 value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public MeasurementConfiguration toOscp20Value() {
		return switch (this) {
			case Continuous -> MeasurementConfiguration.CONTINUOUS;
			case Intermittent -> MeasurementConfiguration.INTERMITTENT;
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 value.
	 * 
	 * @param configuration
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static MeasurementStyle forOscp20Value(MeasurementConfiguration configuration) {
		return switch (configuration) {
			case CONTINUOUS -> Continuous;
			case INTERMITTENT -> Intermittent;
		};
	}

	/**
	 * Create an enum instance from a code value.
	 * 
	 * @param code
	 *        the code value
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	public static MeasurementStyle forCode(int code) {
		for ( MeasurementStyle e : MeasurementStyle.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid MeasurementStyle code [%s]", code));
	}

	/**
	 * Create an enum instance from a string value.
	 * 
	 * @param value
	 *        the string representation; both enum names and code values are
	 *        supported
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	@JsonCreator
	public static MeasurementStyle fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( MeasurementStyle e : MeasurementStyle.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid MeasurementStyle value [%s]", value));
	}

}
