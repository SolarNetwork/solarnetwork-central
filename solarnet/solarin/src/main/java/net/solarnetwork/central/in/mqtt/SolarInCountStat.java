/* ==================================================================
 * SolarInCountStat.java - 3/11/2019 12:26:12 pm
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

package net.solarnetwork.central.in.mqtt;

/**
 * SolarIn MQTT statistic types.
 * 
 * @author matt
 * @version 2.1
 * @since 1.1
 */
public enum SolarInCountStat {

	NodeDatumReceived("node datum received"),

	LocationDatumReceived("location datum received"),

	InstructionStatusReceived("instruction status received"),

	LegacyNodeDatumReceived("Legacy node datum received"),

	LegacyLocationDatumReceived("Legacy location datum received"),

	StreamDatumReceived("stream datum received"),

	;

	private final String description;

	private SolarInCountStat(String description) {
		this.description = description;
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
