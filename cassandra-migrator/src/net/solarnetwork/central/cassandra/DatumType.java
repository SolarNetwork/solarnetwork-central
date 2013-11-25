/* ==================================================================
 * DatumType.java - Nov 25, 2013 2:21:55 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cassandra;

/**
 * Cassandra datum type constants.
 * 
 * @author matt
 * @version 1.0
 */
public enum DatumType {

	ConsumptionDatum(1),

	PowerDatum(2),

	PriceDatum(3),

	WeatherDatum(4),

	HardwareControlDatum(5);

	private final int code;

	private DatumType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
