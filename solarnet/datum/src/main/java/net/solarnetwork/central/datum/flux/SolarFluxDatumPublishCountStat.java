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

/**
 * MQTT status for datum publishing.
 *
 * @author matt
 * @version 1.1
 */
public enum SolarFluxDatumPublishCountStat {

	RawDatumPublished("datum published"),

	HourlyDatumPublished("hourly datum published"),

	DailyDatumPublished("daily datum published"),

	MonthlyDatumPublished("monthly datum published");

	private final String description;

	private SolarFluxDatumPublishCountStat(String description) {
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
