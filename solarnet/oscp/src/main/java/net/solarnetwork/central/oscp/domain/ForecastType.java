/* ==================================================================
 * ForecastType.java - 24/08/2022 11:32:10 am
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
import oscp.v20.AdjustGroupCapacityForecast.CapacityForecastType;

/**
 * An enumeration of forecast types.
 * 
 * @author matt
 * @version 1.0
 */
public enum ForecastType implements CodedValue {

	/** The maximum capacity that can be imported by the flexible source. */
	Consumption('c'),

	/** The maximum capacity that can be exported by the flexible source. */
	Generation('g'),

	/**
	 * The maximum capacity that can be imported by the flexible source when
	 * Offline.
	 */
	FallbackConsumption('C'),

	/**
	 * The maximum capacity that can be exported by the flexible source when
	 * Offline.
	 */
	FallbackGeneration('G'),

	/**
	 * The optimum capacity that should either be imported or exported by the
	 * flexible source.
	 */
	Optimum('o'),

	;

	private final char code;

	private ForecastType(char code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get the type alias.
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		return String.valueOf(code);
	}

	/**
	 * Get an OSCP 2.0 capacity value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public CapacityForecastType toOscp20CapacityValue() {
		return switch (this) {
			case Consumption -> CapacityForecastType.CONSUMPTION;
			case Generation -> CapacityForecastType.GENERATION;
			case FallbackConsumption -> CapacityForecastType.FALLBACK_CONSUMPTION;
			case FallbackGeneration -> CapacityForecastType.FALLBACK_GENERATION;
			case Optimum -> CapacityForecastType.OPTIMUM;
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 capacity value.
	 * 
	 * @param type
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static ForecastType forOscp20Value(CapacityForecastType type) {
		return switch (type) {
			case CONSUMPTION -> Consumption;
			case GENERATION -> Generation;
			case FALLBACK_CONSUMPTION -> FallbackConsumption;
			case FALLBACK_GENERATION -> FallbackGeneration;
			case OPTIMUM -> Optimum;
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
	public static ForecastType forCode(int code) {
		for ( ForecastType e : ForecastType.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid ForecastType code [%s]", code));
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
	public static ForecastType fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( ForecastType e : ForecastType.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid ForecastType value [%s]", value));
	}

}
