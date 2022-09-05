/* ==================================================================
 * Measurement.java - 5/09/2022 3:05:50 pm
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

import java.math.BigDecimal;
import java.time.Instant;
import oscp.v20.EnergyMeasurement;
import oscp.v20.InstantaneousMeasurement;

/**
 * A measurement record.
 * 
 * @param value
 *        the measured value
 * @param phase
 *        the measured AC phase
 * @param unit
 *        the measurement unit
 * @param measureTime
 *        the timestamp of the measurement
 * @param energyType
 *        for energy measurements the energy type, otherwise {@literal null}
 * @param energyDirection
 *        for energy measurements the energy direction, otherwise
 *        {@literal null}
 * @param startMeasureTime
 *        for energy measurements the start of the measurement period, otherwise
 *        {@literal null}
 * @author matt
 * @version 1.0
 */
public record Measurement(BigDecimal value, Phase phase, MeasurementUnit unit, Instant measureTime,
		EnergyType energyType, EnergyDirection energyDirection, Instant startMeasureTime) {

	/**
	 * Get an instantaneous measurement instance.
	 * 
	 * @param value
	 *        the measured value
	 * @param phase
	 *        the measured AC phase
	 * @param unit
	 *        the measurement unit
	 * @param measureTime
	 *        the timestamp of the measurement
	 * @return the new instance
	 */
	public static Measurement instantaneousMeasurement(BigDecimal value, Phase phase,
			MeasurementUnit unit, Instant measureTime) {
		return new Measurement(value, phase, unit, measureTime, null, null, null);
	}

	/**
	 * Get an energy measurement instance.
	 * 
	 * @param value
	 *        the measured value
	 * @param phase
	 *        the measured AC phase
	 * @param unit
	 *        the measurement unit
	 * @param measureTime
	 *        the timestamp of the measurement
	 * @param energyType
	 *        the energy type
	 * @param energyDirection
	 *        the energy direction
	 * @param startMeasureTime
	 *        the start of the measurement period
	 * @return
	 */
	public static Measurement energyMeasurement(BigDecimal value, Phase phase, MeasurementUnit unit,
			Instant measureTime, EnergyType energyType, EnergyDirection energyDirection,
			Instant startMeasureTime) {
		return new Measurement(value, phase, unit, measureTime, energyType, energyDirection,
				startMeasureTime);
	}

	/**
	 * Get an OSCP 2.0 instantaneous value for this instance.
	 * 
	 * @return the OSCP 2.0 instantaneous value
	 */
	public InstantaneousMeasurement toOscp20InstantaneousValue() {
		InstantaneousMeasurement result = new InstantaneousMeasurement(
				value != null ? value.doubleValue() : null, phase != null ? phase.toOscp20Value() : null,
				unit != null ? unit.toOscp20InstantaneousValue() : null, measureTime);
		return result;
	}

	/**
	 * Get an OSCP 2.0 energy value for this instance.
	 * 
	 * @return the OSCP 2.0 energy value
	 */
	public EnergyMeasurement toOscp20EnergyValue() {
		EnergyMeasurement result = new EnergyMeasurement(value != null ? value.doubleValue() : null,
				phase != null ? phase.toOscp20Value() : null,
				unit != null ? unit.toOscp20EnergyValue() : null,
				energyDirection != null ? energyDirection.toOscp20Value() : null, measureTime);
		result.setInitialMeasureTime(startMeasureTime);
		return result;
	}

	/**
	 * Test if this instance represents an energy measurement.
	 * 
	 * <p>
	 * This only checks for a non-null {@code energyType} property value.
	 * </p>
	 * 
	 * @return {@literal true} if {@code energyType} is not {@literal null}
	 */
	public boolean isEnergyMeasurement() {
		return energyType != null;
	}

	/**
	 * Get an instance for an OSCP 2.0 instantaneous value.
	 * 
	 * @param measurement
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static Measurement forOscp20Value(InstantaneousMeasurement measurement) {
		if ( measurement == null ) {
			return null;
		}
		return instantaneousMeasurement(
				measurement.getValue() != null ? BigDecimal.valueOf(measurement.getValue()) : null,
				Phase.forOscp20Value(measurement.getPhase()),
				MeasurementUnit.forOscp20Value(measurement.getUnit()), measurement.getMeasureTime());
	}

	/**
	 * Get an instance for an OSCP 2.0 energy value.
	 * 
	 * @param measurement
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static Measurement forOscp20Value(EnergyMeasurement measurement) {
		if ( measurement == null ) {
			return null;
		}
		return energyMeasurement(
				measurement.getValue() != null ? BigDecimal.valueOf(measurement.getValue()) : null,
				Phase.forOscp20Value(measurement.getPhase()),
				MeasurementUnit.forOscp20Value(measurement.getUnit()), measurement.getMeasureTime(),
				EnergyType.forOscp20Value(measurement.getEnergyType()),
				EnergyDirection.forOscp20Value(measurement.getDirection()),
				measurement.getInitialMeasureTime());
	}

}
