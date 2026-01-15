/* ==================================================================
 * EgaugeTypeCode.java - 26/10/2024 7:48:53 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils.MILLIS;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enumeration of eGauge register types.
 *
 * @author matt
 * @version 1.0
 */
public enum EgaugeTypeCode {

	/** Whole number. */
	WholeNumber("#", null, BigDecimal.ONE),

	/** Number with 3 decimal places. */
	DecimalThree("#3", null, MILLIS),

	/** Percentage. */
	Percentage("%", "%", MILLIS),

	/** Monetary accrual rate. */
	Monetary("$", "$/s", new BigDecimal(BigInteger.TWO, 29)),

	/** Angle. */
	Angle("a", "\u00B0", MILLIS),

	/** Air quality index (0=good, 500=bad). */
	AirQualityIndex("aq", "s", MILLIS),

	/** Discrete number. */
	DiscreteNumber("d", null, BigDecimal.ONE),

	/** Irradiance. */
	Irradiance("Ee", "W/m2", BigDecimal.ONE),

	/** Frequency. */
	Frequency("F", "Hz", MILLIS),

	/** Relative humidity. */
	RelativeHumidity("h", "%", MILLIS),

	/** Electrical current. */
	ElectricCurrent("I", "A", MILLIS),

	/** Mass. */
	Mass("m", "g", MILLIS),

	/** Power. */
	Power("P", "W", BigDecimal.ONE),

	/** Pressure. */
	Pressure("Pa", "Pa", BigDecimal.ONE),

	/** Parts per million. */
	PartsPerMillion("ppm", "ppm", MILLIS),

	/** Reactive power. */
	ReactivePower("var", "var", BigDecimal.ONE),

	/** Mass flow. */
	MassFlow("Q", "g/s", BigDecimal.ONE),

	/** Electric charge. */
	ElectricCharge("Qe", "Ah", MILLIS),

	/** Volumetric flow. */
	VolumetricFlow("Qv", "m3/s", new BigDecimal(BigInteger.TEN, 9)),

	/** Electric resistance. */
	ElectricResistance("R", "\u2126", BigDecimal.ONE),

	/** Apparent power. */
	ApparentPower("S", "VA", BigDecimal.ONE),

	/** Temperature. */
	Temperature("T", "\u2103", MILLIS),

	/** Total harmonic distortion. */
	TotalHarmonicDistortion("THD", "%", MILLIS),

	/** Voltage. */
	Voltage("V", "V", MILLIS),

	/** Speed. */
	Speed("v", "m/s", MILLIS),;

	private final String key;
	private final String unit;
	private final BigDecimal quantum;

	EgaugeTypeCode(String key, String unit, BigDecimal quantum) {
		this.key = key;
		this.unit = unit;
		this.quantum = quantum;
	}

	/**
	 * Get the key value.
	 *
	 * @return the key
	 */
	public final String getKey() {
		return key;
	}

	/**
	 * Get the unit of measure.
	 *
	 * @return the unit
	 */
	public final String getUnit() {
		return unit;
	}

	/**
	 * Get the quantum.
	 *
	 * @return the quantum
	 */
	public final BigDecimal getQuantum() {
		return quantum;
	}

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name (case insensitive) or key value
	 *        (case-sensitve)
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static EgaugeTypeCode fromValue(String value) {
		if ( value != null ) {
			for ( EgaugeTypeCode e : EgaugeTypeCode.values() ) {
				if ( value.equals(e.key) || value.equalsIgnoreCase(e.name()) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException("Unknown EgaugeTypeCode value [" + value + "]");
	}

}
