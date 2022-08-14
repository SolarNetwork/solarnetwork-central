/* ==================================================================
 * MeasurementUnit.java - 11/08/2022 4:09:57 pm
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
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;
import oscp.v20.EnergyMeasurement.EnergyMeasurementUnit;
import oscp.v20.InstantaneousMeasurement.InstantaneousMeasurementUnit;

/**
 * Enumeration of registration status values.
 * 
 * @author matt
 * @version 1.0
 */
public enum MeasurementUnit implements CodedValue {

	/** Ampere per phase (current). */
	A('a'),

	/** Watt (power). */
	W('p'),

	/** Kilowatt (power). */
	kW('P'),

	/**
	 * Watt-hours (energy). Represents the State Of Charge in case of measuring
	 * a battery.
	 */
	Wh('e'),

	/**
	 * Kilowatt-hours (energy). Represents the State Of Charge in case of
	 * measuring a battery.
	 */
	kWh('E'),

	;

	/** A multiplier to convert a unit measurement into kilo-units. */
	public static final BigDecimal KILO_MULTIPLIER = new BigDecimal("0.001");

	private final char code;

	private MeasurementUnit(char c) {
		this.code = c;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an OSCP 2.0 instantaneous unit value for this instance.
	 * 
	 * @return the OSCP 2.0 instantaneous value
	 */
	public InstantaneousMeasurementUnit toOscp20InstantaneousValue() {
		return switch (this) {
			case A -> InstantaneousMeasurementUnit.A;
			case W -> InstantaneousMeasurementUnit.W;
			case kW -> InstantaneousMeasurementUnit.KW;
			case Wh -> InstantaneousMeasurementUnit.WH;
			case kWh -> InstantaneousMeasurementUnit.KWH;
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 instantaneous value.
	 * 
	 * @param unit
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static MeasurementUnit forOscp20Value(InstantaneousMeasurementUnit unit) {
		return switch (unit) {
			case A -> A;
			case W -> W;
			case KW -> kW;
			case WH -> Wh;
			case KWH -> kWh;
		};
	}

	/**
	 * Get an OSCP 2.0 energy unit value for this instance.
	 * 
	 * @return the OSCP 2.0 energy value
	 */
	public EnergyMeasurementUnit toOscp20EnergyValue() {
		return switch (this) {
			case Wh -> EnergyMeasurementUnit.WH;
			case kWh -> EnergyMeasurementUnit.KWH;
			default -> throw new IllegalArgumentException(format(
					"The [%s] MeasurementUnit cannot be represented as a EnergyMeasurementUnit", this));
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 energy value.
	 * 
	 * @param unit
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static MeasurementUnit forOscp20Value(EnergyMeasurementUnit unit) {
		return switch (unit) {
			case WH -> Wh;
			case KWH -> kWh;
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
	public static MeasurementUnit forCode(int code) {
		for ( MeasurementUnit e : MeasurementUnit.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid MeasurementUnit code [%s]", code));
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
	public static MeasurementUnit fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( MeasurementUnit e : MeasurementUnit.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid MeasurementUnit value [%s]", value));
	}

}
