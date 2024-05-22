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

/**
 * OSCP MQTT statistic types.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public enum OscpMqttCountStat {

	/** Total instructions queued. */
	InstructionsQueued("instructions queued"),

	/** Total instructions received. */
	InstructionsReceived("instructions received"),

	/** Total instruction handling errors. */
	InstructionErrors("instruction errors"),

	/** AdjustGroupCapacityForecast instructions received. */
	AdjustGroupCapacityForecastInstructionsReceived("AdjustGroupCapacityForecast instructions received"),

	/** AdjustGroupCapacityForecast instruction errors. */
	AdjustGroupCapacityForecastInstructionErrors("AdjustGroupCapacityForecast instruction errors"),

	/** GroupCapacityComplianceError instructions received. */
	GroupCapacityComplianceErrorInstructionsReceived(
			"GroupCapacityComplianceError instructions received"),

	/** GroupCapacityComplianceError instruction errors. */
	GroupCapacityComplianceErrorInstructionErrors("GroupCapacityComplianceError instruction errors"),

	/** Handshake instructions received. */
	HandshakeInstructionsReceived("Handshake instructions received"),

	/** Handshake instruction errors. */
	HandshakeInstructionErrors("Handshake instruction errors"),

	/** UpdateAssetMeasurement instructions received. */
	UpdateAssetMeasurementInstructionsReceived("UpdateAssetMeasurement instructions received"),

	/** UpdateAssetMeasurement instruction errors. */
	UpdateAssetMeasurementInstructionErrors("UpdateAssetMeasurement instruction errors"),

	/** UpdateGroupMeasurements instructions received. */
	UpdateGroupMeasurementsInstructionsReceived("UpdateGroupMeasurements instructions received"),

	/** UpdateGroupMeasurements instruction errors. */
	UpdateGroupMeasurementsInstructionErrors("UpdateGroupMeasurements instruction errors"),

	;

	private final String description;

	private OscpMqttCountStat(String description) {
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

	/**
	 * Get the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
