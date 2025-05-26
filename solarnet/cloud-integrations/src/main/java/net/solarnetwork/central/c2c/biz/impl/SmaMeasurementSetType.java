/* ==================================================================
 * SmaMeasurementSetType.java - 30/03/2025 2:36:47 pm
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

import static net.solarnetwork.central.c2c.biz.impl.SmaMeasurementType.indexedNumberType;
import static net.solarnetwork.central.c2c.biz.impl.SmaMeasurementType.numberType;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * SMA measurement set type enumeration.
 *
 * @author matt
 * @version 1.0
 */
public enum SmaMeasurementSetType {

	/** Energy and power battery. */
	EnergyAndPowerBattery(
			"Battery-charging and discharging(energy and power), state of charge (power) and state of health (energy) values."),

	/** Energy and power consumption. */
	EnergyAndPowerConsumption("Consumption(energy and power) values."),

	/** Energy and power in/out. */
	EnergyAndPowerInOut("Feed-In and external consumption(energy and power) values."),

	/** Energy and power PV. */
	EnergyAndPowerPv("PV-generation(energy and power) values."),

	/** Energy mix. */
	EnergyMix("Energy mix values (PV, battery and grid consumption) consumed by a consumer."),

	/** Power AC. */
	PowerAc(
			"Current, active, reactive and apparent power values as well as grid voltage values for phases A, B, C and in total on device level."),

	/** Power DC. */
	PowerDc(
			"Direct power input, voltage and current values as well as insulation resistance on device level."),

	/** Energy DC. */
	EnergyDc("Direct energy input."),

	/** Sensor. */
	Sensor("External sensor insolation, ambient temperature, module temperature and wind speed values."),

	;

	private final String description;
	private final SequencedMap<String, SmaMeasurementType<?>> measurements;

