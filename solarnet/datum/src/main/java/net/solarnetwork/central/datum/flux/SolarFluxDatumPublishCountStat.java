/* ==================================================================
 * SolarFluxDatumPublishCountStat.java - 28/02/2020 3:24:04 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.flux;

import net.solarnetwork.common.mqtt.MqttStats.MqttStat;

/**
 * MQTT status for datum publishing.
 * 
 * @author matt
 * @version 1.0
 */
public enum SolarFluxDatumPublishCountStat implements MqttStat {

	RawDatumPublished(0, "datum published"),

	HourlyDatumPublished(1, "hourly datum published"),

	DailyDatumPublished(2, "daily datum published"),

	MonthlyDatumPublished(3, "monthly datum published");

	private final int index;
	private final String description;

	private SolarFluxDatumPublishCountStat(int index, String description) {
		this.index = index;
		this.description = description;
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
