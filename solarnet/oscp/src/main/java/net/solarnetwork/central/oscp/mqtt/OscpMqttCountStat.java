/* ==================================================================
 * OscpMqttCountStat.java - 3/11/2019 12:26:12 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.mqtt;

import net.solarnetwork.common.mqtt.MqttStats.MqttStat;

/**
 * OSCP MQTT statistic types.
 * 
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public enum OscpMqttCountStat implements MqttStat {

	/** Total instructions queued. */
	InstructionsQueued(0, "instructions queued"),

	/** Total instructions received. */
	InstructionsReceived(1, "instructions received"),

	/** Total instruction handling errors. */
	InstructionErrors(2, "instruction errors"),

	/** AdjustGroupCapacityForecast instructions received. */
	AdjustGroupCapacityForecastInstructionsReceived(
			3,
			"AdjustGroupCapacityForecast instructions received"),

	/** AdjustGroupCapacityForecast instruction errors. */
	AdjustGroupCapacityForecastInstructionErrors(4, "AdjustGroupCapacityForecast instruction errors"),

	/** GroupCapacityComplianceError instructions received. */
	GroupCapacityComplianceErrorInstructionsReceived(
			5,
			"GroupCapacityComplianceError instructions received"),

	/** GroupCapacityComplianceError instruction errors. */
	GroupCapacityComplianceErrorInstructionErrors(6, "GroupCapacityComplianceError instruction errors"),

	/** Handshake instructions received. */
	HandshakeInstructionsReceived(7, "Handshake instructions received"),

	/** Handshake instruction errors. */
	HandshakeInstructionErrors(8, "Handshake instruction errors"),

	/** UpdateAssetMeasurement instructions received. */
	UpdateAssetMeasurementInstructionsReceived(9, "UpdateAssetMeasurement instructions received"),

	/** UpdateAssetMeasurement instruction errors. */
	UpdateAssetMeasurementInstructionErrors(10, "UpdateAssetMeasurement instruction errors"),

	/** UpdateGroupMeasurements instructions received. */
	UpdateGroupMeasurementsInstructionsReceived(11, "UpdateGroupMeasurements instructions received"),

	/** UpdateGroupMeasurements instruction errors. */
	UpdateGroupMeasurementsInstructionErrors(12, "UpdateGroupMeasurements instruction errors"),

	;

	private final int index;
	private final String description;

	private OscpMqttCountStat(int index, String description) {
		this.index = index;
		this.description = description;
	}

	/**
	 * Get a statistic instance for an action.
	 * 
	 * @param action
	 *        the action
	 * @return the statistic, or {@literal null} if {@code action} is
	 *         {@literal null} or unsupported
	 */
	public static OscpMqttCountStat instructionReceivedStat(String action) {
		if ( action == null ) {
			return null;
		}
		return switch (action) {
			case "AdjustGroupCapacityForecast" -> AdjustGroupCapacityForecastInstructionsReceived;
			case "GroupCapacityComplianceError" -> GroupCapacityComplianceErrorInstructionsReceived;
			case "Handshake" -> HandshakeInstructionsReceived;
			case "UpdateAssetMeasurement" -> UpdateAssetMeasurementInstructionsReceived;
			case "UpdateGroupMeasurements" -> UpdateGroupMeasurementsInstructionsReceived;
			default -> null;
		};
	}

	/**
	 * Get an error statistic instance for an action.
	 * 
	 * @param action
	 *        the action
	 * @return the error statistic, or {@literal null} if {@code action} is
	 *         {@literal null} or unsupported
	 */
	public static OscpMqttCountStat instructionErrorStat(String action) {
		if ( action == null ) {
			return null;
		}
		return switch (action) {
			case "AdjustGroupCapacityForecast" -> AdjustGroupCapacityForecastInstructionErrors;
			case "GroupCapacityComplianceError" -> GroupCapacityComplianceErrorInstructionErrors;
			case "Handshake" -> HandshakeInstructionErrors;
			case "UpdateAssetMeasurement" -> UpdateAssetMeasurementInstructionErrors;
			case "UpdateGroupMeasurements" -> UpdateGroupMeasurementsInstructionErrors;
			default -> null;
		};
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getDescription() {
		return description;
	}

}