	private SmaMeasurementSetType(String description) {
		this.description = description;
		this.measurements = createMeasurements(this.name());
	}

	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	private static SequencedMap<String, SmaMeasurementType<?>> createMeasurements(
			String measurementSetType) {
		SequencedMap<String, SmaMeasurementType<?>> types = new LinkedHashMap<>(8);

		switch (measurementSetType) {
			case "EnergyAndPowerBattery":
				// EnergyAndPowerBattery
				types.put("batteryCharging",
						numberType("batteryCharging", "Energy that the system stored in the battery."));
				types.put("batteryDischarging", numberType("batteryDischarging",
						"Power/energy that the consumers in the system consumed from the battery."));
				types.put("batteryStateOfCharge", numberType("batteryStateOfCharge",
						"State of charge of the battery in percent, in relation to the battery capacity."));
				types.put("batteryStateOfHealth", numberType("batteryStateOfHealth",
						"State of health of the battery in percent, in relation to the battery state."));
				types.put("batteryStateOfChargeArray", indexedNumberType("batteryStateOfChargeArray",
						"State of charge of the battery in percent in relation to the battery capacity as array."));
				types.put("batteryStateOfHealthArray", indexedNumberType("batteryStateOfHealthArray",
						"State of health of the battery in percent in relation to the battery state as array."));
				types.put("batteryVoltage", indexedNumberType("batteryVoltage",
						"Battery voltage (DC) of each battery stack in V as array."));
				types.put("batteryCurrent", indexedNumberType("batteryCurrent",
						"Battery current (DC) of each battery stack in A as array."));
				types.put("batteryTemperature", indexedNumberType("batteryTemperature",
						"Battery temperature of each battery stack in °C as array."));
				types.put("currentBatteryChargingSetVoltage", indexedNumberType(
						"currentBatteryChargingSetVoltage",
						"Current battery setpoint charging voltage (DC) of each battery stack in V as array."));
				break;

			case "EnergyAndPowerConsumption":
				types.put("consumption", numberType("consumption", "The consumption."));
				break;

			case "EnergyAndPowerInOut":
				types.put("gridFeedIn", numberType("gridFeedIn", "Energy fed into the grid."));
				types.put("gridConsumption",
						numberType("gridConsumption", "Energy consumed from the grid."));
				break;

			case "EnergyAndPowerPv":
				types.put("pvGeneration",
						numberType("pvGeneration", "Energy generated by PV inverters in the system."));
				break;

			case "EnergyDc":
				types.put("dcEnergyInput", indexedNumberType("dcEnergyInput",
						"Energy generated by PV inverters in the system."));
				break;

			case "EnergyMix":
				types.put("pvConsumptionRate", numberType("pvConsumptionRate",
						"Rate of power consumed by the given device from pv inverters in the device's plant which is returned as a value between 0 and 1."));
				types.put("batteryConsumptionRate", numberType("batteryConsumptionRate",
						"Rate of power consumed by the given device from the batteries in the device's plant which is returned as a value between 0 and 1."));
				types.put("gridConsumptionRate", numberType("gridConsumptionRate",
						"Rate of power consumed by the given device from the grid which is returned as a value between 0 and 1."));
				types.put("totalConsumption", numberType("totalConsumption",
						"Total amount of power/energy consumed by the given device."));
				break;

			case "PowerAc":
				types.put("voltagePhaseA2B",
						numberType("voltagePhaseA2B", "Grid voltage phase A against B in V."));
				types.put("voltagePhaseB2C",
						numberType("voltagePhaseB2C", "Grid voltage phase B against C in V."));
				types.put("voltagePhaseC2A",
						numberType("voltagePhaseC2A", "Grid voltage phase C against A in V."));
				types.put("voltagePhaseA", numberType("voltagePhaseA", "Grid voltage phase A in V."));
				types.put("voltagePhaseB", numberType("voltagePhaseB", "Grid voltage phase B in V."));
				types.put("voltagePhaseC", numberType("voltagePhaseC", "Grid voltage phase C in V."));
				types.put("currentPhaseA", numberType("currentPhaseA", "Grid current phase A in A."));
				types.put("currentPhaseB", numberType("currentPhaseB", "Grid current phase B in A."));
				types.put("currentPhaseC", numberType("currentPhaseC", "Grid current phase C in A."));
				types.put("activePowerPhaseA", numberType("activePowerPhaseA", "Active power A in W."));
				types.put("activePowerPhaseB", numberType("activePowerPhaseB", "Active power B in W."));
				types.put("activePowerPhaseC", numberType("activePowerPhaseC", "PV power in W."));
				types.put("activePower", numberType("activePower", "The consumption."));
				types.put("reactivePowerPhaseA",
						numberType("reactivePowerPhaseA", "Reactive power A in VAr."));
				types.put("reactivePowerPhaseB",
						numberType("reactivePowerPhaseB", "Reactive power B in VAr."));
				types.put("reactivePowerPhaseC",
						numberType("reactivePowerPhaseC", "Reactive power C in VAr."));
				types.put("reactivePower", numberType("reactivePower", "Reactive power in VAr."));
				types.put("apparentPowerPhaseA",
						numberType("apparentPowerPhaseA", "Apparent power A in VA."));
				types.put("apparentPowerPhaseB",
						numberType("apparentPowerPhaseB", "Apparent power B in VA."));
				types.put("apparentPowerPhaseC",
						numberType("apparentPowerPhaseC", "Apparent power C in VA."));
				types.put("apparentPower", numberType("apparentPower", "Apparent power in VA."));
				types.put("gridFrequency", numberType("gridFrequency", "Grid frequency in Hz."));
				types.put("displacementPowerFactor", numberType("displacementPowerFactor",
						"The displacement power factor is the ratio of true power to apparent power due to the phase displacement between the current and voltage and is calculated as cos(Phi) where Phi is the difference between the phase of the voltage and the phase of the current (phase displacement) in degrees. The range of values ​​is therefore between -1 and 1."));
				break;

			case "PowerDc":
				types.put("dcPowerInput",
						indexedNumberType("dcPowerInput", "DC power input values in W."));
				types.put("dcVoltageInput",
						indexedNumberType("dcVoltageInput", "DC voltage input values in V."));
				types.put("dcCurrentInput",
						indexedNumberType("dcCurrentInput", "DC current input values in A."));
				types.put("isolationResistance",
						numberType("isolationResistance", "Isolation resistance in Ohm."));
				break;

			case "Sensor":
				types.put("externalInsolation",
						numberType("externalInsolation", "External insolation for the sensor in W/m²."));
				types.put("ambientTemperature",
						numberType("ambientTemperature", "Ambient temperature for the sensor in °C."));
				types.put("moduleTemperature",
						numberType("moduleTemperature", "Module temperature for the sensor in °C."));
				types.put("windSpeed", numberType("windSpeed", "Wind speed for the sensor in m/s."));
				break;
		}
		return types;
	}

	/**
	 * Test if this set should include the {@code ReturnEnergyValues} query
	 * parameter.
	 *
	 * @return {@code true} if queries for this measurement set's data should
	 *         include the {@code ReturnEnergyValues} query parameter
	 */
	public boolean shouldReturnEnergyValues() {
		return name().startsWith("EnergyAndPower");
	}

	/**
	 * Get an enum instance for a name value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum; if {@code value} is {@literal null} or empty then
	 *         {@code null} is returned
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static SmaMeasurementSetType fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		for ( SmaMeasurementSetType e : SmaMeasurementSetType.values() ) {
			if ( value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown SmaMeasurementSetType value [" + value + "]");
	}

	/**
	 * Get the key.
	 *
	 * @return the key
	 */
	public String getKey() {
		return name();
	}

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Get the measurements.
	 *
	 * @return the measurements
	 */
	public SequencedMap<String, SmaMeasurementType<?>> getMeasurements() {
		return measurements;
	}

}
